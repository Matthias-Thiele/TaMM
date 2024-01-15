/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import java.io.File;
import java.util.prefs.Preferences;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class TammLogger {
  private static volatile boolean loggerPrepared = false;
  private static volatile String loggerDir;
  
  public static Logger prepareLogger(Class loggerForClass) {
    
    if (!loggerPrepared) {
      String tmpdir = java.lang.System.getProperty("java.io.tmpdir") + "/logs";
      java.lang.System.out.println("Tmp dir is " + tmpdir);
      var prefs = Preferences.userRoot().node("Tamm");
      loggerDir = prefs.get(ApplicationData.LOG_DIR, tmpdir);
      java.lang.System.out.println("Dir from prefs is " + loggerDir);
      File dir = new File(loggerDir);
      dir.mkdirs();

      java.lang.System.setProperty(ApplicationData.LOG_DIR, loggerDir);
      loggerPrepared = true;
    }
    
    Logger syslogger = LogManager.getLogger(loggerForClass);
    syslogger.info("Logger started.");
    java.lang.System.out.println("Logger started at " + loggerDir);
    
    return syslogger;
  }
  
  public static String getLoggerDir() {
    return loggerDir;
  }
  
}
