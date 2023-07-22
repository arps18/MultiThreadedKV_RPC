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
