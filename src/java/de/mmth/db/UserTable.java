/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.db;

import de.mmth.TammError;
import de.mmth.data.UserData;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Read/ Write access to the user table.
 * 
 * @author matthias
 */
public class UserTable extends DBTable {
  private static final Logger logger = LogManager.getLogger(UserTable.class);
  
  private static final String TABLE_CONFIG = 
    """
    id I G
    name V 100
    pwd V 100
    mail V 100
    flags I
    supervisor I
    """;
  
  /**
   * Open and create if needed the user table
   * 
   * @param conn
   * @param tableName 
   */
  public UserTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG);
  }
  
  /**
   * Read a user by id or by name or mail address.
   * 
   * @param byId ignored if other parameter is not null
   * @param byNameOrMail null if read by id
   * @return
   * @throws TammError 
   */
  public UserData readUser(int byId, String byNameOrMail) throws TammError {
    UserData result = new UserData();
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE ";
    if (byNameOrMail != null) {
      cmd += " name = ? or mail = ?";
    } else {
      cmd += " id = ?";
    }
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        if (byNameOrMail == null) {
          stmt.setInt(1, byId);
        } else {
          stmt.setString(1, byNameOrMail);
          stmt.setString(2, byNameOrMail);
        }
        
        var userRows = stmt.executeQuery();
        if (!userRows.next()) {
          logger.info("User not found: " + byId + ", " + byNameOrMail);
          throw new TammError("User not found.");
        }
        
        result.id = userRows.getInt(1);
        result.name = userRows.getString(2);
        result.pwd = userRows.getString(3);
        result.mail = userRows.getString(4);
        result.setFlags(userRows.getInt(5));
        result.supervisorId = userRows.getInt(6);
      }
    } catch (SQLException ex) {
      logger.warn("Error writing user data.", ex);
      throw new TammError("Error writing user data.");
    }
    return result;
  }
  
  /**
   * Write user data into database.
   * 
   * Create a new User with user.id == -1
   * 
   * @param user
   * @throws TammError 
   */
  public void writeUser(UserData user) throws TammError {
    String cmd;
    if (user.id == -1) {
      cmd = "INSERT INTO " + tableName + " (" + insertNames + ") values " + paramPlaceholders;
    } else {
      cmd = "UPDATE " + tableName + " SET " + updateNames;
    }
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setString(col++, user.name);
        stmt.setString(col++, user.pwd);
        stmt.setString(col++, user.mail);
        stmt.setInt(col++, user.getFlags());
        stmt.setInt(col++, user.supervisorId);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error writing user data.", ex);
      throw new TammError("Error writing user data.");
    }
  }
}
