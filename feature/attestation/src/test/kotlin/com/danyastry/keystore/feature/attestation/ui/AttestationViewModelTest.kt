package com.danyastry.keystore.feature.attestation.ui

import com.danyastry.keystore.feature.attestation.AttestationLevel
import com.danyastry.keystore.feature.attestation.AttestationReport
import com.danyastry.keystore.feature.attestation.Attestor
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
class AttestationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val report = AttestationReport(
        chainSubjects = listOf("CN=Key", "CN=Root"),
        isChainSigned = true,
        attestationVersion = 100,
        attestationLevel = AttestationLevel.TRUSTED_ENVIRONMENT,
        keymasterVersion = 100,
        keymasterLevel = AttestationLevel.TRUSTED_ENVIRONMENT,
        isChallengeMatching = true,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts empty`() = runTest(dispatcher) {
        val viewModel = AttestationViewModel(attestor = { report }, dispatcher = dispatcher)

        assertThat(viewModel.state.value.report).isNull()
        assertThat(viewModel.state.value.isBusy).isFalse()
    }

    @Test
    fun `shows the report`() = runTest(dispatcher) {
        val viewModel = AttestationViewModel(attestor = { report }, dispatcher = dispatcher)

        viewModel.onAttest()
        advanceUntilIdle()

        assertThat(viewModel.state.value.report).isEqualTo(report)
        assertThat(viewModel.state.value.errorMessage).isNull()
        assertThat(viewModel.state.value.isBusy).isFalse()
    }

    @Test
    fun `shows the error when attestation fails`() = runTest(dispatcher) {
        val viewModel = AttestationViewModel(attestor = Attestor { error("No attestation on this device") }, dispatcher = dispatcher)

        viewModel.onAttest()
        advanceUntilIdle()

        assertThat(viewModel.state.value.report).isNull()
        assertThat(viewModel.state.value.errorMessage).isEqualTo("No attestation on this device")
    }

    @Test
    fun `a new attestation replaces an old error`() = runTest(dispatcher) {
        var fail = true
        val viewModel = AttestationViewModel(attestor = Attestor { if (fail) error("busy") else report }, dispatcher = dispatcher)
        viewModel.onAttest()
        advanceUntilIdle()

        fail = false
        viewModel.onAttest()
        advanceUntilIdle()

        assertThat(viewModel.state.value.errorMessage).isNull()
        assertThat(viewModel.state.value.report).isEqualTo(report)
    }
}
