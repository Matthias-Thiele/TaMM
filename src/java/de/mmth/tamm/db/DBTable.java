/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm.db;

import de.mmth.tamm.TammLogger;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.Logger;

/**
 * Base class for all postgres tables.
 * 
 * Should be abstract but for unit testing purposes
 * it is not. Do not instantiate directly.
 * 
 * @author matthias
 */
public class DBTable {
  private static final Logger logger = TammLogger.prepareLogger(DBTable.class);
  protected boolean isNewTable = false;
  protected String tableName;
  
  protected String insertNames;
  protected String selectNames;
  protected String updateNames;
  protected String paramPlaceholders;
  
  final DBConnect conn;
  
  /**
   * Opens the given database table.
   * 
   * If the table does not exist, it will be created automatically.
   * All missing columns will be added automatically.
   * 
   * @param conn
   * @param tableName 
   * @param columns 
   * @param postprocessing 
   */
  public DBTable(DBConnect conn, String tableName, String columns, String postprocessing) {
    this.conn = conn;
    this.tableName = tableName;
    
    String checkCmd = "SELECT relname FROM pg_catalog.pg_class WHERE relname = '" + tableName + "' AND relkind = 'r'";
    String createCmd = "CREATE TABLE " + tableName + "()";
    logger.info("Check table cmd: " + checkCmd);
    
    try {
      try (java.sql.PreparedStatement tableInfo = conn.getConnection().prepareStatement(checkCmd)) {
        var result = tableInfo.executeQuery();
        if (!result.next()) {
          // table does not exist, create it now
          logger.warn("Table does not exist, create now: " + createCmd);
          try (java.sql.PreparedStatement createStmt = conn.getConnection().prepareStatement(createCmd)) {
            createStmt.execute();
            isNewTable = true;
          }
        }
      }
      
      buildColumns(columns);
      buildIndexes(postprocessing);
      
    } catch (SQLException ex) {
      logger.warn("Error opening table.", ex);
    }
  }
  
  /**
   * Indicates that the table has been created.
   * 
   * This information can be used for initialisation
   * of the table content.
   * 
   * @return 
   */
  public boolean isNewTable() {
    return isNewTable;
  }

  /**
   * Returns the database table name.
   * @return 
   */
  public String getTableName() {
    return tableName;
  }
  
  /**
   * Compares the existing columns with the table definition.
   * 
   * Missing columns will be created automatically. Changing
   * column types will not be recognized.
   * 
   * @param columns table definition
   */
  private void buildColumns(String columns) {
    var selectList = new StringBuilder();
    var insertList = new StringBuilder();
    var updateList = new StringBuilder();
    var paramList = new StringBuilder();
    
    var nameSet = getColumnNames();
    String[] cols = columns.split("\\R");
    for (String col: cols) {
      String[] parts = col.trim().split(" ");
      String name = parts[0];
      String type = parts[1];
      String value = "";
      if (parts.length > 2) {
        value = parts[2];
      }
      
      if (!nameSet.contains(name)) {
        addColumn(name, type, value);
      }
      
      if (!type.equals("I") || !value.startsWith("G")) {
        condAppend(insertList, ",");
        insertList.append(name);
        condAppend(paramList, ",");
        paramList.append("?");

        condAppend(updateList, ",");
        updateList.append(name);
        updateList.append("=?");
      }
      
      condAppend(selectList, ",");
      selectList.append(name);
    }
    
    insertList.append(" ");
    insertNames = " " + insertList.toString();
    logger.debug("InsertList: " + insertNames);
    
    selectList.append(" ");
    selectNames = " " + selectList.toString();
    logger.debug("SelectList: " + selectNames);
    
    updateList.append(" ");
    updateNames = " " + updateList.toString();
    logger.debug("UpdateList: " + updateNames);
    
    paramList.append(") ");
    paramPlaceholders = " (" + paramList.toString();
    logger.debug("ParamList: " + paramPlaceholders);
  }
  
  /**
   * Appends the given value to the list only if it is not empty.
   * @param list
   * @param value 
   */
  private void condAppend(StringBuilder list, String value) {
    if (!list.isEmpty()) {
      list.append(value);
    }
  }
  
  /**
   * Add a new column into the table.
   * 
   * @param parts column definition
   */
  private void addColumn(String name, String type, String value) {
    String sqlType = "";
    switch (type) {
      case "I":
        sqlType = "INTEGER";
        if (value.startsWith("G")) {
          sqlType += " GENERATED ALWAYS AS IDENTITY";
          if (value.length() > 1) {
            sqlType += " (START WITH " + value.substring(1) + " ) ";
          }
        }
        break;
    
      case "B":
        sqlType = "BOOLEAN";
        break;
        
      case "V":
        sqlType = "VARCHAR(" + value + ")";
        break;
        
      case "L":
        sqlType = "BIGINT";
        break;
    }
    
    String cmd = "ALTER TABLE " + tableName + " ADD COLUMN " + name + " " + sqlType;
    logger.info("Add column cmd: " + cmd);
    
    try {
      try (java.sql.Statement stmt = conn.getConnection().createStatement()) {
        stmt.execute(cmd);
      }
    } catch (SQLException ex) {
      logger.warn("Cannot add column.", ex);
    }
  }
  
  /**
   * Returns a set with all existing column names.
   * 
   * @return 
   */
  private Set<String> getColumnNames() {
    var result = new HashSet<String>();
    String cmd = "SELECT * from " + tableName + " LIMIT 1";
    try {
      try (java.sql.Statement stmt = conn.getConnection().createStatement()) {
        var data = stmt.executeQuery(cmd);
        var md = data.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
          result.add(md.getColumnName(i));
        }
      }
    } catch (SQLException ex) {
      logger.warn("Cannot read metadata.", ex);
    }
    
    return result;
  }

  private void buildIndexes(String postprocessing) {
    if (postprocessing.isEmpty()) {
      logger.debug("No index information supplied, nothing to do.");
      return;
    }
    
    postprocessing = postprocessing.replace("{[tablename]}", tableName);
    logger.info("buildIndexes cmd: " + postprocessing);
    
    try {
      try (java.sql.Statement stmt = conn.getConnection().createStatement()) {
        stmt.execute(postprocessing);
      }
    } catch (SQLException ex) {
      logger.warn("Cannot add indexes.", ex);
    }
  }
}
