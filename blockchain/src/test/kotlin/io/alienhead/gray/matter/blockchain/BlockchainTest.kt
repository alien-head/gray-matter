package io.alienhead.gray.matter.blockchain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import java.time.Instant
import java.time.LocalDate

class BlockchainTest : DescribeSpec({
    describe("Blockchain") {
        describe("processArticle") {

            it("should accept valid article") {
                val blockchain = Blockchain(mutableListOf(Block.genesis()))

                val goodArticle = randomArticle()

                // Adding one article should not create a block
                blockchain.processArticle(goodArticle).shouldBeNull()
            }

            describe("maximum amount of articles") {
                it("should mint new block") {
                    val genesisBlock = Block.genesis()
                    val blockchain = Blockchain(mutableListOf(genesisBlock))

                    // First add the maximum amount of articles
                    repeat(9) {
                        blockchain.processArticle(randomArticle()).shouldBeNull()
                    }

                    val newBlock = blockchain.processArticle(randomArticle()).shouldNotBeNull()

                    newBlock.previousHash shouldBe genesisBlock.hash
                    newBlock.height shouldBe 1u

                    blockchain.unprocessedCount shouldBe 0
                }
            }
        }

        describe("processBlock") {
            it("should accept block with valid height and matching previousHash") {
                val genesisBlock = Block.genesis()
                val blockchain = Blockchain(mutableListOf(genesisBlock))
                val goodBlock = randomBlock(genesisBlock.hash, genesisBlock.height + 1u)

                blockchain.processBlock(goodBlock) shouldBe true
            }

            it("should reject block with invalid height") {
                val blockchain = Blockchain(mutableListOf(Block.genesis()))
                val badBlock = randomBlock("", 0u)

                blockchain.processBlock(badBlock) shouldBe false
            }

            it("should reject block without matching previousHash") {
                val blockchain = Blockchain(mutableListOf(Block.genesis()))
                val badBlock = randomBlock("", 1u)

                blockchain.processBlock(badBlock) shouldBe false
            }

            it("should reject block with timestamp in the past") {
                val genesisBlock = Block.genesis()
                val blockchain = Blockchain(mutableListOf(genesisBlock))
                val goodBlock = randomBlock(genesisBlock.hash, genesisBlock.height + 1u)

                blockchain.processBlock(goodBlock) shouldBe true

                val badBlock = randomBlock(goodBlock.hash, goodBlock.height + 1u, goodBlock.timestamp - 1)
                blockchain.processBlock(badBlock) shouldBe false
            }
        }
    }

    describe("genesisBlock") {
        it("should have no previous hash and 0 height") {
            val genesisBlock = Block.genesis()

            genesisBlock.previousHash.shouldBeEmpty()
            genesisBlock.height shouldBe 0u
            genesisBlock.data shouldBe "Genesis"
        }
    }
})

fun randomBlock(previousHash: String, height: UInt, timestamp: Long = Instant.now().toEpochMilli()) = Block(
    previousHash = previousHash,
    data = "test",
    timestamp = timestamp,
    height = height,
)

fun randomArticle() = Article(
    publisherId = "publisherId",
    byline = "byline",
    headline = "headline",
    section = "section",
    content = "content",
    date = LocalDate.now().toString(),
)
