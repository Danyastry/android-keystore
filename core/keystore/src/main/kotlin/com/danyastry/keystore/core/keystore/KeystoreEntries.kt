package com.danyastry.keystore.core.keystore

import android.os.Build
import android.security.keystore.KeyInfo
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

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

    fun certificateChain(alias: String): List<X509Certificate> =
        keyStore.getCertificateChain(alias)?.map { it as X509Certificate }.orEmpty()

    fun details(alias: String): KeyDetails? {
        val key = keyStore.getKey(alias, null) ?: return null
        val (info, kind) = when (key) {
            is SecretKey -> SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEY_STORE)
                .getKeySpec(key, KeyInfo::class.java) as KeyInfo to KeyKind.SECRET
            is PrivateKey -> KeyFactory.getInstance(key.algorithm, ANDROID_KEY_STORE)
                .getKeySpec(key, KeyInfo::class.java) to KeyKind.PRIVATE
            else -> return null
        }
        return KeyDetails(
            alias = alias,
            algorithm = key.algorithm,
            sizeBits = info.keySize,
            kind = kind,
            location = KeyInfoMapper.location(securityLevelOf(info), insideSecureHardware(info)),
            purposes = KeyInfoMapper.purposes(info.purposes),
            origin = KeyInfoMapper.origin(info.origin),
            requiresUserAuthentication = info.isUserAuthenticationRequired,
        )
    }

    private fun securityLevelOf(info: KeyInfo): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.securityLevel else null

    @Suppress("DEPRECATION")
    private fun insideSecureHardware(info: KeyInfo): Boolean = info.isInsideSecureHardware

    companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}
