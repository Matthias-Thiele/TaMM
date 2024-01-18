/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.TammError;
import de.mmth.tamm.db.DBConnect;
import java.sql.SQLException;

/**
 *
 * @author matthias
 */
public class DBUtils {
  
  /**
   * Delete all attachments of the given user.
   * 
   * @param conn
   * @param userId
   * @param taskTableName
   * @param attachmentTableName
   * @return
   * @throws TammError 
   */
  public static int deleteAttachmentsOfUser(DBConnect conn, int userId, String taskTableName, String attachmentTableName) throws TammError {
    String cmd;
    int result = 0;
    cmd = "DELETE FROM " + attachmentTableName + " where lid in (SELECT lid FROM " + taskTableName + " where owner = ?)";
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setInt(col++, userId);

        result = stmt.executeUpdate();
      }
    } catch (SQLException ex) {
      throw new TammError("Error deleting attachments.");
    }
    
    return result;
  }
  
}
