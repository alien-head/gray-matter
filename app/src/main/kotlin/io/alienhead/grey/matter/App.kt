package io.alienhead.grey.matter

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
import io.ktor.server.application.log
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import java.time.Instant

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

suspend fun Application.module() {
    val selfAddress = environment.config.property("ktor.node.address").getString()
    val donorNode = environment.config.propertyOrNull("node.donor")

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
        })
    }

    val blockchain = blockchainModule(donorNode == null)

    // TODO change self address to environment variable
    val network = Network(mutableListOf(Node(selfAddress, NodeType.SELF)))

    // If a donor node has been specified,
    // get the blockchain, transactions, and network from it
    if (donorNode != null) {
        val httpClient = HttpClient(CIO) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        val response = httpClient.get("$donorNode/network")

        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("Failed to load from donor node with address: $donorNode")
        }
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        route("/network") {
            get {
                call.respond(network)
            }

            route("/node") {
                post {

                    val broadcast = call.parameters["broadcast"]

                    val node = call.receive<Node>()

                    if (node.type == NodeType.SELF || node.address.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest)
                    }

                    val added = network.addNode(node)

                    // Broadcast the addition of the node to the network
                    if (broadcast?.toBooleanStrictOrNull() == true && added) {
                        val client = HttpClient(CIO) {
                            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                                json()
                            }
                        }

                        network.nodes()
                            .filter {
                                it.type != NodeType.SELF && it.address != node.address
                            }
                            .forEach {
                                log.info("Broadcasting new node to: ${it.address}")
                                client.post("${it.address}/network/node?broadcast=true") {
                                    contentType(ContentType.Application.Json)
                                    setBody(node)
                                }
                            }

                        client.close()
                    }

                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route("/blockchain") {
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
