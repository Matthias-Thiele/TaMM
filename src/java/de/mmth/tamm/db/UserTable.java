/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.TammLogger;
import de.mmth.tamm.data.KeyValue;
import de.mmth.tamm.data.UserData;
import de.mmth.tamm.utils.PasswordUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

/**
 * Read/ Write access to the user table.
 * 
 * @author matthias
 */
public final class UserTable extends DBTable {
  private static final Logger logger = TammLogger.prepareLogger(UserTable.class);
  
  protected static final String TABLE_CONFIG = 
    """
    id I G
    clientid I
    name V 100
    pwd V 100
    mail V 100
    flags I
    supervisor I
    administrator I
    lastlogin V 20
    """;
  
  protected static final String INDEX_CONFIG = 
    """
    create unique index if not exists ixuserids on {[tablename]} (id);
    create index if not exists ixclientid on {[tablename]} (clientid);
    """;
  
  /**
   * Open and create if needed the user table
   * 
   * @param conn
   * @param tableName 
   */
  public UserTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG, INDEX_CONFIG);
    assureAdminUser();
  }
  
  /**
   * Make sure that a new table at least contains
   * the default admin user.
   */
  private void assureAdminUser() {
    if (isNewTable) {
      UserData admin = new UserData();
      admin.name = "admin";
      admin.pwd = PasswordUtils.encodePassword("tamm279");
      admin.clientId = 1;
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
   * @param clientId
   * @param byId ignored if other parameter is not null
   * @param byNameOrMail null if read by id
   * @return
   * @throws TammError 
   */
  public UserData readUser(int clientId, int byId, String byNameOrMail) throws TammError {
    UserData result = null;
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE clientid = ? AND ";
    
    if (byNameOrMail != null) {
      cmd += " (name = ? OR mail = ?)";
    } else {
      cmd += " id = ?";
    }
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        stmt.setInt(1, clientId);
        if (byNameOrMail == null) {
          stmt.setInt(2, byId);
        } else {
          stmt.setString(2, byNameOrMail);
          stmt.setString(3, byNameOrMail);
        }
        
        var userRows = stmt.executeQuery();
        if (!userRows.next()) {
          logger.info("User not found: " + byId + ", " + byNameOrMail);
          throw new TammError("User not found.");
        }
        
        result = getData(userRows);
      }
    } catch (SQLException ex) {
      logger.warn("Error reading user data.", ex);
      throw new TammError("Error reading user data.");
    }
    return result;
  }
  
  /**
   * Write user data into database.
   * 
   * Create a new User with user.id == -1
   * 
   * @param user
   * @return 
   * @throws TammError 
   */
  public int writeUser(UserData user) throws TammError {
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
        stmt.setInt(col++, user.clientId);
        stmt.setString(col++, user.name.trim());
        stmt.setString(col++, user.pwd);
        stmt.setString(col++, user.mail);
        stmt.setInt(col++, user.getFlags());
        stmt.setInt(col++, user.supervisorId);
        stmt.setInt(col++, user.administratorId);
        stmt.setString(col++, user.lastLogin);

        stmt.execute();
        }

      if (user.id == -1) {
        try (var stmt = conn.getConnection().prepareStatement("SELECT LASTVAL()")) {
          var result = stmt.executeQuery();
          result.next();
          user.id = result.getInt(1);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error writing user data.", ex);
      throw new TammError("Error writing user data.");
    }
    
    return user.id;
  }
  
  /**
   * Update last login date of the given user.
   * 
   * Create a new User with user.id == -1
   * 
   * @param clientId
   * @param userId
   * @param loginDate
   * @throws TammError 
   */
  public void updateLoginDate(int clientId, int userId, String loginDate) throws TammError {
    String cmd;
    cmd = "UPDATE " + tableName + " SET lastlogin = ? WHERE clientid = ? AND id = ?";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setString(col++, loginDate);
        stmt.setInt(col++, clientId);
        stmt.setInt(col++, userId);
        
        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error updating login date.", ex);
      throw new TammError("Error updating login date.");
    }
  }
  
  /**
   * Lists all users of an admininstrator oder subadmin.
   * 
   * @param clientId
   * @param administratorId
   * @param filter
   * @param onlyAdmins
   * @return
   * @throws TammError 
   */
  public List<UserData> listUsers(int clientId, int administratorId, String filter, boolean onlyAdmins) throws TammError {
    boolean hasClientId = clientId > 0;
    boolean hasId = administratorId > 0;
    boolean hasFilter = filter != null && !filter.isBlank();
    
    List<UserData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName;
    
    if (hasClientId || hasId || hasFilter || onlyAdmins) {
      cmd += " WHERE ";
    }
    
    if (hasClientId) {
      cmd += "clientid = ? ";
      if (hasId || hasFilter || onlyAdmins) {
        cmd += " AND ";
      }
    }
    
    if (hasId) {
      cmd += "administrator = ? ";
      if (hasFilter || onlyAdmins) {
        cmd += "AND ";
      }
    }
    
    if (hasFilter) {
      cmd += "( name ILIKE ? or mail ILIKE ? ) ";
      if (onlyAdmins) {
        cmd += "AND ";
      }
    }
    
    if (onlyAdmins) {
      cmd += " ((flags & 3) <> 0) ";
    }
    
    cmd += " ORDER BY name";
    
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        int paramCol = 1;
        if (hasClientId) {
          stmt.setInt(paramCol++, clientId);
        }
        
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
   * Lists all usersnames.
   * 
   * @param clientId
   * @return
   * @throws TammError 
   */
  public List<KeyValue> listUserNames(int clientId) throws TammError {
    List<KeyValue> result = new ArrayList<>();
    
    var cmd = "SELECT id, name FROM " + tableName + " WHERE clientid = ? ORDER BY name";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        stmt.setInt(1, clientId);
        
        var userRows = stmt.executeQuery();
        while (userRows.next()) {
          int id = userRows.getInt(1);
          String name = userRows.getString(2);
          logger.debug("User found: " + name);
          result.add(new KeyValue(id, name));
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading user/id list.", ex);
      throw new TammError("Error reading user/id list.");
    }
    return result;
  }
  
  /**
   * Returns the number of users of the given client.
   * 
   * @param clientId
   * @return
   * @throws TammError 
   */
  public int getUserCount(int clientId) throws TammError {
    int result = 0;
    
    var cmd = "SELECT count(*) FROM " + tableName + " WHERE clientid = ?";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        stmt.setInt(1, clientId);
        
        var userRows = stmt.executeQuery();
        if (userRows.next()) {
          result = userRows.getInt(1);
          logger.debug("Number of users: " + result);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading user count.", ex);
      throw new TammError("Error reading user count.");
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
    result.clientId = userRows.getInt(2);
    result.name = userRows.getString(3);
    result.pwd = userRows.getString(4);
    result.mail = userRows.getString(5);
    result.setFlags(userRows.getInt(6));
    result.supervisorId = userRows.getInt(7);
    result.administratorId = userRows.getInt(8);
    result.lastLogin = userRows.getString(9);
    
    return result;
  }
}
