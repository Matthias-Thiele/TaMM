/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.progress;

/**
 *
 * @author matthias
 */
public enum Repeat {
  SINGLE("single"),
  DAILY("daily"),
  WEEKLY("weekly"),
  MONTHLY("monthly"),
  YEARLY("yearly");
  
  public final String label;
  
  public static Repeat fromLabel(String label) {
    for (Repeat r: values()) {
      if (r.label.equals(label)) {
        return r;
      }
    }
    
    return null;
  }
  
  private Repeat(String label) {
    this.label = label;
  }
}
