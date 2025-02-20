package io.alienhead.gray.matter.crypto

import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun hash(data: String) = try {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(data.toByteArray(Charsets.UTF_8))

    bytes.joinToString("") { "%02x".format(it) }
} catch( e: NoSuchAlgorithmException) {
    ""
} catch( e: UnsupportedEncodingException) {
    ""
}
