/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.progress;

import de.mmth.tamm.utils.DateUtils;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serializes and calculates Interval dates.
 * 
 * The date list is expected to be in ISO
 * format and sorted.
 * 
 * @author matthias
 */
public class Interval {
  private static final Logger logger = LogManager.getLogger(Interval.class);
  
  private Repeat repeat = Repeat.SINGLE;
  private int divider = 1;
  private String[] isoDates = new String[0];
  private boolean valid = false;
  
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
      case DAILY -> nextDailyDate(afterThisDate, divider, isoDates);
      case WEEKLY -> nextDailyDate(afterThisDate, 7 * divider, isoDates);
      case MONTHLY -> nextMonthlyDate(afterThisDate);
      case YEARLY -> nextYearlyDate(afterThisDate);
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
    if (data != null) {
      var parts = data.split("\\|");
      if (parts.length == 3) {
        repeat = Repeat.fromLabel(parts[0]);
        try {
          divider = Integer.parseInt(parts[1]);
          isoDates = parts[2].split(";");
          valid = (repeat != null) && (isoDates.length > 0);
          for (var dt: isoDates) {
            DateUtils.fromIso(dt);
          }
        } catch(NumberFormatException | DateTimeException ex2) {
          valid = false;
        }
      }
    }
    if (!valid) {
      logger.info("Invalid interval ignored: " + data);
    }
  }
  
  /**
   * A valid interval has been constructed.
   * 
   * @return 
   */
  public boolean isValid() {
    return valid;
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
  
  /**
   * Find the next activation date after the given date.
   * 
   * The weekly date is calculated the same way as
   * daily dates, just the difference is multiplied by 7.
   * 
   * @param afterThisDate
   * @param localDivider
   * @param localIsoDates
   * @return 
   */
  private String nextDailyDate(String afterThisDate, int localDivider, String[] localIsoDates) {
    LocalDate dateAfter = DateUtils.fromIso(afterThisDate);
    if (localIsoDates.length == 1) {
      LocalDate nextDay = dateAfter.plusDays(localDivider);
      return DateUtils.formatL(nextDay);
    } else {
      long daysAfter = dateAfter.getLong(ChronoField.EPOCH_DAY) + 1;
      long nowModDivider = daysAfter % localDivider;
      long minDay = Long.MAX_VALUE;
      for (String next: localIsoDates) {
        long isoDays = DateUtils.fromIso(next).getLong(ChronoField.EPOCH_DAY);
        long isoModDivider = isoDays % localDivider;
        long checkDay = daysAfter + isoModDivider - nowModDivider;
        
        if (checkDay < daysAfter) {
          checkDay += localDivider;
        }
        if (checkDay < minDay) {
          minDay = checkDay;
        }
      }
      LocalDate nextDay = LocalDate.ofEpochDay(minDay);
      return DateUtils.formatL(nextDay);
    }
  }
  
  /**
   * Find the next monthly activation after the given date.
   * 
   * @param afterThisDate
   * @return 
   */
  private String nextMonthlyDate(String afterThisDate) {
    String nextDate = "Z";
    for (var iso: isoDates) {
      String checkMonth = iso.substring(5, 7);
      String checkYear = iso.substring(0, 4);
      String checkDay = iso.substring(8, 10);

      for (var retry = 0; retry < 100; retry++) {
        String nextCheck = checkYear + "-" + checkMonth + "-" + checkDay;
        if (nextCheck.compareTo(afterThisDate) > 0) {
          if (nextCheck.compareTo(nextDate) < 0) {
            nextDate = nextCheck;
          }

          break;
        }

        int nextMonth = Integer.parseInt(checkMonth) + divider;
        while (nextMonth > 12) {
          nextMonth -= 12;
          checkYear = Integer.toString(Integer.parseInt(checkYear) + 1);
        }
        
        checkMonth = (nextMonth < 10) ? "0" + nextMonth : Integer.toString(nextMonth);
      }
    }

    return nextDate;
  }
  
  /**
   * Find the next yearly activation after the given date.
   * 
   * @param afterThisDate
   * @return 
   */
  private String nextYearlyDate(String afterThisDate) {
    String nextDate = "Z";
    for (var iso: isoDates) {
      int startYear = Integer.parseInt(iso.substring(0, 4));
      String isoDatePart = iso.substring(4);
      for(int y = 0; y < 100; y++) {
        String checkDate = Integer.toString(startYear) + isoDatePart;
        if (checkDate.compareTo(afterThisDate) > 0) {
          if (checkDate.compareTo(nextDate) < 0) {
            nextDate = checkDate;
          }

          break;
        }

        startYear += divider;
      }
    }

    return nextDate;
  }
}
