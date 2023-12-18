/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.mmth.db;

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
