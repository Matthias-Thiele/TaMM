/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.data;

/**
 *
 * @author matthias
 */
public class KeyValue {
  public KeyValue(int key, String value) {
    this.key = key;
    this.value = value;
  }
  
  public int key;
  public String value;
}
