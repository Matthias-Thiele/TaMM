/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
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
    String result = DateUtils.formatZ(date);
    assertEquals(19, result.length());
    
    LocalDate ldate = LocalDate.now();
    result = DateUtils.formatL(ldate);
    assertEquals(10, result.length());
    
    Date now = new Date();
    result = DateUtils.formatD(now);
    assertEquals(19, result.length());
  }

  /**
   * Test of fromIso method, of class DateUtils.
   */
  @Test
  public void testFromIso() {
    System.out.println("fromIso");
    String isoDate = "2024-12-31";
    LocalDate result = DateUtils.fromIso(isoDate);
    assertEquals(2024, result.getYear());
    assertEquals(12, result.getMonthValue());
    assertEquals(31, result.getDayOfMonth());
  }
  
}
