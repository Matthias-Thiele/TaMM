/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm.data;

/**
 * Data object for the user data table.
 * 
 * @author matthias
 */
public class UserData {
  private final static int FG_ADMIN = 1;
  private final static int FG_SUBADMIN = 2;
  
  public int id = -1;
  public String name;
  public String pwd;
  public String mail;
  
  public boolean mainAdmin;
  public boolean subAdmin;
  
  public int supervisorId;
  public int administratorId;
  
  /**
   * Split the integer flag set into booleans.
   * 
   * @return 
   */
  public int getFlags() {
    int flags = 0;
    if (mainAdmin) {
      flags |= FG_ADMIN;
    }
    
    if (subAdmin) {
      flags |= FG_SUBADMIN;
    }
    
    return flags;
  }
  
  /**
   * Combines the user data boolean flags into 
   * an integer flag set for the database.
   * 
   * @param flags 
   */
  public void setFlags(int flags) {
    mainAdmin = (flags & FG_ADMIN) != 0;
    subAdmin = (flags & FG_SUBADMIN) != 0;
  }
}
