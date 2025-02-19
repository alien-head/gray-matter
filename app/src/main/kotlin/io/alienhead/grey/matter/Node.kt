package io.alienhead.grey.matter

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    val address: String,
    val type: NodeType,
)

enum class NodeType {
    AUTHOR,
    BASIC,
    SELF,
}

@Serializable
data class Network(
    private val nodes: MutableList<Node>,
) {
    /**
     * Adds a node to the network.
     * If the node already exists, do not add it.
     *
     * A duplicate node has the same address.
     */
    fun addNode(participant: Node): Boolean {
        val duplicateNodes = nodes.firstOrNull { it.address == participant.address }
        if (duplicateNodes != null) return false

        nodes.add(participant)
        return true
    }

    fun nodes() = nodes.toList()
}
