import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

/**
 * The `Client` class represents a simple RMI client that interacts with a distributed key-value
 * store. It provides options for PUT, GET, and DELETE operations on the key-value store.
 * The client connects to the replicas and sends requests to the coordinator replica to manage
 * the distributed key-value store.
 * <p>
 * The client uses a timestamp in UTC format to track the time of each operation.
 */
public class Client {

  /**
   * Gets the current timestamp in UTC format.
   *
   * @return a string representing the current timestamp.
   */
  private static String getCurrentTimestamp() {
    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return "<Time: " + sdf.format(new Date()) + "> ";
  }

  /**
   * The main method of the `Client` class.
   *
   * @param args command-line arguments (not used).
   */
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);

    try {
      // Lists to store replica stubs and their corresponding registry ports.
      List<RemoteInterface> replicaStubs = new ArrayList<>();
      List<Integer> replicaRegistryPorts = new ArrayList<>();

      int replicaIndex = 1;
      boolean foundReplica = true;

      // Connecting to available replicas until no more replicas are found.
      while (foundReplica) {
        int registryPort = 1009 + replicaIndex;
        RemoteInterface replicaStub = connectToReplica(registryPort);

        if (replicaStub != null) {
          replicaStubs.add(replicaStub);
          replicaRegistryPorts.add(registryPort);
          System.out.println("Connected to Replica " + replicaIndex);
        } else {
          foundReplica = false;
        }

        replicaIndex++;
      }

      // If no replicas are found, exit the client.
      if (replicaStubs.isEmpty()) {
        System.out.println("No replica servers found. Exiting...");
        System.exit(0);
      }

      // The coordinator replica is the first replica in the list.
      RemoteInterface coordinatorStub = replicaStubs.get(0);

      // Registering all other replicas with the coordinator.
      for (int i = 1; i < replicaStubs.size(); i++) {
        coordinatorStub.registerReplicaServer(replicaStubs.get(i));
      }

      // Client's main loop to handle user commands.
      while (true) {
        System.out.println("Choose an option:");
        System.out.println("1. PUT");
        System.out.println("2. GET");
        System.out.println("3. DELETE");
        System.out.println("4. Exit");

        int option = sc.nextInt();
        sc.nextLine();

        switch (option) {
          case 1:
            System.out.print("Enter key-value pair (e.g., key=value): ");
            String keyValue = sc.nextLine();
            String[] keyValueArr = keyValue.split("=");
            String key = keyValueArr[0].trim();
            String value = keyValueArr[1].trim();

            System.out.println("Choose a replica to connect (1-" + replicaStubs.size() + "):");
            int replicaChoicePut = sc.nextInt();
            sc.nextLine();

            // Checking if the replica choice is valid.
            if (replicaChoicePut < 1 || replicaChoicePut > replicaStubs.size()) {
              System.out.println("Invalid replica choice. Please try again.");
              break;
            }

            RemoteInterface replicaStubPut = replicaStubs.get(replicaChoicePut - 1);

            boolean prepareResult = coordinatorStub.preparePut(key, value);
            if (prepareResult) {
              boolean commitResult = coordinatorStub.receivePreparePutResponse(key, value, true);
              if (commitResult) {
                System.out.println(getCurrentTimestamp() + "PUT request processed.");
              } else {
                System.out.println(getCurrentTimestamp() + "Failed to process PUT request.");
              }
            } else {
              System.out.println(getCurrentTimestamp() + "Failed to process PUT request.");
            }
            break;

          case 2:
            System.out.print("Enter key: ");
            String k = sc.nextLine();

            System.out.println("Choose a replica to connect (1-" + replicaStubs.size() + "):");
            int replicaChoiceGet = sc.nextInt();
            sc.nextLine();

            // Checking if the replica choice is valid.
            if (replicaChoiceGet < 1 || replicaChoiceGet > replicaStubs.size()) {
              System.out.println("Invalid replica choice. Please try again.");
              break;
            }

            RemoteInterface replicaStubGet = replicaStubs.get(replicaChoiceGet - 1);

            String getResponse = replicaStubGet.processRequest("GET " + k);
            System.out.println(getCurrentTimestamp() + "Response: " + getResponse);
            break;

          case 3:
            System.out.print("Enter key to delete: ");
            String deleteKey = sc.nextLine();

            System.out.println("Choose a replica to connect (1-" + replicaStubs.size() + "):");
            int replicaChoiceDelete = sc.nextInt();
            sc.nextLine();

            // Checking if the replica choice is valid.
            if (replicaChoiceDelete < 1 || replicaChoiceDelete > replicaStubs.size()) {
              System.out.println("Invalid replica choice. Please try again.");
              break;
            }

            RemoteInterface replicaStubDelete = replicaStubs.get(replicaChoiceDelete - 1);

            boolean prepareResult2 = coordinatorStub.prepareDelete(deleteKey);
            if (prepareResult2) {
              boolean commitResult = coordinatorStub.receivePrepareDeleteResponse(deleteKey, true);
              if (commitResult) {
                System.out.println(getCurrentTimestamp() + "DELETE request processed.");
              } else {
                System.out.println(getCurrentTimestamp() + "Failed to process DELETE request.");
              }
            } else {
              System.out.println(getCurrentTimestamp() + "Failed to process DELETE request.");
            }
            break;

          case 4:
            System.out.println("Exiting...");
            System.exit(0);

          default:
            System.out.println("Invalid option. Please try again.");
            break;
        }

        System.out.println("-------------------------------------");
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      sc.close();
    }
  }

  /**
   * Connects to a replica server using the provided registry port.
   *
   * @param registryPort the registry port of the replica server.
   * @return the stub of the connected replica server, or null if the connection failed.
   */
  private static RemoteInterface connectToReplica(int registryPort) {
    try {
      Registry registry = LocateRegistry.getRegistry("localhost", registryPort);
      RemoteInterface replicaStub = (RemoteInterface) registry.lookup("RemoteInterface");
      return replicaStub;
    } catch (Exception e) {
      return null;
    }
  }
}
