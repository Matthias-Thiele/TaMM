/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.TammLogger;
import de.mmth.tamm.data.ClientData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public final class ClientTable extends DBTable {
  private static final Logger logger = TammLogger.prepareLogger(ClientTable.class);
  
  protected static final String TABLE_CONFIG = 
    """
    id I G
    name V 100
    hostname V 20
    hostname2 V 20
    hostname3 V 20
    maxdocmb I
    maxuser I
    """;
  
  protected static final String INDEX_CONFIG = 
    """
    create unique index if not exists ixclientids on {[tablename]} (id);
    """;
  
  /**
   * Open the clients table and create it if needed.
   * 
   * @param conn
   * @param tableName 
   */
  public ClientTable(DBConnect conn, String tableName) {
    super(conn, tableName, TABLE_CONFIG, INDEX_CONFIG);
  }
  
  /**
   * Make sure that at least one Client is available.
   * 
   * The default client matches all hosts.
   */
  public void assureMainClient() {
    if (getRowCount() == 0) {
      ClientData defaultClient = new ClientData();
      defaultClient.id = -1;
      defaultClient.name = "TaMM";
      defaultClient.hostName = "*";
      defaultClient.maxDocMB = 100;
      defaultClient.maxUser = 100;
      try {
        writeClient(defaultClient);
      } catch (TammError ex) {
        logger.warn("Cannot create default client.", ex);
      }
    }
  }
  
  /**
   * Read a client by id.
   * 
   * @param byId
   * @return
   * @throws TammError 
   */
  
  public ClientData readClient(int byId) throws TammError {
    ClientData result = null;
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " WHERE id = ?";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        stmt.setInt(1, byId);
        
        var clientRows = stmt.executeQuery();
        if (!clientRows.next()) {
          logger.info("Client not found: " + byId);
          throw new TammError("Client not found.");
        }
        
        result = getData(clientRows);
      }
    } catch (SQLException ex) {
      logger.warn("Error reading client data.", ex);
      throw new TammError("Error reading client data.");
    }
    return result;
  }
  
  /**
   * Write client data into database.
   * 
   * Create a new Client with client.id == -1
   * 
   * @param client
   * @throws TammError 
   */
  public void writeClient(ClientData client) throws TammError {
    String cmd;
    if (client.id == -1) {
      cmd = "INSERT INTO " + tableName + " (" + insertNames + ") values " + paramPlaceholders;
    } else {
      cmd = "UPDATE " + tableName + " SET " + updateNames + " WHERE id = " + client.id;
    }
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var col = 1;
        stmt.setString(col++, client.name);
        stmt.setString(col++, client.hostName);
        stmt.setString(col++, client.hostName2);
        stmt.setString(col++, client.hostName3);
        stmt.setInt(col++, client.maxDocMB);
        stmt.setInt(col++, client.maxUser);

        stmt.execute();
      }
    } catch (SQLException ex) {
      logger.warn("Error writing client data.", ex);
      throw new TammError("Error writing client data.");
    }
  }
  
  /**
   * Lists all clients.
   * 
   * @return
   * @throws TammError 
   */
  public List<ClientData> listClients() throws TammError {
    List<ClientData> result = new ArrayList<>();
    
    var cmd = "SELECT " + this.selectNames + " FROM " + tableName + " ORDER BY name";
    logger.debug("SQL: " + cmd);
    
    try {
      try (var stmt = conn.getConnection().prepareStatement(cmd)) {
        var clientRows = stmt.executeQuery();
        while (clientRows.next()) {
          var client = getData(clientRows);
          logger.debug("Client found: " + client.name);
          result.add(client);
        }
      }
    } catch (SQLException ex) {
      logger.warn("Error reading client list.", ex);
      throw new TammError("Error reading client list.");
    }
    return result;
  }
  
  /**
   * Copies the ResultSet client data into an ClientData object.
   * 
   * @param clientRows
   * @return
   * @throws SQLException 
   */
  private ClientData getData(ResultSet clientRows) throws SQLException {
    ClientData result = new ClientData();
    
    result.id = clientRows.getInt(1);
    result.name = clientRows.getString(2);
    result.hostName = clientRows.getString(3);
    result.hostName2 = clientRows.getString(4);
    result.hostName3 = clientRows.getString(5);
    result.maxDocMB = clientRows.getInt(6);
    result.maxUser = clientRows.getInt(7);
    
    return result;
  }
  
}
