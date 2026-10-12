package com.danyastry.keystore.feature.ecdsa.ui

import com.danyastry.keystore.core.keystore.KeyPairStore
import com.danyastry.keystore.feature.ecdsa.EcdsaKeys
import com.google.common.truth.Truth.assertThat
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
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
class EcdsaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: EcdsaViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = EcdsaViewModel(keys = InMemoryKeyPairs(), dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts without a key and with a sample message`() = test {
        assertThat(state.hasKey).isFalse()
        assertThat(state.message).isEqualTo("Pay Alice 100 dollars")
        assertThat(state.verification).isNull()
    }

    @Test
    fun `signing produces a valid signature`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()

        viewModel.onSign()
        advanceUntilIdle()

        assertThat(state.signature).isNotEmpty()
        assertThat(state.signatureBytes).isIn(64..72)
        assertThat(state.verification).isEqualTo(Verification.VALID)
    }

    @Test
    fun `changing the message after signing makes it invalid`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onSign()
        advanceUntilIdle()

        viewModel.onMessageChange("Pay Alice 900 dollars")
        advanceUntilIdle()

        assertThat(state.verification).isEqualTo(Verification.INVALID)
    }

    @Test
    fun `changing it back makes it valid again`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onSign()
        advanceUntilIdle()
        viewModel.onMessageChange("Pay Alice 900 dollars")
        advanceUntilIdle()

        viewModel.onMessageChange("Pay Alice 100 dollars")
        advanceUntilIdle()

        assertThat(state.verification).isEqualTo(Verification.VALID)
    }

    @Test
    fun `garbage signature is reported`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()

        viewModel.onSignatureChange("not base64!")
        advanceUntilIdle()

        assertThat(state.verification).isEqualTo(Verification.NOT_BASE64)
    }

    @Test
    fun `deleting the key clears the signature`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onSign()
        advanceUntilIdle()

        viewModel.onDeleteKey()
        advanceUntilIdle()

        assertThat(state.hasKey).isFalse()
        assertThat(state.signature).isEmpty()
        assertThat(state.verification).isNull()
    }

    @Test
    fun `a new key rejects the old signature`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onSign()
        advanceUntilIdle()
        val oldSignature = state.signature

        viewModel.onDeleteKey()
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onSignatureChange(oldSignature)
        advanceUntilIdle()

        assertThat(state.verification).isEqualTo(Verification.INVALID)
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class InMemoryKeyPairs : KeyPairStore {
        private val pairs = mutableMapOf<String, KeyPair>()

        override fun get(alias: String): KeyPair? = pairs[alias]

        override fun create(alias: String): KeyPair =
            KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(EcdsaKeys.CURVE)) }.generateKeyPair()
                .also { pairs[alias] = it }

        override fun delete(alias: String) {
            pairs.remove(alias)
        }
    }
}
