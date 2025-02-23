package io.alienhead.gray.matter.network

import io.alienhead.gray.matter.blockchain.Block
import io.alienhead.gray.matter.blockchain.Blockchain
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Node(
    val address: String,
    val type: NodeType,
)

enum class NodeType {
    PUBLISHER,
    REPLICA,
    UTILITY,
}

/**
 * Information of the current application
 */
@Serializable
data class Info(
    val node: NodeInfo,
)

/**
 * Information for the current application's node
 */
@Serializable
data class NodeInfo(
    val address: String,
    val type: NodeType,
) {
    fun toNode() = Node(
        address,
        type,
    )
}

@Serializable
data class Network(
    @Transient private val client: NetworkClient = NetworkWebClient(),
    private val peers: MutableList<Node>,
) {
    fun peers() = peers.toList()

    /**
     * Adds a node to the network.
     * If the node already exists, do not add it.
     *
     * A duplicate node has the same address.
     */
    private fun addPeer(participant: Node): Boolean {
        val duplicateNodes = peers.firstOrNull { it.address == participant.address }
        if (duplicateNodes != null) return false

        peers.add(participant)
        return true
    }

    /**
     * Adds a node to the network. Returns true if successfully added.
     * If the node already exists do not add it and return false.
     */
    suspend fun addPeer(node: Node, broadcast: Boolean?) {
        if (node.address.isBlank()) {
            throw RuntimeException("Address cannot be empty")
        }

        val added = addPeer(node)

        // Broadcast the addition of the node to the network
        if (broadcast == true && added) {
            peers
                .filter {
                    it.address != node.address
                }
                .forEach {
//                    log.info("Broadcasting new peer to: ${it.address}")
                    client.broadcastPeer(it.address, node)

                    // TODO track if the peer was unsuccessfully broadcast
                }
        }
    }

    suspend fun broadcastBlock(newBlock: Block) {
        peers.forEach {
            client.broadcastBlock(it.address, newBlock)
        }
    }

    suspend fun downloadPeers(donorNode: String) {
        peers.addAll(client.downloadPeers(donorNode))
    }

    suspend fun downloadPeerInfo(peer: String) {
        val peerInfo = client.downloadPeerInfo(peer)

        peers.add(peerInfo.node.toNode())
    }

    suspend fun downloadBlockchain(peer: String) = client.downloadBlockchain(peer)

    suspend fun updatePeer(peer: String, node: Node) {
        client.updatePeer(peer, node)
    }
}

interface NetworkClient {
    suspend fun broadcastPeer(address: String, node: Node)
    suspend fun broadcastBlock(address: String, block: Block)
    suspend fun downloadPeers(address: String): List<Node>
    suspend fun downloadPeerInfo(address: String): Info
    suspend fun downloadBlockchain(address: String): List<Block>
    suspend fun updatePeer(address: String, node: Node)
}

class NetworkWebClient: NetworkClient {
    private val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }

    override suspend fun broadcastPeer(address: String, node: Node) {
        client.post("$address/network/node?broadcast=true") {
            contentType(ContentType.Application.Json)
            setBody(node)
        }
    }

    override suspend fun broadcastBlock(address: String, block: Block) {
        client.post("$address/blockchain/block") {
            contentType(ContentType.Application.Json)
            setBody(block)
        }
    }

    override suspend fun downloadPeers(address: String): List<Node> {
        val response = runBlocking  { client.get("$address/network") }

        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("Failed to download peers from address: $address")
        }

        return response.body<Network>().peers()
    }

    override suspend fun downloadPeerInfo(address: String): Info {
        val response = runBlocking { client.get("$address/status") }
        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("Failed to download peer info with address: $address")
        }

        return response.body<Info>()
    }

    override suspend fun downloadBlockchain(address: String): List<Block> {
        // Download the blockchain
        var page = 0

        var newBlocks = downloadBlockchain(address, page).toMutableList()

        val blocks = newBlocks.toMutableList()
        while (newBlocks.isNotEmpty()) {
            page++
            newBlocks = downloadBlockchain(address, page).toMutableList()
            blocks.addAll(newBlocks)
        }

        return blocks
    }

    override suspend fun updatePeer(address: String, node: Node) {
        val response = runBlocking { client.post("$address/network/node?broadcast=true") {
            contentType(ContentType.Application.Json)
            setBody(node)
        } }
        if (response.status != HttpStatusCode.Created) {
            // TODO since we have a list of nodes, go down the list and keep trying other nodes
            throw RuntimeException("Failed to submit self to the network to address: $address")
        }
    }

    // TODO since we have a list of nodes, we should be able to pick back up from another node if this donor goes down
    private suspend fun downloadBlockchain(address: String, page: Int): List<Block> {
        val response = client.get("$address/blockchain?page=$page&size=10")
        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("Failed to download the blockchain from address: $address")
        }
        return response.body<List<Block>>()
    }
}
