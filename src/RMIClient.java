import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RMIClient {

  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);

    try {
      List<RemoteInterface> replicaStubs = new ArrayList<>();
      List<Integer> replicaRegistryPorts = new ArrayList<>();

      // Start from 1 and keep trying until a replica is not found
      int replicaIndex = 1;
      boolean foundReplica = true;

      while (foundReplica) {
        int registryPort = 1099 + replicaIndex;
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

      if (replicaStubs.isEmpty()) {
        System.out.println("No replica servers found. Exiting...");
        System.exit(0);
      }

      RemoteInterface coordinatorStub = replicaStubs.get(0);

      for (int i = 1; i < replicaStubs.size(); i++) {
        coordinatorStub.registerReplicaServer(replicaStubs.get(i));
      }

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

            if (replicaChoicePut < 1 || replicaChoicePut > replicaStubs.size()) {
              System.out.println("Invalid replica choice. Please try again.");
              break;
            }

            RemoteInterface replicaStubPut = replicaStubs.get(replicaChoicePut - 1);

            boolean prepareResult = coordinatorStub.preparePut(key, value);
            if (prepareResult) {
              boolean commitResult = coordinatorStub.receivePreparePutResponse(key, value, true);
              if (commitResult) {
                System.out.println("PUT request processed.");
              } else {
                System.out.println("Failed to process PUT request.");
              }
            } else {
              System.out.println("Failed to process PUT request.");
            }
            break;

          case 2:

            System.out.print("Enter key: ");
            String k = sc.nextLine();

            System.out.println("Choose a replica to connect (1-" + replicaStubs.size() + "):");
            int replicaChoiceGet = sc.nextInt();
            sc.nextLine();

            if (replicaChoiceGet < 1 || replicaChoiceGet > replicaStubs.size()) {
              System.out.println("Invalid replica choice. Please try again.");
              break;
            }

            RemoteInterface replicaStubGet = replicaStubs.get(replicaChoiceGet - 1);

            String getResponse = replicaStubGet.processRequest("GET " + k);
            System.out.println("Response: " + getResponse);
            break;

          case 3:

            System.out.print("Enter key to delete: ");
            String deleteKey = sc.nextLine();

            System.out.println("Choose a replica to connect (1-" + replicaStubs.size() + "):");
            int replicaChoiceDelete = sc.nextInt();
            sc.nextLine();

            if (replicaChoiceDelete < 1 || replicaChoiceDelete > replicaStubs.size()) {
              System.out.println("Invalid replica choice. Please try again.");
              break;
            }

            RemoteInterface replicaStubDelete = replicaStubs.get(replicaChoiceDelete - 1);

            boolean prepareResult2 = coordinatorStub.prepareDelete(deleteKey);
            if (prepareResult2) {
              boolean commitResult = coordinatorStub.receivePrepareDeleteResponse(deleteKey, true);
              if (commitResult) {
                System.out.println("DELETE request processed.");
              } else {
                System.out.println("Failed to process DELETE request.");
              }
            } else {
              System.out.println("Failed to process DELETE request.");
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

  private static RemoteInterface connectToReplica(int registryPort) {
    try {
      Registry registry = LocateRegistry.getRegistry("localhost", registryPort);
      RemoteInterface replicaStub = (RemoteInterface) registry.lookup("RemoteInterface");
      return replicaStub;
    } catch (Exception e) {
      // Replica not found, return null
      return null;
    }
  }
}
