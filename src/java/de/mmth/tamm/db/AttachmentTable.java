/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.AttachmentData;
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
public class AttachmentTable extends DBTable {
  private static final Logger logger = LogManager.getLogger(AttachmentTable.class);
  
  protected static final String TABLE_CONFIG = 
    """
    lid L
    clientid I
    name V 100
    guid V 40
    """;
  
  /**
   *
   * @param conn
   * @param tableName
   */
  public AttachmentTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG);
  }
  
  /**
   * Write attachment data into database.
   * 
   * Only insert, there is no update for attachments.
   * 
   * @param attachment
   * @throws TammError 
   */
  public void writeAttachment(AttachmentData attachment) throws TammError {
    String cmd;
    cmd = "INSERT INTO " + tableName + " (" + insertNames + ") values " + paramPlaceholders;
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setLong(col++, attachment.taskId);
        stmt.setInt(col++, attachment.clientId);
        stmt.setString(col++, attachment.fileName);
        stmt.setString(col++, attachment.guid);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error writing attachment data.", ex);
      throw new TammError("Error writing attachment data.");
    }
  }

  /**
   * Remove attachment from the attachment table.
   * 
   * This function does not delete the file. This has
   * to be done by the caller after successful deletion.
   * 
   * @param attachment
   * @throws TammError 
   */
  public void removeAttachment(AttachmentData attachment) throws TammError {
    String cmd;
    cmd = "DELETE FROM " + tableName + " where clientid = ? and guid = ? ";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setInt(col++, attachment.clientId);
        stmt.setString(col++, attachment.guid);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error deleting attachment data.", ex);
      throw new TammError("Error deleting attachment data.");
    }
  }

  /**
   * Returns a list of tasks of the given user matching the filter.
   * 
   * The filter matches the name part of the task.
   * 
   * @param clientId
   * @param taskId
   * @return
   * @throws TammError 
   */
  public List<AttachmentData> listAttachments(int clientId, long taskId) throws TammError {
    List<AttachmentData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE clientid = ? and lid = ? ORDER BY name";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        int paramCol = 1;
        stmt.setInt(paramCol++, clientId);
        stmt.setLong(paramCol++, taskId);

        var rows = stmt.executeQuery();
        while (rows.next()) {
          var attachment = getData(rows);
          logger.debug("Attachment found: " + attachment.fileName);
          result.add(attachment);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading attachment list.", ex);
      throw new TammError("Error reading attachment list.");
    }
    return result;
  }
  
  /**
   * Copies the ResultSet attachment data into an AttachmentData object.
   * 
   * @param rows
   * @return
   * @throws SQLException 
   */
  private AttachmentData getData(ResultSet rows) throws SQLException {
    var result = new AttachmentData();
    
    int col = 1;
    result.taskId = rows.getLong(col++);
    result.clientId = rows.getInt(col++);
    result.fileName = rows.getString(col++);
    result.guid = rows.getString(col++);
    
    return result;
  }
  
}
