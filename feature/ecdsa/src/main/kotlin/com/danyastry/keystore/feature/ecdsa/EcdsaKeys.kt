package com.danyastry.keystore.feature.ecdsa

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.danyastry.keystore.core.keystore.AndroidKeyStoreKeyPairs
import com.danyastry.keystore.core.keystore.KeyPairStore
import java.security.spec.ECGenParameterSpec

object EcdsaKeys {

    const val CURVE = "secp256r1"

    fun spec(alias: String): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()

    fun store(): KeyPairStore = AndroidKeyStoreKeyPairs(KeyProperties.KEY_ALGORITHM_EC, ::spec)
}
