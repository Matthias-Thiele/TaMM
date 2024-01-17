/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.TammLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author matthias
 */
public class KeepAliveCache {
  private final long CLEANUP_DELTA_MILLIS = 1000 * 60 * 60 * 12; // twice a day
  private static final org.apache.logging.log4j.Logger logger = TammLogger.prepareLogger(KeepAliveCache.class);
  
  private final Map<String, LoginCacheItem> loginCache = new HashMap<>();
  private long nextCleanup;
  
  /**
   * Add a user to the KeepAlive cache.
   * 
   * Create an UUID as cookie, add the user, valid time 
   * and uuid to the cache and return the uuid.
   * 
   * @param userId
   * @param keepAliveDuration 
   * @return 
   */
  public String addLogin(int userId, long keepAliveDuration) {
    var uuid = UUID.randomUUID().toString();
    var cacheItem = new LoginCacheItem();
    cacheItem.expirationDate = (new Date()).getTime() + keepAliveDuration;
    cacheItem.cookie = uuid;
    cacheItem.userId = userId;
    
    // have at most 3 keep alive cookies active
    LoginCacheItem it1 = null, it2 = null, forDeletion = null;
    for (var check: loginCache.values()) {
      if (check.userId == userId) {
        if (it1 == null) {
          it1 = check;
        } else if (it2 == null) {
          it2 = check;
        } else {
          if (it1.expirationDate < it2.expirationDate) {
            forDeletion = it1;
          } else {
            forDeletion = it2;
          }
          
          break;
        }
      }
    }
    
    if (forDeletion != null) {
      loginCache.remove(forDeletion.cookie);
    }
    
    loginCache.put(uuid, cacheItem);
    
    return uuid;
  }
  
  /**
   * Checks if the cookie is in the loginCache and returns
   * the user id if valid. 
   * 
   * Otherwise return -1:
   * 
   * @param cookie
   * @return 
   */
  public int getUserId(String cookie) {
    var cacheItem = loginCache.get(cookie);
    if (cacheItem == null) {
      return -1;
    } else {
      return cacheItem.userId;
    }
  }
  
  /**
   * Remove all old cookie entries.

   */
  public void cleanup() {
    long now = System.currentTimeMillis();
    if (now > nextCleanup) {
      nextCleanup = now + CLEANUP_DELTA_MILLIS;
      List<String> forRemoval = null;
      for (var item: loginCache.entrySet()) {
        if (item.getValue().expirationDate < now) {
          if (forRemoval == null) {
            forRemoval = new ArrayList<>();
          }

          forRemoval.add(item.getKey());
        }
      }
      
      if (forRemoval != null) {
        for (var key: forRemoval) {
          loginCache.remove(key);
        }
      }
    }
  }
  
  /**
   * Persist keep alive cache when servlet is destroyed.
   * 
   * @param destination 
   */
  public void save(Path destination) {
    logger.info("Save keep alive cache.");

    for (var item: loginCache.values()) {
      var line = item.expirationDate + "|" + item.userId + "|" + item.cookie + "\r\n"; 
      try {
        Files.writeString(destination, line, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
      } catch (IOException ex) {
        logger.warn("Cannot persist keep alive cache.", ex);
        break;
      }
    }
  }
  
  /**
   * Load persisted keep alive cache on startup.
   * 
   * @param source 
   */
  public void load(Path source) {
    logger.info("Load keep alive cache.");
    List<String> lines;
    try {
      lines = Files.readAllLines(source);
      for (var line: lines) {
        String[] parts = line.split("\\|");
        if (parts.length == 3) {
          String uuid = parts[2];
          var newEntry = new LoginCacheItem();
          newEntry.cookie = uuid;
          newEntry.userId = Integer.parseInt(parts[1]);
          newEntry.expirationDate = Long.parseLong(parts[0]);

          loginCache.put(uuid, newEntry);
        }
      }
    } catch (IOException ex) {
      logger.warn("Cannot read keep alive cache.", ex);
    }
  }
}

class LoginCacheItem {
  int userId;
  long expirationDate;
  String cookie;
}