# gray-matter

Gray Matter is a blockchain and network used to secure news articles from edits, tampering, and censorship.

# Docs

## Organization

### Repository
The repository is split into several packages:
* app: the main logic for starting the application, serving the JSON API, etc.
* blockchain: everything to do with just the blockchain
* crypto: cryptography utilities
* network: everything to do with the networking layer and nodes

This project uses [Gradle](https://gradle.org/).
To build and run the application, use the *Gradle* tool window by clicking the Gradle icon in the right-hand toolbar,
or run it directly from the terminal:

* Run `./gradlew run` to build and run the application.
* Run `./gradlew build` to only build the application.
* Run `./gradlew check` to run all checks, including tests.
* Run `./gradlew clean` to clean all build outputs.

### Project
A roadmap can be found here: https://github.com/orgs/alien-head/projects/3/views/1

The roadmap is organized into several categories:
* blockchain: changes that affect the behavior of the blockchain
* network: changes that affect the behavior of the network
* consensus: changes that affect the consensus algorithm(s)
* meta: changes that do not fit into the previous categories (tests, library upgrades, documentation updates, etc.)


## Environment Variables
* `PORT`: the given port for the app to run on. Defaults to 8081 if not provided.
* `NODE_ADDRESS`: the url address (host, port, etc.) for accessing the node. Broadcast to the other nodes in the network. Required.
* `NODE_DONOR`: the url address (host, port, etc.) for accessing another node to populate the blockchain and network pool. If not populated, the node will run in genesis mode. (See Network/Starting up for the first time)
* `NODE_MODE`: the mode to run the node in: `PUBLISHER` or `REPLICA`.
  * `PUBLISHER`: a node that has the ability to publish transactions (articles), mint blocks, and broadcast new blocks to the network.
  * `REPLICA`: a node that is a full replica of the network and blockchain. Receives updates from publisher nodes.

## Network
### Terminology
* Node: a member of a network.
* Genesis node: the first member of the network. Creates the genesis block.
* Donor node: a node that is used to get the blockchain and the list of nodes in the network.
* Publisher node: a node in the network that has permission to mint blocks. 
* Proof of Authority: the consensus mechanism for Gray. Only certain nodes can mint new blocks.
* Transaction: a single news article with its metadata.

### Starting up for the first time
The application should be provided with a donor node url address from an existing network.
If a donor node is provided, then the app's blockchain and network will be populated from the donor.
If a donor node is not provided, the application assumes it is the genesis node and will start its own network/blockchain.
