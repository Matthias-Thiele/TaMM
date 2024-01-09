/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author matthias
 */
public class LimitSentMails {
  private final int maxSendLimit;
  private final int maxSendPerDomainLimit;
  private final Map<String, Integer> accessCounter = new HashMap<>();
  private final long delayMillis;
  private long nextCleanup = 0;
  private int mailsSent = 0;
  
  /**
   * Create cache, define the maximum of mails
   * sent within a time frame and the clear
   * time.
   * 
   * @param maxSendLimit
   * @param maxSendPerDomainLimit
   * @param delayMillis 
   */
  public LimitSentMails(int maxSendLimit, int maxSendPerDomainLimit, long delayMillis) {
    this.maxSendLimit = maxSendLimit;
    this.maxSendPerDomainLimit = maxSendPerDomainLimit;
    this.delayMillis = delayMillis;
  }

  /**
   * Increments the sent counter of the given
   * domain and returns if the mail can be send.
   * 
   * @param domain
   * @return 
   */
  public boolean checkMaySend(String domain) {
    Integer count = accessCounter.get(domain);
    
    synchronized (accessCounter) {
      Integer counter = accessCounter.get(domain);
      if (counter == null) {
        counter = 1;
        accessCounter.put(domain, counter);
      } else {
        counter++;
      }

      accessCounter.replace(domain, counter);
    }
    
    var ok = (count == null) || count < maxSendPerDomainLimit;
    if (ok) {
      mailsSent++;
      if (mailsSent > maxSendLimit) {
        ok = false;
      }
    }
    
    return ok;
  }
  
  /**
   * Clears the counter list after the configured interval.
   * @return 
   */
  public int cleanup() {
    int result = mailsSent;
    
    long now = System.currentTimeMillis();
    if (now > nextCleanup) {
      nextCleanup = now + delayMillis;
      
      clear();
    }
    
    return result;
  }
  
  /**
   * Clears the counter list immediately.
   * 
   * @return 
   */
  public int clear() {
    int result = mailsSent;
    
    accessCounter.clear();
    mailsSent = 0;
    
    return result;
  }
}
