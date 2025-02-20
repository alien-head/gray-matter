package io.alienhead.gray.matter

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    val address: String,
    val type: NodeType,
)

enum class NodeType {
    AUTHOR,
    BASIC,
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
    private val peers: MutableList<Node>,
) {
    /**
     * Adds a node to the network.
     * If the node already exists, do not add it.
     *
     * A duplicate node has the same address.
     */
    fun addPeer(participant: Node): Boolean {
        val duplicateNodes = peers.firstOrNull { it.address == participant.address }
        if (duplicateNodes != null) return false

        peers.add(participant)
        return true
    }

    fun peers() = peers.toList()
    fun addPeers(peers: List<Node>) {
        peers.forEach { addPeer(it) }
    }
}
