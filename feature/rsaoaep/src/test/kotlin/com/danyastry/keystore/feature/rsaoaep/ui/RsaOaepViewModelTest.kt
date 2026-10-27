package com.danyastry.keystore.feature.rsaoaep.ui

import com.danyastry.keystore.core.keystore.KeyPairStore
import com.danyastry.keystore.core.ui.components.CipherMode
import com.danyastry.keystore.feature.rsaoaep.RsaOaepCipher
import com.google.common.truth.Truth.assertThat
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64
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
class RsaOaepViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RsaOaepViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = RsaOaepViewModel(keys = InMemoryKeyPairs(), dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts without a key`() = test {
        assertThat(state.hasKey).isFalse()
        assertThat(state.publicKeyBase64).isEmpty()
    }

    @Test
    fun `asks for a key before encrypting`() = test {
        viewModel.onInputChange("Hello")
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(RsaOaepError.NO_KEY)
    }

    @Test
    fun `creating a key shows the public key and encrypts`() = test {
        viewModel.onCreateKey()
        viewModel.onInputChange("Hello")
        advanceUntilIdle()

        assertThat(state.hasKey).isTrue()
        assertThat(Base64.getDecoder().decode(state.publicKeyBase64)).isNotEmpty()
        assertThat(Base64.getDecoder().decode(state.output)).hasLength(256)
        assertThat(state.inputBytes).isEqualTo(5)
    }

    @Test
    fun `counts bytes, not characters`() = test {
        viewModel.onCreateKey()
        viewModel.onInputChange("\u00A3\u20AC")
        advanceUntilIdle()

        assertThat(state.inputBytes).isEqualTo(5)
    }

    @Test
    fun `too long text is reported`() = test {
        viewModel.onCreateKey()
        viewModel.onInputChange("a".repeat(RsaOaepCipher.MAX_PLAINTEXT_BYTES + 1))
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(RsaOaepError.TOO_LONG)
        assertThat(state.output).isEmpty()
    }

    @Test
    fun `use output as input decrypts back`() = test {
        viewModel.onCreateKey()
        viewModel.onInputChange("Attack at dawn")
        advanceUntilIdle()

        viewModel.onUseOutputAsInput()
        advanceUntilIdle()

        assertThat(state.mode).isEqualTo(CipherMode.DECRYPT)
        assertThat(state.output).isEqualTo("Attack at dawn")
    }

    @Test
    fun `invalid ciphertext is reported`() = test {
        viewModel.onCreateKey()
        viewModel.onModeChange(CipherMode.DECRYPT)
        viewModel.onInputChange("AAAA")
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(RsaOaepError.INVALID_MESSAGE)
    }

    @Test
    fun `a new key pair cannot decrypt old messages`() = test {
        viewModel.onCreateKey()
        viewModel.onInputChange("Hello")
        advanceUntilIdle()
        viewModel.onUseOutputAsInput()
        advanceUntilIdle()

        viewModel.onDeleteKey()
        viewModel.onCreateKey()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(RsaOaepError.DECRYPTION_FAILED)
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class InMemoryKeyPairs : KeyPairStore {
        private val pairs = mutableMapOf<String, KeyPair>()

        override fun get(alias: String): KeyPair? = pairs[alias]

        override fun create(alias: String): KeyPair =
            KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().also { pairs[alias] = it }

        override fun delete(alias: String) {
            pairs.remove(alias)
        }
    }
}
