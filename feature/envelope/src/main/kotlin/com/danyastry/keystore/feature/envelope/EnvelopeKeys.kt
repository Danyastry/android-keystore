package com.danyastry.keystore.feature.envelope

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.danyastry.keystore.core.keystore.AndroidKeyStoreKeyPairs
import com.danyastry.keystore.core.keystore.KeyPairStore

object EnvelopeKeys {

    fun spec(alias: String): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(EnvelopeCipher.RSA_KEY_SIZE_BITS)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .build()

    fun store(): KeyPairStore = AndroidKeyStoreKeyPairs(KeyProperties.KEY_ALGORITHM_RSA, ::spec)
}
