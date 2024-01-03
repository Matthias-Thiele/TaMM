/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class PlaceholderTest {
  
  public PlaceholderTest() {
  }

  /**
   * Test of resolve method, of class Placeholder.
   */
  @Test
  public void testResolve() {
    System.out.println("resolve");
    Map<String, String> placeholderData = new HashMap<>();
    placeholderData.put("something", "another thing");
    placeholderData.put("number", "0123456789");
    placeholderData.put("name", "Jon Doe");
    placeholderData.put("empty", "");
    
    Placeholder instance = new Placeholder();
    String source = "";
    String expResult = "";
    String result = instance.resolve(source, placeholderData);
    assertEquals(expResult, result);
    
    // normal internal replacement
    source = "text{[number]}more";
    expResult = "text0123456789more";
    result = instance.resolve(source, placeholderData);
    assertEquals("normal replacement", expResult, result);
    
    // from start replacement
    source = "{[something]}more";
    expResult = "another thingmore";
    result = instance.resolve(source, placeholderData);
    assertEquals("from start replacement", expResult, result);
    
    // to the end replacement
    source = "text{[something]}";
    expResult = "textanother thing";
    result = instance.resolve(source, placeholderData);
    assertEquals("from start replacement", expResult, result);
    
    // empty replacement
    source = "text{[empty]}more";
    expResult = "textmore";
    result = instance.resolve(source, placeholderData);
    assertEquals("empty replacement", expResult, result);
    
    // all replacement
    source = "{[something]}";
    expResult = "another thing";
    result = instance.resolve(source, placeholderData);
    assertEquals("all replacement", expResult, result);
    
    // all multi replacement
    source = "{[something]}{[name]}{[number]}{[empty]}";
    expResult = "another thingJon Doe0123456789";
    result = instance.resolve(source, placeholderData);
    assertEquals("all multi replacement", expResult, result);
    
    // only start, no replacement
    source = "{[something more";
    expResult = "{[something more";
    result = instance.resolve(source, placeholderData);
    assertEquals("only start, no replacement", expResult, result);
    
    // not a key, no replacement
    source = "text{[totally not a key]}more";
    expResult = "text{[totally not a key]}more";
    result = instance.resolve(source, placeholderData);
    assertEquals("not a key, no replacement", expResult, result);
    
    // a key within a key replacement
    source = "text{[totally not a key{[name]}more";
    expResult = "text{[totally not a keyJon Doemore";
    result = instance.resolve(source, placeholderData);
    assertEquals("a key within a key replacement", expResult, result);
    
    // double replacement
    source = "text{[name]}and{[name]}more";
    expResult = "textJon DoeandJon Doemore";
    result = instance.resolve(source, placeholderData);
    assertEquals("double replacement", expResult, result);
    
    // lots of brackets
    source = "{[text{[empty]}]}]}]}{[{[{[and{[number]}more]}";
    expResult = "{[text]}]}]}{[{[{[and0123456789more]}";
    result = instance.resolve(source, placeholderData);
    assertEquals("lots of brackets", expResult, result);
    
  }
  
}
