package io.alienhead.gray.matter.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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

    fun addPeers(peers: List<Node>) {
        peers.forEach { addPeer(it) }
    }

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
                }

        }
    }
}

interface NetworkClient {
    suspend fun broadcastPeer(address: String, node: Node)
}

class NetworkWebClient: NetworkClient {
    private val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }

    override suspend fun broadcastPeer(address: String, node: Node) {
        client.post("${address}/network/node?broadcast=true") {
            contentType(ContentType.Application.Json)
            setBody(node)
        }
    }
}
