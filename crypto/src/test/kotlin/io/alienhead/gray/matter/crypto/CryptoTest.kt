package io.alienhead.gray.matter.crypto

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class CryptoTest: DescribeSpec({
    setupSecurity()

    describe("verify") {

        it("should verify the signature and public key") {

            val keyPair = generateKeyPair().shouldNotBeNull()

            val signature = sign(keyPair.first, "test")

            verifySignature(keyPair.second, "test", signature) shouldBe true
        }
    }
})
