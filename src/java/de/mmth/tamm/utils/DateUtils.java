/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 *
 * @author matthias
 */
public class DateUtils {
  private static final DateTimeFormatter timeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  /**
   * Format a  zoned date time object into an ISO date string.
   * 
   * A null date will be replaced by 'now'.
   * 
   * @param date
   * @return 
   */
  public static String formatZ(ZonedDateTime date) {
    if (date == null) {
      date = ZonedDateTime.now();
    }
    
    return date.format(timeformatter);
  }
  
  /**
   * Returns a Date with time as ISO string.
   * 
   * @param date
   * @return 
   */
  public static String formatD(Date date) {
    return isoDate.format(date);
  }
  
  /**
   * Format a local date object into an ISO date string.
   * 
   * A null date will be replaced by 'now'.
   * 
   * @param date
   * @return 
   */
  public static String formatL(LocalDate date) {
    if (date == null) {
      date = LocalDate.now();
    }
    
    return date.format(dateformatter);
  }
  
  public static ZonedDateTime now() {
    return ZonedDateTime.now();
  }
  
  public static String toDay() {
    return formatL(LocalDate.now());
  }
  
  public static LocalDate fromIso(String isoDate) {
    if ((isoDate == null) || (isoDate.length() != 10)) {
      throw new DateTimeException("Invalid ISO Date length.");
    }
    
    int year = Integer.parseInt(isoDate.substring(0, 4));
    int month = Integer.parseInt(isoDate.substring(5, 7));
    int day = Integer.parseInt(isoDate.substring(8, 10));
    
    return LocalDate.of(year, month, day);
  }
}
