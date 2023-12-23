/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.TaskData;
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
public class TaskTable extends DBTable {
  private static final Logger logger = LogManager.getLogger(TaskTable.class);
  
  protected static final String TABLE_CONFIG = 
    """
    lid L
    name V 100
    description V 2000
    creator I
    createdate V 20
    lastchanged V 20
    owner I
    startdate V 20
    nextduedate V 20
    """;
  
  public TaskTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG);
  }
  
  /**
   * Write task data into database.
   * 
   * Create a new Task with task.lId == -1. The lId
   * will be build from the nanoTime, this should
   * be pretty random.
   * 
   * @param task
   * @return returns task long Id
   * @throws TammError 
   */
  public long writeTask(TaskData task) throws TammError {
    String cmd;
    if (task.lId == -1) {
      cmd = "INSERT INTO " + tableName + " (" + insertNames + ") values " + paramPlaceholders;
      task.lId = System.nanoTime();
    } else {
      cmd = "UPDATE " + tableName + " SET " + updateNames + " WHERE lid = " + task.lId;
    }
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setLong(col++, task.lId);
        stmt.setString(col++, task.name);
        stmt.setString(col++, task.description);
        stmt.setInt(col++, task.creator);
        stmt.setString(col++, task.createDate);
        stmt.setString(col++, task.lastChanged);
        stmt.setInt(col++, task.owner);
        stmt.setString(col++, task.startDate);
        stmt.setString(col++, task.nextDueDate);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error writing user data.", ex);
      throw new TammError("Error writing user data.");
    }
    
    return task.lId;
  }
  
  /**
   * Read a task object by id.
   * 
   * @param byId
   * @return
   * @throws TammError 
   */
  public TaskData readTask(long byId) throws TammError {
    TaskData result = null;
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE lid = ?";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        stmt.setLong(1, byId);
         
        var taskRows = stmt.executeQuery();
        if (!taskRows.next()) {
          logger.info("Task not found: " + byId);
          throw new TammError("Task not found.");
        }
        
        result = getData(taskRows);
      }
    } catch (SQLException ex) {
      logger.warn("Error writing user data.", ex);
      throw new TammError("Error writing user data.");
    }
    return result;
  }

  /**
   * Returns a list of tasks of the given user matching the filter.
   * 
   * The filter matches the name part of the task.
   * 
   * @param ownerId optional, -1: all tasks of all users
   * @param filter optional null: no filter
   * @return
   * @throws TammError 
   */
  public List<TaskData> listTasks(int ownerId, String filter) throws TammError {
    boolean hasId = ownerId != -1;
    boolean hasFilter = filter != null;
    
    List<TaskData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName;
    
    if (hasId || hasFilter) {
      cmd += " WHERE ";
    }
    
    if (hasId) {
      cmd += "owner = ? ";
      if (hasFilter) {
        cmd += "and ";
      }
    }
    
    if (hasFilter) {
      cmd += "( name ILIKE ? ) ";
    }
    
    cmd += " ORDER BY nextduedate, createdate";
    
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        int paramCol = 1;
        if (hasId) {
          stmt.setInt(paramCol++, ownerId);
        }
        
        if (hasFilter) {
          stmt.setString(paramCol++, filter);
        }
        
        var taskRows = stmt.executeQuery();
        while (taskRows.next()) {
          var task = getData(taskRows);
          logger.debug("Task found: " + task.name);
          result.add(task);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading task list.", ex);
      throw new TammError("Error reading task list.");
    }
    return result;
  }
  
  /**
   * Copies the ResultSet user data into an TaskData object.
   * 
   * @param taskRows
   * @return
   * @throws SQLException 
   */
  private TaskData getData(ResultSet taskRows) throws SQLException {
    var result = new TaskData();
    
    int col = 1;
    result.lId = taskRows.getLong(col++);
    result.name = taskRows.getString(col++);
    result.description = taskRows.getString(col++);
    result.creator = taskRows.getInt(col++);
    result.createDate = taskRows.getString(col++);
    result.lastChanged = taskRows.getString(col++);
    result.owner = taskRows.getInt(col++);
    result.startDate = taskRows.getString(col++);
    result.nextDueDate = taskRows.getString(col++);
    
    return result;
  }

}