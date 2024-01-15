/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.TammLogger;
import de.mmth.tamm.data.RoleAssignmentData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

/**
 * Contains the list of roles assigned to an user.
 * 
 * @author matthias
 */
public class RoleAssignmentTable extends DBTable {
  private static final Logger logger = TammLogger.prepareLogger(RoleAssignmentTable.class);
  
  protected static final String TABLE_CONFIG = 
    """
    userid I
    roleid I
    """;
  
  protected static final String INDEX_CONFIG = 
    """
    create index if not exists ixroleuserids on {[tablename]} (userid);
    """;
  
  public RoleAssignmentTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG, INDEX_CONFIG);
  }
  
  /**
   * Remove user-role assignment of a given user or role.
   * 
   * @param id
   * @param userOrRole true: id is an user id, false: id is a role id
   * @throws TammError 
   */
  public void clearRoleAssignments(int id, boolean userOrRole) throws TammError {
    String cmd;
    cmd = "DELETE FROM " + tableName + " WHERE " + ((userOrRole) ? "userid" : "roleid") + " = ? ";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setInt(col++, id);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error deleting role assignment data.", ex);
      throw new TammError("Error deleting role assignment data.");
    }
    
  }
  
  /**
   * Writes the role assignments of an user into the database.
   * 
   * There is no update, just inserts.
   * All existing roles will be removed before the new roles will be written.
   * 
   * @param roles
   * @throws TammError 
   */
  public void writeRoleAssignments(RoleAssignmentData roles) throws TammError {
    clearRoleAssignments(roles.user, true);
    
    String cmd = "INSERT INTO " + tableName + " (" + insertNames + ") values " + paramPlaceholders;
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        for (var roleid: roles.roles) {
          var col = 1;

          stmt.setInt(col++, roles.user);
          stmt.setInt(col++, roleid);

          stmt.execute();
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error writing role assignment data.", ex);
      throw new TammError("Error writing role assignment data.");
    }
  }
  
  /**
   * Returns an array of assigned roles of the given user.
   * 
   * @param userId
   * @return
   * @throws TammError 
   */
  public RoleAssignmentData listRoleAssignments(int userId) throws TammError {
    List<Integer> roleList = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE userid = ? ";    
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        stmt.setInt(1, userId);
        
        var rows = stmt.executeQuery();
        while (rows.next()) {
          var role = rows.getInt(2);
          roleList.add(role);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading roles list.", ex);
      throw new TammError("Error reading roles list.");
    }
    
    var result = new RoleAssignmentData();
    result.user = userId;
    result.roles = roleList.stream().mapToInt(i->i).toArray();
    
    return result;
  }
}
