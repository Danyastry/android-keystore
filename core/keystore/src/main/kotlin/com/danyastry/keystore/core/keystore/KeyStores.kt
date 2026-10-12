package com.danyastry.keystore.core.keystore

import android.security.keystore.KeyGenParameterSpec
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

interface SecretKeyStore {
    fun get(alias: String): SecretKey?
    fun create(alias: String): SecretKey
    fun delete(alias: String)
}

interface KeyPairStore {
    fun get(alias: String): KeyPair?
    fun create(alias: String): KeyPair
    fun delete(alias: String)
}

class AndroidKeyStoreSecretKeys(
    private val algorithm: String,
    private val spec: (alias: String) -> KeyGenParameterSpec,
    private val entries: KeystoreEntries = KeystoreEntries(),
) : SecretKeyStore {

    override fun get(alias: String): SecretKey? = entries.secretKey(alias)

    override fun create(alias: String): SecretKey =
        KeyGenerator.getInstance(algorithm, KeystoreEntries.ANDROID_KEY_STORE).apply { init(spec(alias)) }.generateKey()

    override fun delete(alias: String) = entries.delete(alias)
}

class AndroidKeyStoreKeyPairs(
    private val algorithm: String,
    private val spec: (alias: String) -> KeyGenParameterSpec,
    private val entries: KeystoreEntries = KeystoreEntries(),
) : KeyPairStore {

    override fun get(alias: String): KeyPair? = entries.keyPair(alias)

    override fun create(alias: String): KeyPair =
        KeyPairGenerator.getInstance(algorithm, KeystoreEntries.ANDROID_KEY_STORE).apply { initialize(spec(alias)) }.generateKeyPair()

    override fun delete(alias: String) = entries.delete(alias)
}
