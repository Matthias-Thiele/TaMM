/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author matthias
 */
public class DateUtils {
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  
  /**
   * Format a date object into an ISO date string.
   * 
   * A null date will be replaced by 'now'.
   * 
   * @param date
   * @return 
   */
  public static String format(ZonedDateTime date) {
    if (date == null) {
      date = ZonedDateTime.now();
    }
    
    return date.format(formatter);
  }
  
  public static ZonedDateTime now() {
    return ZonedDateTime.now();
  }
}
