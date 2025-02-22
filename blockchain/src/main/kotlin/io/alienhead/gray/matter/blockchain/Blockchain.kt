package io.alienhead.gray.matter.blockchain

import io.alienhead.gray.matter.crypto.hash
import io.alienhead.gray.matter.storage.Storage
import io.alienhead.gray.matter.storage.StoreBlock
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * The chain must be initialized with a genesis block.
 */
data class Blockchain(
    val storage: Storage,
    private val unprocessedArticles: MutableList<Article> = mutableListOf(),
) {
    val unprocessedCount get() = unprocessedArticles.size

    /**
     * Returns a set of blocks from the blockchain by page number and size.
     * If size is null, use the default of 10.
     */
    fun chain(page: Int, size: Int?, sort: String?): List<Block> {

        return if (page < 0) {
            emptyList()
        } else {
            storage.blocks(page, size ?: 10, sort).toBlocks()
        }
    }

    fun processArticle(article: Article): Block? {
        unprocessedArticles.add(article)

        /*
         * if the unprocessed article count is 10 or greater,
         * mint the articles into a Block and add it to the chain.
         */
        if (unprocessedArticles.size >= 10) {
            val data = Json.encodeToString(unprocessedArticles)
            val timestamp = Instant.now().toEpochMilli()
            val latestBlock = storage.latestBlock()
            val previousHash = latestBlock!!.hash

            val newBlock = Block(
                previousHash,
                data,
                timestamp,
                latestBlock.height + 1u
            )

            storage.storeBlock(newBlock.toStore())

            unprocessedArticles.clear()

            return newBlock
        }

        return null
    }

    /**
     * When adding a block from another node, verify and process it.
     * If it fails to verify, do not add it to the chain.
     */
    fun processBlock(block: Block): Boolean {
        val latestBlock = storage.latestBlock()

        if (latestBlock != null) {
            if (block.height <= storage.chainSize().toUInt() - 1u) return false

            if (block.previousHash != latestBlock.hash) return false

            if (block.timestamp <= latestBlock.timestamp) return false
        }

        storage.storeBlock(block.toStore())
        return true
    }
}

@Serializable
data class Block(
    val previousHash: String,
    // Data represents articles that have been minted into a json string
    val data: String,
    val timestamp: Long,
    val height: UInt,
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault val hash: String = hash(previousHash + timestamp + data),
) {
    companion object {
        fun genesis(): Block {
            return Block(
                "",
                "Genesis",
                Instant.now().toEpochMilli(),
                0u,
            )
        }
    }
}

@Serializable
data class Article(
    /**
     * The public key corresponding to the publisher of the article
     */
    val publisherId: String,
    val byline: String,
    val headline: String,
    /**
     * The news section for the article (this may be different per publisher)
     */
    val section: String,
    /**
     * The html content of the article
     */
    val content: String,
    /**
     * The date in format YYYY-MM-dd
     */
    val date: String,
) {
    val id = hash(publisherId + byline + headline + section + content + date)
}

fun Block.toStore() = StoreBlock(hash, previousHash, data, timestamp, height)
fun StoreBlock.toBlock() = Block(previousHash, data, timestamp, height, hash)
fun List<StoreBlock>.toBlocks() = map { it.toBlock() }
