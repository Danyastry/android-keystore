package com.danyastry.keystore.feature.keyimport

import java.security.SecureRandom

object KeyHex {

    val SIZES_BYTES = setOf(16, 24, 32)
    const val GENERATED_SIZE_BYTES = 32

    fun parseOrNull(text: String): ByteArray? {
        val hex = text.filterNot { it.isWhitespace() || it == ':' }
        if (hex.length % 2 != 0 || hex.length / 2 !in SIZES_BYTES) return null
        if (!hex.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) return null
        return ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    fun format(bytes: ByteArray): String = bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    fun random(random: SecureRandom = SecureRandom()): String = format(ByteArray(GENERATED_SIZE_BYTES).also(random::nextBytes))
}
