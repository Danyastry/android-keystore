package com.danyastry.keystore.feature.unlockeddevice.ui

import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.feature.unlockeddevice.DeviceLock
import com.danyastry.keystore.feature.unlockeddevice.KeyProbe
import com.danyastry.keystore.feature.unlockeddevice.ProbeKey
import com.danyastry.keystore.feature.unlockeddevice.ProbeResult
import com.google.common.truth.Truth.assertThat
import java.security.InvalidAlgorithmParameterException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnlockedDeviceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val device = FakeDeviceLock()
    private val stores = ProbeKey.entries.associateWith { InMemorySecretKeys() }
    private lateinit var viewModel: UnlockedDeviceViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = UnlockedDeviceViewModel(
            device = device,
            keys = stores,
            probe = LockAwareProbe(device, stores),
            delaySeconds = DELAY_SECONDS,
            dispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts without keys and reports the screen lock`() = test {
        assertThat(state.hasScreenLock).isTrue()
        assertThat(state.hasKeys).isFalse()
        assertThat(state.canTest).isFalse()
    }

    @Test
    fun `reports a device without a screen lock`() = test {
        device.hasScreenLock = false
        viewModel.onRefresh()
        advanceUntilIdle()

        assertThat(state.hasScreenLock).isFalse()
    }

    @Test
    fun `creates both keys`() = test {
        viewModel.onCreateKeys()
        advanceUntilIdle()

        assertThat(state.hasKeys).isTrue()
        assertThat(state.canCreateKeys).isFalse()
        assertThat(state.canTest).isTrue()
    }

    @Test
    fun `a refused key creation is reported`() = test {
        stores.getValue(ProbeKey.UNLOCKED_ONLY).failCreation = true

        viewModel.onCreateKeys()
        advanceUntilIdle()

        assertThat(state.isKeyCreationFailed).isTrue()
        assertThat(state.hasKeys).isFalse()
    }

    @Test
    fun `both keys work while the device is unlocked`() = test {
        createKeys()

        viewModel.onTestNow()
        advanceUntilIdle()

        val round = state.rounds.single()
        assertThat(round.number).isEqualTo(1)
        assertThat(round.wasDeviceLocked).isFalse()
        assertThat(round.results).containsExactly(ProbeKey.REGULAR, ProbeResult.Allowed, ProbeKey.UNLOCKED_ONLY, ProbeResult.Allowed)
    }

    @Test
    fun `counts down before the delayed test`() = test {
        createKeys()

        viewModel.onTestLater()
        runCurrent()
        assertThat(state.secondsUntilTest).isEqualTo(DELAY_SECONDS)
        assertThat(state.canTest).isFalse()

        advanceTimeBy(3_000)
        runCurrent()
        assertThat(state.secondsUntilTest).isEqualTo(DELAY_SECONDS - 3)
        assertThat(state.rounds).isEmpty()
    }

    @Test
    fun `only the unlocked-only key is refused on a locked screen`() = test {
        createKeys()

        viewModel.onTestLater()
        runCurrent()
        device.locked = true
        advanceUntilIdle()

        val round = state.rounds.single()
        assertThat(round.wasDeviceLocked).isTrue()
        assertThat(round.results[ProbeKey.REGULAR]).isEqualTo(ProbeResult.Allowed)
        assertThat(round.results[ProbeKey.UNLOCKED_ONLY]).isEqualTo(ProbeResult.Refused("InvalidKeyException"))
        assertThat(state.isCountingDown).isFalse()
    }

    @Test
    fun `canceling the countdown skips the test`() = test {
        createKeys()
        viewModel.onTestLater()
        runCurrent()

        viewModel.onCancelTest()
        advanceUntilIdle()

        assertThat(state.isCountingDown).isFalse()
        assertThat(state.rounds).isEmpty()
    }

    @Test
    fun `newest round comes first and old rounds are dropped`() = test {
        createKeys()
        repeat(UnlockedDeviceViewModel.MAX_ROUNDS + 2) {
            viewModel.onTestNow()
            advanceUntilIdle()
        }

        assertThat(state.rounds).hasSize(UnlockedDeviceViewModel.MAX_ROUNDS)
        assertThat(state.rounds.first().number).isEqualTo(UnlockedDeviceViewModel.MAX_ROUNDS + 2)
    }

    @Test
    fun `deleting the keys clears results and stops the countdown`() = test {
        createKeys()
        viewModel.onTestNow()
        advanceUntilIdle()
        viewModel.onTestLater()
        runCurrent()

        viewModel.onDeleteKeys()
        advanceUntilIdle()

        assertThat(state.hasKeys).isFalse()
        assertThat(state.rounds).isEmpty()
        assertThat(state.isCountingDown).isFalse()
    }

    private fun TestScope.createKeys() {
        viewModel.onCreateKeys()
        advanceUntilIdle()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class FakeDeviceLock : DeviceLock {
        var hasScreenLock = true
        var locked = false

        override fun hasScreenLock(): Boolean = hasScreenLock

        override fun isLocked(): Boolean = locked
    }

    private class LockAwareProbe(
        private val device: FakeDeviceLock,
        private val stores: Map<ProbeKey, InMemorySecretKeys>,
    ) : KeyProbe {
        override fun probe(key: SecretKey): ProbeResult {
            val isUnlockedOnly = stores.getValue(ProbeKey.UNLOCKED_ONLY).holds(key)
            return if (isUnlockedOnly && device.locked) ProbeResult.Refused("InvalidKeyException") else ProbeResult.Allowed
        }
    }

    private class InMemorySecretKeys : SecretKeyStore {
        private val keys = mutableMapOf<String, SecretKey>()
        var failCreation = false

        fun holds(key: SecretKey): Boolean = keys.values.any { it === key }

        override fun get(alias: String): SecretKey? = keys[alias]

        override fun create(alias: String): SecretKey {
            if (failCreation) throw InvalidAlgorithmParameterException("Keystore refused")
            return KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also { keys[alias] = it }
        }

        override fun delete(alias: String) {
            keys.remove(alias)
        }
    }

    private companion object {
        const val DELAY_SECONDS = 10
    }
}
