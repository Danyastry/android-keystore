package com.danyastry.keystore.feature.ecdh

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.danyastry.keystore.core.keystore.AndroidKeyStoreKeyPairs
import com.danyastry.keystore.core.keystore.KeyPairStore
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

object EcdhKeys {

    const val CURVE = "secp256r1"

    @RequiresApi(Build.VERSION_CODES.S)
    fun spec(alias: String): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_AGREE_KEY)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
            .build()

    @RequiresApi(Build.VERSION_CODES.S)
    fun store(): KeyPairStore = AndroidKeyStoreKeyPairs(KeyProperties.KEY_ALGORITHM_EC, ::spec)

    fun softwareKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(CURVE)) }.generateKeyPair()
}
