package com.danyastry.keystore.feature.unlockeddevice

import android.app.KeyguardManager
import android.content.Context
import java.security.GeneralSecurityException
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.SecretKey

sealed interface ProbeResult {
    data object Allowed : ProbeResult
    data class Refused(val reason: String) : ProbeResult
}

fun interface KeyProbe {
    fun probe(key: SecretKey): ProbeResult
}

class AesGcmKeyProbe : KeyProbe {

    override fun probe(key: SecretKey): ProbeResult =
        try {
            Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }.doFinal(PROBE_TEXT)
            ProbeResult.Allowed
        } catch (e: GeneralSecurityException) {
            ProbeResult.Refused(e.javaClass.simpleName)
        } catch (e: ProviderException) {
            ProbeResult.Refused(e.javaClass.simpleName)
        }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val PROBE_TEXT = "probe".encodeToByteArray()
    }
}

interface DeviceLock {
    fun hasScreenLock(): Boolean
    fun isLocked(): Boolean
}

class AndroidDeviceLock(context: Context) : DeviceLock {

    private val keyguard = context.getSystemService(KeyguardManager::class.java)

    override fun hasScreenLock(): Boolean = keyguard.isDeviceSecure

    override fun isLocked(): Boolean = keyguard.isDeviceLocked
}
