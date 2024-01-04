/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class TxtTest {
  

  @Test
  public void testSomeMethod() throws IOException {
    String text_de = 
      """
      rechnung=Rechnung
      lieferdat=Lieferdatum
      """;      
            
    String text_en =
      """
      rechnung=Invoice
      lieferdat=Due date
      """;
            
            
            
    InputStream inputStreamDe = new ByteArrayInputStream(text_de.getBytes());
    Txt.addLanguageText("de", inputStreamDe);
    
    InputStream inputStreamEn = new ByteArrayInputStream(text_en.getBytes());
    Txt.addLanguageText("en", inputStreamEn);
    
    String invoice = Txt.get("de", "rechnung");
    assertEquals("Rechnung", invoice);
    invoice = Txt.get("en", "rechnung");
    assertEquals("Invoice", invoice);
    
    String duedate = Txt.get("de", "lieferdat");
    assertEquals("Lieferdatum", duedate);
    duedate = Txt.get("en", "lieferdat");
    assertEquals("Due date", duedate);
  }
  
}
