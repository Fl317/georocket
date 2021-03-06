package io.georocket.query.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import io.georocket.query.parser.QueryParser.AndContext;
import io.georocket.query.parser.QueryParser.ExprContext;
import io.georocket.query.parser.QueryParser.NotContext;
import io.georocket.query.parser.QueryParser.OrContext;
import io.georocket.query.parser.QueryParser.QueryContext;
import io.georocket.query.parser.QueryParser.StringContext;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Test {@link QueryParser}
 * @author Michel Kraemer
 */
public class QueryParserTest {
  /**
   * Convert a parse tree to a JsonObject
   */
  private static class ToJsonTreeListener extends QueryBaseListener {
    Deque<JsonObject> tree = new ArrayDeque<JsonObject>();
    
    final static String TYPE = "type";
    final static String TEXT = "text";
    final static String QUERY = "query";
    final static String STRING = "string";
    final static String OR = "or";
    final static String AND = "and";
    final static String NOT = "not";
    final static String CHILDREN = "children";
    
    ToJsonTreeListener() {
      push(QUERY);
    }
    
    private void push(String type) {
      push(type, null);
    }
    
    private void push(String type, String text) {
      JsonObject obj = new JsonObject().put(TYPE, type);
      if (text != null) {
        obj.put(TEXT, text);
      }
      if (!tree.isEmpty()) {
        JsonArray children = tree.peek().getJsonArray(CHILDREN);
        if (children == null) {
          children = new JsonArray();
          tree.peek().put(CHILDREN, children);
        }
        children.add(obj);
      }
      tree.push(obj);
    }
    
    @Override
    public void exitExpr(ExprContext ctx) {
      tree.pop();
    }
    
    @Override
    public void enterOr(OrContext ctx) {
      push(OR);
    }
    
    @Override
    public void enterAnd(AndContext ctx) {
      push(AND);
    }
    
    @Override
    public void enterNot(NotContext ctx) {
      push(NOT);
    }
    
    @Override
    public void enterString(StringContext ctx) {
      push(STRING, ctx.getText());
    }
  }
  
  /**
   * Load a fixture, parse and check the result
   * @param fixture the name of the fixture to load (without path and extension)
   */
  private void expectFixture(String fixture) {
    // load file
    URL u = this.getClass().getResource("fixtures/" + fixture + ".json");
    String fixtureStr;
    try {
      fixtureStr = IOUtils.toString(u);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    // get query and expected tree
    JsonObject fixtureObj = new JsonObject(fixtureStr);
    String query = fixtureObj.getString("query");
    JsonObject expected = fixtureObj.getJsonObject("expected");
    
    // parse query
    QueryLexer lexer = new QueryLexer(new ANTLRInputStream(query.trim()));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    QueryParser parser = new QueryParser(tokens);
    QueryContext ctx = parser.query();
    ToJsonTreeListener listener = new ToJsonTreeListener();
    ParseTreeWalker.DEFAULT.walk(listener, ctx);
    
    // assert tree
    assertEquals(1, listener.tree.size());
    JsonObject root = listener.tree.pop();
    assertEquals(expected, root);
  }
  
  /**
   * Query with a single string
   */
  @Test
  public void string() {
    expectFixture("string");
  }
  
  /**
   * Query with two strings
   */
  @Test
  public void strings() {
    expectFixture("strings");
  }
  
  /**
   * Explicit OR
   */
  @Test
  public void or() {
    expectFixture("or");
  }
  
  /**
   * Logical AND
   */
  @Test
  public void and() {
    expectFixture("and");
  }
  
  /**
   * Logical NOT
   */
  @Test
  public void not() {
    expectFixture("not");
  }
  
  /**
   * Query with a double-quoted string
   */
  @Test
  public void doubleQuotedString() {
    expectFixture("double_quoted_string");
  }
  
  /**
   * Query with a single-quoted string
   */
  @Test
  public void singleQuotedString() {
    expectFixture("single_quoted_string");
  }
  
  /**
   * Query with a quoted OR
   */
  @Test
  public void quotedOr() {
    expectFixture("quoted_or");
  }
}
