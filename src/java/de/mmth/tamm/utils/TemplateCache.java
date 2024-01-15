/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.TammError;
import de.mmth.tamm.TammLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author matthias
 */
public class TemplateCache {
  private static final org.apache.logging.log4j.Logger logger = TammLogger.prepareLogger(TemplateCache.class);
  private final Map<String, String> templates = new HashMap<>();
  private final String templateDirName;
  private File templateRoot = null;
  
  
  /**
   * Constructor with the name of the template
   * directory within the TaMM filesystem storage.
   * 
   * @param templateDirName 
   */
  public TemplateCache(String templateDirName) {
    this.templateDirName = templateDirName;
  }
  
  /**
   * Sets the root directory of the TaMM
   * filesystem storage.
   * 
   * @param rootDir 
   */
  public void setFileRoot(File rootDir) {
    if (rootDir.exists()) {
      templateRoot = new File(rootDir, templateDirName);
      if (!templateRoot.exists()) {
        templateRoot.mkdir();
        if (!templateRoot.exists()) {
          logger.warn("Cannot open or create template root directory: " + templateRoot.getPath());
          templateRoot = null;
        }
      }
    }
  }
  
  /**
   * Clears the template cache and return the
   * number of stored entries.
   * 
   * @return 
   */
  public int clear() {
    int count = templates.size();
    templates.clear();
    
    return count;
  }
  
  /**
   * Read one entry from the template cache.
   * 
   * If the template is not in the cache, at first
   * it will search the filesystem.
   * 
   * If it is not in the filesystem, second search
   * in the internal resources.
   * 
   * If not found there then throw an exception.
   * 
   * @param templateName
   * @return
   * @throws TammError 
   */
  public String getTemplate(String templateName) throws TammError {
    // first try: lookup cache
    String text = templates.get(templateName);
    if (text != null) {
      return text;
    }
    
    // second try: check template directory
    File templateFile = lookupTemplateRoot(templateName);
    if (templateFile != null) {
      try {
        // use template file
        String fileText = Files.readString(templateFile.toPath(), StandardCharsets.UTF_8);
        templates.put(templateName, fileText);
        return fileText;
      } catch (IOException ex) {
        logger.warn("Error reading template file.", ex);
        throw new TammError("Error reading template file.");
      }
    }
    
    // last try: check program resources
    InputStream is = getClass().getClassLoader().getResourceAsStream("resources/" + templateName);
    if (is == null) {
      throw new TammError("Template not found: " + templateName);
    }
    
    try {
      String resourceText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      templates.put(templateName, resourceText);
      return resourceText;
    } catch (IOException ex) {
        logger.warn("Error reading template resource.", ex);
        throw new TammError("Error reading template resource.");
    }
  }
  
  /**
   * Builds the filesystem path of the
   * requested item and checks if it
   * is available.
   * 
   * @param templateName
   * @return 
   */
  private File lookupTemplateRoot(String templateName) {
    if (templateName.contains("..") || templateName.startsWith(File.separator)) {
      // no directory traversal
      return null;
    }
    
    if (templateRoot == null) {
      // no template directory defined
      return null;
    }
    
    File result = new File(templateRoot, templateName);
    if (result.exists()) {
      return result;
    } else {
      return null;
    }
  }
}
