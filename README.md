# Distributed Key-Value Store System 

This repository contains the implementation of a simple distributed key-value store system using Java Remote Method Invocation (RMI). The system comprises multiple replica servers and a client that can interact with the store.

## Components

The project consists of two main components:

1. **Server**: The `Server` class represents a replica server in the distributed key-value store system. It implements the `RemoteInterface` for remote method invocation. Each replica server maintains a local key-value store and communicates with other replicas to ensure consistency.

2. **Client**: The `Client` class serves as the client to the distributed key-value store system. It connects to the available replica servers, sends requests to the coordinator replica, and handles PUT, GET, and DELETE operations.

## Setup

1. Compile the code:

   ```bash
   javac Server.java
   javac Client.java
   ```

## Starting the Replica Servers

To start the replica servers, you need to run the `Server` class with the number of replicas you want to create. The first replica server will become the coordinator.

```bash
java Server
```

You will be prompted to enter the number of replicas. After entering the number, the server will start creating replica instances.

## Using the Client

Once the replica servers are running, you can run the `Client` class to interact with the distributed key-value store system.

```bash
java Client
```

The client will prompt you with options for PUT, GET, DELETE, or exiting the system. You can follow the on-screen instructions to perform the desired operation.


#### Starting Replica Servers

The main method of the `Server` class allows you to start multiple replica servers based on the number of replicas you want. The first replica will become the coordinator. The method uses the `startServer` private method to start each server instance.

## Server Working Demo

```
Enter the number of replicas:
5
-------------------------------------
The Replica 1 is the Coordinator.
Server started on port: 1010
Server started on port: 1011
Server started on port: 1012
Server started on port: 1013
Server started on port: 1014
[Time: 07-22-2023 17:22:21.419] PUT request processed.
[Time: 07-22-2023 17:22:26.033] GET request processed
```

## Client Working Demo

```
Connected to Server Replica 1
Connected to Server Replica 2
Connected to Server Replica 3
Connected to Server Replica 4
Connected to Server Replica 5
Choose an option:
1. PUT
2. GET
3. DELETE
4. Exit
1
Enter the values as: key=value
Enter key-value pair: 1=a
Choose a replica to connect (1-5 only):
2
<Time: 07-22-2023 17:22:21.450> PUT request processed.
-------------------------------------
Choose an option:
1. PUT
2. GET
3. DELETE
4. Exit
2
Enter key: 1
Choose a replica to connect (1-5 only):
3
<Time: 07-22-2023 17:22:26.035> Response: Value: a
-------------------------------------
```