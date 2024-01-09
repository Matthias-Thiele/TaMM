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
public class LimitSentMailsTest {
  
  /**
   * Test of checkAccess method, of class LimitSentMails.
   */
  @Test
  public void testCheckAccess() {
    System.out.println("checkAccess");
    String domain1 = "@test1.de";
    String domain2 = "@test2.de";
    LimitSentMails instance = new LimitSentMails(5, 3, 0);

    boolean result = instance.checkMaySend(domain1);
    assertTrue("First access should be possible", result);
    result = instance.checkMaySend(domain1);
    assertTrue("Second access should be possible", result);
    result = instance.checkMaySend(domain1);
    assertTrue("Third access should be possible", result);
    
    result = instance.checkMaySend(domain1);
    assertFalse("Forth access should be blocked", result);

    result = instance.checkMaySend(domain2);
    assertTrue("First access to domain2 should be possible", result);
    result = instance.checkMaySend(domain2);
    assertTrue("Second access to domain2 should be possible", result);
    result = instance.checkMaySend(domain1);
    assertFalse("Limit of total mails per period reached, should be blocked", result);
    
    instance.cleanup();
    result = instance.checkMaySend(domain1);
    assertTrue("First access after clear should be possible", result);
  }
  
  /**
   * Cleanup (interval checked from background worker) vs. Clean (direct from user interface)
   */
  @Test
  public void testCleanupVsClear() {
    System.out.println("checkAccess");
    String domain = "@test.de";
    LimitSentMails instance = new LimitSentMails(5, 2, 600000);
    instance.cleanup(); // initialize timestamp
    
    for (var i = 0; i < 5; i++) {
      instance.checkMaySend(domain);
    }
    
    boolean result = instance.checkMaySend(domain);
    assertFalse("Access should be blocked", result);

    int count = instance.cleanup();
    assertEquals("Only 2 expected, not 5 because of the domain limit.", 2, count);
    result = instance.checkMaySend(domain);
    assertFalse("Access should still be blocked because of time interval", result);
    
    int count2 = instance.clear();
    assertEquals("Only 2 expected, not 5 because of the domain limit.", 2, count2);
    result = instance.checkMaySend(domain);
    assertTrue("First access after clear should be possible", result);
  }
}
