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
  
  /**
   * returns the label of a Repeat object.
   * Used in the toString method.
   */
  public final String label;
  
  /**
   * Returns the Repeat object assigned to the given label.
   * Needed by the fromString method.
   * 
   * @param label
   * @return 
   */
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
