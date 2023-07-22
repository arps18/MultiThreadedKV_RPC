import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Server class represents a replica server in a distributed key-value store system.
 * It implements the {@link RemoteInterface} for remote method invocation.
 */
public class Server implements RemoteInterface {
  // Private fields for the server
  private Map<String, String> keyValueStore;
  private Set<RemoteInterface> replicaServers;
  private ExecutorService executorService;
  private boolean isCoordinator;
  private static List<RemoteInterface> replicaStubs;
  private static List<Integer> replicaRegistryPorts;

  /**
   * Constructs a new Server instance.
   * Initializes the key-value store, the set of replica servers, and the executor service.
   * Sets the initial state of this server as a non-coordinator.
   */
  public Server() {
    keyValueStore = new ConcurrentHashMap<>();
    replicaServers = ConcurrentHashMap.newKeySet();
    executorService = Executors.newFixedThreadPool(15);
    isCoordinator = false;
    replicaStubs = new ArrayList<>();
    replicaRegistryPorts = new ArrayList<>();
  }
  /**
   * The main method to start the replica servers and coordinate the system.
   *
   * @param args command-line arguments.
   */
  public static void main(String[] args) {
    System.out.println("Enter the number of replicas:");
    Scanner sc = new Scanner(System.in);
    int numReplicas = sc.nextInt();
    sc.close();

    Server coordinator = null;

    for (int i = 1; i <= numReplicas; i++) {
      Server server = new Server();

      if (i == 1) {
        coordinator = server;
        coordinator.isCoordinator = true;
        System.out.println("The Replica " + i + " is the Coordinator.");
      }

      int registryPort = 1009 + i;
      startServer(server, registryPort, coordinator);
    }
  }

  /**
   * Starts the replica server with the provided registry port and coordinator instance.
   *
   * @param server the server instance to be started.
   * @param registryPort the registry port for RMI communication.
   * @param coordinator the coordinator instance for coordinating replicas.
   */
  private static void startServer(Server server, int registryPort, Server coordinator) {
    try {
      RemoteInterface replicaStub = (RemoteInterface) UnicastRemoteObject.exportObject(server,
          registryPort);

      Registry registry = null;
      try {
        registry = LocateRegistry.createRegistry(registryPort);
      } catch (RemoteException e) {
        registry = LocateRegistry.getRegistry(registryPort);
      }

      registry.rebind("RemoteInterface", replicaStub);

      System.out.println("Server started on port: " + registryPort);

      if (coordinator == null) {
        System.out.println("Replica " + registryPort + " is the Coordinator.");
      } else {
        if (coordinator != server) {
          try {
            int coordinatorRegistryPort = 1009;
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
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets the current timestamp in the UTC time zone.
   *
   * @return a string representing the current timestamp in the format "[Time: MM-dd-yyyy HH:mm:ss.SSS]".
   */
  private String getCurrentTimestamp() {
    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return "[Time: " + sdf.format(new Date()) + "] ";
  }
  /**
   * Processes the incoming client request based on the command provided.
   * If the command is "PUT", it prepares and performs the PUT operation on the key-value store.
   * If the command is "GET", it retrieves the value for the given key from the key-value store.
   * If the command is "DELETE", it prepares and performs the DELETE operation on the key-value store.
   *
   * @param request the client request in the format "COMMAND KEY=VALUE" or "COMMAND KEY".
   * @return a response message indicating the success or failure of the request.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public String processRequest(String request) throws RemoteException {
    String[] parts = request.split(" ", 2);
    String command = parts[0].trim();

    if (command.equalsIgnoreCase("PUT")) {
      String[] keyValue = parts[1].split("=", 2);
      String key = keyValue[0].trim();
      String value = keyValue[1].trim();

      boolean prepareResult = preparePut(key, value);

      if (prepareResult) {
        performCommitPut(key, value);
        return getCurrentTimestamp() + "Request processed";
      } else {
        return getCurrentTimestamp() + "Failed to process request";
      }
    } else if (command.equalsIgnoreCase("GET")) {
      String key = parts[1].trim();
      String value = keyValueStore.get(key);
      System.out.println(getCurrentTimestamp() + "GET request processed");

      if (value != null) {
        return  "Value: " + value;
      } else {
        return  "Key not found";
      }
    } else if (command.equalsIgnoreCase("DELETE")) {
      String key = parts[1].trim();

      boolean prepareResult = prepareDelete(key);

      if (prepareResult) {
        performCommitDelete(key);
        return getCurrentTimestamp() + "Request processed";
      } else {
        return getCurrentTimestamp() + "Failed to process request";
      }
    }

    return getCurrentTimestamp() + "Invalid command";
  }

  /**
   * Sends a message with acknowledgment (ACK) to the provided replica server.
   *
   * @param replica the replica server to which the message is sent.
   * @param message the message to be sent.
   * @return true if the ACK is received from the replica, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  private boolean sendMessageWithACK(RemoteInterface replica, String message)
      throws RemoteException {
    boolean ackReceived = replica.receiveMessageWithACK(message);
    return ackReceived;
  }

  /**
   * Prepares the PUT operation by checking if the key-value pair can be committed.
   *
   * @param key the key for the new key-value pair.
   * @param value the value for the new key-value pair.
   * @return true if the PUT operation can be prepared and committed, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean preparePut(String key, String value) throws RemoteException {
    if (canCommitPut(key, value)) {
      performCommitPut(key, value);
      return true;
    }
    return false;
  }

  /**
   * Receives a prepare PUT request from another replica server and checks if the key-value pair
   * can be committed.
   *
   * @param key the key for the new key-value pair.
   * @param value the value for the new key-value pair.
   * @return true if the PUT operation can be prepared and committed, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean receivePreparePutRequest(String key, String value) throws RemoteException {
    return canCommitPut(key, value);
  }

  /**
   * Receives the response to the prepare PUT request from another replica server.
   * If the response is positive (canCommit=true), it sends ACKs to all replicas to commit the PUT.
   *
   * @param key the key for the new key-value pair.
   * @param value the value for the new key-value pair.
   * @param canCommit true if the PUT operation can be committed, false otherwise.
   * @return true if all ACKs are received and the PUT is committed successfully, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean receivePreparePutResponse(String key, String value, boolean canCommit)
      throws RemoteException {
    if (!canCommit) {
      return false;
    }

    for (RemoteInterface replica : replicaServers) {
      if (!sendMessageWithACK(replica, "DO_COMMIT_PUT " + key + "=" + value)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if the PUT operation for the provided key-value pair can be committed.
   * The PUT can be committed if the key is not present in the key-value store.
   *
   * @param key the key for the new key-value pair.
   * @param value the value for the new key-value pair.
   * @return true if the PUT operation can be committed, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean canCommitPut(String key, String value) throws RemoteException {
    boolean canCommit = !keyValueStore.containsKey(key);
    return canCommit;
  }

  /**
   * Performs the commit operation for the PUT request.
   * It sends prepare PUT requests to all replicas and waits for their responses.
   * If all replicas can commit, it performs the PUT operation in the key-value store
   * and sends ACKs to all replicas to commit the PUT.
   *
   * @param key the key for the new key-value pair.
   * @param value the value for the new key-value pair.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public void performCommitPut(String key, String value) throws RemoteException {
    boolean allCanCommit = true;
    for (RemoteInterface replica : replicaServers) {
      if (!replica.receivePreparePutRequest(key, value)) {
        allCanCommit = false;
        break;
      }
    }

    if (allCanCommit) {
      keyValueStore.put(key, value);
      List<Boolean> ackResults = new ArrayList<>();
      for (RemoteInterface replica : replicaServers) {
        sendMessageWithACK(replica, "DO_COMMIT_PUT " + key + "=" + value);
        ackResults.add(true);
      }

      boolean allACKsReceived = ackResults.stream().allMatch(result -> result);
      if (allACKsReceived) {
        System.out.println(getCurrentTimestamp() + "PUT request processed.");
      } else {
        System.out.println(getCurrentTimestamp() + "Failed to process PUT request.");
      }
    } else {
      System.out.println(getCurrentTimestamp() + "Failed to process PUT request.");
    }
  }

  /**
   * Sends a message to the provided replica without waiting for an acknowledgment (ACK).
   *
   * @param replica the replica server to which the message is sent.
   * @param message the message to be sent.
   * @throws RemoteException if a remote communication error occurs.
   */
  private void sendMessageWithoutACK(RemoteInterface replica, String message)
      throws RemoteException {
    replica.receiveMessageWithoutACK(message);
  }

  /**
   * Receives a message without ACK from another replica and performs the COMMIT DELETE operation
   * for the given key in the key-value store.
   *
   * @param message the message containing the key to be deleted.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public void receiveMessageWithoutACK(String message) throws RemoteException {
    String[] parts = message.split(" ", 2);
    String command = parts[0].trim();

    if (command.equalsIgnoreCase("DO_COMMIT_DELETE")) {
      String key = parts[1].trim();
      keyValueStore.remove(key);
    }
  }

  /**
   * Prepares the DELETE operation by checking if the key exists in the key-value store.
   *
   * @param key the key to be deleted.
   * @return true if the DELETE operation can be prepared and committed, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean prepareDelete(String key) throws RemoteException {
    if (canCommitDelete(key)) {
      performCommitDelete(key);
      return true;
    }
    return false;
  }

  /**
   * Receives a prepare DELETE request from another replica and checks if the key exists
   * in the key-value store and can be deleted.
   *
   * @param key the key to be deleted.
   * @return true if the DELETE operation can be prepared and committed, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean receivePrepareDeleteRequest(String key) throws RemoteException {
    return canCommitDelete(key);
  }

  /**
   * Receives the response to the prepare DELETE request from another replica.
   * If the response is positive (canCommit=true), it sends ACKs to all replicas to commit the DELETE.
   *
   * @param key the key to be deleted.
   * @param canCommit true if the DELETE operation can be committed, false otherwise.
   * @return true if all ACKs are received and the DELETE is committed successfully, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean receivePrepareDeleteResponse(String key, boolean canCommit)
      throws RemoteException {
    if (!canCommit) {
      return false;
    }

    for (RemoteInterface replica : replicaServers) {
      if (!sendMessageWithACK(replica, "DO_COMMIT_DELETE " + key)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if the DELETE operation for the provided key can be committed.
   * The DELETE can be committed if the key exists in the key-value store.
   *
   * @param key the key to be deleted.
   * @return true if the DELETE operation can be committed, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean canCommitDelete(String key) throws RemoteException {
    boolean canCommit = keyValueStore.containsKey(key);
    return canCommit;
  }

  /**
   * Performs the commit operation for the DELETE request.
   * It sends prepare DELETE requests to all replicas and waits for their responses.
   * If all replicas can commit, it removes the key from the key-value store
   * and sends ACKs to all replicas to commit the DELETE.
   *
   * @param key the key to be deleted.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public void performCommitDelete(String key) throws RemoteException {
    boolean allCanCommit = true;
    for (RemoteInterface replica : replicaServers) {
      if (!replica.receivePrepareDeleteRequest(key)) {
        allCanCommit = false;
        break;
      }
    }

    if (allCanCommit) {
      keyValueStore.remove(key);

      List<Boolean> ackResults = new ArrayList<>();
      for (RemoteInterface replica : replicaServers) {
        sendMessageWithACK(replica, "DO_COMMIT_DELETE " + key);
      }

      boolean allAcksReceived = ackResults.stream().allMatch(result -> result);
      if (allAcksReceived) {
        System.out.println(getCurrentTimestamp() + "DELETE request processed.");
      } else {
        System.out.println(getCurrentTimestamp() + "Failed to process DELETE request.");
      }
    } else {
      System.out.println(getCurrentTimestamp() + "Failed to process DELETE request.");
    }
  }

  /**
   * Updates the local key-value store with a new key-value store provided by the coordinator.
   *
   * @param newKeyValueStore the new key-value store to update.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public void updateKeyValueStore(Map<String, String> newKeyValueStore) throws RemoteException {
    keyValueStore = new ConcurrentHashMap<>(newKeyValueStore);
  }

  /**
   * Receives a message with ACK from another replica and performs the corresponding action
   * (PUT or DELETE) in the key-value store.
   *
   * @param message the message with the action to be performed.
   * @return true if the action is successfully performed, false otherwise.
   * @throws RemoteException if a remote communication error occurs.
   */
  @Override
  public boolean receiveMessageWithACK(String message) throws RemoteException {
    String[] parts = message.split(" ", 2);
    String command = parts[0].trim();

    if (command.equalsIgnoreCase("DO_COMMIT_PUT")) {
      String[] keyValue = parts[1].split("=", 2);
      String key = keyValue[0].trim();
      String value = keyValue[1].trim();

      keyValueStore.put(key, value);

      return true;
    } else if (command.equalsIgnoreCase("DO_COMMIT_DELETE")) {
      String key = parts[1].trim();

      keyValueStore.remove(key);
      return true;
    }

    return false;
  }

  /**
   * Registers a new replica server and adds it to the set of replica servers.
   * If it's the first replica server, it becomes the coordinator.
   *
   * @param replicaServer the replica server to be registered.
   */
  @Override
  public void registerReplicaServer(RemoteInterface replicaServer) {
    replicaServers.add(replicaServer);
    if (replicaServers.size() == 1) {
      isCoordinator = true;
    }
  }
  /**
   * Unregisters a replica server and removes it from the set of replica servers.
   * If there are no remaining replica servers, it resigns as the coordinator.
   *
   * @param replicaServer the replica server to be unregistered.
   */
  @Override
  public void unregisterReplicaServer(RemoteInterface replicaServer) {
    replicaServers.remove(replicaServer);
    if (replicaServers.size() == 0) {
      isCoordinator = false;
    }
  }
}
