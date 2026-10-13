package com.danyastry.keystore.feature.strongbox

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.annotation.RequiresApi
import com.danyastry.keystore.core.keystore.AndroidKeyStoreSecretKeys
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.keystore.KeystoreEntries
import javax.crypto.Cipher
import kotlin.time.Duration
import kotlin.time.measureTime

enum class KeyTarget { REGULAR, STRONGBOX }

sealed interface KeyCreation {
    data class Created(val location: KeyLocation) : KeyCreation
    data object Unavailable : KeyCreation
}

interface StrongBoxKeys {
    fun isStrongBoxDeclared(): Boolean
    fun create(target: KeyTarget): KeyCreation
    fun measure(target: KeyTarget, rounds: Int): Duration?
    fun deleteAll()
}

@RequiresApi(Build.VERSION_CODES.P)
class AndroidStrongBoxKeys(
    private val context: Context,
    private val entries: KeystoreEntries = KeystoreEntries(),
) : StrongBoxKeys {

    override fun isStrongBoxDeclared(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

    override fun create(target: KeyTarget): KeyCreation {
        val alias = aliasOf(target)
        val keys = AndroidKeyStoreSecretKeys(KeyProperties.KEY_ALGORITHM_AES, { spec(it, target) }, entries)
        return try {
            keys.create(alias)
            KeyCreation.Created(entries.details(alias)?.location ?: KeyLocation.UNKNOWN)
        } catch (_: StrongBoxUnavailableException) {
            entries.delete(alias)
            KeyCreation.Unavailable
        }
    }

    override fun measure(target: KeyTarget, rounds: Int): Duration? {
        val key = entries.secretKey(aliasOf(target)) ?: return null
        val data = ByteArray(BENCHMARK_BYTES)
        return measureTime {
            repeat(rounds) {
                Cipher.getInstance(TRANSFORMATION).run {
                    init(Cipher.ENCRYPT_MODE, key)
                    doFinal(data)
                }
            }
        }
    }

    override fun deleteAll() {
        KeyTarget.entries.forEach { entries.delete(aliasOf(it)) }
    }

    private fun spec(alias: String, target: KeyTarget): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setIsStrongBoxBacked(target == KeyTarget.STRONGBOX)
            .build()

    companion object {
        const val KEY_SIZE_BITS = 256
        const val BENCHMARK_BYTES = 1024
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        fun aliasOf(target: KeyTarget): String = "strongbox-demo-${target.name.lowercase()}"
    }
}
