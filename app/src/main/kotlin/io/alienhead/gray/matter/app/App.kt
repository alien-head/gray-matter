package io.alienhead.gray.matter.app

import io.alienhead.gray.matter.blockchain.Article
import io.alienhead.gray.matter.blockchain.Block
import io.alienhead.gray.matter.blockchain.Blockchain
import io.alienhead.gray.matter.network.Info
import io.alienhead.gray.matter.network.Network
import io.alienhead.gray.matter.network.Node
import io.alienhead.gray.matter.network.NodeInfo
import io.alienhead.gray.matter.network.NodeType
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
import java.time.Instant

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

    val blockchain = blockchainModule(donorNode == null)

    val network = Network(mutableListOf())

    val nodeInfo = NodeInfo(
        address = selfAddress,
        type = NodeType.valueOf(nodeMode)
    )

    // If a donor node has been specified,
    // get the blockchain, transactions, and network from it
    if (donorNode != null) {
        val httpClient = HttpClient(CIO) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        var response = runBlocking  { httpClient.get("$donorNode/network") }

        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("Failed to load from donor node with address: $donorNode")
        }

        val peers = runBlocking { response.body<Network>().peers().toMutableList() }

        // Also get the donor node's info
        response = runBlocking { httpClient.get("$donorNode/info") }
        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("Failed to load from donor node with address: $donorNode")
        }

        val donorNodeInfo = runBlocking { response.body<Info>() }

        val peersAndDonor = peers + donorNodeInfo.node.toNode()

        network.addPeers(peersAndDonor)

        // TODO Get the blockchain

        // Update the donor node with the new peer
        response = runBlocking { httpClient.post("$donorNode/network/node?broadcast=true") {
            contentType(ContentType.Application.Json)
            setBody(nodeInfo.toNode())
        } }
        if (response.status != HttpStatusCode.OK) {
            // TODO since we have a list of nodes, go down the list and keep trying other nodes
            throw RuntimeException("Failed to submit self to the network to address: $donorNode")
        }
    }

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
        }

        route("/article") {
            /**
             * Processes the article. If enough articles are processed, mint a new block.
             * TODO notify the network of the new block.
             */
            post {
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

                if (mintedBlock) {
                    call.application.engine.environment.log.info("Minted a new block: ${blockchain.chain.last()}")
                    call.response.status(HttpStatusCode.Created)
                } else {
                    call.application.engine.environment.log.info("Processed a new article")
                    call.response.status(HttpStatusCode.OK)
                }
            }
        }
    }
}

fun Application.blockchainModule(isGenesis: Boolean): Blockchain {
    return if (isGenesis) {
        Blockchain(
            chain = mutableListOf(
                Block(
                    "",
                    "Genesis",
                    Instant.now().toEpochMilli(),
                )
            )
        )
    } else {
        Blockchain(chain = mutableListOf())
    }
}
