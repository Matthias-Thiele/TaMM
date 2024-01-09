/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Limit brute force attacks by restricting
 * ip addresses with too many login
 * faults within a time frame.
 * 
 * Retries are time dependent at a later
 * time possible. No administrative action
 * is needed.
 * 
 * @author matthias
 */
public class InvalidAccessCache {
  private final int maxRetryLimit;
  private final Map<String, Integer> accessCounter = new HashMap<>();
  private final long delayMillis;
  private long nextCleanup = 0;
  
  /**
   * Create cache, define the maximum of login
   * faults within a time frame and the decay
   * time.
   * 
   * @param maxRetryLimit
   * @param delayMillis 
   */
  public InvalidAccessCache(int maxRetryLimit, long delayMillis) {
    this.maxRetryLimit = maxRetryLimit;
    this.delayMillis = delayMillis;
  }
  
  /**
   * Checks if access from this IP address is allowed.
   * 
   * @param ip
   * @return 
   */
  public boolean checkAccess(String ip) {
    Integer count = accessCounter.get(ip);
    return (count == null) || count <= maxRetryLimit;
  }
  
  /**
   * Mark an invalid access from the given IP address.
   * 
   * @param ip 
   */
  public void addInvalidAccess(String ip) {
    synchronized (accessCounter) {
      Integer counter = accessCounter.get(ip);
      if (counter == null) {
        counter = 1;
        accessCounter.put(ip, counter);
      } else {
        counter++;
      }

      accessCounter.replace(ip, counter);
    }
  }
  
  /**
   * Clears the access counter cache and
   * returns the number of former entries.
   * 
   * @return 
   */
  public int clear() {
    int locks = accessCounter.size();
    accessCounter.clear();
    
    return locks;
  }
  
  /**
   * Check decay of invalid access memory.
   * 
   * After the defined time period all
   * invalid access counters will be
   * decremented one access.
   */
  public void cleanup() {
    long now = System.currentTimeMillis();
    if (now > nextCleanup) {
      nextCleanup = now + delayMillis;
      synchronized (accessCounter) {
        List<String> forRemoval = null;
        for (var item: accessCounter.entrySet()) {
          int count = item.getValue();
          if (count > 0) {
            count--;
            accessCounter.replace(item.getKey(), count);
          } else {
            if (forRemoval == null) {
              forRemoval = new ArrayList<>();
            }
            
            forRemoval.add(item.getKey());
          }
        }
        
        if (forRemoval != null) {
          for (var key: forRemoval) {
            accessCounter.remove(key);
          }
        }
      }
      
    }
  }
  
}
