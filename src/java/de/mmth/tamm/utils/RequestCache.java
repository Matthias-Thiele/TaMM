/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.data.UserData;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Local and not persistant cache for password reset requests.
 * 
 * @author matthias
 */
public class RequestCache {
  private final Map<String, CacheItem> cache = new HashMap<>(); 
  
  /**
   * Add a new request into the chache.
   * 
   * @param item
   * @param duration expiration time in milliseconds.
   * @return 
   */
  public String add(UserData item, long duration) {
    String key = generateKey();
    var newEntry = new CacheItem();
    newEntry.key = key;
    newEntry.item = item;
    newEntry.expirationDate = new Date((new Date()).getTime() + duration);
    
    cache.put(key, newEntry);
    return key;
  }
  
  /**
   * Loads a password request from the cache.
   * 
   * Checks the expiration date and removes old entries.
   * 
   * @param key
   * @return 
   */
  public UserData getUserItem(String key) {
    var found = cache.get(key);
    if (found != null) {
      if (found.expirationDate.after(new Date())) {
        var user = found.item;
        return user;
      } else {
        cache.remove(key);
      }
    }
    
    return null;
  }
  
  /**
   * Removes the entry with the given key from the request list.
   * @param key 
   */
  public void removeKey(String key) {
    cache.remove(key);
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

class CacheItem {
  String key;
  Date expirationDate;
  UserData item;
}
