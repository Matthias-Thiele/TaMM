/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author matthias
 */
public class DBHelper {
  /**
   * Execute a database query and return the result set.
   * 
   * @param conn
   * @param cmd
   * @return
   * @throws SQLException 
   */
  public static ResultSet readLine(Connection conn, String cmd) throws SQLException {
    var stmt = conn.createStatement();
    return stmt.executeQuery(cmd);
  }
}
