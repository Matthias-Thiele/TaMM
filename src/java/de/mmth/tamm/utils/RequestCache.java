/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.TammLogger;
import de.mmth.tamm.data.UserData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;

/**
 * Local and not persistant cache for password reset requests.
 * 
 * @author matthias
 */
public class RequestCache {
  private static final Logger logger = TammLogger.prepareLogger(RequestCache.class);
  
  private final Map<String, CacheItem> cache = new HashMap<>(); 
  private Date nextCleanup = new Date();
  
  /**
   * Add a new request into the chache.
   * 
   * @param item
   * @param duration expiration time in milliseconds.
   * @param fromIp
   * @return 
   */
  public String add(UserData item, long duration, String fromIp) {
    String key = generateKey();
    var newEntry = new CacheItem();
    newEntry.key = key;
    newEntry.name = item.name;
    newEntry.mail = item.mail;
    newEntry.id = item.id;
    newEntry.ip = fromIp;
    newEntry.expirationDate = new Date((new Date()).getTime() + duration);
    
    cache.put(key, newEntry);
    
    logger.info("Key added " + key);
    return key;
  }
  
  /**
   * Returns the expiration date of the given key.
   * 
   * @param key
   * @return 
   */
  public Date getValidDate(String key) {
    var found = cache.get(key);
    if (found != null) {
      return found.expirationDate;
    } else {
      return new Date();
    }
  }
  
  /**
   * Loads the mail address of a password request from the cache.
   * 
   * Checks the expiration date and removes old entries.
   * 
   * @param key
   * @return 
   */
  public String getUserMail(String key) {
    var found = cache.get(key);
    if (found != null) {
      if (found.expirationDate.after(new Date())) {
        var user = found.mail;
        return user;
      } else {
        cache.remove(key);
      }
    }
    
    return null;
  }
  
  /**
   * Loads the user id of a password request from the cache.
   * 
   * Checks the expiration date and removes old entries.
   * 
   * @param key
   * @return 
   */
  public int getUserId(String key) {
    var found = cache.get(key);
    if (found != null) {
      if (found.expirationDate.after(new Date())) {
        var id = found.id;
        return id;
      } else {
        cache.remove(key);
      }
    }
    
    return -1;
  }
  
  /**
   * Removes the entry with the given key from the request list.
   * @param key 
   */
  public void removeKey(String key) {
    cache.remove(key);
    logger.info("Key removed: " + key);
  }
  
  /**
   * Check for expired keys and remove them from the cache.
   */
  public void cleanup() {
    Date now = new Date();
    if (now.before(this.nextCleanup)) {
      return;
    }
    
    nextCleanup = new Date((new Date()).getTime() + 60000);
    List<String> forRemoval = new ArrayList<>();
    for (var item: cache.entrySet()) {
      if (item.getValue().expirationDate.before(now)) {
        forRemoval.add(item.getKey());
      }
    }
    
    for (var key: forRemoval) {
      cache.remove(key);
      logger.info("Expired key removed: " + key);
    }
  }
  
  /**
   * Persist request cache when servlet is destroyed.
   * 
   * Only the id, name and mail parts of the user
   * data are persisted.
   * 
   * @param destination 
   */
  public void save(Path destination) {
    logger.info("Save request cache.");

    for (var item: cache.values()) {
      var line = item.key + "|" + item.expirationDate.getTime() + "|" + item.id + "|" + item.name + "|" + item.mail + "\r\n"; 
      try {
        Files.writeString(destination, line, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
      } catch (IOException ex) {
        logger.warn("Cannot persist request cache.", ex);
        break;
      }
    }
  }
  
  /**
   * Load persisted request cache on startup.
   * 
   * Only the id, name and mail parts of the user
   * data are persisted.
   * 
   * @param source 
   */
  public void load(Path source) {
    logger.info("Load request cache.");
    List<String> lines;
    try {
      lines = Files.readAllLines(source);
      for (var line: lines) {
        String[] parts = line.split("\\|");
        if (parts.length == 5) {
          String key = parts[0];
          var newEntry = new CacheItem();
          newEntry.key = key;
          newEntry.id = Integer.parseInt(parts[2]);
          newEntry.name = parts[3];
          newEntry.mail = parts[4];
          newEntry.expirationDate = new Date(Long.parseLong(parts[1]));

          cache.put(key, newEntry);
        }
      }
    } catch (IOException ex) {
      logger.warn("Cannot read request cache.", ex);
    }
  }
  
  /**
   * Generate a random key for the password request.
   * 
   * @return 
   */
  private String generateKey() {
    String key = Long.toHexString((long)(Math.random() * Long.MAX_VALUE)) +
            Long.toHexString((long)(Math.random() * Long.MAX_VALUE));
    return key;
  }
}

