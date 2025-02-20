package io.alienhead.gray.matter.blockchain

import io.alienhead.gray.matter.crypto.hash
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Instant

/**
 * The chain must be initialized with a genesis block.
 */
@Serializable
data class Blockchain(
    val chain: MutableList<Block>,
    private val unprocessedArticles: MutableList<Article> = mutableListOf(),
) {
    /**
     * Returns a set of blocks from the blockchain by page number and size.
     * If size is null, use the default of 10.
     */
    fun chain(page: Int, size: Int?): List<Block> {
        val chunked = this.chain.chunked(size ?: 10)

        return if (page > chunked.size - 1) {
            emptyList()
        } else {
            chunked[page]
        }
    }

    fun isValid(): Boolean {
        if (chain.size < 2) return true

        for (i in 1..chain.size) {
            val currentBlock = chain[i]
            val previousBlock = chain[i-1]

            if (previousBlock.hash != currentBlock.previousHash) {
                return false
            }
        }

        return true
    }

    fun processArticle(article: Article): Boolean {
        unprocessedArticles.add(article)

        /*
         * if the unprocessed article count is 10 or greater,
         * mint the articles into a Block and add it to the chain.
         */
        if (unprocessedArticles.size >= 10) {
            val data = Json.encodeToString(unprocessedArticles)
            val timestamp = Instant.now().toEpochMilli()
            val previousHash = chain.last().hash

            chain.add(
                Block(
                    previousHash,
                    data,
                    timestamp,
                )
            )

            unprocessedArticles.clear()

            return true
        }

        return false
    }
}

@Serializable
data class Block(
    val previousHash: String,
    // Data represents articles that have been minted into a json string
    val data: String,
    val timestamp: Long,
) {
    val hash = hash(previousHash + timestamp + data)
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
