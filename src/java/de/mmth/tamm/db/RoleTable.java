/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class RoleTable extends DBTable {
  private static final Logger logger = LogManager.getLogger(RoleTable.class);
  
  private static final String TABLE_CONFIG = 
    """
    id I G
    name V 100
    supervisor I
    """;
  
  public RoleTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG);
  }
  
  
}
