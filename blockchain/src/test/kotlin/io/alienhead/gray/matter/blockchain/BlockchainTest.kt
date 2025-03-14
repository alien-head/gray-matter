package io.alienhead.gray.matter.blockchain

import io.alienhead.gray.matter.crypto.generateKeyPair
import io.alienhead.gray.matter.crypto.getString
import io.alienhead.gray.matter.crypto.setupSecurity
import io.alienhead.gray.matter.crypto.sign
import io.alienhead.gray.matter.crypto.toHexString
import io.alienhead.gray.matter.storage.Storage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.security.PrivateKey
import java.security.PublicKey
import java.time.Instant
import java.time.LocalDate

class BlockchainTest : DescribeSpec({
    setupSecurity()

    describe("Blockchain") {
        describe("getBlock") {
            it("should return a Block if found") {
                val storage = mockk<Storage>()
                val blockchain = Blockchain(storage)

                val randomBlock = randomBlock("", 1u)
                every { storage.getBlock(any()) } returns randomBlock.toStore()

                blockchain.getBlock(randomBlock.hash) shouldBe randomBlock
            }
        }

        describe("chain") {
            it("should return a list of 10 blocks") {
                val storage = mockk<Storage>()
                val blockchain = Blockchain(storage)

                every { storage.blocks(any(), any(), any()) } returns listOf(
                    Block.genesis().toStore(),
                    randomBlock("", 1u).toStore(),
                    randomBlock("", 2u).toStore(),
                    randomBlock("", 3u).toStore(),
                    randomBlock("", 4u).toStore(),
                    randomBlock("", 5u).toStore(),
                    randomBlock("", 6u).toStore(),
                    randomBlock("", 7u).toStore(),
                    randomBlock("", 8u).toStore(),
                    randomBlock("", 9u).toStore(),
                )

                blockchain.chain(0, null, null, null) shouldHaveSize 10
            }

            it("should return a list of 5 blocks when size of 5 is selected") {
                val storage = mockk<Storage>()
                val blockchain = Blockchain(storage)

                every { storage.blocks(0, any(), any()) } returns listOf(
                    Block.genesis().toStore(),
                    randomBlock("", 1u).toStore(),
                    randomBlock("", 2u).toStore(),
                    randomBlock("", 3u).toStore(),
                    randomBlock("", 4u).toStore(),
                )

                every { storage.blocks(1, any(), any()) } returns listOf(
                    randomBlock("", 5u).toStore(),
                    randomBlock("", 6u).toStore(),
                    randomBlock("", 7u).toStore(),
                    randomBlock("", 8u).toStore(),
                    randomBlock("", 9u).toStore(),
                )

                blockchain.chain(0, 5, null, null) shouldHaveSize 5
                blockchain.chain(1, 5, null, null) shouldHaveSize 5
            }
        }

        describe("processArticle") {

            it("should accept valid article") {
                val storage = mockk<Storage>()
                val blockchain = Blockchain(storage)
                val publisherKeyPair = generateKeyPair().shouldNotBeNull()

                val goodArticle = randomArticle(publisherKeyPair)

                // Adding one article should not create a block
                blockchain.processArticle(goodArticle).processed shouldBe true
            }

            describe("maximum amount of articles") {
                it("should mint new block") {
                    val genesisBlock = Block.genesis()
                    val storage = mockk<Storage>()
                    val blockchain = Blockchain(storage)
                    val publisherKeyPair = generateKeyPair().shouldNotBeNull()

                    every { storage.latestBlock() } returns genesisBlock.toStore()
                    every { storage.storeBlock(any()) } returns Unit

                    // First add the maximum amount of articles
                    repeat(9) {
                        val processArticle = blockchain.processArticle(randomArticle(publisherKeyPair))
                        processArticle.processed shouldBe true
                        processArticle.block.shouldBeNull()
                    }

                    val newBlock = blockchain.processArticle(randomArticle(publisherKeyPair)).block.shouldNotBeNull()

                    newBlock.previousHash shouldBe genesisBlock.hash
                    newBlock.height shouldBe 1u

                    blockchain.unprocessedCount shouldBe 0
                }
            }
        }

        describe("processBlock") {
            it("should accept block with valid height and matching previousHash") {
                val genesisBlock = Block.genesis()
                val storage = mockk<Storage>()
                val blockchain = Blockchain(storage)
                val goodBlock = randomBlock(genesisBlock.hash, genesisBlock.height + 1u, genesisBlock.timestamp + 1)

                every { storage.latestBlock() } returns genesisBlock.toStore()
                every { storage.chainSize() } returns 1
                every { storage.storeBlock(any()) } returns Unit

                blockchain.processBlock(goodBlock) shouldBe true
            }

            it("should reject block with invalid height") {
                val storage = mockk<Storage>()
                val blockchain = Blockchain(storage)
                val badBlock = randomBlock("", 0u)

                every { storage.latestBlock() } returns randomBlock("", 0u).toStore()
                every { storage.chainSize() } returns 1
                blockchain.processBlock(badBlock) shouldBe false
            }

            it("should reject block without matching previousHash") {
                val storage = mockk<Storage>()
                val blockchain = Blockchain(storage)
                val badBlock = randomBlock("", 1u)

                every { storage.latestBlock() } returns randomBlock("", 0u).toStore()
                every { storage.chainSize() } returns 0

                blockchain.processBlock(badBlock) shouldBe false
            }

            it("should reject block with timestamp in the past") {
                val genesisBlock = Block.genesis()
                val storage = mockk<Storage>()
                val blockchain = Blockchain(storage)
                val goodBlock = randomBlock(genesisBlock.hash, genesisBlock.height + 1u, genesisBlock.timestamp + 1)

                every { storage.latestBlock() } returns genesisBlock.toStore()
                every { storage.chainSize() } returns 1
                every { storage.storeBlock(any()) } returns Unit

                blockchain.processBlock(goodBlock) shouldBe true

                every { storage.latestBlock() } returns goodBlock.toStore()
                every { storage.chainSize() } returns 2

                val badBlock = randomBlock(goodBlock.hash, goodBlock.height + 1u, goodBlock.timestamp - 1)
                blockchain.processBlock(badBlock) shouldBe false

                verify(exactly = 1) { storage.storeBlock(any()) }
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

fun randomArticle(publisherKeyPair: Pair<PrivateKey, PublicKey>): Article {
    val byline = "byline"
    val headline = "headline"
    val section = "section"
    val content = "content"
    val date = LocalDate.now().toString()

    val signature = sign(publisherKeyPair.first, byline + headline +section + content + date)

    return Article(
        publisherKey = publisherKeyPair.second.getString(),
        byline,
        headline,
        section,
        content,
        date,
        signature.toHexString(),
    )
}
