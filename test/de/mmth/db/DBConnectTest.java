/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
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
