package com.danyastry.keystore.feature.hmac.ui

import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.feature.hmac.HmacSigner
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
class HmacViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HmacViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = HmacViewModel(keys = InMemorySecretKeys(), dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts without a key`() = test {
        assertThat(state.hasKey).isFalse()
        assertThat(state.check).isNull()
    }

    @Test
    fun `computing a tag gives 64 hex characters that verify`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()

        viewModel.onComputeTag()
        advanceUntilIdle()

        assertThat(state.tag).hasLength(64)
        assertThat(state.check).isEqualTo(TagCheck.VALID)
    }

    @Test
    fun `same message gives the same tag`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onComputeTag()
        advanceUntilIdle()
        val first = state.tag

        viewModel.onComputeTag()
        advanceUntilIdle()

        assertThat(state.tag).isEqualTo(first)
    }

    @Test
    fun `changing the message makes the tag invalid`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onComputeTag()
        advanceUntilIdle()

        viewModel.onMessageChange("Pay Alice 900 dollars")
        advanceUntilIdle()

        assertThat(state.check).isEqualTo(TagCheck.INVALID)
    }

    @Test
    fun `tag accepts uppercase hex and spaces`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onComputeTag()
        advanceUntilIdle()

        viewModel.onTagChange(state.tag.uppercase().chunked(8).joinToString(" "))
        advanceUntilIdle()

        assertThat(state.check).isEqualTo(TagCheck.VALID)
    }

    @Test
    fun `invalid hex is reported`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()

        viewModel.onTagChange("xyz")
        advanceUntilIdle()

        assertThat(state.check).isEqualTo(TagCheck.NOT_HEX)
    }

    @Test
    fun `a new key rejects the old tag`() = test {
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onComputeTag()
        advanceUntilIdle()
        val oldTag = state.tag

        viewModel.onDeleteKey()
        viewModel.onCreateKey()
        advanceUntilIdle()
        viewModel.onTagChange(oldTag)
        advanceUntilIdle()

        assertThat(state.check).isEqualTo(TagCheck.INVALID)
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class InMemorySecretKeys : SecretKeyStore {
        private val keys = mutableMapOf<String, SecretKey>()

        override fun get(alias: String): SecretKey? = keys[alias]

        override fun create(alias: String): SecretKey =
            KeyGenerator.getInstance(HmacSigner.ALGORITHM).generateKey().also { keys[alias] = it }

        override fun delete(alias: String) {
            keys.remove(alias)
        }
    }
}
