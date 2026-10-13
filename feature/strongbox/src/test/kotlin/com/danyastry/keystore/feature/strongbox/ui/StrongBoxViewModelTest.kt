package com.danyastry.keystore.feature.strongbox.ui

import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.feature.strongbox.KeyCreation
import com.danyastry.keystore.feature.strongbox.KeyTarget
import com.danyastry.keystore.feature.strongbox.StrongBoxKeys
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
class StrongBoxViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reports whether the device declares StrongBox`() = runTest(dispatcher) {
        val viewModel = StrongBoxViewModel(FakeKeys(hasStrongBox = true), dispatcher)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isStrongBoxDeclared).isTrue()
        assertThat(viewModel.state.value.keys).isEmpty()
        assertThat(viewModel.state.value.canMeasure).isFalse()
    }

    @Test
    fun `creates both keys on a device with StrongBox`() = runTest(dispatcher) {
        val viewModel = StrongBoxViewModel(FakeKeys(hasStrongBox = true), dispatcher)

        viewModel.onCreateKeys()
        advanceUntilIdle()

        assertThat(viewModel.state.value.keys).containsExactly(
            KeyTarget.REGULAR, KeyCreation.Created(KeyLocation.TRUSTED_ENVIRONMENT),
            KeyTarget.STRONGBOX, KeyCreation.Created(KeyLocation.STRONGBOX),
        )
        assertThat(viewModel.state.value.isBusy).isFalse()
    }

    @Test
    fun `shows StrongBox as unavailable without the chip`() = runTest(dispatcher) {
        val viewModel = StrongBoxViewModel(FakeKeys(hasStrongBox = false), dispatcher)

        viewModel.onCreateKeys()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isStrongBoxDeclared).isFalse()
        assertThat(viewModel.state.value.keys[KeyTarget.STRONGBOX]).isEqualTo(KeyCreation.Unavailable)
        assertThat(viewModel.state.value.canMeasure).isTrue()
    }

    @Test
    fun `measures only the keys that exist`() = runTest(dispatcher) {
        val viewModel = StrongBoxViewModel(FakeKeys(hasStrongBox = false), dispatcher)
        viewModel.onCreateKeys()
        advanceUntilIdle()

        viewModel.onMeasure()
        advanceUntilIdle()

        assertThat(viewModel.state.value.timings).containsExactly(KeyTarget.REGULAR, 10.milliseconds)
    }

    @Test
    fun `measures both keys with StrongBox`() = runTest(dispatcher) {
        val viewModel = StrongBoxViewModel(FakeKeys(hasStrongBox = true), dispatcher)
        viewModel.onCreateKeys()
        advanceUntilIdle()

        viewModel.onMeasure()
        advanceUntilIdle()

        assertThat(viewModel.state.value.timings.keys).containsExactly(KeyTarget.REGULAR, KeyTarget.STRONGBOX)
    }

    @Test
    fun `deleting clears keys and timings`() = runTest(dispatcher) {
        val keys = FakeKeys(hasStrongBox = true)
        val viewModel = StrongBoxViewModel(keys, dispatcher)
        viewModel.onCreateKeys()
        viewModel.onMeasure()
        advanceUntilIdle()

        viewModel.onDeleteKeys()
        advanceUntilIdle()

        assertThat(viewModel.state.value.keys).isEmpty()
        assertThat(viewModel.state.value.timings).isEmpty()
        assertThat(keys.created).isEmpty()
    }

    private class FakeKeys(private val hasStrongBox: Boolean) : StrongBoxKeys {
        val created = mutableSetOf<KeyTarget>()

        override fun isStrongBoxDeclared(): Boolean = hasStrongBox

        override fun create(target: KeyTarget): KeyCreation {
            if (target == KeyTarget.STRONGBOX && !hasStrongBox) return KeyCreation.Unavailable
            created += target
            return KeyCreation.Created(if (target == KeyTarget.STRONGBOX) KeyLocation.STRONGBOX else KeyLocation.TRUSTED_ENVIRONMENT)
        }

        override fun measure(target: KeyTarget, rounds: Int): Duration? =
            if (target in created) (if (target == KeyTarget.STRONGBOX) 300 else 10).milliseconds else null

        override fun deleteAll() {
            created.clear()
        }
    }
}
