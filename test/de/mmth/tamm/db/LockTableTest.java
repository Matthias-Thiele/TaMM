/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.LockData;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class LockTableTest {
  
  private static DBConnect con;
  
  @BeforeClass
  public static void setUpClass() {
    con = new DBConnect("jdbc:postgresql://localhost:5432/postgres", "test", "postgres", "postgres");
  }
  
  @AfterClass
  public static void tearDownClass() {
    con.dropDB();
    con.close();
  }

  /**
   * Test if the number of columns has been changed
   * without changing the unit tests.
   */
  @Test
  public void testCheckColumns() {
    var cols = LockTable.TABLE_CONFIG.split("\\R");
    assertEquals("Number of columns changed", 4, cols.length);
  }
  
  /**
   * Test of write/ list/ delete methods, of class LockTable.
   * @throws TammError
   */
  @Test
  public void testLockTable() throws TammError {
    System.out.println("writeLock");
    LockTable instance = new LockTable(con, "testlocks");
    LockData lock = new LockData();
    lock.mailAddress = "someaddress@test.de";
    lock.lockDate = "20240102";
    lock.lockIP = "192.168.1.1";
    
    instance.writeLock(lock);
    
    List<LockData> result = instance.listLocks("%test.de");
    assertEquals("Just one item written.", 1, result.size());
    LockData data1 = result.get(0);
    assertEquals("Mailaddress mismatch", lock.mailAddress, data1.mailAddress);
    assertEquals("Lockdate mismatch", lock.lockDate, data1.lockDate);
    assertEquals("Lock IP address mismatch", lock.lockIP, data1.lockIP);
    assertEquals("No increment yet", 0, data1.lockCounter);
    
    instance.incrementLockCount(lock.mailAddress);
    List<LockData> result2 = instance.listLocks("%test.de");
    LockData data2 = result2.get(0);
    assertEquals("One increment yet", 1, data2.lockCounter);
    
    List<LockData> result3 = instance.listLocks("test.de");
    assertEquals("No match for this filter expected.", 0, result3.size());
    
    instance.removeLock(lock);
    List<LockData> result4 = instance.listLocks("%test.de");
    assertEquals("No match for deleted item expected.", 0, result4.size());
  }

  
}
