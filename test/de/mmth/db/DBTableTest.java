/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.db;

import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class DBTableTest {

  private static DBConnect con;
  
  @BeforeClass
  public static void setUpClass() {
    con = new DBConnect("jdbc:postgresql://localhost:5432/postgres", "test", "postgres", "postgres");
  }
  
  @AfterClass
  public static void tearDownClass() {
    con.dropDB("test");
    con.close();
  }

  /**
   * Test of the not so abstract base class for all postgres tables.
   * @throws SQLException 
   */
  @Test
  public void testTableCreation() throws SQLException {
    String tableDefinition = "col1 V 20\ncol2 I\ncol3 B";
    DBTable table1 = new DBTable(con, "testtable", tableDefinition);
    assertNotNull(table1);
    assertTrue("Table should be new.", table1.isNewTable());
    
    DBTable table2 = new DBTable(con, "testtable", tableDefinition);
    assertNotNull(table2);
    assertFalse("Table should exist.", table2.isNewTable());
    
    String extendedDefinition = tableDefinition + "\ncol4 V 200";
    DBTable table3 = new DBTable(con, "testtable", extendedDefinition);
    assertNotNull(table3);
    assertFalse("Table should exist.", table3.isNewTable());
    
    DBHelper.readLine(con.getConnection(), "select col1, col4 from testtable");
    // no exception - col4 exists
    
    try {
      DBHelper.readLine(con.getConnection(), "select thisisnotavalidcolumn from testtable");
      fail("Invalid column name should have raised an exception");
    } catch(SQLException ex) {
      // ok, exception was expected.
    }
  }
  
}
