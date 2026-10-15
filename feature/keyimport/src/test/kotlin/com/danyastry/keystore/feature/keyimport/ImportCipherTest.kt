package com.danyastry.keystore.feature.keyimport

import com.google.common.truth.Truth.assertThat
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportCipherTest {

    private val keyBytes = KeyHex.parseOrNull("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4")!!

    @Test
    fun `two keys made from the same bytes are interchangeable`() {
        val message = ImportCipher.encrypt(ImportCipher.softwareKey(keyBytes), "Attack at dawn".encodeToByteArray())

        assertThat(ImportCipher.decrypt(ImportCipher.softwareKey(keyBytes.copyOf()), message).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `other bytes cannot decrypt`() {
        val message = ImportCipher.encrypt(ImportCipher.softwareKey(keyBytes), "Attack at dawn".encodeToByteArray())
        val other = keyBytes.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) { ImportCipher.decrypt(ImportCipher.softwareKey(other), message) }
    }
}
