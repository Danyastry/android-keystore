package com.danyastry.keystore.feature.ecdh.ui

import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.core.keystore.KeyPairStore
import com.danyastry.keystore.feature.ecdh.EcdhKeys
import com.danyastry.keystore.feature.ecdh.SoftwareAgreement
import com.google.common.truth.Truth.assertThat
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EcdhViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val deviceKeys = InMemoryKeyPairs()
    private lateinit var viewModel: EcdhViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = EcdhViewModel(
            deviceKeys = deviceKeys,
            deviceAgreement = SoftwareAgreement(),
            peerAgreement = SoftwareAgreement(),
            newPeer = EcdhKeys::softwareKeyPair,
            dispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with a peer but no device key`() = test {
        assertThat(state.hasDeviceKey).isFalse()
        assertThat(state.peerPublicKey).isNotEmpty()
        assertThat(state.canAgree).isFalse()
    }

    @Test
    fun `creating the device key shows its public key`() = test {
        createDeviceKey()

        assertThat(state.hasDeviceKey).isTrue()
        assertThat(state.devicePublicKey).matches("([0-9a-f]{2}:){7}[0-9a-f]{2}")
        assertThat(state.canAgree).isTrue()
    }

    @Test
    fun `a refused key creation is reported`() = test {
        deviceKeys.failCreation = true

        viewModel.onCreateDeviceKey()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(EcdhError.KEY_CREATION_FAILED)
        assertThat(state.hasDeviceKey).isFalse()
    }

    @Test
    fun `both sides agree on the same secret`() = test {
        createDeviceKey()

        agree()

        assertThat(state.isAgreed).isTrue()
        assertThat(state.secretsMatch).isTrue()
        assertThat(state.sessionKey).isNotEmpty()
        assertThat(state.sessionKey).isNotEqualTo(state.deviceSecret)
    }

    @Test
    fun `the peer reads what the device encrypted`() = test {
        createDeviceKey()
        agree()
        viewModel.onInputChange("Attack at dawn")

        viewModel.onEncryptOnDevice()
        advanceUntilIdle()
        viewModel.onDecryptOnPeer()
        advanceUntilIdle()

        assertThat(EncryptedMessage.fromBase64OrNull(state.sealed)).isNotNull()
        assertThat(state.peerDecrypted).isEqualTo("Attack at dawn")
        assertThat(state.error).isNull()
    }

    @Test
    fun `a new peer resets the agreement`() = test {
        createDeviceKey()
        agree()
        val oldPeer = state.peerPublicKey

        viewModel.onNewPeer()
        advanceUntilIdle()

        assertThat(state.peerPublicKey).isNotEqualTo(oldPeer)
        assertThat(state.isAgreed).isFalse()
        assertThat(state.canEncrypt).isFalse()
    }

    @Test
    fun `a new peer cannot read a message meant for the old one`() = test {
        createDeviceKey()
        agree()
        viewModel.onInputChange("Attack at dawn")
        viewModel.onEncryptOnDevice()
        advanceUntilIdle()

        viewModel.onNewPeer()
        advanceUntilIdle()
        agree()
        viewModel.onDecryptOnPeer()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(EcdhError.PEER_CANNOT_DECRYPT)
        assertThat(state.peerDecrypted).isEmpty()
    }

    @Test
    fun `encrypting before agreeing asks for an agreement`() = test {
        createDeviceKey()
        viewModel.onInputChange("Hello")

        viewModel.onEncryptOnDevice()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(EcdhError.NOT_AGREED)
    }

    @Test
    fun `deleting the device key clears the agreement and message`() = test {
        createDeviceKey()
        agree()
        viewModel.onInputChange("Hello")
        viewModel.onEncryptOnDevice()
        advanceUntilIdle()

        viewModel.onDeleteDeviceKey()
        advanceUntilIdle()

        assertThat(state.hasDeviceKey).isFalse()
        assertThat(state.devicePublicKey).isEmpty()
        assertThat(state.isAgreed).isFalse()
        assertThat(state.sealed).isEmpty()
    }

    private fun TestScope.createDeviceKey() {
        viewModel.onCreateDeviceKey()
        advanceUntilIdle()
    }

    private fun TestScope.agree() {
        viewModel.onAgree()
        advanceUntilIdle()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class InMemoryKeyPairs : KeyPairStore {
        private val pairs = mutableMapOf<String, KeyPair>()
        var failCreation = false

        override fun get(alias: String): KeyPair? = pairs[alias]

        override fun create(alias: String): KeyPair {
            if (failCreation) throw InvalidAlgorithmParameterException("Keystore refused")
            return EcdhKeys.softwareKeyPair().also { pairs[alias] = it }
        }

        override fun delete(alias: String) {
            pairs.remove(alias)
        }
    }
}
