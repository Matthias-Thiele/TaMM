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
import de.mmth.tamm.db.DBTable;
import de.mmth.tamm.db.LockTable;
import de.mmth.tamm.db.RoleAssignmentTable;
import de.mmth.tamm.db.RoleTable;
import de.mmth.tamm.db.TaskTable;
import de.mmth.tamm.db.UserTable;
import de.mmth.tamm.progress.SendMail;
import de.mmth.tamm.utils.FileUtils;
import de.mmth.tamm.utils.InvalidAccessCache;
import de.mmth.tamm.utils.KeepAliveCache;
import de.mmth.tamm.utils.LimitSentMails;
import de.mmth.tamm.utils.Obfuscator;
import de.mmth.tamm.utils.Placeholder;
import de.mmth.tamm.utils.RequestCache;
import de.mmth.tamm.utils.TemplateCache;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

/**
 *
 * @author matthias
 */
public class ApplicationData {
  private static final org.apache.logging.log4j.Logger logger = TammLogger.prepareLogger(ApplicationData.class);
  protected static final String FILE_UPLOAD_BASE = "uploadbase";
  protected static final String LOG_DIR = "loggerdirectory";
  private static final String DB_URL = "dburl";
  private static final String DB_NAME = "dbname";
  private static final String DB_PASSWORD = "dbpassword";
  private static final String MAIL_HOST = "mailhost";
  private static final String MAIL_ADMIN = "mailadmin";
  private static final String MAIL_PWD = "mailpwd";
  private static final String MAIL_REPLY = "mailreply";
  private static final String CONST_KEEP_ALIVE = "keepalive";
  private static final String CONST_MAILS_PER_DOMAIN = "mailsperdomain";
  private static final String CONST_MAILS_PER_DAY = "mailsperday";
  private static final String CONST_LOGIN_RETRY = "loginretry";
  private static final String CONST_PWD_REQ_HOURS = "pwdreqvaildhours";
  
  private static final long DECAY_INTERVAL = 600000;
  private static final int CLEAR_MAIL_COUNTER_PERIOD = 1000 * 60 * 60 * 24; // one day
  
  private String schemaName;
  private String hostName;
  private String obfuscatorKey;
  private ClientData defaultClient = null;
  
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
  public File backupbase;

  public AttachmentTable attachments;
  public SendMail mailer = null;
  public TemplateCache templates = new TemplateCache("templates");
  public InvalidAccessCache accessCache = null;
  public LimitSentMails mailCounter = null;
  public KeepAliveCache keepAlive = new KeepAliveCache();
  
  public Placeholder placeholder = new Placeholder();
  public TaskReport taskReport;
  public CopyManager copyManager;
  private File restorebase;
  
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
    adminData.loggerbase = prefs.get(LOG_DIR, "/var/TaMM/logs");
    adminData.mailadminname = prefs.get(MAIL_ADMIN, "");
    adminData.mailadminpwd = prefs.get(MAIL_PWD, "");
    adminData.mailhost = prefs.get(MAIL_HOST, "");
    adminData.mailreply = prefs.get(MAIL_REPLY, "");
    adminData.keepalivetime = prefs.getInt(CONST_KEEP_ALIVE, 100);
    adminData.mailsperday = prefs.getInt(CONST_MAILS_PER_DAY, 300);
    adminData.mailsperdomainperday = prefs.getInt(CONST_MAILS_PER_DOMAIN, 100);
    adminData.loginretry = prefs.getInt(CONST_LOGIN_RETRY, 3);
    adminData.pwdreqvaildhours = prefs.getInt(CONST_PWD_REQ_HOURS, 24);
    
    var obf = prepareObfuscator();
    adminData.password = obf.decrypt(obfuscatorKey, hostName, adminData.password);
    adminData.mailadminpwd = obf.decrypt(obfuscatorKey, hostName, adminData.mailadminpwd);
    obfuscatorKey = "";
    
    if (!adminData.dburl.isBlank() && !adminData.name.isBlank() && !adminData.password.isBlank()) {
      DBConnect con = new DBConnect(adminData.dburl, schemaName, adminData.name, adminData.password);
      if (con.isValid()) {
        db = con;
        
        users = new UserTable(db, "userlist");
        tasks = new TaskTable(db, "tasklist");
        history = new TaskTable(db, "taskhistory");
        clients = new ClientTable(db, "clientlist");
        locks = new LockTable(db, "locklist");
        attachments = new AttachmentTable(db, "attachments");
        roles = new RoleTable(db, "roleslist");
        assignments = new RoleAssignmentTable(db, "roleassignments");
        mailCounter = new LimitSentMails(adminData.mailsperday, adminData.mailsperdomainperday, CLEAR_MAIL_COUNTER_PERIOD);
        accessCache = new InvalidAccessCache(adminData.loginretry, DECAY_INTERVAL);
        taskReport = new TaskReport(adminData.uploadbase);
        prepareCopyManager();
        
        if (!adminData.mailadminname.isBlank() && !adminData.mailadminpwd.isBlank() && !adminData.mailhost.isBlank()) {
          mailer = new SendMail(adminData.mailhost, adminData.mailadminname, adminData.mailadminpwd);
        }
        
        requests = new RequestCache();
        prepareUploadBase();
        
        if (rootPath != null) {
          File requestCache = new File(rootPath, "requestCache.lines");
          if (requestCache.exists()) {
            requests.load(requestCache.toPath());
          }
          
          File keepAliveCache = new File(rootPath, "keepAliveCache.lines");
          if (keepAliveCache.exists()) {
            keepAlive.load(keepAliveCache.toPath());
          }
          
          templates.setFileRoot(rootPath);
          checkRestore();
          
          users.assureAdminUser();
          clients.assureMainClient();
          refreshClientNames();
       }
      }
    }
    
    return db != null;
  }
  
  /**
   * Prepares the CopyManager for the backup function.
   * If this fails, only a warning in the log will be seen,
   * the application will not be stopped. Up to discussion...
   */
  private void prepareCopyManager() {
    try {
      copyManager = new CopyManager((BaseConnection)db.getConnection());
    } catch (SQLException ex) {
      logger.warn("Cannot prepare CopyManager, no backup available.", ex);
    }
    
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
      
      backupbase = new File(rootFile.getParentFile(), "backup");
      backupbase.mkdir();
      
      restorebase = new File(rootFile.getParentFile(), "restore");
      restorebase.mkdir();
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
   * Returns the client associated with the host name.
   * If not found, use default client.
   * If no default client exists, return null.
   * 
   * @param hostName
   * @return 
   */
  public ClientData getClient(String hostName) {
    logger.debug("GetClient for host " + hostName);
    ClientData client = clientNames.get(hostName);
    if (client == null) {
      client = defaultClient;
      if (client == null) {
        logger.debug("No default client available.");
      } else {
        logger.debug("No host found, use default client " + client.name);
      }
    }
    
    return client;
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
        
        if (c.hostName.equals("*") ||c.hostName2.equals("*") ||c.hostName3.equals("*")) {
          if (defaultClient != null) {
            logger.warn("More than one catch-all client defined: " + c.name + " - " + defaultClient.name);
          } else {
            defaultClient = c;
          }
        }
      }

      clientNames = localClientNames;
      for (var lcn: localClientNames.entrySet()) {
        logger.info("Client " + lcn.getValue().name + " as host " + lcn.getKey());
      }
      
      if (defaultClient == null) {
        logger.warn("No default client defined.");
      } else {
        logger.info("Default client: " + defaultClient.name);
      }
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
    data.password = obf.encrypt(obfuscatorKey, hostName, adminData.password);
    data.mailadminpwd = obf.encrypt(obfuscatorKey, hostName, adminData.mailadminpwd);
    obfuscatorKey = "";
    
    var prefs = Preferences.userRoot().node("Tamm");
    prefs.put(DB_URL, data.dburl);
    prefs.put(DB_NAME, data.name);
    prefs.put(DB_PASSWORD, data.password);
    prefs.put(FILE_UPLOAD_BASE, data.uploadbase);
    prefs.put(LOG_DIR, data.loggerbase);
    prefs.put(MAIL_ADMIN, data.mailadminname);
    prefs.put(MAIL_PWD, data.mailadminpwd);
    prefs.put(MAIL_HOST, data.mailhost);
    prefs.put(MAIL_REPLY, data.mailreply);
    prefs.putInt(CONST_KEEP_ALIVE, data.keepalivetime);
    prefs.putInt(CONST_MAILS_PER_DAY, data.mailsperday);
    prefs.putInt(CONST_MAILS_PER_DOMAIN, data.mailsperdomainperday);
    prefs.putInt(CONST_LOGIN_RETRY, data.loginretry);
    prefs.putInt(CONST_PWD_REQ_HOURS, data.pwdreqvaildhours);
  }

  void setSchema(String name) {
    schemaName = name;
  }
  
  /**
   * Checks the restore directory for ZIP files with restore data.
   * 
   * After processing the file it will be renamed with the
   * extension ".processed" - so further checks will ignore it.
   * 
   * After successful restore it should be removed manually.
   * For retries just remove the additional extension.
   */
  public void checkRestore() {
    if (restorebase.exists()) {
      File[] entries = restorebase.listFiles();
      if (entries != null) {
        for (var file: entries) {
          if (file.isFile() && FileUtils.hasExtension(file.getName(), "zip")) {
            try {
              doRestore(file);
              
              var renamedFile = new File(file.getParentFile(), file.getName() + ".processed");
              file.renameTo(renamedFile);
            } catch(IOException ex) {
              logger.warn("Restore error.", ex);
            }
          }
        }
      }
    }
  }
  
  /**
   * Tries to restore the given file into the database.
   * 
   * Each table has its own data file. On successful
   * restore operation the file will be deleted. If
   * all tables are restored successfully the unzip
   * directory will be deleted. Otherwise it remains
   * with the files of the not successfull restored
   * tables.
   * 
   * @param sourceData
   * @throws IOException 
   */
  private void doRestore(File sourceData) throws IOException {
    File unzipDir = FileUtils.unzipIntoDirectory(sourceData);
    if (unzipDir != null && unzipDir.exists()) {
      DBTable[] tables = {users, tasks, history, clients, locks, assignments, attachments, roles};
      for (var table: tables) {
        var file = table.restore(copyManager, unzipDir);
        if (file != null) {
          logger.debug("Table " + file.getName() + " restored, delete file.");
          file.delete();
        } else {
          logger.warn("Table " + table.getTableName() + " not restored, keep file.");
        }
      }
      
      unzipDir.delete();
    }
  }
}
