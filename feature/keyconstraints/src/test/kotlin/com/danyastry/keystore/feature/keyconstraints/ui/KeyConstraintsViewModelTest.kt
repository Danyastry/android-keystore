package com.danyastry.keystore.feature.keyconstraints.ui

import com.danyastry.keystore.feature.keyconstraints.ConstrainedKey
import com.danyastry.keystore.feature.keyconstraints.ConstrainedKeys
import com.danyastry.keystore.feature.keyconstraints.UseResult
import com.google.common.truth.Truth.assertThat
import java.security.InvalidAlgorithmParameterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeyConstraintsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now: () -> Long = { START_MILLIS + dispatcher.scheduler.currentTime }
    private val keys = FakeConstrainedKeys(now)
    private lateinit var viewModel: KeyConstraintsViewModel

    private val window
        get() = viewModel.state.value.window

    private val limit
        get() = viewModel.state.value.limit

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = newViewModel(deviceSdk = 31)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with no keys and clears old ones`() = test {
        assertThat(window.hasKey).isFalse()
        assertThat(window.phase).isNull()
        assertThat(limit.hasKey).isFalse()
        assertThat(keys.deleted).containsAtLeast(ConstrainedKey.WINDOW, ConstrainedKey.LIMITED)
    }

    @Test
    fun `window key is made with the right dates`() = test {
        viewModel.onCreateWindowKey()
        runCurrent()

        assertThat(keys.window).isEqualTo(START_MILLIS + 10_000 to START_MILLIS + 30_000)
        assertThat(window.phase).isEqualTo(WindowPhase.NOT_YET_VALID)
        assertThat(window.secondsToNextPhase).isEqualTo(10)
    }

    @Test
    fun `the phase follows the clock`() = test {
        viewModel.onCreateWindowKey()
        runCurrent()

        advanceTimeBy(12_000)
        runCurrent()
        assertThat(window.phase).isEqualTo(WindowPhase.VALID)
        assertThat(window.secondsToNextPhase).isEqualTo(18)

        advanceTimeBy(20_000)
        runCurrent()
        assertThat(window.phase).isEqualTo(WindowPhase.EXPIRED)
    }

    @Test
    fun `window key is refused, then allowed, then refused`() = test {
        viewModel.onCreateWindowKey()
        runCurrent()

        useWindowAt(seconds = 2)
        useWindowAt(seconds = 15)
        useWindowAt(seconds = 31)

        assertThat(window.attempts.map { it.secondsAfterCreation }).containsExactly(31L, 15L, 2L).inOrder()
        assertThat(window.attempts.map { it.result }).containsExactly(
            UseResult.Refused("KeyExpiredException"),
            UseResult.Allowed,
            UseResult.Refused("KeyNotYetValidException"),
        ).inOrder()
    }

    @Test
    fun `a new window key restarts the clock and the log`() = test {
        viewModel.onCreateWindowKey()
        runCurrent()
        useWindowAt(seconds = 15)

        viewModel.onCreateWindowKey()
        runCurrent()

        assertThat(window.attempts).isEmpty()
        assertThat(window.secondsSinceCreation).isEqualTo(0)
    }

    @Test
    fun `a refused window key creation is reported`() = test {
        keys.failCreation = true

        viewModel.onCreateWindowKey()
        runCurrent()

        assertThat(window.isCreationFailed).isTrue()
        assertThat(window.hasKey).isFalse()
    }

    @Test
    fun `limited key counts down and the keystore deletes it after the last use`() = test {
        viewModel.onCreateLimitedKey()
        runCurrent()
        assertThat(limit.usesLeft).isEqualTo(3)

        repeat(3) {
            viewModel.onUseLimitedKey()
            runCurrent()
        }
        assertThat(limit.usesLeft).isEqualTo(0)
        assertThat(limit.hasKey).isFalse()

        viewModel.onUseLimitedKey()
        runCurrent()

        assertThat(limit.attempts.map { it.result }).containsExactly(
            UseResult.Missing,
            UseResult.Allowed,
            UseResult.Allowed,
            UseResult.Allowed,
        ).inOrder()
    }

    @Test
    fun `usage limit is unavailable before android 12`() = test {
        viewModel = newViewModel(deviceSdk = 30)
        runCurrent()

        assertThat(limit.isSupported).isFalse()
    }

    @Test
    fun `deleting keys resets each card`() = test {
        viewModel.onCreateWindowKey()
        viewModel.onCreateLimitedKey()
        runCurrent()
        useWindowAt(seconds = 15)

        viewModel.onDeleteWindowKey()
        viewModel.onDeleteLimitedKey()
        runCurrent()

        assertThat(window).isEqualTo(WindowState())
        assertThat(limit).isEqualTo(LimitState(isSupported = true))
    }

    private fun newViewModel(deviceSdk: Int) = KeyConstraintsViewModel(keys = keys, clock = now, deviceSdk = deviceSdk, dispatcher = dispatcher)

    private fun TestScope.useWindowAt(seconds: Long) {
        advanceTimeBy(START_MILLIS + seconds * 1_000 - now())
        runCurrent()
        viewModel.onUseWindowKey()
        runCurrent()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        runCurrent()
        block()
    }

    private class FakeConstrainedKeys(private val now: () -> Long) : ConstrainedKeys {
        var window: Pair<Long, Long>? = null
        var usesLeft: Int? = null
        var failCreation = false
        val deleted = mutableListOf<ConstrainedKey>()

        override fun createWindowKey(validFromMillis: Long, validUntilMillis: Long) {
            if (failCreation) throw InvalidAlgorithmParameterException("Keystore refused")
            window = validFromMillis to validUntilMillis
        }

        override fun createLimitedKey(maxUses: Int) {
            if (failCreation) throw InvalidAlgorithmParameterException("Keystore refused")
            usesLeft = maxUses
        }

        override fun use(key: ConstrainedKey): UseResult = when (key) {
            ConstrainedKey.WINDOW -> {
                val (from, until) = window ?: return UseResult.Missing
                when {
                    now() < from -> UseResult.Refused("KeyNotYetValidException")
                    now() > until -> UseResult.Refused("KeyExpiredException")
                    else -> UseResult.Allowed
                }
            }
            ConstrainedKey.LIMITED -> {
                val left = usesLeft ?: return UseResult.Missing
                usesLeft = (left - 1).takeIf { it > 0 }
                UseResult.Allowed
            }
        }

        override fun exists(key: ConstrainedKey): Boolean = when (key) {
            ConstrainedKey.WINDOW -> window != null
            ConstrainedKey.LIMITED -> usesLeft != null
        }

        override fun delete(key: ConstrainedKey) {
            deleted += key
            when (key) {
                ConstrainedKey.WINDOW -> window = null
                ConstrainedKey.LIMITED -> usesLeft = null
            }
        }
    }

    private companion object {
        const val START_MILLIS = 1_760_000_000_000L
    }
}
