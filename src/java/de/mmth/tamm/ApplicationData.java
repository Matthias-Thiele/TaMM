/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.AdminData;
import de.mmth.tamm.db.DBConnect;
import de.mmth.tamm.db.UserTable;
import de.mmth.tamm.utils.RequestCache;
import java.util.prefs.Preferences;

/**
 *
 * @author matthias
 */
public class ApplicationData {
  private static final String DB_URL = "dburl";
  private static final String DB_NAME = "dbname";
  private static final String DB_PASSWORD = "dbpassword";
  
  private String schemaName;
  private String dbUrl;
  private String dbAdmin;
  private String dbPassword;
  
  public DBConnect db;
  public UserTable users;
  public RequestCache requests;
  public String tammUrl;
  
  /**
   * Reads the database access information from the registry
   * and tries to connect.
   * 
   * @return 
   */
  public boolean checkInit() {
    if (db != null && db.isValid()) {
      return true;
    }
    
    var prefs = Preferences.userRoot().node("Tamm");
    dbUrl = prefs.get(DB_URL, "");
    dbAdmin = prefs.get(DB_NAME, "");
    dbPassword = prefs.get(DB_PASSWORD, "");
    
    if (!dbUrl.isBlank() && !dbAdmin.isBlank() && !dbPassword.isBlank()) {
      DBConnect con = new DBConnect(dbUrl, schemaName, dbAdmin, dbPassword);
      if (con.isValid()) {
        db = con;
        
        users = new UserTable(db, "userlist");
        requests = new RequestCache();
      }
    }
    
    return db != null;
  }
  
  /**
   * Stores the database access information into the registry.
   * 
   * TODO: encrypt password.
   * 
   * @param data 
   */
  public void setAdminData(AdminData data) {
    dbUrl = data.dburl;
    dbAdmin = data.name;
    dbPassword = data.password;
    
    var prefs = Preferences.userRoot().node("Tamm");
    prefs.put(DB_URL, dbUrl);
    prefs.put(DB_NAME, dbAdmin);
    prefs.put(DB_PASSWORD, dbPassword);
  }

  void setSchema(String name) {
    schemaName = name;
  }
}
