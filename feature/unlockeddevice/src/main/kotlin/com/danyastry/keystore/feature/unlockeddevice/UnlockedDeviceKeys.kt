package com.danyastry.keystore.feature.unlockeddevice

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.danyastry.keystore.core.keystore.AndroidKeyStoreSecretKeys
import com.danyastry.keystore.core.keystore.SecretKeyStore

enum class ProbeKey { REGULAR, UNLOCKED_ONLY }

@RequiresApi(Build.VERSION_CODES.P)
object UnlockedDeviceKeys {

    const val KEY_SIZE_BITS = 256

    fun aliasOf(key: ProbeKey): String = "unlocked-device-demo-${key.name.lowercase().replace('_', '-')}"

    fun spec(alias: String, key: ProbeKey): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUnlockedDeviceRequired(key == ProbeKey.UNLOCKED_ONLY)
            .build()

    fun store(key: ProbeKey): SecretKeyStore = AndroidKeyStoreSecretKeys(KeyProperties.KEY_ALGORITHM_AES, { spec(it, key) })
}
