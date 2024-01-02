/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.LockData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class LockTable extends DBTable {
  private static final Logger logger = LogManager.getLogger(AttachmentTable.class);
  
  protected static final String TABLE_CONFIG = 
    """
    mailaddress V 250
    lockdate V 20
    lockip V 30
    lockcounter I
    """;
  
  /**
   *
   * @param conn
   * @param tableName
   */
  public LockTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG);
  }
  
  /**
   * Write mail lock data into database.
   * 
   * Only insert, there is no update for locks.
   * 
   * @param lock information
   * @throws TammError 
   */
  public void writeLock(LockData lock) throws TammError {
    String cmd = "INSERT INTO " + tableName + " (" + insertNames + ") values " + paramPlaceholders;
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setString(col++, lock.mailAddress.toLowerCase());
        stmt.setString(col++, lock.lockDate);
        stmt.setString(col++, lock.lockIP);
        stmt.setInt(col++, 0);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error writing lock data.", ex);
      throw new TammError("Error writing lock data.");
    }
  }
  
  /**
   * Increment the lock counter every time someone tries
   * to use a locked address.
   * 
   * @param mailAddress
   * @throws TammError 
   */
  public void incrementLockCount(String mailAddress) throws TammError {
    String cmd = "UPDATE " + tableName + " SET lockcounter = lockcounter + 1 WHERE mailaddress = ?";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setString(col++, mailAddress.toLowerCase());

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error incrementing lock data.", ex);
    }
  }
  
  /**
   * Returns a list of matching locks.
   * 
   * @param filter match mailaddress
   * @return
   * @throws TammError 
   */
  public List<LockData> listLocks(String filter) throws TammError {
    List<LockData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE mailaddress ILIKE ? ORDER BY mailaddress";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        int paramCol = 1;
        stmt.setString(paramCol++, filter);

        var rows = stmt.executeQuery();
        while (rows.next()) {
          var lock = getData(rows);
          logger.debug("Lock found: " + lock.mailAddress);
          result.add(lock);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading lock list.", ex);
      throw new TammError("Error reading lock list.");
    }
    return result;
  }
  
  /**
   * Returns if the given address is part of the lock list.
   * 
   * @param mailAddress
   * @return
   * @throws TammError 
   */
  public boolean checkLock(String mailAddress) throws TammError {
    boolean result = false;
    
    var cmd = "SELECT lockdate FROM " + tableName + " WHERE mailaddress = ? LIMIT 1";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        int paramCol = 1;
        stmt.setString(paramCol++, mailAddress);

        var rows = stmt.executeQuery();
        result = rows.next();
      }
    } catch (SQLException ex) {
      logger.warn("Error checking lock list.", ex);
      throw new TammError("Error checking lock list.");
    }
    
    return result;
  }
  
  /**
   * Removes the give lock from the lock table.
   * 
   * @param lock
   * @throws TammError 
   */
  public void removeLock(LockData lock) throws TammError {
    String cmd;
    cmd = "DELETE FROM " + tableName + " where mailaddress = ? and lockdate = ? ";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setString(col++, lock.mailAddress);
        stmt.setString(col++, lock.lockDate);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error deleting lock entry.", ex);
      throw new TammError("Error deleting lock entry.");
    }
  }
  
  /**
   * Reads the current sql row and copies it to the LockData return object.
   * 
   * @param rows
   * @return
   * @throws SQLException 
   */
  private LockData getData(ResultSet rows) throws SQLException {
    var result = new LockData();
    
    int col = 1;
    result.mailAddress = rows.getString(col++);
    result.lockDate = rows.getString(col++);
    result.lockIP = rows.getString(col++);
    result.lockCounter = rows.getInt(col++);
    
    return result;
  }
  
}
