/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.AdminData;
import de.mmth.tamm.data.ClientData;
import de.mmth.tamm.data.KeyValue;
import de.mmth.tamm.db.AttachmentTable;
import de.mmth.tamm.db.ClientTable;
import de.mmth.tamm.db.DBConnect;
import de.mmth.tamm.db.TaskTable;
import de.mmth.tamm.db.UserTable;
import de.mmth.tamm.utils.RequestCache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author matthias
 */
public class ApplicationData {
  private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ApplicationData.class);
  private static final String DB_URL = "dburl";
  private static final String DB_NAME = "dbname";
  private static final String DB_PASSWORD = "dbpassword";
  private static final String FILE_UPLOAD_BASE = "uploadbase";
  
  private String schemaName;
  private String dbUrl;
  private String dbAdmin;
  private String dbPassword;
  private String uploadBase;
  
  public DBConnect db;
  public UserTable users;
  public TaskTable tasks;
  public TaskTable history;
  public ClientTable clients;
  
  public RequestCache requests;
  public String tammUrl;
  public Map<String, List<KeyValue>> userNamesMap = new HashMap<>();
  public List<ClientData> clientList;
  public Map<String, ClientData> clientNames;
  public File rootPath;
  public AttachmentTable attachments;
  
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
    uploadBase = prefs.get(FILE_UPLOAD_BASE, "/var/TaMM/files");
    
    if (!dbUrl.isBlank() && !dbAdmin.isBlank() && !dbPassword.isBlank()) {
      DBConnect con = new DBConnect(dbUrl, schemaName, dbAdmin, dbPassword);
      if (con.isValid()) {
        db = con;
        
        users = new UserTable(db, "userlist");
        users.assureAdminUser();
        tasks = new TaskTable(db, "tasklist");
        history = new TaskTable(db, "taskhistory");
        clients = new ClientTable(db, "clientlist");
        attachments = new AttachmentTable(db, "attachments");
        refreshClientNames();
        requests = new RequestCache();
        prepareUploadBase();
      }
    }
    
    return db != null;
  }
  
  /**
   * Creates all directories for the root path of the upload base.
   */
  private void prepareUploadBase() {
    if ((uploadBase == null) || uploadBase.isBlank()) {
      rootPath = null;
      return;
    }
    
    File rootFile = new File(uploadBase);
    try {
      Files.createDirectories(rootFile.toPath());
      rootPath = rootFile;
    } catch (IOException ex) {
      logger.warn("Cannot create upload base directory. No file upload available.", ex);
      rootPath = null;
    }
  }
  
  /**
   * Creates a map with all client names as key.
   * 
   * A client can have up to three host names and can
   * be identified by each of them.
   */
  public void refreshClientNames() {
    try {
      clientList = clients.listClients();
      var localClientNames = new HashMap<String, ClientData>(clientList.size());
      for (var c: clientList) {
        localClientNames.put(c.hostName, c);
        if ((c.hostName2 != null) && !c.hostName2.isBlank()) {
          localClientNames.put(c.hostName2, c);
        }
        if ((c.hostName3 != null) && !c.hostName3.isBlank()) {
          localClientNames.put(c.hostName3, c);
        }
      }
      
      clientNames = localClientNames;
    } catch(TammError ex) {
      logger.warn("Cannot read client list.");
    }
    
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
    uploadBase = data.uploadbase;
    
    var prefs = Preferences.userRoot().node("Tamm");
    prefs.put(DB_URL, dbUrl);
    prefs.put(DB_NAME, dbAdmin);
    prefs.put(DB_PASSWORD, dbPassword);
    prefs.put(FILE_UPLOAD_BASE, uploadBase);
  }

  void setSchema(String name) {
    schemaName = name;
  }
}
