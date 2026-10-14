package com.danyastry.keystore.feature.unlockeddevice

import com.google.common.truth.Truth.assertThat
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import org.junit.Test

class AesGcmKeyProbeTest {

    private val probe = AesGcmKeyProbe()

    @Test
    fun `a usable key is allowed`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        assertThat(probe.probe(key)).isEqualTo(ProbeResult.Allowed)
    }

    @Test
    fun `a refused key reports the exception name`() {
        val notAesKey = SecretKeySpec(ByteArray(7), "AES")

        assertThat(probe.probe(notAesKey)).isEqualTo(ProbeResult.Refused("InvalidKeyException"))
    }
}
