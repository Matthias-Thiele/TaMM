/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;



/**
 *
 * @author matthias
 */
public class DBConnectTest {
  
  @Test
  public void testDatabaseCreation() {
    DBConnect con = new DBConnect("jdbc:postgresql://localhost:5432/postgres", "test", "postgres", "postgres");
    assertTrue("Error creating/ connecting database", con.isValid());
    assertTrue("Error dropping database", con.dropDB("test"));
    assertNotNull("No JDBC handle available", con.getConnection());
    con.close();
  }
  
}
