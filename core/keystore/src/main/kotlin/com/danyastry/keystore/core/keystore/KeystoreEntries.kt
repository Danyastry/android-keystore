package com.danyastry.keystore.core.keystore

import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import javax.crypto.SecretKey

class KeystoreEntries {

    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) } }

    fun aliases(): List<String> = keyStore.aliases().toList().sorted()

    fun contains(alias: String): Boolean = keyStore.containsAlias(alias)

    fun delete(alias: String) {
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    fun secretKey(alias: String): SecretKey? = keyStore.getKey(alias, null) as? SecretKey

    fun keyPair(alias: String): KeyPair? {
        val privateKey = keyStore.getKey(alias, null) as? PrivateKey ?: return null
        val publicKey = keyStore.getCertificate(alias)?.publicKey ?: return null
        return KeyPair(publicKey, privateKey)
    }

    companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}
