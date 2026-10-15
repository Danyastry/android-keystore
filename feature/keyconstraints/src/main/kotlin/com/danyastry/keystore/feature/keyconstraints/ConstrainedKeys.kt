package com.danyastry.keystore.feature.keyconstraints

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.danyastry.keystore.core.keystore.KeystoreEntries
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

enum class ConstrainedKey { WINDOW, LIMITED }

sealed interface UseResult {
    data object Allowed : UseResult
    data class Refused(val reason: String) : UseResult
    data object Missing : UseResult
}

interface ConstrainedKeys {
    fun createWindowKey(validFromMillis: Long, validUntilMillis: Long)
    fun createLimitedKey(maxUses: Int)
    fun use(key: ConstrainedKey): UseResult
    fun exists(key: ConstrainedKey): Boolean
    fun delete(key: ConstrainedKey)
}

class AndroidConstrainedKeys(private val entries: KeystoreEntries = KeystoreEntries()) : ConstrainedKeys {

    override fun createWindowKey(validFromMillis: Long, validUntilMillis: Long) {
        generate(windowSpec(aliasOf(ConstrainedKey.WINDOW), Date(validFromMillis), Date(validUntilMillis)))
    }

    override fun createLimitedKey(maxUses: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            generate(baseSpec(aliasOf(ConstrainedKey.LIMITED)).setMaxUsageCount(maxUses).build())
        } else {
            throw UnsupportedOperationException("setMaxUsageCount needs Android 12")
        }
    }

    override fun use(key: ConstrainedKey): UseResult {
        val secret = entries.secretKey(aliasOf(key)) ?: return UseResult.Missing
        return try {
            Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secret) }.doFinal(PROBE_TEXT)
            UseResult.Allowed
        } catch (e: GeneralSecurityException) {
            UseResult.Refused(e.javaClass.simpleName)
        } catch (e: ProviderException) {
            UseResult.Refused(e.javaClass.simpleName)
        }
    }

    override fun exists(key: ConstrainedKey): Boolean = entries.contains(aliasOf(key))

    override fun delete(key: ConstrainedKey) = entries.delete(aliasOf(key))

    private fun generate(spec: KeyGenParameterSpec) {
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KeystoreEntries.ANDROID_KEY_STORE).apply { init(spec) }.generateKey()
    }

    companion object {
        const val KEY_SIZE_BITS = 256
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val PROBE_TEXT = "probe".encodeToByteArray()

        fun aliasOf(key: ConstrainedKey): String = "key-constraints-demo-${key.name.lowercase()}"

        fun windowSpec(alias: String, validFrom: Date, validUntil: Date): KeyGenParameterSpec =
            baseSpec(alias).setKeyValidityStart(validFrom).setKeyValidityEnd(validUntil).build()

        private fun baseSpec(alias: String): KeyGenParameterSpec.Builder =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
    }
}
