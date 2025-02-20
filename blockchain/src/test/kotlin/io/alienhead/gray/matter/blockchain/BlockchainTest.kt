package io.alienhead.gray.matter.blockchain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import java.time.Instant

class BlockchainTest : DescribeSpec({
    describe("Blockchain") {

        describe("processBlock") {
            it("should accept block with valid height and matching previousHash") {
                val genesisBlock = Block.genesis()
                val blockchain = Blockchain(mutableListOf(genesisBlock))
                val goodBlock = Block(
                    previousHash = genesisBlock.hash,
                    data = "test",
                    timestamp = Instant.now().toEpochMilli(),
                    height = 1u,
                )

                blockchain.processBlock(goodBlock) shouldBe true
            }

            it("should reject block with invalid height") {
                val blockchain = Blockchain(mutableListOf(Block.genesis()))
                val badBlock = Block(
                    previousHash = "",
                    data = "test",
                    timestamp = Instant.now().toEpochMilli(),
                    height = 0u,
                )

                blockchain.processBlock(badBlock) shouldBe false
            }

            it("should reject block without matching previousHash") {
                val blockchain = Blockchain(mutableListOf(Block.genesis()))
                val badBlock = Block(
                    previousHash = "asdf",
                    data = "test",
                    timestamp = Instant.now().toEpochMilli(),
                    height = 1u,
                )

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
