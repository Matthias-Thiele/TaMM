/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.RoleData;
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
public class RoleTable extends DBTable {
  private static final Logger logger = LogManager.getLogger(RoleTable.class);
  
  protected static final String TABLE_CONFIG = 
    """
    id I G16777216
    clientid I
    name V 100
    owner I
    """;
  
  protected static final String INDEX_CONFIG = 
    """
    create unique index if not exists ixroleids on {[tablename]} (clientid, id);
    """;

  public RoleTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG, INDEX_CONFIG);
  }
  
  /**
   * Write role data into database.
   * 
   * Create a new Role with role.id == -1
   * 
   * @param role
   * @return new or existing role id.
   * @throws TammError 
   */
  public int writeRole(RoleData role) throws TammError {
    int roleId = role.id;
    String cmd;
    if (role.id == -1) {
      cmd = "INSERT INTO " + tableName + " (" + insertNames + ") values " + paramPlaceholders;
    } else {
      cmd = "UPDATE " + tableName + " SET " + updateNames + " WHERE id = " + role.id;
    }
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        if (role.id != -1) {
          stmt.setInt(col++, role.id);
        }
        
        stmt.setInt(col++, role.clientId);
        stmt.setString(col++, role.name);
        stmt.setInt(col++, role.owner);

        stmt.execute();
      }
      
      if (roleId == -1) {
        try (var stmt = conn.getConnection().prepareStatement("SELECT LASTVAL()")) {
          var result = stmt.executeQuery();
          result.next();
          roleId = result.getInt(1);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error writing role data.", ex);
      throw new TammError("Error writing role data.");
    }
    
    return roleId;
  }
  
  /**
   * Lists all roles.
   * 
   * @param clientId
   * @param owner
   * @return
   * @throws TammError 
   */
  public List<RoleData> listRoles(int clientId, int owner) throws TammError {
    List<RoleData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE clientid = ? ";
    if (owner != -1) {
      cmd += " AND owner = ? ";
    }
    
    cmd += " ORDER BY name";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        stmt.setInt(1, clientId);
        if (owner != -1) {
          stmt.setInt(2, owner);
        }
        
        var rows = stmt.executeQuery();
        while (rows.next()) {
          var role = getData(rows);
          logger.debug("Role found: " + role.name);
          result.add(role);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading roles list.", ex);
      throw new TammError("Error reading roles list.");
    }
    
    return result;
  }

  /**
   * Remove role from the role table.
   * 
   * @param clientId
   * @param roleId
   * @throws TammError 
   */
  public void removeRole(int clientId, int roleId) throws TammError {
    String cmd;
    cmd = "DELETE FROM " + tableName + " where clientid = ? and id = ? ";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setInt(col++, clientId);
        stmt.setInt(col++, roleId);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error deleting role data.", ex);
      throw new TammError("Error deleting role data.");
    }
  }
  
  /**
   * Copies the ResultSet role data into an RoleData object.
   * 
   * @param rows
   * @return
   * @throws SQLException 
   */
  private RoleData getData(ResultSet rows) throws SQLException {
    RoleData result = new RoleData();
    
    result.id = rows.getInt(1);
    result.clientId = rows.getInt(2);
    result.name = rows.getString(3);
    result.owner = rows.getInt(4);
    
    return result;
  }
  
}
