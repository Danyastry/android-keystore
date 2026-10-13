package com.danyastry.keystore.feature.keyinfo.ui

import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.core.keystore.KeyKind
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.keystore.KeyOrigin
import com.danyastry.keystore.core.keystore.KeyPurpose
import com.danyastry.keystore.feature.keyinfo.KeyInspector
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeyInfoViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val inspector = FakeInspector()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts loading and then shows no keys`() = runTest(dispatcher) {
        val viewModel = KeyInfoViewModel(inspector, dispatcher)
        assertThat(viewModel.state.value.isLoading).isTrue()

        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.keys).isEmpty()
    }

    @Test
    fun `shows the keys the inspector finds`() = runTest(dispatcher) {
        inspector.stored += details("aes-gcm-demo")
        val viewModel = KeyInfoViewModel(inspector, dispatcher)

        advanceUntilIdle()

        assertThat(viewModel.state.value.keys.map { it.alias }).containsExactly("aes-gcm-demo")
    }

    @Test
    fun `creating the sample key adds it to the list`() = runTest(dispatcher) {
        val viewModel = KeyInfoViewModel(inspector, dispatcher)
        advanceUntilIdle()

        viewModel.onCreateSampleKey()
        advanceUntilIdle()

        assertThat(viewModel.state.value.keys.map { it.alias }).containsExactly("key-info-sample")
    }

    @Test
    fun `refresh picks up keys made elsewhere`() = runTest(dispatcher) {
        val viewModel = KeyInfoViewModel(inspector, dispatcher)
        advanceUntilIdle()
        inspector.stored += details("ecdsa-demo")

        viewModel.onRefresh()
        advanceUntilIdle()

        assertThat(viewModel.state.value.keys.map { it.alias }).containsExactly("ecdsa-demo")
    }

    private fun details(alias: String) = KeyDetails(
        alias = alias,
        algorithm = "AES",
        sizeBits = 256,
        kind = KeyKind.SECRET,
        location = KeyLocation.TRUSTED_ENVIRONMENT,
        purposes = setOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT),
        origin = KeyOrigin.GENERATED,
        requiresUserAuthentication = false,
    )

    private inner class FakeInspector : KeyInspector {
        val stored = mutableListOf<KeyDetails>()

        override fun keys(): List<KeyDetails> = stored.toList()

        override fun createSampleKey() {
            stored += details("key-info-sample")
        }
    }
}
