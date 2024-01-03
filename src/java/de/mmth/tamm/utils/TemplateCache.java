/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.TammError;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author matthias
 */
public class TemplateCache {
  private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(TemplateCache.class);
  private final Map<String, String> templates = new HashMap<>();
  private final String templateDirName;
  private File templateRoot = null;
  
  public TemplateCache(String templateDirName) {
    this.templateDirName = templateDirName;
  }
  
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
  
  public void clear() {
    templates.clear();
  }
  
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
