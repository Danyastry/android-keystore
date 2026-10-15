package com.danyastry.keystore.feature.keyimport

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.core.keystore.KeystoreEntries
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface KeyImporter {
    fun import(alias: String, keyBytes: ByteArray)
    fun get(alias: String): SecretKey?
    fun details(alias: String): KeyDetails?
    fun delete(alias: String)
}

class AndroidKeyImporter(private val entries: KeystoreEntries = KeystoreEntries()) : KeyImporter {

    override fun import(alias: String, keyBytes: ByteArray) {
        entries.importSecretKey(alias, SecretKeySpec(keyBytes, KeyProperties.KEY_ALGORITHM_AES), protection())
    }

    override fun get(alias: String): SecretKey? = entries.secretKey(alias)

    override fun details(alias: String): KeyDetails? = entries.details(alias)

    override fun delete(alias: String) = entries.delete(alias)

    companion object {
        fun protection(): KeyProtection =
            KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
    }
}

object ImportCipher {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun encrypt(key: SecretKey, plaintext: ByteArray): EncryptedMessage {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        return EncryptedMessage(cipher.iv, cipher.doFinal(plaintext))
    }

    fun decrypt(key: SecretKey, message: EncryptedMessage): ByteArray =
        Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(EncryptedMessage.TAG_SIZE * Byte.SIZE_BITS, message.iv))
            doFinal(message.ciphertext)
        }

    fun softwareKey(keyBytes: ByteArray): SecretKey = SecretKeySpec(keyBytes, "AES")
}
