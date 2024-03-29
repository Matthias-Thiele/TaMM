/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.data;

/**
 * Data object for the global system configuration.
 * 
 * @author matthias
 */
public class AdminData {
  // Database access
  public String dburl;
  public String name;
  public String password;
  
  // Filesystem storage
  public String uploadbase;
  public String loggerbase;
  
  // Mail access
  public String mailadminname;
  public String mailadminpwd;
  public String mailhost;
  public String mailreply;
  
  // System constants
  public int keepalivetime;
  public int mailsperdomainperday;
  public int mailsperday;
  public int loginretry;
  public int pwdreqvaildhours; 
}
