package com.danyastry.keystore.feature.hmac

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.danyastry.keystore.core.keystore.AndroidKeyStoreSecretKeys
import com.danyastry.keystore.core.keystore.SecretKeyStore

object HmacKeys {

    fun spec(alias: String): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY).build()

    fun store(): SecretKeyStore = AndroidKeyStoreSecretKeys(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ::spec)
}
