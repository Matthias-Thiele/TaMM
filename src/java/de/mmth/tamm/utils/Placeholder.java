/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Position independent placeholder replacement.
 * 
 * Placeholders are surrounded by {[ opening
 * and ]} closing brackets. These strings are
 * allowed in the source text and will only
 * be replaced if there is a valid key between
 * them. Otherwise they will stay unchanged.
 * 
 * {[here insert your key]} will be unchanged unless
 * there is a value with the key name 
 * "here insert your key"
 * @author matthias
 */
public class Placeholder {
  
  /**
   * Resolves text with a single placeholder parameter.
   * @param source
   * @param name1
   * @param param1
   * @return 
   */
  public String resolve(String source, String name1, String param1) {
    Map<String, String> placeholderData = new HashMap<>();
    placeholderData.put(name1, param1);
    return resolve(source, placeholderData);
  }
  
  /**
   * Resolves text with a map of placeholders.
   * @param source
   * @param placeholderData
   * @return 
   */
  public String resolve(String source, Map<String, String> placeholderData) {
    StringBuilder buf = new StringBuilder(source.length() * 2);
    
    for (int startPos = 0, copyMarker = 0;;) {
      int nextPos = source.indexOf("{[", startPos);
      if (nextPos < 0) {
        // no more opening brackets, add remainder and quit
        buf.append(source.substring(copyMarker));
        break;
      }
      
      int endPos = source.indexOf("]}", nextPos + 2);
      if (endPos < 0) {
        // no more closing brackets, add remainder and quit
        buf.append(source.substring(copyMarker));
        break;
      }
      
      String key = source.substring(nextPos + 2, endPos);
      String value = placeholderData.get(key);
      if (value == null) {
        // not a valid placeholder, try next match
        startPos = nextPos + 2;
        continue;
      }
      
      buf.append(source.substring(copyMarker, nextPos));
      buf.append(value);
      copyMarker = endPos + 2;
      startPos = copyMarker;
    }
    
    return buf.toString();
  }
}
