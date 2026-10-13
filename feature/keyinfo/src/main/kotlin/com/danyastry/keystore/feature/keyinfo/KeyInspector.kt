package com.danyastry.keystore.feature.keyinfo

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.danyastry.keystore.core.keystore.AndroidKeyStoreSecretKeys
import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.core.keystore.KeystoreEntries

interface KeyInspector {
    fun keys(): List<KeyDetails>
    fun createSampleKey()
}

class AndroidKeyInspector(private val entries: KeystoreEntries = KeystoreEntries()) : KeyInspector {

    private val sampleKeys = AndroidKeyStoreSecretKeys(KeyProperties.KEY_ALGORITHM_AES, ::sampleSpec, entries)

    override fun keys(): List<KeyDetails> = entries.aliases().mapNotNull(entries::details)

    override fun createSampleKey() {
        sampleKeys.create(SAMPLE_ALIAS)
    }

    private fun sampleSpec(alias: String) =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

    companion object {
        const val SAMPLE_ALIAS = "key-info-sample"
    }
}
