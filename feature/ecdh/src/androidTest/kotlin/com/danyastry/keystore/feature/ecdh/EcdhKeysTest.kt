package com.danyastry.keystore.feature.ecdh

import android.os.Build
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.danyastry.keystore.core.keystore.KeystoreEntries
import com.google.common.truth.Truth.assertThat
import java.security.KeyFactory
import java.security.PrivateKey
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class EcdhKeysTest {

    private val keys = EcdhKeys.store()
    private val keystore = KeystoreAgreement()
    private val software = SoftwareAgreement()

    @After
    fun tearDown() {
        keys.delete(ALIAS)
    }

    @Test
    fun keyPairIsForAgreementOnly() {
        val pair = keys.create(ALIAS)

        val info = keyInfo(pair.private)
        assertThat(info.purposes).isEqualTo(KeyProperties.PURPOSE_AGREE_KEY)
        assertThat(info.keySize).isEqualTo(256)
    }

    @Test
    fun privateKeyMaterialCannotBeRead() {
        assertThat(keys.create(ALIAS).private.encoded).isNull()
    }

    @Test
    fun keystoreAndSoftwareSidesComputeTheSameSecret() {
        val device = keys.create(ALIAS)
        val peer = EcdhKeys.softwareKeyPair()

        val deviceSecret = keystore.sharedSecret(device.private, peer.public)
        val peerSecret = software.sharedSecret(peer.private, device.public)

        assertThat(deviceSecret).hasLength(32)
        assertThat(deviceSecret).isEqualTo(peerSecret)
    }

    @Test
    fun peerDecryptsWhatTheDeviceEncrypted() {
        val device = keys.create(ALIAS)
        val peer = EcdhKeys.softwareKeyPair()
        val deviceKey = SessionKeys.derive(keystore.sharedSecret(device.private, peer.public))
        val peerKey = SessionKeys.derive(software.sharedSecret(peer.private, device.public))

        val message = SessionCipher.encrypt(deviceKey, "Attack at dawn".encodeToByteArray())

        assertThat(SessionCipher.decrypt(peerKey, message).decodeToString()).isEqualTo("Attack at dawn")
    }

    private fun keyInfo(key: PrivateKey): KeyInfo =
        KeyFactory.getInstance(key.algorithm, KeystoreEntries.ANDROID_KEY_STORE).getKeySpec(key, KeyInfo::class.java)

    private companion object {
        const val ALIAS = "ecdh-test"
    }
}
