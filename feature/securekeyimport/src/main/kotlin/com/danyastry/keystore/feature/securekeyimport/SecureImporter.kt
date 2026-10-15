package com.danyastry.keystore.feature.securekeyimport

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.WrappedKeyEntry
import androidx.annotation.RequiresApi
import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.core.keystore.KeystoreEntries
import java.security.KeyPairGenerator
import java.security.PublicKey
import javax.crypto.SecretKey

interface SecureImporter {
    fun createWrappingKey(): PublicKey
    fun wrappingKey(): PublicKey?
    fun importWrapped(wrappedKey: ByteArray)
    fun importedKey(): SecretKey?
    fun importedDetails(): KeyDetails?
    fun deleteImported()
    fun deleteAll()
}

@RequiresApi(Build.VERSION_CODES.P)
class AndroidSecureImporter(private val entries: KeystoreEntries = KeystoreEntries()) : SecureImporter {

    override fun createWrappingKey(): PublicKey {
        entries.delete(WRAPPING_ALIAS)
        return KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KeystoreEntries.ANDROID_KEY_STORE)
            .apply { initialize(wrappingSpec(WRAPPING_ALIAS)) }
            .generateKeyPair()
            .public
    }

    override fun wrappingKey(): PublicKey? = entries.keyPair(WRAPPING_ALIAS)?.public

    override fun importWrapped(wrappedKey: ByteArray) {
        entries.delete(IMPORTED_ALIAS)
        val entry = WrappedKeyEntry(wrappedKey, WRAPPING_ALIAS, SecureKeyWrapper.RSA_TRANSFORMATION, unwrapSpec())
        entries.importEntry(IMPORTED_ALIAS, entry)
    }

    override fun importedKey(): SecretKey? = entries.secretKey(IMPORTED_ALIAS)

    override fun importedDetails(): KeyDetails? = entries.details(IMPORTED_ALIAS)

    override fun deleteImported() = entries.delete(IMPORTED_ALIAS)

    override fun deleteAll() {
        entries.delete(IMPORTED_ALIAS)
        entries.delete(WRAPPING_ALIAS)
    }

    companion object {
        const val WRAPPING_ALIAS = "secure-import-wrapping"
        const val IMPORTED_ALIAS = "secure-import-demo"
        const val WRAPPING_KEY_SIZE_BITS = 2048

        fun unwrapSpec(): KeyGenParameterSpec =
            KeyGenParameterSpec.Builder(WRAPPING_ALIAS, KeyProperties.PURPOSE_WRAP_KEY)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()

        fun wrappingSpec(alias: String): KeyGenParameterSpec =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_WRAP_KEY)
                .setKeySize(WRAPPING_KEY_SIZE_BITS)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .build()
    }
}
