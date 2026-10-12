package com.danyastry.keystore.feature.aesgcm

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.danyastry.keystore.core.keystore.AndroidKeyStoreSecretKeys
import com.danyastry.keystore.core.keystore.SecretKeyStore

object AesGcmKeys {

    const val KEY_SIZE_BITS = 256

    fun spec(alias: String): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()

    fun store(): SecretKeyStore = AndroidKeyStoreSecretKeys(KeyProperties.KEY_ALGORITHM_AES, ::spec)
}
