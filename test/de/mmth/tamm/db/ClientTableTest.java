/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.ClientData;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class ClientTableTest {

  private static DBConnect con;
  
  public ClientTableTest() {
  }
  
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
    String[] cols = ClientTable.TABLE_CONFIG.split("\\R");
    assertEquals("Number of columns changed", 5, cols.length);
  }
  
  /**
   * Test of readClient, listClients and writeClient methods, of class ClientTable.
   */
  @Test
  public void testWriteReadClient() throws TammError {
    System.out.println("writeReadListClient");
    ClientTable instance = new ClientTable(con, "testclients");
    
    ClientData client = new ClientData();
    client.id = -1;
    client.name = "Test client 1";
    client.hostName = "testc1";
    client.maxDocMB = 100;
    client.maxUser = 10;
    
    instance.writeClient(client);
    
    List<ClientData> clients = instance.listClients();
    assertEquals("One client expected.", 1, clients.size());
    int id = clients.get(0).id;
    
    ClientData client2 = instance.readClient(id);
    
    assertEquals("Client name mismatch", client.name, client2.name);
    assertEquals("Host mismatch", client.hostName, client2.hostName);
    assertEquals("Max document storage size mismatch", client.maxDocMB, client2.maxDocMB);
    assertEquals("Max user count mismatch", client.maxUser, client2.maxUser);
    
    ClientData client3 = new ClientData();
    client3.id = -1;
    client3.name = "Test client 2";
    client3.hostName = "testc2";
    client3.maxDocMB = 0;
    client3.maxUser = 10;
    instance.writeClient(client3);
    
    clients = instance.listClients(); // order by name
    assertEquals("Two clients expected.", 2, clients.size());
    assertEquals("First client host name mismatch", "testc1", clients.get(0).hostName);
    assertEquals("Second client host name mismatch", "testc2", clients.get(1).hostName);
    
    client.id = id;
    client.name = "ZTest client 1";
    instance.writeClient(client);
    
    clients = instance.listClients(); // order by name
    assertEquals("Two clients expected.", 2, clients.size());
    assertEquals("First client host name mismatch", "testc2", clients.get(0).hostName);
    assertEquals("Second client host name mismatch", "testc1", clients.get(1).hostName);
  }
  
}
