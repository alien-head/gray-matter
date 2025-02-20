package io.alienhead.gray.matter.app

import io.alienhead.gray.matter.blockchain.Article
import io.alienhead.gray.matter.blockchain.Block
import io.alienhead.gray.matter.blockchain.Blockchain
import io.alienhead.gray.matter.network.Info
import io.alienhead.gray.matter.network.Network
import io.alienhead.gray.matter.network.NetworkWebClient
import io.alienhead.gray.matter.network.Node
import io.alienhead.gray.matter.network.NodeInfo
import io.alienhead.gray.matter.network.NodeType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    /**
     * Environment Variables
     *
     * Optional variables use propertyOrNull() while required properties use property()
     */
    val selfAddress = environment.config.property("ktor.node.address").getString()
    val donorNode = environment.config.propertyOrNull("ktor.node.donor")?.getString()
    val nodeMode = environment.config.property("ktor.node.mode").getString()

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
        })
    }

    val network = Network(NetworkWebClient(), mutableListOf())

    val nodeInfo = NodeInfo(
        address = selfAddress,
        type = NodeType.valueOf(nodeMode)
    )

    environment.log.info("Starting up node as ${nodeInfo.type}")

    // If a donor node has been specified,
    // get the blockchain, transactions, and network from it
    val blockchain = if (donorNode != null) {
        environment.log.info("Donor node address found. Starting donor process.")

        environment.log.info("Starting download of network peers from donor...")
        runBlocking { network.downloadPeers(donorNode) }

        // Also get the donor node's info
        runBlocking { network.downloadPeerInfo(donorNode) }

        environment.log.info("Done downloading peers.")

        environment.log.info("Downloading full blockchain...")
        val blockchain = runBlocking { network.downloadBlockchain(donorNode)}

        environment.log.info("Full blockchain downloaded from donor.")

        environment.log.info("Adding self to the network...")
        // Update the donor node with the new peer
        runBlocking { network.updatePeer(donorNode, nodeInfo.toNode())}

        blockchain
    } else {
        environment.log.info("No donor node address found. Beginning genesis...")
         Blockchain(chain = mutableListOf(Block.genesis()))
    }

    environment.log.info("Completed node startup.")

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/info") {
            call.respond(Info(nodeInfo))
        }

        route("/network") {
            /**
             * Gets the full network object.
             *
             * Mostly used to see all peers in the network.
             */
            get {
                call.respond(network)
            }

            route("/node") {

                /**
                 * Add a node to the network.
                 * If url param 'broadcast' is true,
                 * send the node to all peers too.
                 */
                post {

                    val broadcast = call.parameters["broadcast"]?.toBooleanStrictOrNull()

                    val node = call.receive<Node>()

                    network.addPeer(node, broadcast)

                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route("/blockchain") {

            /**
             * Gets a section of the blockchain with url params page and size
             */
            get {
                val page = call.parameters["page"]?.toIntOrNull()
                val size = call.parameters["size"]?.toIntOrNull()

                if (page == null || page < 0 || (size != null && size < 1)) {
                    call.response.status(HttpStatusCode.BadRequest)
                    return@get
                }

                val subchain = blockchain.chain(page, size)

                call.respond(HttpStatusCode.OK, subchain)
            }

            route("/block") {
                post {
                    val newBlock = call.receive<Block>()

                    blockchain.processBlock(newBlock)
                }
            }
        }

        route("/article") {
            /**
             * Processes the article. If enough articles are processed, mint a new block.
             * TODO notify the network of the new article if a block was not minted.
             * Only a node running in PUBLISHER mode should be able to accept an unprocessed article.
             * TODO only a PUBLISHER node that is the selected broadcaster should be able to broadcast the new block.
             */
            post {

                if (nodeInfo.type == NodeType.REPLICA) {
                    call.response.status(HttpStatusCode.Forbidden)
                    return@post
                }

                val article = call.receive<Article>()

                if (article.publisherId == ""
                    || article.byline == ""
                    || article.headline == ""
                    || article.section == ""
                    || article.content == ""
                    || article.date == "") {
                    call.response.status(HttpStatusCode.BadRequest)
                    return@post
                }

                val mintedBlock = blockchain.processArticle(article)

                if (mintedBlock != null) {
                    call.application.engine.environment.log.info("Minted a new block: $mintedBlock")

                    // Broadcast the new block to peers
                    network.broadcastBlock(mintedBlock)

                    call.response.status(HttpStatusCode.Created)
                } else {
                    call.application.engine.environment.log.info("Processed a new article")
                    call.response.status(HttpStatusCode.OK)
                }
            }
        }
    }
}
