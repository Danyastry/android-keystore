package com.danyastry.keystore.feature.ecdsa

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EcdsaKeysTest {

    private val keys = EcdsaKeys.store()
    private val signer = EcdsaSigner()

    @After
    fun tearDown() {
        keys.delete(ALIAS)
    }

    @Test
    fun createsAP256KeyPair() {
        val pair = keys.create(ALIAS)

        val publicKey = pair.public as ECPublicKey
        assertThat(publicKey.params.order.bitLength()).isEqualTo(256)
        assertThat(pair.private.encoded).isNull()
    }

    @Test
    fun signsInsideTheKeystoreAndVerifies() {
        keys.create(ALIAS)
        val pair = keys.get(ALIAS)!!

        val signature = signer.sign("Pay Alice 100 dollars".encodeToByteArray(), pair.private)

        assertThat(signer.verify("Pay Alice 100 dollars".encodeToByteArray(), signature, pair.public)).isTrue()
        assertThat(signer.verify("Pay Alice 900 dollars".encodeToByteArray(), signature, pair.public)).isFalse()
    }

    @Test
    fun exportedPublicKeyVerifiesOutsideTheKeystore() {
        val pair = keys.create(ALIAS)
        val exported = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(pair.public.encoded))

        val signature = signer.sign("Hello".encodeToByteArray(), pair.private)

        assertThat(signer.verify("Hello".encodeToByteArray(), signature, exported)).isTrue()
    }

    @Test
    fun deleteRemovesTheKeyPair() {
        keys.create(ALIAS)

        keys.delete(ALIAS)

        assertThat(keys.get(ALIAS)).isNull()
    }

    private companion object {
        const val ALIAS = "ecdsa-test"
    }
}
