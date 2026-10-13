package com.danyastry.keystore.feature.attestation

import java.math.BigInteger

class DerElement(val tag: Int, val value: ByteArray) {

    fun children(): List<DerElement> = DerReader(value).readAll()

    fun asInt(): Int {
        require(tag == TAG_INTEGER || tag == TAG_ENUMERATED) { "Expected INTEGER or ENUMERATED, got tag $tag" }
        return BigInteger(value).toInt()
    }

    fun asBytes(): ByteArray {
        require(tag == TAG_OCTET_STRING) { "Expected OCTET STRING, got tag $tag" }
        return value
    }

    companion object {
        const val TAG_INTEGER = 0x02
        const val TAG_OCTET_STRING = 0x04
        const val TAG_ENUMERATED = 0x0A
        const val TAG_SEQUENCE = 0x30
    }
}

class DerReader(private val bytes: ByteArray) {

    private var position = 0

    fun readAll(): List<DerElement> = buildList {
        while (position < bytes.size) add(read())
    }

    fun read(): DerElement {
        val tag = next()
        val length = readLength()
        require(position + length <= bytes.size) { "DER length $length runs past the end" }
        val value = bytes.copyOfRange(position, position + length)
        position += length
        return DerElement(tag, value)
    }

    private fun readLength(): Int {
        val first = next()
        if (first < 0x80) return first
        val count = first and 0x7F
        require(count in 1..3) { "Unsupported DER length of $count bytes" }
        return (0 until count).fold(0) { length, _ -> (length shl 8) or next() }
    }

    private fun next(): Int {
        require(position < bytes.size) { "Unexpected end of DER data" }
        return bytes[position++].toInt() and 0xFF
    }
}
