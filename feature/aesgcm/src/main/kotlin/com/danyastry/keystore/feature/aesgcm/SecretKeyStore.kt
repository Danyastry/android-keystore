package com.danyastry.keystore.feature.aesgcm

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

interface SecretKeyStore {
    fun get(alias: String): SecretKey?
    fun create(alias: String): SecretKey
    fun delete(alias: String)
}

class AndroidKeyStoreSecretKeys : SecretKeyStore {

    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) } }

    override fun get(alias: String): SecretKey? = keyStore.getKey(alias, null) as? SecretKey

    override fun create(alias: String): SecretKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).apply { init(spec) }.generateKey()
    }

    override fun delete(alias: String) {
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_SIZE_BITS = 256
    }
}
