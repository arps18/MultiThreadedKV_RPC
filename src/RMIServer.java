import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The RMIServer class implements the RemoteInterface and acts as a key-value store server that
 * supports PUT, GET, and DELETE operations using the Two-Phase Commit (2PC) protocol with
 * replication.
 */
public class RMIServer implements RemoteInterface {

  private Map<String, String> keyValueStore;
  private Set<RemoteInterface> replicaServers;
  private ExecutorService executorService;
  private boolean isCoordinator;
  private static List<RemoteInterface> replicaStubs;
  private static List<Integer> replicaRegistryPorts;

  /**
   * Creates a new instance of RMIServer with an empty key-value store, initializes data structures
   * for replica servers and sets up a thread pool for concurrent execution.
   */
  public RMIServer() {
    keyValueStore = new HashMap<>();
    replicaServers = new HashSet<>();
    executorService = Executors.newFixedThreadPool(10); // Number of threads in the thread pool
    isCoordinator = false;
    replicaStubs = new ArrayList<>();
    replicaRegistryPorts = new ArrayList<>();
  }

  /**
   * The main method that starts multiple replica servers based on user input.
   * The first replica server created acts as the coordinator.
   */

  public static void main(String[] args) {
    System.out.println("Enter the number of replicas:");
    Scanner sc = new Scanner(System.in);
    int numReplicas = sc.nextInt();
    sc.close();

    RMIServer coordinator = null;

    // Create and start multiple replica servers
    for (int i = 1; i <= numReplicas; i++) {
      RMIServer server = new RMIServer();

      if (i == 1) {
        // The first replica will act as the coordinator
        coordinator = server;
        coordinator.isCoordinator = true;
        System.out.println("Replica " + i + " is the Coordinator.");
      }

      int registryPort = 1099 + i; // Use different registry ports for each replica
      startServer(server, registryPort, coordinator);
    }
  }

  private static void startServer(RMIServer server, int registryPort, RMIServer coordinator) {
    try {
      RemoteInterface replicaStub = (RemoteInterface) UnicastRemoteObject.exportObject(server,
          registryPort);
      //KeyValueStore keyValueStoreStub = (KeyValueStore) UnicastRemoteObject.exportObject(server, registryPort);

      // Create the registry if it doesn't exist
      Registry registry = null;
      try {
        registry = LocateRegistry.createRegistry(registryPort);
      } catch (RemoteException e) {
        // Registry already exists
        registry = LocateRegistry.getRegistry(registryPort);
      }

      // Bind the remote object's stub to the registry
      registry.rebind("RemoteInterface", replicaStub);
      //registry.rebind("KeyValueStore", keyValueStoreStub);

      System.out.println("Server started on port: " + registryPort); // Debug statement

      if (coordinator == null) {
        System.out.println("Replica " + registryPort + " is the Coordinator.");
      } else {
        // Connect to the coordinator if not already connected
        if (coordinator != server) {
          try {
            // Get the registry port of the coordinator
            int coordinatorRegistryPort = 1099; // Coordinator's port
            for (int port : replicaRegistryPorts) {
              if (port == coordinatorRegistryPort) {
                RemoteInterface coordinatorStub = replicaStubs.get(
                    replicaRegistryPorts.indexOf(port));
                server.registerReplicaServer(coordinatorStub);
                break;
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        //System.out.println("Replica " + registryPort + " started and connected to the Coordinator.");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public String processRequest(String request) throws RemoteException {
    System.out.println("Received request: " + request); // Debug statement

    String[] parts = request.split(" ", 2);
    String command = parts[0].trim();

    if (command.equalsIgnoreCase("PUT")) {
      String[] keyValue = parts[1].split("=", 2);
      String key = keyValue[0].trim();
      String value = keyValue[1].trim();

      // Prepare phase
      boolean prepareResult = preparePut(key, value);

      if (prepareResult) {
        // Commit phase
        doCommitPut(key, value);
        return "Request processed";
      } else {
        // Abort phase
        return "Failed to process request";
      }
    } else if (command.equalsIgnoreCase("GET")) {
      String key = parts[1].trim();
      String value = keyValueStore.get(key);
      System.out.println("GET request processed"); // Debug statement
      if (value != null) {
        return "Value: " + value;
      } else {
        return "Key not found";
      }
    } else if (command.equalsIgnoreCase("DELETE")) {
      String key = parts[1].trim();

      // Prepare phase
      boolean prepareResult = prepareDelete(key);

      if (prepareResult) {
        // Commit phase
        doCommitDelete(key);
        return "Request processed";
      } else {
        // Abort phase
        return "Failed to process request";
      }
    }

    return "Invalid command";
  }

  // Helper method to send a message to a replica server and wait for the ACK
  private boolean sendMessageWithACK(RemoteInterface replica, String message)
      throws RemoteException {
    boolean ackReceived = replica.receiveMessageWithACK(message);
    return ackReceived;
  }

  @Override
  public boolean preparePut(String key, String value) throws RemoteException {
    //System.out.println("Received preparePut request for key: " + key + ", value: " + value);
    if (canCommitPut(key, value)) {
      doCommitPut(key, value);
      return true;// Check if the key can be updated
    }
    return false;
  }


  @Override
  public boolean receivePreparePutRequest(String key, String value) throws RemoteException {
    return canCommitPut(key, value); // Check if the key can be updated
  }

  @Override
  public synchronized boolean receivePreparePutResponse(String key, String value, boolean canCommit)
      throws RemoteException {
//    System.out.println("Received receivePreparePutResponse for key: " + key + ", value: " + value + ", canCommit: " + canCommit);
//    System.out.println("Number of replica servers connected: " + replicaServers.size());
    if (!canCommit) {
      // Abort the PUT update
      return false;
    }

    // Send doCommit request to all replica servers and wait for ACKs
    for (RemoteInterface replica : replicaServers) {
      //System.out.println("Here");
      if (!sendMessageWithACK(replica, "DO_COMMIT_PUT " + key + "=" + value)) {

        // If any replica failed to respond, abort the update
        return false;
      }
    }

    return true;
  }


  @Override
  public boolean canCommitPut(String key, String value) throws RemoteException {
    boolean canCommit = !keyValueStore.containsKey(key);
    //System.out.println("canCommitPut for key: " + key + ", value: " + value + ", canCommit: " + canCommit);
    return canCommit; // Check if the key can be updated
  }


  @Override
  public synchronized void doCommitPut(String key, String value) throws RemoteException {
    // Send preparePut request to all replica servers and wait for their responses
    boolean allCanCommit = true;
    for (RemoteInterface replica : replicaServers) {
      if (!replica.receivePreparePutRequest(key, value)) {
        allCanCommit = false;
        break;
      }
    }

    //System.out.println("Received doCommitPut request for key: " + key + ", value: " + value);

    if (allCanCommit) {
      keyValueStore.put(key, value);
      //System.out.println("Added key-value pair: " + key + "=" + value + " to key-value store.");
      // Propagate the updated key-value store to all replicas
      List<Boolean> ackResults = new ArrayList<>();
      for (RemoteInterface replica : replicaServers) {
//        replica.updateKeyValueStore(new HashMap<>(keyValueStore));
//        ackResults.add(sendMessageWithACK(replica, "DO_COMMIT_PUT " + key + "=" + value));
        sendMessageWithACK(replica, "DO_COMMIT_PUT " + key + "=" + value);
        // Update ackResults list with ACK responses from replica servers
        ackResults.add(true); // Assuming the message sent to the replica was successful
      }

      // Wait for all ACK responses to ensure successful update propagation
      boolean allAcksReceived = ackResults.stream().allMatch(result -> result);
      if (allAcksReceived) {
        System.out.println("PUT request processed.");
      } else {
        System.out.println("Failed to process PUT request.");
        // In case of a failure, we can optionally handle retries or error recovery.
      }
    } else {
      System.out.println("Failed to process PUT request.");
    }
  }


  // Helper method to send a message to a replica server without requiring an ACK response
  private void sendMessageWithoutACK(RemoteInterface replica, String message)
      throws RemoteException {
    replica.receiveMessageWithoutACK(message);
  }

  @Override
  public void receiveMessageWithoutACK(String message) throws RemoteException {
    // Log the received message without requiring an ACK response
    //System.out.println("Received message without ACK: " + message);

    String[] parts = message.split(" ", 2);
    String command = parts[0].trim();

    if (command.equalsIgnoreCase("DO_COMMIT_DELETE")) {
      String key = parts[1].trim();

      // Apply the DELETE operation to the replica's key-value store
      keyValueStore.remove(key);
      //System.out.println("DELETE request processed on replica.");
    }
  }


  @Override
  public boolean prepareDelete(String key) throws RemoteException {
    if (canCommitDelete(key)) {
      doCommitDelete(key);
      return true;
    }
    return false;
  }

  @Override
  public boolean receivePrepareDeleteRequest(String key) throws RemoteException {
    return canCommitDelete(key); // Check if the key exists and can be deleted
  }

  @Override
  public synchronized boolean receivePrepareDeleteResponse(String key, boolean canCommit)
      throws RemoteException {
//    System.out.println("Received receivePrepareDeleteResponse for key: " + key + ", canCommit: " + canCommit);
//    System.out.println("Number of replica servers connected: " + replicaServers.size());

    if (!canCommit) {
      // Abort the DELETE operation
      return false;
    }

    // Send doCommit request to all replica servers and wait for ACKs
    for (RemoteInterface replica : replicaServers) {
      if (!sendMessageWithACK(replica, "DO_COMMIT_DELETE " + key)) {
        //System.out.println("andar ahiya");
        // If any replica failed to respond, abort the update
        return false;
      }
    }

    return true;
  }


  @Override
  public boolean canCommitDelete(String key) throws RemoteException {
    boolean canCommit = keyValueStore.containsKey(key);
//    System.out.println("canCommitDelete for key: " + key + ", canCommit: " + canCommit);
//    System.out.println("Key-value store content: " + keyValueStore);
    return canCommit; // Check if the key exists and can be deleted
  }


  @Override
  public synchronized void doCommitDelete(String key) throws RemoteException {
    // Send prepareDelete request to all replica servers and wait for their responses
    boolean allCanCommit = true;
    for (RemoteInterface replica : replicaServers) {
      if (!replica.receivePrepareDeleteRequest(key)) {
        allCanCommit = false;
        break;
      }
    }

//    System.out.println("Received doCommitDelete request for key: " + key);
//    System.out.println("Key-value store content before delete: " + keyValueStore);

    if (allCanCommit) {
      keyValueStore.remove(key);

      // Propagate the DELETE request to all replicas, including the coordinator
      List<Boolean> ackResults = new ArrayList<>();
      for (RemoteInterface replica : replicaServers) {
        sendMessageWithACK(replica, "DO_COMMIT_DELETE " + key);
      }

      // Wait for all ACK responses to ensure successful update propagation
      boolean allAcksReceived = ackResults.stream().allMatch(result -> result);
      if (allAcksReceived) {
        System.out.println("DELETE request processed.");
      } else {
        System.out.println("Failed to process DELETE request.");
        // In case of a failure, we can optionally handle retries or error recovery.
      }
    } else {
      System.out.println("Failed to process DELETE request.");
    }
  }


  @Override
  public void updateKeyValueStore(Map<String, String> newKeyValueStore) throws RemoteException {
    keyValueStore = newKeyValueStore;
  }

  @Override
  public boolean receiveMessageWithACK(String message) throws RemoteException {
    //System.out.println("Received message with ACK: " + message);

    String[] parts = message.split(" ", 2);
    String command = parts[0].trim();

    if (command.equalsIgnoreCase("DO_COMMIT_PUT")) {
      String[] keyValue = parts[1].split("=", 2);
      String key = keyValue[0].trim();
      String value = keyValue[1].trim();

      // Apply the PUT operation to the replica's key-value store
      keyValueStore.put(key, value);
      //System.out.println("PUT request processed on replica.");

      // Return true to simulate an ACK response
      return true;
    } else if (command.equalsIgnoreCase("DO_COMMIT_DELETE")) {
      String key = parts[1].trim();

      // Apply the DELETE operation to the replica's key-value store
      keyValueStore.remove(key);
      //System.out.println("DELETE request processed on replica.");
      return true;
    }

    // For other commands, simply return false (no ACK response)
    return false;
  }

  // Add methods to register and unregister replica servers

  @Override
  public synchronized void registerReplicaServer(RemoteInterface replicaServer) {
    replicaServers.add(replicaServer);
    //System.out.println("Registered replica server");
    if (replicaServers.size() == 1) {
      isCoordinator = true;
    }
  }

  @Override
  public synchronized void unregisterReplicaServer(RemoteInterface replicaServer) {
    replicaServers.remove(replicaServer);
    //System.out.println("Unregistered replica server");
    if (replicaServers.size() == 0) {
      isCoordinator = false;
    }
  }


}
