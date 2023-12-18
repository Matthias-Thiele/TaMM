/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package de.mmth;

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
