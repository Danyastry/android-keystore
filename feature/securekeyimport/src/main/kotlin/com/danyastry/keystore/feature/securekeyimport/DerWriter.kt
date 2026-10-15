package com.danyastry.keystore.feature.securekeyimport

import java.io.ByteArrayOutputStream
import java.math.BigInteger

object DerWriter {

    private const val TAG_INTEGER = 0x02
    private const val TAG_OCTET_STRING = 0x04
    private const val TAG_NULL = 0x05
    private const val TAG_SEQUENCE = 0x30
    private const val TAG_SET = 0x31
    private const val CLASS_CONTEXT_CONSTRUCTED = 0xA0
    private const val HIGH_TAG_NUMBER = 0x1F
    private const val MAX_LOW_TAG_NUMBER = 30

    fun integer(value: Long): ByteArray = element(TAG_INTEGER, BigInteger.valueOf(value).toByteArray())

    fun octetString(value: ByteArray): ByteArray = element(TAG_OCTET_STRING, value)

    fun nullValue(): ByteArray = element(TAG_NULL, ByteArray(0))

    fun sequence(vararg items: ByteArray): ByteArray = element(TAG_SEQUENCE, concat(items.toList()))

    fun setOf(vararg items: ByteArray): ByteArray = element(TAG_SET, concat(items.sortedWith(::compareDer)))

    fun explicit(tagNumber: Int, content: ByteArray): ByteArray {
        require(tagNumber >= 0) { "Tag number must not be negative, was $tagNumber" }
        val identifier = if (tagNumber <= MAX_LOW_TAG_NUMBER) {
            byteArrayOf((CLASS_CONTEXT_CONSTRUCTED or tagNumber).toByte())
        } else {
            byteArrayOf((CLASS_CONTEXT_CONSTRUCTED or HIGH_TAG_NUMBER).toByte()) + base128(tagNumber)
        }
        return identifier + length(content.size) + content
    }

    fun length(size: Int): ByteArray {
        require(size >= 0) { "Length must not be negative, was $size" }
        if (size < 0x80) return byteArrayOf(size.toByte())
        val bytes = BigInteger.valueOf(size.toLong()).toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
        return byteArrayOf((0x80 or bytes.size).toByte()) + bytes
    }

    private fun element(tag: Int, content: ByteArray): ByteArray = byteArrayOf(tag.toByte()) + length(content.size) + content

    private fun base128(value: Int): ByteArray {
        val groups = generateSequence(value) { it ushr 7 }.takeWhile { it > 0 }.map { it and 0x7F }.toList().reversed()
        return ByteArray(groups.size) { index -> (if (index < groups.lastIndex) groups[index] or 0x80 else groups[index]).toByte() }
    }

    private fun concat(items: List<ByteArray>): ByteArray =
        ByteArrayOutputStream().apply { items.forEach(::write) }.toByteArray()

    private fun compareDer(a: ByteArray, b: ByteArray): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val diff = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (diff != 0) return diff
        }
        return a.size - b.size
    }
}
