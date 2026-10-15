package com.danyastry.keystore.feature.envelope.ui

import com.danyastry.keystore.core.keystore.KeyPairStore
import com.google.common.truth.Truth.assertThat
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPair
import java.security.KeyPairGenerator
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
class EnvelopeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val keys = InMemoryKeyPairs()
    private lateinit var viewModel: EnvelopeViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = EnvelopeViewModel(keys = keys, dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts without a key or payload`() = test {
        assertThat(state.hasKey).isFalse()
        assertThat(state.payloadBytes).isEqualTo(0)
        assertThat(state.canSeal).isFalse()
    }

    @Test
    fun `counts payload bytes for text and the sample`() = test {
        viewModel.onInputChange("Hello")
        assertThat(state.payloadBytes).isEqualTo(5)

        viewModel.onSourceChange(PayloadSource.SAMPLE)
        assertThat(state.payloadBytes).isEqualTo(EnvelopeViewModel.SAMPLE_BYTES)

        viewModel.onSourceChange(PayloadSource.TEXT)
        assertThat(state.payloadBytes).isEqualTo(5)
    }

    @Test
    fun `a refused key creation is reported`() = test {
        keys.failCreation = true

        viewModel.onCreateKey()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(EnvelopeError.KEY_CREATION_FAILED)
        assertThat(state.hasKey).isFalse()
    }

    @Test
    fun `rsa alone takes short text`() = test {
        createKey()
        viewModel.onInputChange("Attack at dawn")

        viewModel.onRsaOnly()
        advanceUntilIdle()

        assertThat(state.rsaOnly).isEqualTo(RsaOnlyResult(14, isAllowed = true))
    }

    @Test
    fun `rsa alone refuses the 64 kb sample`() = test {
        createKey()
        viewModel.onSourceChange(PayloadSource.SAMPLE)

        viewModel.onRsaOnly()
        advanceUntilIdle()

        assertThat(state.rsaOnly?.isAllowed).isFalse()
        assertThat(state.rsaOnly?.payloadBytes).isEqualTo(EnvelopeViewModel.SAMPLE_BYTES)
        assertThat(state.rsaOnly?.reason).isNotEmpty()
    }

    @Test
    fun `the envelope carries the 64 kb sample`() = test {
        createKey()
        viewModel.onSourceChange(PayloadSource.SAMPLE)

        viewModel.onSeal()
        advanceUntilIdle()

        val sealed = state.sealed!!
        assertThat(sealed.wrappedKeyBytes).isEqualTo(256)
        assertThat(sealed.ivBytes).isEqualTo(12)
        assertThat(sealed.ciphertextBytes).isEqualTo(EnvelopeViewModel.SAMPLE_BYTES + 16)
        assertThat(sealed.totalBytes).isEqualTo(2 + 256 + 12 + EnvelopeViewModel.SAMPLE_BYTES + 16)
    }

    @Test
    fun `opening gives back the original`() = test {
        createKey()
        viewModel.onSourceChange(PayloadSource.SAMPLE)
        viewModel.onSeal()
        advanceUntilIdle()

        viewModel.onOpen()
        advanceUntilIdle()

        assertThat(state.opened?.bytes).isEqualTo(EnvelopeViewModel.SAMPLE_BYTES)
        assertThat(state.opened?.matchesOriginal).isTrue()
        assertThat(state.opened?.preview).startsWith("The quick brown fox")
    }

    @Test
    fun `changing the text after sealing still opens what was sealed`() = test {
        createKey()
        viewModel.onInputChange("Attack at dawn")
        viewModel.onSeal()
        advanceUntilIdle()

        viewModel.onInputChange("Something else")
        viewModel.onOpen()
        advanceUntilIdle()

        assertThat(state.opened?.preview).isEqualTo("Attack at dawn")
        assertThat(state.opened?.matchesOriginal).isTrue()
    }

    @Test
    fun `a new key pair cannot open an old envelope`() = test {
        createKey()
        viewModel.onInputChange("Attack at dawn")
        viewModel.onSeal()
        advanceUntilIdle()

        viewModel.onDeleteKey()
        advanceUntilIdle()
        createKey()
        viewModel.onOpen()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(EnvelopeError.CANNOT_OPEN)
        assertThat(state.opened).isNull()
    }

    private fun TestScope.createKey() {
        viewModel.onCreateKey()
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
            return KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().also { pairs[alias] = it }
        }

        override fun delete(alias: String) {
            pairs.remove(alias)
        }
    }
}
