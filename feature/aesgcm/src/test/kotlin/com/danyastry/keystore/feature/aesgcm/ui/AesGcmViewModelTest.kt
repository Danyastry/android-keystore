package com.danyastry.keystore.feature.aesgcm.ui

import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.core.ui.components.CipherMode
import com.google.common.truth.Truth.assertThat
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
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
class AesGcmViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val keys = InMemorySecretKeys()
    private lateinit var viewModel: AesGcmViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = AesGcmViewModel(keys = keys, dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts without a key`() = test {
        assertThat(state.hasKey).isFalse()
        assertThat(state.output).isEmpty()
    }

    @Test
    fun `asks for a key before encrypting`() = test {
        viewModel.onInputChange("Hello")
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(AesGcmError.NO_KEY)
        assertThat(state.output).isEmpty()
    }

    @Test
    fun `creating a key enables encryption`() = test {
        viewModel.onInputChange("Hello")
        viewModel.onCreateKey()
        advanceUntilIdle()

        assertThat(state.hasKey).isTrue()
        assertThat(state.error).isNull()
        assertThat(EncryptedMessage.fromBase64OrNull(state.output)).isNotNull()
        assertThat(state.ivHex).hasLength(24)
    }

    @Test
    fun `key material of the fake key is visible`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()

        assertThat(state.isKeyMaterialHidden).isFalse()
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
    fun `flipping a bit makes decryption fail`() = test {
        viewModel.onCreateKey()
        viewModel.onInputChange("Attack at dawn")
        advanceUntilIdle()
        viewModel.onUseOutputAsInput()
        advanceUntilIdle()

        viewModel.onFlipBit()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(AesGcmError.TAMPERED)
        assertThat(state.output).isEmpty()
    }

    @Test
    fun `invalid ciphertext is reported`() = test {
        viewModel.onCreateKey()
        viewModel.onModeChange(CipherMode.DECRYPT)
        viewModel.onInputChange("hello")
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(AesGcmError.INVALID_MESSAGE)
    }

    @Test
    fun `deleting the key makes old messages unreadable`() = test {
        viewModel.onCreateKey()
        viewModel.onInputChange("Hello")
        advanceUntilIdle()
        viewModel.onUseOutputAsInput()
        advanceUntilIdle()

        viewModel.onDeleteKey()
        advanceUntilIdle()

        assertThat(state.hasKey).isFalse()
        assertThat(state.error).isEqualTo(AesGcmError.NO_KEY)
    }

    @Test
    fun `a new key cannot read messages of the old one`() = test {
        viewModel.onCreateKey()
        viewModel.onInputChange("Hello")
        advanceUntilIdle()
        viewModel.onUseOutputAsInput()
        advanceUntilIdle()

        viewModel.onDeleteKey()
        viewModel.onCreateKey()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(AesGcmError.TAMPERED)
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class InMemorySecretKeys : SecretKeyStore {
        private val keys = mutableMapOf<String, SecretKey>()

        override fun get(alias: String): SecretKey? = keys[alias]

        override fun create(alias: String): SecretKey =
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also { keys[alias] = it }

        override fun delete(alias: String) {
            keys.remove(alias)
        }
    }
}
