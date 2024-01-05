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
    this.id = key;
    this.name = value;
  }
  
  public int id;
  public String name;
}
