package com.danyastry.keystore.feature.securekeyimport

import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

class WrappedKey(
    val bytes: ByteArray,
    val encryptedTransportKey: ByteArray,
    val iv: ByteArray,
    val keyDescription: ByteArray,
    val encryptedKey: ByteArray,
    val tag: ByteArray,
)

object SecureKeyWrapper {

    const val WRAPPER_FORMAT_VERSION = 0L
    const val KEY_FORMAT_RAW = 3L
    const val TRANSPORT_KEY_BYTES = 32
    const val IV_BYTES = 12
    const val TAG_BYTES = 16

    const val TAG_PURPOSE = 1
    const val TAG_ALGORITHM = 2
    const val TAG_KEY_SIZE = 3
    const val TAG_BLOCK_MODE = 4
    const val TAG_PADDING = 6
    const val TAG_MIN_MAC_LENGTH = 8
    const val TAG_NO_AUTH_REQUIRED = 503

    const val PURPOSE_ENCRYPT = 0L
    const val PURPOSE_DECRYPT = 1L
    const val ALGORITHM_AES = 32L
    const val BLOCK_MODE_GCM = 32L
    const val PADDING_NONE = 1L
    const val MIN_MAC_LENGTH_BITS = 128L

    const val RSA_TRANSFORMATION = "RSA/ECB/OAEPPadding"
    val OAEP_PARAMETERS = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)

    fun aesGcmAuthorizations(keySizeBits: Int): ByteArray = DerWriter.sequence(
        DerWriter.explicit(TAG_PURPOSE, DerWriter.setOf(DerWriter.integer(PURPOSE_ENCRYPT), DerWriter.integer(PURPOSE_DECRYPT))),
        DerWriter.explicit(TAG_ALGORITHM, DerWriter.integer(ALGORITHM_AES)),
        DerWriter.explicit(TAG_KEY_SIZE, DerWriter.integer(keySizeBits.toLong())),
        DerWriter.explicit(TAG_BLOCK_MODE, DerWriter.setOf(DerWriter.integer(BLOCK_MODE_GCM))),
        DerWriter.explicit(TAG_PADDING, DerWriter.setOf(DerWriter.integer(PADDING_NONE))),
        DerWriter.explicit(TAG_MIN_MAC_LENGTH, DerWriter.integer(MIN_MAC_LENGTH_BITS)),
        DerWriter.explicit(TAG_NO_AUTH_REQUIRED, DerWriter.nullValue()),
    )

    fun wrap(keyMaterial: ByteArray, wrappingKey: PublicKey, random: SecureRandom = SecureRandom()): WrappedKey {
        val keyDescription = DerWriter.sequence(
            DerWriter.integer(KEY_FORMAT_RAW),
            aesGcmAuthorizations(keyMaterial.size * Byte.SIZE_BITS),
        )
        val transportKey = ByteArray(TRANSPORT_KEY_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)

        val encryptedTransportKey = Cipher.getInstance(RSA_TRANSFORMATION).run {
            init(Cipher.ENCRYPT_MODE, wrappingKey, OAEP_PARAMETERS)
            doFinal(transportKey)
        }
        val sealed = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(transportKey, "AES"), GCMParameterSpec(TAG_BYTES * Byte.SIZE_BITS, iv))
            updateAAD(keyDescription)
            doFinal(keyMaterial)
        }
        transportKey.fill(0)
        val encryptedKey = sealed.copyOfRange(0, sealed.size - TAG_BYTES)
        val tag = sealed.copyOfRange(sealed.size - TAG_BYTES, sealed.size)

        val bytes = DerWriter.sequence(
            DerWriter.integer(WRAPPER_FORMAT_VERSION),
            DerWriter.octetString(encryptedTransportKey),
            DerWriter.octetString(iv),
            keyDescription,
            DerWriter.octetString(encryptedKey),
            DerWriter.octetString(tag),
        )
        return WrappedKey(bytes, encryptedTransportKey, iv, keyDescription, encryptedKey, tag)
    }
}
