package com.danyastry.keystore.feature.aesgcm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import javax.crypto.AEADBadTagException
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeyStoreSecretKeysTest {

    private val keys = AndroidKeyStoreSecretKeys()
    private val cipher = AesGcmCipher()

    @After
    fun tearDown() {
        keys.delete(ALIAS)
    }

    @Test
    fun createsKeyInsideTheKeystore() {
        keys.create(ALIAS)

        val key = keys.get(ALIAS)
        assertThat(key).isNotNull()
        assertThat(key!!.algorithm).isEqualTo("AES")
    }

    @Test
    fun keyMaterialCannotBeRead() {
        val key = keys.create(ALIAS)

        assertThat(key.encoded).isNull()
    }

    @Test
    fun encryptsAndDecryptsWithTheKeystoreKey() {
        val key = keys.create(ALIAS)

        val message = cipher.encrypt("Attack at dawn".encodeToByteArray(), key)

        assertThat(cipher.decrypt(message, keys.get(ALIAS)!!).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun keystoreRejectsTamperedData() {
        val key = keys.create(ALIAS)
        val message = cipher.encrypt("Attack at dawn".encodeToByteArray(), key)
        val tampered = message.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(EncryptedMessage(message.iv, tampered), key) }
    }

    @Test
    fun deleteRemovesTheKey() {
        keys.create(ALIAS)

        keys.delete(ALIAS)

        assertThat(keys.get(ALIAS)).isNull()
    }

    @Test
    fun deletingAMissingKeyIsAllowed() {
        keys.delete(ALIAS)

        assertThat(keys.get(ALIAS)).isNull()
    }

    private companion object {
        const val ALIAS = "aes-gcm-test"
    }
}
