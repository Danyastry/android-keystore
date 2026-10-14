package com.danyastry.keystore.feature.authtimeout

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.danyastry.keystore.core.keystore.AndroidKeyStoreSecretKeys
import com.danyastry.keystore.core.keystore.SecretKeyStore

@RequiresApi(Build.VERSION_CODES.R)
object AuthTimeoutKeys {

    const val KEY_SIZE_BITS = 256
    const val TIMEOUT_SECONDS = 15
    const val AUTHENTICATORS = KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL

    fun spec(alias: String): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(TIMEOUT_SECONDS, AUTHENTICATORS)
            .build()

    fun store(): SecretKeyStore = AndroidKeyStoreSecretKeys(KeyProperties.KEY_ALGORITHM_AES, ::spec)
}
