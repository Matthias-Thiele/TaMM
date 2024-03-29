/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.TammLogger;
import de.mmth.tamm.data.TaskData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class TaskTable extends DBTable {
  private static final Logger logger = TammLogger.prepareLogger(TaskTable.class);
  
  protected static final String TABLE_CONFIG = 
    """
    lid L
    clientid I
    name V 100
    description V 2000
    creator I
    createdate V 20
    lastchanged V 20
    owner I
    startdate V 20
    nextduedate V 20
    interval V 2000
    """;
  
  protected static final String INDEX_CONFIG = 
    """
    create unique index if not exists ixtaskids on tasklist (lid);
    create index if not exists ixduedate on tasklist (nextduedate);
    """;
  
  public TaskTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG, INDEX_CONFIG);
  }
  
  /**
   * Write task data into database.
   * 
   * Create a new Task with task.lId == -1. The lId
   * will be build from the nanoTime, this should
   * be pretty random.
   * 
   * @param task
   * @param insertAlways
   * @return returns task long Id
   * @throws TammError 
   */
  public long writeTask(TaskData task, boolean insertAlways) throws TammError {
    String cmd;
    if (task.lId == -1 || insertAlways) {
      cmd = "INSERT INTO " + tableName + " (" + insertNames + ") values " + paramPlaceholders;
      if (task.lId < 1) {
        task.lId = System.nanoTime();
      }
    } else {
      cmd = "UPDATE " + tableName + " SET " + updateNames + " WHERE lid = " + task.lId;
    }
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setLong(col++, task.lId);
        stmt.setInt(col++, task.clientId);
        stmt.setString(col++, task.name);
        stmt.setString(col++, task.description);
        stmt.setInt(col++, task.creator);
        stmt.setString(col++, task.createDate);
        stmt.setString(col++, task.lastChanged);
        stmt.setInt(col++, task.owner);
        stmt.setString(col++, task.startDate);
        stmt.setString(col++, task.nextDueDate);
        stmt.setString(col++, task.interval);

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
   * @param clientId
   * @param byId
   * @return
   * @throws TammError 
   */
  public TaskData readTask(int clientId, long byId) throws TammError {
    TaskData result = null;
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE clientId = ? and lid = ?";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        stmt.setInt(1, clientId);
        stmt.setLong(2, byId);
         
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
   * @param clientId
   * @param ownerId optional, -1: all tasks of all users
   * @param filter optional null: no filter
   * @param withRoleTasks
   * @return
   * @throws TammError 
   */
  public List<TaskData> listTasks(int clientId, int ownerId, String filter, boolean withRoleTasks) throws TammError {
    boolean hasId = ownerId != -1;
    boolean hasFilter = filter != null;
    
    List<TaskData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE clientid = ? ";
    
    if (hasId || hasFilter) {
      cmd += " AND ";
    }
    
    if (hasId) {
      if (withRoleTasks) {
        cmd += " owner in ( select ? union select roleid from roleassignments r  where r.userid = ? )";
      } else {
        cmd += "owner = ? ";
      }
      if (hasFilter) {
        cmd += "AND ";
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
        stmt.setInt(paramCol++, clientId);
        
        if (hasId) {
          stmt.setInt(paramCol++, ownerId);
          if (withRoleTasks) {
            stmt.setInt(paramCol++, ownerId);
          }
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
   * Read task history list of the given task.
   * 
   * @param clientId
   * @param taskId
   * @return
   * @throws TammError 
   */
  public List<TaskData> listTasks(int clientId, long taskId) throws TammError {
    List<TaskData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE clientid = ? ";
    if (taskId > 0) {
      cmd += "AND lid = ? ";
    }
    
    cmd += "ORDER BY lastchanged desc, createdate LIMIT 100";
    
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        int paramCol = 1;
        stmt.setInt(paramCol++, clientId);
        if (taskId > 0) {
          stmt.setLong(paramCol++, taskId);
        }
        
        var taskRows = stmt.executeQuery();
        while (taskRows.next()) {
          var task = getData(taskRows);
          logger.debug("Task found: " + task.startDate);
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
   * Copies the ResultSet task data into an TaskData object.
   * 
   * @param taskRows
   * @return
   * @throws SQLException 
   */
  private TaskData getData(ResultSet taskRows) throws SQLException {
    var result = new TaskData();
    
    int col = 1;
    result.lId = taskRows.getLong(col++);
    result.clientId = taskRows.getInt(col++);
    result.name = taskRows.getString(col++);
    result.description = taskRows.getString(col++);
    result.creator = taskRows.getInt(col++);
    result.createDate = taskRows.getString(col++);
    result.lastChanged = taskRows.getString(col++);
    result.owner = taskRows.getInt(col++);
    result.startDate = taskRows.getString(col++);
    result.nextDueDate = taskRows.getString(col++);
    result.interval = taskRows.getString(col++);
    
    return result;
  }

  /**
   * Removes task from database.
   * 
   * This call does not remove the attachment list, since
   * these attachments are also used by the history table.
   * 
   * @param taskData
   * @throws TammError 
   */
  public void removeTask(TaskData taskData) throws TammError {
    String cmd;
    cmd = "DELETE FROM " + tableName + " where lId = ?";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setLong(col++, taskData.lId);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error deleting task.", ex);
      throw new TammError("Error deleting task.");
    }
  }

  /**
   * Move all tasks of oldOwnerId to newOwnerId.
   * 
   * newOwnerId can be a role id.
   * 
   * @param clientId
   * @param oldOwnerId
   * @param newOwnerId
   * @return
   * @throws TammError 
   */
  public int moveTasksOwner(int clientId, int oldOwnerId, int newOwnerId) throws TammError {
    String cmd;
    cmd = "UPDATE " + tableName + " set owner = ? where owner = ? and clientid = ?";
    logger.debug("SQL: " + cmd);
    
    int count = 0;
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setInt(col++, newOwnerId);
        stmt.setInt(col++, oldOwnerId);
        stmt.setInt(col++, clientId);

        count = stmt.executeUpdate();
      }
    } catch (SQLException ex) {
      logger.warn("Error moving tasks.", ex);
      throw new TammError("Error moving tasks.");
    }
    
    return count;
  }

  /**
   * Delete all tasks of the given owner.
   * 
   * @param clientId
   * @param id
   * @throws TammError 
   */
  public void deleteTasksOfOwner(int clientId, int id) throws TammError {
    String cmd;
    cmd = "DELETE FROM " + tableName + " where owner = ? and clientid = ?";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setInt(col++, id);
        stmt.setInt(col++, clientId);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error deleting tasks of owner.", ex);
      throw new TammError("Error deleting tasks.");
    }
  }
  
}
