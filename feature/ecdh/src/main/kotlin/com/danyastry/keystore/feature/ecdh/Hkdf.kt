package com.danyastry.keystore.feature.ecdh

import java.io.ByteArrayOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Hkdf {

    private const val ALGORITHM = "HmacSHA256"
    private const val HASH_SIZE = 32
    private const val MAX_BLOCKS = 255

    fun extract(salt: ByteArray, inputKeyMaterial: ByteArray): ByteArray =
        mac(if (salt.isEmpty()) ByteArray(HASH_SIZE) else salt).doFinal(inputKeyMaterial)

    fun expand(pseudoRandomKey: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length in 1..MAX_BLOCKS * HASH_SIZE) { "HKDF-SHA256 can produce 1 to ${MAX_BLOCKS * HASH_SIZE} bytes, asked for $length" }
        val mac = mac(pseudoRandomKey)
        val output = ByteArrayOutputStream(length)
        var block = ByteArray(0)
        var counter = 1
        while (output.size() < length) {
            mac.update(block)
            mac.update(info)
            mac.update(counter.toByte())
            block = mac.doFinal()
            output.write(block, 0, minOf(block.size, length - output.size()))
            counter++
        }
        return output.toByteArray()
    }

    fun derive(inputKeyMaterial: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray =
        expand(extract(salt, inputKeyMaterial), info, length)

    private fun mac(key: ByteArray): Mac = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(key, ALGORITHM)) }
}
