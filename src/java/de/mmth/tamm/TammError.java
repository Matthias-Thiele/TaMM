/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm;

/**
 * Application error.
 * 
 * This error will be shown in the user interface. Hide all internal
 * error information from the internet.
 * 
 * @author matthias
 */
public class TammError extends Exception {

  /**
   * Creates a new instance of <code>TammError</code> without detail message.
   */
  public TammError() {
  }

  /**
   * Constructs an instance of <code>TammError</code> with the specified detail message.
   *
   * @param msg the detail message.
   */
  public TammError(String msg) {
    super(msg);
  }
}
