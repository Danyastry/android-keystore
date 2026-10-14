package com.danyastry.keystore.feature.ecdh

import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.core.keystore.KeystoreEntries
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

fun interface Agreement {
    fun sharedSecret(privateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray
}

class KeystoreAgreement : Agreement {
    override fun sharedSecret(privateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray =
        KeyAgreement.getInstance(ALGORITHM, KeystoreEntries.ANDROID_KEY_STORE).run {
            init(privateKey)
            doPhase(peerPublicKey, true)
            generateSecret()
        }
}

class SoftwareAgreement : Agreement {
    override fun sharedSecret(privateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray =
        KeyAgreement.getInstance(ALGORITHM).run {
            init(privateKey)
            doPhase(peerPublicKey, true)
            generateSecret()
        }
}

private const val ALGORITHM = "ECDH"

object SessionKeys {

    const val KEY_SIZE_BYTES = 32
    val SALT = "android-keystore ecdh salt".encodeToByteArray()
    val INFO = "aes-256-gcm session key".encodeToByteArray()

    fun derive(sharedSecret: ByteArray): SecretKey =
        SecretKeySpec(Hkdf.derive(sharedSecret, SALT, INFO, KEY_SIZE_BYTES), "AES")
}

object SessionCipher {

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
}

object Fingerprint {

    private const val SHOWN_BYTES = 8

    fun of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).take(SHOWN_BYTES)
            .joinToString(":") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
