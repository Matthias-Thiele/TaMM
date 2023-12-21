/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.time.ZonedDateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class DateUtilsTest {
  
  /**
   * Test of format method, of class DateUtils.
   */
  @Test
  public void testFormat() {
    System.out.println("format");
    ZonedDateTime date = ZonedDateTime.now();
    String result = DateUtils.format(date);
    assertEquals(19, result.length());
  }
  
}
