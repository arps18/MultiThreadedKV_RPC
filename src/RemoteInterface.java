import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * This interface defines the remote methods for the key-value store service.
 * It allows clients to interact with the key-value store through RMI (Remote Method Invocation).
 */
public interface RemoteInterface extends Remote {

  /**
   * Processes the request and returns the result.
   *
   * @param request The request to be processed.
   * @return The result of the request processing.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  String processRequest(String request) throws RemoteException;

  // Methods for PUT and DELETE operations with Two-Phase Commit (2PC) protocol

  /**
   * Prepares to put a key-value pair in the key-value store using Two-Phase Commit protocol.
   *
   * @param key   The key of the entry to be put.
   * @param value The value of the entry to be put.
   * @return True if the preparation for PUT is successful, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean preparePut(String key, String value) throws RemoteException;

  /**
   * Receives a prepare PUT request from the coordinator during Two-Phase Commit protocol.
   *
   * @param key   The key of the entry to be put.
   * @param value The value of the entry to be put.
   * @return True if the prepare PUT request is received successfully, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean receivePreparePutRequest(String key, String value) throws RemoteException;

  /**
   * Receives the response to a prepare PUT request during Two-Phase Commit protocol.
   *
   * @param key       The key of the entry to be put.
   * @param value     The value of the entry to be put.
   * @param canCommit True if the coordinator allows the PUT operation, false otherwise.
   * @return True if the response is received successfully, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean receivePreparePutResponse(String key, String value, boolean canCommit)
      throws RemoteException;

  /**
   * Commits the PUT operation after receiving approval from the coordinator during Two-Phase Commit protocol.
   *
   * @param key   The key of the entry to be put.
   * @param value The value of the entry to be put.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  void doCommitPut(String key, String value) throws RemoteException;

  /**
   * Commits the DELETE operation after receiving approval from the coordinator during Two-Phase Commit protocol.
   *
   * @param key The key of the entry to be deleted.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  void doCommitDelete(String key) throws RemoteException;

  /**
   * Checks if the PUT operation can be committed for a key-value pair during Two-Phase Commit protocol.
   *
   * @param key   The key of the entry to be put.
   * @param value The value of the entry to be put.
   * @return True if the PUT operation can be committed, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean canCommitPut(String key, String value) throws RemoteException;

  /**
   * Prepares to delete a key-value pair from the key-value store using Two-Phase Commit protocol.
   *
   * @param key The key of the entry to be deleted.
   * @return True if the preparation for DELETE is successful, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean prepareDelete(String key) throws RemoteException;

  /**
   * Receives a prepare DELETE request from the coordinator during Two-Phase Commit protocol.
   *
   * @param key The key of the entry to be deleted.
   * @return True if the prepare DELETE request is received successfully, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean receivePrepareDeleteRequest(String key) throws RemoteException;

  /**
   * Receives the response to a prepare DELETE request during Two-Phase Commit protocol.
   *
   * @param key       The key of the entry to be deleted.
   * @param canCommit True if the coordinator allows the DELETE operation, false otherwise.
   * @return True if the response is received successfully, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean receivePrepareDeleteResponse(String key, boolean canCommit) throws RemoteException;

  /**
   * Checks if the DELETE operation can be committed for a key-value pair during Two-Phase Commit protocol.
   *
   * @param key The key of the entry to be deleted.
   * @return True if the DELETE operation can be committed, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean canCommitDelete(String key) throws RemoteException;

  /**
   * Updates the key-value store with a new set of key-value pairs.
   *
   * @param newKeyValueStore The new key-value store to be updated.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  void updateKeyValueStore(Map<String, String> newKeyValueStore) throws RemoteException;

  /**
   * Receives a message with an acknowledgment (ACK) from a remote entity.
   *
   * @param message The message received along with the acknowledgment.
   * @return True if the message with acknowledgment is received successfully, false otherwise.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  boolean receiveMessageWithACK(String message) throws RemoteException;

  /**
   * Receives a message without an acknowledgment (ACK) from a remote entity.
   *
   * @param message The message received without acknowledgment.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  void receiveMessageWithoutACK(String message) throws RemoteException;

  /**
   * Registers a replica server to the key-value store service.
   *
   * @param replicaServer The remote interface of the replica server to be registered.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  void registerReplicaServer(RemoteInterface replicaServer) throws RemoteException;

  /**
   * Unregisters a replica server from the key-value store service.
   *
   * @param replicaServer The remote interface of the replica server to be unregistered.
   * @throws RemoteException If an RMI communication-related exception occurs.
   */
  void unregisterReplicaServer(RemoteInterface replicaServer) throws RemoteException;
}
