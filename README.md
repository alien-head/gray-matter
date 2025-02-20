# grey-matter

This project uses [Gradle](https://gradle.org/).
To build and run the application, use the *Gradle* tool window by clicking the Gradle icon in the right-hand toolbar,
or run it directly from the terminal:

* Run `./gradlew run` to build and run the application.
* Run `./gradlew build` to only build the application.
* Run `./gradlew check` to run all checks, including tests.
* Run `./gradlew clean` to clean all build outputs.

# Docs

## Environment Variables
* `PORT`: the given port for the app to run on. Defaults to 8081 if not provided.
* `NODE_ADDRESS`: the url address (host, port, etc.) for accessing the node. Broadcast to the other nodes in the network. Required.
* `NODE_DONOR`: the url address (host, port, etc.) for accessing another node to populate the blockchain and network pool. If not populated, the node will run in genesis mode. (See Network/Starting up for the first time)
* `NODE_MODE`: the mode to run the node in: `AUTHOR` or `BASIC`. If `AUTHOR` is selected, `AUTHOR_PRIVATE_KEY` is required.

## Network
### Terminology
* Node: a member of a network.
* Genesis node: the first member of the network. Creates the genesis block.
* Donor node: a node that is used to get the blockchain and the list of nodes in the network.
* Author node: a node in the network that has permission to mint blocks. 
* Proof of Authority: the consensus mechanism for Gray. Only certain nodes can mint new blocks.

### Starting up for the first time
The application should be provided with a donor node url address from an existing network.
If a donor node is provided, then the app's blockchain and network will be populated from the donor.
If a donor node is not provided, the application assumes it is the genesis node and will start its own network/blockchain.
