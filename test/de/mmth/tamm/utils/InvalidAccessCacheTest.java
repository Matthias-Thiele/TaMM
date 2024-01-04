/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class InvalidAccessCacheTest {
  
  /**
   * Test of addInvalidAccess method, of class InvalidAccessCache.
   */
  @Test
  public void testAddInvalidAccess() {
    System.out.println("addInvalidAccess");
    InvalidAccessCache instance = new InvalidAccessCache(2, 0);
    
    String ip = "192.168.1.1";
    
    boolean result = instance.checkAccess(ip);
    assertTrue("no invalid access yet, access should be possible", result);
    
    instance.addInvalidAccess(ip);
    result = instance.checkAccess(ip);
    assertTrue("only one invalid access yet, access should be possible", result);
    
    instance.addInvalidAccess(ip);
    result = instance.checkAccess(ip);
    assertTrue("second invalid access, access should be possible", result);
    
    instance.addInvalidAccess(ip);
    result = instance.checkAccess(ip);
    assertFalse("third invalid access, access should be locked", result);
    
    instance.cleanup();
    result = instance.checkAccess(ip);
    assertTrue("after cleanup once access should be possible", result);
    
    instance.addInvalidAccess(ip);
    result = instance.checkAccess(ip);
    assertFalse("new invalid access, access should be locked", result);
    
  }
  
}
