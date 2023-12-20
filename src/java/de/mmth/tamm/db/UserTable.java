/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.UserData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
    administrator I
    """;
  
  /**
   * Open and create if needed the user table
   * 
   * @param conn
   * @param tableName 
   */
  public UserTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG);
    if (isNewTable) {
      UserData admin = new UserData();
      admin.name = "admin";
      admin.pwd = "tamm279";
      admin.mainAdmin = true;
      admin.subAdmin = true;
      admin.supervisorId = 1;
      admin.administratorId = 1;
      try {
        writeUser(admin);
      } catch (TammError ex) {
        logger.warn("Cannot create admin user.", ex);
      }
    }
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
    UserData result = null;
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
        
        result = getData(userRows);
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
      cmd = "UPDATE " + tableName + " SET " + updateNames + " WHERE id = " + user.id;
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
        stmt.setInt(col++, user.administratorId);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error writing user data.", ex);
      throw new TammError("Error writing user data.");
    }
  }
  
  /**
   * Lists all users of an admininstrator oder subadmin.
   * 
   * @param administratorId
   * @param filter
   * @return
   * @throws TammError 
   */
  public List<UserData> listUsers(int administratorId, String filter) throws TammError {
    boolean hasId = administratorId != -1;
    boolean hasFilter = filter != null && !filter.isBlank();
    
    List<UserData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName;
    
    if (hasId || hasFilter) {
      cmd += " WHERE ";
    }
    
    if (hasId) {
      cmd += "administrator = ? ";
      if (hasFilter) {
        cmd += "and ";
      }
    }
    
    if (hasFilter) {
      cmd += "( name ILIKE ? or mail ILIKE ? ) ";
    }
    
    cmd += " ORDER BY name";
    
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        int paramCol = 1;
        if (hasId) {
          stmt.setInt(paramCol++, administratorId);
        }
        
        if (hasFilter) {
          stmt.setString(paramCol++, filter);
          stmt.setString(paramCol++, filter);
        }
        
        var userRows = stmt.executeQuery();
        while (userRows.next()) {
          var user = getData(userRows);
          logger.debug("User found: " + user.name);
          result.add(user);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading user list.", ex);
      throw new TammError("Error reading user list.");
    }
    return result;
  }
  
  /**
   * Copies the ResultSet user data into an UserData object.
   * 
   * @param userRows
   * @return
   * @throws SQLException 
   */
  private UserData getData(ResultSet userRows) throws SQLException {
    UserData result = new UserData();
    
    result.id = userRows.getInt(1);
    result.name = userRows.getString(2);
    result.pwd = userRows.getString(3);
    result.mail = userRows.getString(4);
    result.setFlags(userRows.getInt(5));
    result.supervisorId = userRows.getInt(6);
    result.administratorId = userRows.getInt(7);
    
    return result;
  }
}
