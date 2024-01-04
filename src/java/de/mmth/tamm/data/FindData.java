/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.data;

/**
 * Used by html Filter or Search commands.
 * 
 * @author matthias
 */
public class FindData {
  public String source;
  public String filterText;
  public int userId;
  public boolean onlyAdmins;
  public boolean withRoleTasks;
}
