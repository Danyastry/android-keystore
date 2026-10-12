package com.danyastry.keystore.feature.rsaoaep

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeyStoreRsaKeysTest {

    private val keys = AndroidKeyStoreRsaKeys()
    private val cipher = RsaOaepCipher()

    @After
    fun tearDown() {
        keys.delete(ALIAS)
    }

    @Test
    fun createsAKeyPairInsideTheKeystore() {
        keys.create(ALIAS)

        val pair = keys.get(ALIAS)
        assertThat(pair).isNotNull()
        assertThat(pair!!.public.algorithm).isEqualTo("RSA")
    }

    @Test
    fun publicKeyCanBeExportedButPrivateKeyCannot() {
        val pair = keys.create(ALIAS)

        assertThat(pair.public.encoded).isNotEmpty()
        assertThat(pair.private.encoded).isNull()
    }

    @Test
    fun encryptsWithThePublicKeyAndDecryptsInsideTheKeystore() {
        keys.create(ALIAS)
        val pair = keys.get(ALIAS)!!

        val ciphertext = cipher.encrypt("Attack at dawn".encodeToByteArray(), pair.public)

        assertThat(cipher.decrypt(ciphertext, pair.private).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun exportedPublicKeyWorksOutsideTheKeystore() {
        val pair = keys.create(ALIAS)
        val exported = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(pair.public.encoded))

        val ciphertext = cipher.encrypt("Hello from outside".encodeToByteArray(), exported)

        assertThat(cipher.decrypt(ciphertext, pair.private).decodeToString()).isEqualTo("Hello from outside")
    }

    @Test
    fun largestMessageFits() {
        val pair = keys.create(ALIAS)
        val largest = ByteArray(RsaOaepCipher.MAX_PLAINTEXT_BYTES) { it.toByte() }

        assertThat(cipher.decrypt(cipher.encrypt(largest, pair.public), pair.private)).isEqualTo(largest)
    }

    @Test
    fun deleteRemovesTheKeyPair() {
        keys.create(ALIAS)

        keys.delete(ALIAS)

        assertThat(keys.get(ALIAS)).isNull()
    }

    private companion object {
        const val ALIAS = "rsa-oaep-test"
    }
}
