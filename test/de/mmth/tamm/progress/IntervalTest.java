/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.progress;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class IntervalTest {
  
  /**
   * Test of getRepeat method, of class Interval.
   */
  @Test
  public void testGetRepeat() {
    System.out.println("getRepeat");
    for (Repeat r: Repeat.values()) {
      String interval = r.label + "|1|2024-01-01";
      Interval instance = new Interval(interval);
      Repeat result = instance.getRepeat();
      assertEquals(r, result);
    }
  }

  /**
   * Test of getDivider method, of class Interval.
   */
  @Test
  public void testGetDivider() {
    System.out.println("getDivider");
    Interval instance = new Interval("weekly|17|2024-01-02");
    int expResult = 17;
    int result = instance.getDivider();
    assertEquals(expResult, result);
  }

  /**
   * Test of getDates method, of class Interval.
   */
  @Test
  public void testGetDates() {
    System.out.println("getDates");
    Interval instance = new Interval("weekly|17|2024-01-02;2024-01-12;2024-01-22");
    String[] expResult = {"2024-01-02","2024-01-12", "2024-01-22"};
    String[] result = instance.getDates();
    assertArrayEquals(expResult, result);
  }

  /**
   * Test of toString method, of class Interval.
   */
  @Test
  public void testToString() {
    System.out.println("toString");
    String source = "weekly|17|2024-01-02;2024-01-12;2024-01-22";
    Interval instance = new Interval(source);
    String result = instance.toString();
    assertEquals(source, result);
  }

  /**
   * Test of nextDate method, of class Interval.
   */
  @Test
  public void testNextSingleDate() {
    System.out.println("nextDate SINGLE");
    String source = "single|1|2024-01-02;2024-01-12;2024-01-22";
    Interval instance = new Interval(source);
    String nextDate = "2024-01-01";
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-02", nextDate);
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-12", nextDate);
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-22", nextDate);
  }
  
  /**
   * Test of nextDate method, of class Interval.
   */
  @Test
  public void testNextDailyDate() {
    System.out.println("nextDate DAILY");
    String source = "daily|5|2024-01-12;2024-01-13";
    Interval instance = new Interval(source);
    String nextDate = "2024-01-14";
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-17", nextDate);
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-18", nextDate);
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-22", nextDate);
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-23", nextDate);
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-27", nextDate);
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-01-28", nextDate);
    nextDate = instance.nextDate(nextDate);
    assertEquals("2024-02-01", nextDate);
  }
  
}
