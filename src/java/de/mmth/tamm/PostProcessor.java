/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.LoginData;
import de.mmth.tamm.data.SessionData;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tomcat.jakartaee.commons.io.Charsets;

/**
 *
 * @author matthias
 */
public class PostProcessor {
  public void process(SessionData session, String uri, InputStream sourceData, OutputStream resultData) throws IOException {
    String[] uriParts = uri.split("/");
    try (Reader reader = new InputStreamReader(sourceData)) {
      switch (uriParts[3]) {
        case "login":
          LoginData loginData = new Gson().fromJson(reader, LoginData.class);
          if (loginData.name.equals("admin")) {
            try {
              resultData.write("{\"result\":\"ok\"}".getBytes(Charsets.UTF_8));
            } catch (IOException ex) {
              Logger.getLogger(PostProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
      }
    }
  }
}
