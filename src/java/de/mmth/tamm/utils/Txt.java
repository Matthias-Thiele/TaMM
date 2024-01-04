/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.TammError;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class Txt {
  private static final Logger logger = LogManager.getLogger(Txt.class);
  private static final Txt instance = new Txt();
  
  private final Map<String, Map<String, String>> allText = new HashMap<>();
  private String mainLanguage = null;
  private final Set<String> languages = new HashSet<>();
  
  public static String get(String language, String key) {
    return instance.getFromKey(language, key);
  }
  
  public static void addLanguageText(String language, InputStream is) throws IOException {
    instance.add(language, is);
  }
 
  public static void addLanguageText(File rootDirectory, String languages, String fileName) {
    try {
      instance.addLanguageTextFiles(rootDirectory, languages, fileName);
    } catch(Exception ex) {
      logger.warn("Error reading languages file: " + languages + " - " + fileName, ex);
    }
  }
  
  public static boolean hasLanguage(String language) {
    return instance.checkLanguage(language);
  }
  
  public static String getMainLanguage() {
    return instance.mainLanguage();
  }
  
  private Txt() {
  }

  private String mainLanguage() {
    return mainLanguage;
  }
  
  private boolean checkLanguage(String language) {
    if (language.length() > 2) {
      language = language.substring(0, 2);
    }
    
    return languages.contains(language);
  }
  
  private void addLanguageTextFiles(File rootDirectory, String languages, String fileName) throws FileNotFoundException, IOException, TammError {
    File translateDir = null;
    if (rootDirectory.exists()) {
      File translate = new File(rootDirectory, "translate");
      if (translate.exists()) {
        translateDir = translate;
      }
    }
    
    String[] langList = languages.split(" ");
    for (String lang: langList) {
      addOneLanguage(translateDir, lang, fileName);
    }
  }
  
  private void addOneLanguage(File translateDir, String language, String fileName) throws FileNotFoundException, IOException, TammError {
    fileName = language + "_" + fileName;
    languages.add(language);
    
    File file = new File(translateDir, fileName);
    if (file.exists()) {
      // if translation file exists - use this one
      try (InputStream stream = new FileInputStream(file)) {
        instance.add(language, stream);
      }
    } else {
      // check for internal resource
      try (InputStream stream = getClass().getClassLoader().getResourceAsStream("resources/translation/" + fileName)) {
        if (stream == null) {
          throw new TammError("Translation not found: " + language + " - " + fileName);
        }
        
        instance.add(language, stream);
      }
    }
  }
  
  private String getFromKey(String language, String key) {
    Map<String, String> text = allText.get(language);
    if (text == null) {
      text = allText.get(mainLanguage);
    }
    
    String msg = text.get(key);
    return (msg == null) ? "" : msg;
  }
  
  private void add(String language, InputStream is) throws IOException {
    if (mainLanguage == null) {
      mainLanguage = language;
    }
    
    Map<String, String> addHere = allText.get(language);
    if (addHere == null) {
      addHere = new HashMap<>();
      allText.put(language, addHere);
    }
    
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      for (;;) {
        String line = reader.readLine();
        if (line == null) {
          break;
        }

        int splitPos = line.indexOf('=');
        if ((splitPos > 0) && (splitPos < (line.length() - 1))) {
          String key = line.substring(0, splitPos);
          String value = line.substring(splitPos + 1);
          addHere.put(key, value);
        }
      }
    }
  }
  
 }
