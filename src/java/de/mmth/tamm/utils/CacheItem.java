/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.data.UserData;
import java.util.Date;

/**
 *
 * @author matthias
 */
public class CacheItem {
  String key;
  Date expirationDate;
  UserData item;
}
