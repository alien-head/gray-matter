package io.alienhead.gray.matter.storage

import java.time.Instant

interface Storage {
    fun storeBlock(block: StoreBlock)
    fun getBlock(hash: String): StoreBlock?
}

data class StoreBlock(
    val hash: String,
    val previousHash: String,
    val data: String,
    val timestamp: Long,
    val height: UInt,
    val createDate: Instant = Instant.now()
)
