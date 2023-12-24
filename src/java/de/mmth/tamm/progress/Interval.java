/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.progress;

import de.mmth.tamm.utils.DateUtils;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;

/**
 * Serializes and calculates Interval dates.
 * 
 * The date list is expected to be in ISO
 * format and sorted.
 * 
 * @author matthias
 */
public class Interval {
  private Repeat repeat = Repeat.SINGLE;
  private int divider = 1;
  private String[] isoDates = new String[0];
  
  /**
   * Construct from string representation.
   * @param data 
   */
  public Interval(String data) {
    if (data != null) {
      init(data);
    }
  }
  
  public String nextDate(String afterThisDate) {
    if (isoDates == null || isoDates.length == 0) {
      // no dates available
      return null;
    }
    
    return switch (repeat) {
      case SINGLE -> nextSingleDate(afterThisDate);
      case DAILY -> nextDailyDate(afterThisDate);
      default -> null;
    };
  }
  
  /**
   * Gets the Repeat type of this interval.
   * @return 
   */
  public Repeat getRepeat() {
    return repeat;
  }
  
  /**
   * Gets the divider of this interval.
   * 
   * e.g.: bi-weekly -> divider = 2
   *       half-year -> divider = 6
   * 
   * @return 
   */
  public int getDivider() {
    return divider;
  }
  
  /**
   * Gets the list with the start dates.
   * 
   * @return 
   */
  public String[] getDates() {
    return isoDates;
  }
  
  /**
   * Returns a string representation as expected
   * for the constructor.
   * 
   * "repeat type"|"divider"|"date1";"date2"...
   * @return 
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(repeat.label);
    buf.append("|");
    buf.append(divider);
    buf.append("|");
    
    boolean isFirst = true;
    for (var iso: isoDates) {
      if (isFirst) {
        isFirst = false;
      } else {
        buf.append(";");
      }
      buf.append(iso);
    }
    
    return buf.toString();
  }
  
  /**
   * Initializes the object with the constructor string.
   * @param data 
   */
  private void init(String data) {
    var parts = data.split("\\|");
    if (parts.length == 3) {
      repeat = Repeat.fromLabel(parts[0]);
      divider = Integer.parseInt(parts[1]);
      isoDates = parts[2].split(";");
    }
  }
  
  /**
   * Returns the next date of the iso dates list
   * after the given date.
   * 
   * @param afterThisDate
   * @return 
   */
  private String nextSingleDate(String afterThisDate) {
    for (String dt: isoDates) {
      if (dt.compareTo(afterThisDate) > 0) {
        // the next later date
        return dt;
      }
    }
    
    // no more dates available
    return null;
  }
  
  private String nextDailyDate(String afterThisDate) {
    LocalDate dateAfter = DateUtils.fromIso(afterThisDate);
    if (isoDates.length == 1) {
      LocalDate nextDay = dateAfter.plusDays(divider);
      return DateUtils.formatL(nextDay);
    } else {
      long daysAfter = dateAfter.getLong(ChronoField.EPOCH_DAY) + 1;
      long nowModDivider = daysAfter % divider;
      long minDay = Long.MAX_VALUE;
      for (String next: isoDates) {
        long isoDays = DateUtils.fromIso(next).getLong(ChronoField.EPOCH_DAY);
        long isoModDivider = isoDays % divider;
        long checkDay = daysAfter + isoModDivider - nowModDivider;
        
        if (checkDay < daysAfter) {
          checkDay += divider;
        }
        if (checkDay < minDay) {
          minDay = checkDay;
        }
      }
      LocalDate nextDay = LocalDate.ofEpochDay(minDay);
      return DateUtils.formatL(nextDay);
    }
  }
}
