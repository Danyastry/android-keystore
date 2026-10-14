package com.danyastry.keystore.feature.unlockeddevice

import android.app.KeyguardManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
class UnlockedDeviceKeysTest {

    private val probe = AesGcmKeyProbe()
    private val keyguard = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(KeyguardManager::class.java)

    @After
    fun tearDown() {
        ProbeKey.entries.forEach { UnlockedDeviceKeys.store(it).delete(UnlockedDeviceKeys.aliasOf(it)) }
    }

    @Test
    fun onlyTheUnlockedOnlySpecRequiresAnUnlockedDevice() {
        assertThat(UnlockedDeviceKeys.spec("a", ProbeKey.UNLOCKED_ONLY).isUnlockedDeviceRequired).isTrue()
        assertThat(UnlockedDeviceKeys.spec("b", ProbeKey.REGULAR).isUnlockedDeviceRequired).isFalse()
    }

    @Test
    fun bothKeysAreCreatedUnderDifferentAliases() {
        ProbeKey.entries.forEach { UnlockedDeviceKeys.store(it).create(UnlockedDeviceKeys.aliasOf(it)) }

        assertThat(ProbeKey.entries.map(UnlockedDeviceKeys::aliasOf).toSet()).hasSize(2)
        ProbeKey.entries.forEach { assertThat(UnlockedDeviceKeys.store(it).get(UnlockedDeviceKeys.aliasOf(it))).isNotNull() }
    }

    @Test
    fun bothKeysWorkWhileTheDeviceIsUnlocked() {
        assumeFalse(keyguard.isDeviceLocked)

        ProbeKey.entries.forEach { key ->
            val secret = UnlockedDeviceKeys.store(key).create(UnlockedDeviceKeys.aliasOf(key))

            assertThat(probe.probe(secret)).isEqualTo(ProbeResult.Allowed)
        }
    }
}
