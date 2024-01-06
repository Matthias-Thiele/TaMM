/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm.db;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class DBConnect {
  
  private static final Logger logger = LogManager.getLogger(DBConnect.class);
  private Connection conn;
  private String dbName;
  private boolean isValid;
  
  /**
   * Creates a connection to the given database.
   * 
   * @param url jdbc connection string
   * @param dbName schema name/ database name
   * @param userName user name
   * @param password user password
   */
  public DBConnect(String url, String dbName, String userName, String password) {
    isValid = false;
    try {
      logger.info("Start connecting to database " + dbName);
      try {
        conn = DriverManager.getConnection(url, userName, password);
      } catch(SQLException ex2) {
        // for some reason the first try always fails in Tomcat.
        conn = DriverManager.getConnection(url, userName, password);        
      }
      this.dbName = dbName;
      
      if (conn != null) {
        logger.info("Connected");
        checkCreateDB(dbName);
      }
    } catch (SQLException ex) {
      logger.warn("Error connecting database.", ex);
      isValid = false;
    }    
  }
  
  /**
   * Closes the database connection.
   */
  public void close() {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException ex) {
        logger.warn("Error closing db connection.", ex);
      }
    }
  }
  
  /**
   * Indicates if the connection has been established.
   * 
   * @return 
   */
  public boolean isValid() {
    return isValid;
  }
  
  /**
   * Returns the JDBC Connection handle.
   * @return 
   */
  public Connection getConnection() {
    return conn;
  }
  
  /**
   * Drops the given database.
   * 
   * Be careful, all data and all tables will be lost!
   * 
   * @return true if successful deleted
   */
  public boolean dropDB() {
    String dropCmd = "DROP SCHEMA " + dbName + " CASCADE";
    try {
      try (java.sql.PreparedStatement dropSchema = conn.prepareStatement(dropCmd)) {
        dropSchema.execute();
        logger.info("Database dropped: " + dbName);
        return true;
      }
    } catch (SQLException ex) {
      logger.warn("Error dropping database.", ex);
    }
    
    return false;
  }
  
  /**
   * Checks if a database with the given name exists and creates it if needed.
   * 
   * @param dbName 
   */
  private void checkCreateDB(String dbName) {
    String listCmd = "SELECT schema_name FROM information_schema.schemata where schema_name = ?;";
    String createCmd = "CREATE SCHEMA " + dbName;
    String selectCmd = "SET search_path TO " + dbName;
    
    try {
      try (java.sql.PreparedStatement stmt = conn.prepareStatement(listCmd)) {
        stmt.setString(1, dbName);
        var result = stmt.executeQuery();
        if (result.next()) {
          logger.info("Use existing database.");
          isValid = true;
        } else {
          logger.info("Create new database.");
          try (java.sql.PreparedStatement createSchema = conn.prepareStatement(createCmd)) {
            createSchema.execute();
            logger.info("Database created.");
            isValid = true;
          }
        }
        
        try (java.sql.PreparedStatement selectSchema = conn.prepareStatement(selectCmd)) {
          selectSchema.execute();
          logger.info("Database selected.");
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error creating database.", ex);
    }
  }
}
