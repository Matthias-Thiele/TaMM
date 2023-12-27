/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.data;

import java.util.List;

/**
 * Session data.
 * 
 * @author matthias
 */
public class SessionData {
  public String clientIp;
  public String loginTime;
  public UserData user;
  public List<KeyValue> userNames;
  public ClientData client;
  public String clientName;

  // only for system admins, null otherwise  
  public List<KeyValue> clientList;

}
