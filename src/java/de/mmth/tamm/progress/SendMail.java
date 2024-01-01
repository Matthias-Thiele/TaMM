/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.progress;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;

/**
 * Helper class for sending password request mails.
 * 
 * @author matthias
 */
public class SendMail {

  private final String host;
  private final String user;
  private final String password;
  
  /**
   * Define SMTP server connection information.
   * 
   * @param host
   * @param user
   * @param password 
   */
  public SendMail(String host, String user, String password) {
    this.host = host;
    this.user = user;
    this.password = password;
  }
  
  /**
   * Send mail via SMTP server.
   * 
   * @param from
   * @param to
   * @param subject
   * @param message
   * @return
   * @throws EmailException 
   */
  public String send(String from, String to, String subject, String message) throws EmailException {
      MultiPartEmail email = new MultiPartEmail();
      email.setAuthenticator( new DefaultAuthenticator( user, password ) );
      email.setSSLOnConnect( true );
      email.setHostName(host);
      email.setFrom(from);
      email.addTo(to);
      email.setCharset(EmailConstants.UTF_8);
      email.setSubject(subject);
      email.setMsg(message);
      return email.send();
   }
    
}
