/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.progress;

import org.apache.commons.mail.EmailException;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author matthias
 */
public class SendMailTest {
  
  public SendMailTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Test
  public void testSend() throws EmailException {
    System.out.println("send mail test");
    SendMail instance = new SendMail("smtp.strato.de", "tamm@auchmonoabspielbar.de", "missing password");
    String result = instance.send("NoReply@mmth.de", "matthias@auchmonoabspielbar.de", "Test", "das ist eine Testmail");
    assertEquals("Mailresult", "test", result);
  }
}
