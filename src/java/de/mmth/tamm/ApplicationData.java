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
import de.mmth.tamm.db.LockTable;
import de.mmth.tamm.db.RoleAssignmentTable;
import de.mmth.tamm.db.RoleTable;
import de.mmth.tamm.db.TaskTable;
import de.mmth.tamm.db.UserTable;
import de.mmth.tamm.progress.SendMail;
import de.mmth.tamm.utils.InvalidAccessCache;
import de.mmth.tamm.utils.Obfuscator;
import de.mmth.tamm.utils.Placeholder;
import de.mmth.tamm.utils.RequestCache;
import de.mmth.tamm.utils.TemplateCache;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
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
  public static final String FILE_UPLOAD_BASE = "uploadbase";
  private static final String MAIL_HOST = "mailhost";
  private static final String MAIL_ADMIN = "mailadmin";
  private static final String MAIL_PWD = "mailpwd";
  private static final String MAIL_REPLY = "mailreply";
  
  private static final int MAX_RETRIES = 3;
  private static final long DECAY_INTERVAL = 600000;
  
  private String schemaName;
  private String hostName;
  private String obfuscatorKey;
  public AdminData adminData = new AdminData();
  
  public DBConnect db;
  public UserTable users;
  public TaskTable tasks;
  public TaskTable history;
  public ClientTable clients;
  public LockTable locks;
  public RoleTable roles;
  public RoleAssignmentTable assignments;
  
  public RequestCache requests;
  public String tammUrl;
  public Map<String, List<KeyValue>> userNamesMap = new HashMap<>();
  public Map<String, List<KeyValue>> roleNamesMap = new HashMap<>();
  public List<ClientData> clientList;
  public Map<String, ClientData> clientNames;
  public File rootPath;
  public AttachmentTable attachments;
  public SendMail mailer = null;
  public TemplateCache templates = new TemplateCache("templates");
  public InvalidAccessCache accessCache = new InvalidAccessCache(MAX_RETRIES, DECAY_INTERVAL);
  public Placeholder placeholder = new Placeholder();
  
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
    adminData.dburl = prefs.get(DB_URL, "");
    adminData.name = prefs.get(DB_NAME, "");
    adminData.password = prefs.get(DB_PASSWORD, "");
    adminData.uploadbase = prefs.get(FILE_UPLOAD_BASE, "/var/TaMM/files");
    adminData.mailadminname = prefs.get(MAIL_ADMIN, "");
    adminData.mailadminpwd = prefs.get(MAIL_PWD, "");
    adminData.mailhost = prefs.get(MAIL_HOST, "");
    adminData.mailreply = prefs.get(MAIL_REPLY, "");
    
    var obf = prepareObfuscator();
    adminData.password = obf.decrypt(obfuscatorKey, hostName, adminData.password);
    adminData.mailadminpwd = obf.decrypt(obfuscatorKey, hostName, adminData.mailadminpwd);
    obfuscatorKey = "";
    
    if (!adminData.dburl.isBlank() && !adminData.name.isBlank() && !adminData.password.isBlank()) {
      DBConnect con = new DBConnect(adminData.dburl, schemaName, adminData.name, adminData.password);
      if (con.isValid()) {
        db = con;
        
        users = new UserTable(db, "userlist");
        users.assureAdminUser();
        tasks = new TaskTable(db, "tasklist");
        history = new TaskTable(db, "taskhistory");
        clients = new ClientTable(db, "clientlist");
        locks = new LockTable(db, "locklist");
        attachments = new AttachmentTable(db, "attachments");
        roles = new RoleTable(db, "roleslist");
        assignments = new RoleAssignmentTable(db, "roleassignments");
        
        if (!adminData.mailadminname.isBlank() && !adminData.mailadminpwd.isBlank() && !adminData.mailhost.isBlank()) {
          mailer = new SendMail(adminData.mailhost, adminData.mailadminname, adminData.mailadminpwd);
        }
        
        refreshClientNames();
        requests = new RequestCache();
        prepareUploadBase();
        
        if (rootPath != null) {
          File destination = new File(rootPath, "requestCache.lines");
          if (destination.exists()) {
            requests.load(destination.toPath());
          }
          templates.setFileRoot(rootPath);
        }
      }
    }
    
    return db != null;
  }
  
  /**
   * Creates all directories for the root path of the upload base.
   */
  private void prepareUploadBase() {
    if ((adminData.uploadbase == null) || adminData.uploadbase.isBlank()) {
      rootPath = null;
      return;
    }
    
    File rootFile = new File(adminData.uploadbase);
    try {
      Files.createDirectories(rootFile.toPath());
      rootPath = rootFile;
    } catch (IOException ex) {
      logger.warn("Cannot create upload base directory. No file upload available.", ex);
      rootPath = null;
    }
  }
  
  private Obfuscator prepareObfuscator() {
    byte[] iv = {(byte)0x53, (byte)0xaa, (byte)0x67, (byte)0x31, (byte)0x5a, (byte)0x1d, (byte)0x25, (byte)0x77,
                 (byte)0x79, (byte)0x40, (byte)0x36, (byte)0x01, (byte)0x2c, (byte)0x55, (byte)0x6f, (byte)0x18};
    obfuscatorKey = new String(iv);
    
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException ex) {
      hostName = "Unknown host name.";
    }
    
    return new Obfuscator();
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
   * @param data 
   */
  public void setAdminData(AdminData data) {
    adminData = data;
    
    var obf = prepareObfuscator();
    adminData.password = obf.encrypt(obfuscatorKey, hostName, adminData.password);
    adminData.mailadminpwd = obf.encrypt(obfuscatorKey, hostName, adminData.mailadminpwd);
    obfuscatorKey = "";
    
    var prefs = Preferences.userRoot().node("Tamm");
    prefs.put(DB_URL, data.dburl);
    prefs.put(DB_NAME, data.name);
    prefs.put(DB_PASSWORD, data.password);
    prefs.put(FILE_UPLOAD_BASE, data.uploadbase);
    prefs.put(MAIL_ADMIN, data.mailadminname);
    prefs.put(MAIL_PWD, data.mailadminpwd);
    prefs.put(MAIL_HOST, data.mailhost);
    prefs.put(MAIL_REPLY, data.mailreply);
  }

  void setSchema(String name) {
    schemaName = name;
  }
}
