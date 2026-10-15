package com.danyastry.keystore.feature.securekeyimport

class DerNode(val tag: Int, val content: ByteArray, val encoded: ByteArray) {

    fun children(): List<DerNode> {
        val nodes = mutableListOf<DerNode>()
        var offset = 0
        while (offset < content.size) {
            val node = DerReader.read(content, offset)
            nodes += node
            offset += node.encoded.size
        }
        return nodes
    }

    fun asLong(): Long = content.fold(if (content.first() < 0) -1L else 0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xff) }
}

object DerReader {

    fun read(bytes: ByteArray, start: Int = 0): DerNode {
        var offset = start
        var tag = bytes[offset++].toInt() and 0xff
        if (tag and 0x1f == 0x1f) {
            var number = 0
            do {
                val next = bytes[offset++].toInt() and 0xff
                number = (number shl 7) or (next and 0x7f)
            } while (next and 0x80 != 0)
            tag = (tag shl 16) or number
        }
        var length = bytes[offset++].toInt() and 0xff
        if (length and 0x80 != 0) {
            val count = length and 0x7f
            length = 0
            repeat(count) { length = (length shl 8) or (bytes[offset++].toInt() and 0xff) }
        }
        return DerNode(tag, bytes.copyOfRange(offset, offset + length), bytes.copyOfRange(start, offset + length))
    }
}
