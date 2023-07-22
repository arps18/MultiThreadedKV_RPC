import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * The RemoteInterface defines the methods that can be remotely accessed by clients and other replicas
 * in a distributed key-value store system.
 *
 * <p>The interface extends the {@link java.rmi.Remote} interface, making it RMI-compatible for remote method invocation.
 */
public interface RemoteInterface extends Remote {

  /**
   * Processes the given request and returns a response.
   *
   * @param request the request to be processed, in the format of a command followed by arguments.
   * @return the response from processing the request.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  String processRequest(String request) throws RemoteException;

  /**
   * Prepares to perform a PUT operation on the key-value store.
   *
   * @param key the key for the key-value pair to be put.
   * @param value the value for the key-value pair to be put.
   * @return {@code true} if the operation can be committed, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean preparePut(String key, String value) throws RemoteException;

  /**
   * Receives a prepare PUT request from a replica and checks if the operation can be committed.
   *
   * @param key the key for the key-value pair to be put.
   * @param value the value for the key-value pair to be put.
   * @return {@code true} if the operation can be committed, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean receivePreparePutRequest(String key, String value) throws RemoteException;

  /**
   * Receives the prepare PUT response from the coordinator and performs the commit if allowed.
   *
   * @param key the key for the key-value pair to be put.
   * @param value the value for the key-value pair to be put.
   * @param canCommit whether the operation is allowed to be committed.
   * @return {@code true} if the commit was successful, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean receivePreparePutResponse(String key, String value, boolean canCommit) throws RemoteException;

  /**
   * Performs the commit for the PUT operation.
   *
   * @param key the key for the key-value pair to be put.
   * @param value the value for the key-value pair to be put.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  void performCommitPut(String key, String value) throws RemoteException;


  /**
   * Prepares to perform a DELETE operation on the key-value store.
   *
   * @param key the key for the key-value pair to be deleted.
   * @return {@code true} if the operation can be committed, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean prepareDelete(String key) throws RemoteException;

  /**
   * Receives a prepare DELETE request from a replica and checks if the operation can be committed.
   *
   * @param key the key for the key-value pair to be deleted.
   * @return {@code true} if the operation can be committed, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean receivePrepareDeleteRequest(String key) throws RemoteException;

  /**
   * Receives the prepare DELETE response from the coordinator and performs the commit if allowed.
   *
   * @param key the key for the key-value pair to be deleted.
   * @param canCommit whether the operation is allowed to be committed.
   * @return {@code true} if the commit was successful, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean receivePrepareDeleteResponse(String key, boolean canCommit) throws RemoteException;

  /**
   * Performs the commit for the DELETE operation.
   *
   * @param key the key for the key-value pair to be deleted.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  void performCommitDelete(String key) throws RemoteException;


  /**
   * Checks if a PUT operation can be committed for the given key-value pair.
   *
   * @param key the key for the key-value pair.
   * @param value the value for the key-value pair.
   * @return {@code true} if the operation can be committed, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean canCommitPut(String key, String value) throws RemoteException;

  /**
   * Checks if a DELETE operation can be committed for the given key.
   *
   * @param key the key for the key-value pair to be deleted.
   * @return {@code true} if the operation can be committed, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean canCommitDelete(String key) throws RemoteException;

  /**
   * Updates the key-value store with the provided map.
   *
   * @param newKeyValueStore the new key-value store to update.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  void updateKeyValueStore(Map<String, String> newKeyValueStore) throws RemoteException;

  /**
   * Receives a message with an ACK (acknowledgment) from another replica.
   *
   * @param message the message to be received with ACK.
   * @return {@code true} if the ACK is received successfully, {@code false} otherwise.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  boolean receiveMessageWithACK(String message) throws RemoteException;

  /**
   * Receives a message without an ACK (acknowledgment) from another replica.
   *
   * @param message the message to be received without ACK.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  void receiveMessageWithoutACK(String message) throws RemoteException;

  /**
   * Registers a replica server to the coordinator.
   *
   * @param replicaServer the replica server to be registered.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  void registerReplicaServer(RemoteInterface replicaServer) throws RemoteException;

  /**
   * Unregisters a replica server from the coordinator.
   *
   * @param replicaServer the replica server to be unregistered.
   * @throws RemoteException if a communication-related exception occurs during remote method invocation.
   */
  void unregisterReplicaServer(RemoteInterface replicaServer) throws RemoteException;
}
