package com.danyastry.keystore.feature.authtimeout.ui

import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.feature.authtimeout.AesGcmTimedCrypto
import com.danyastry.keystore.feature.authtimeout.CredentialAvailability
import com.danyastry.keystore.feature.authtimeout.KeyLockedException
import com.danyastry.keystore.feature.authtimeout.SealedMessage
import com.danyastry.keystore.feature.authtimeout.TimedCrypto
import com.google.common.truth.Truth.assertThat
import java.security.InvalidKeyException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class AuthTimeoutViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now: () -> Long = { dispatcher.scheduler.currentTime }
    private val keys = InMemorySecretKeys()
    private val crypto = WindowCrypto(now, TIMEOUT_SECONDS)
    private var availability = CredentialAvailability.AVAILABLE
    private lateinit var viewModel: AuthTimeoutViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = AuthTimeoutViewModel(
            status = { availability },
            keys = keys,
            crypto = crypto,
            clock = now,
            timeoutSeconds = TIMEOUT_SECONDS,
            dispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts locked without a key`() = test {
        assertThat(state.hasKey).isFalse()
        assertThat(state.isUnlocked).isFalse()
        assertThat(state.canCreateKey).isTrue()
        assertThat(state.canUnlock).isFalse()
    }

    @Test
    fun `cannot create a key without a screen lock`() = test {
        availability = CredentialAvailability.NO_LOCK_SCREEN
        viewModel.onRefresh()
        advanceUntilIdle()

        assertThat(state.canCreateKey).isFalse()
    }

    @Test
    fun `unlock asks for the prompt`() = test {
        createKeyAndType("Hello")

        viewModel.onUnlock()
        viewModel.unlockRequests.first()

        assertThat(state.isAwaitingAuth).isTrue()
        assertThat(state.canUnlock).isFalse()
    }

    @Test
    fun `the keystore refuses before the first unlock`() = test {
        createKeyAndType("Hello")

        viewModel.onEncrypt()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(AuthTimeoutError.KEY_LOCKED)
        assertThat(state.sealed).isEmpty()
        assertThat(state.attempts).containsExactly(Attempt(TimedOperation.ENCRYPT, isAllowed = false, secondsSinceUnlock = null))
    }

    @Test
    fun `encrypts without another prompt inside the window`() = test {
        createKeyAndType("Hello")
        unlock()

        viewModel.onEncrypt()
        runCurrent()
        advanceTimeBy(5_000)
        viewModel.onEncrypt()
        runCurrent()

        assertThat(state.error).isNull()
        assertThat(SealedMessage.fromBase64OrNull(state.sealed)).isNotNull()
        assertThat(state.attempts.map { it.isAllowed }).containsExactly(true, true)
    }

    @Test
    fun `the countdown runs down to locked`() = test {
        createKeyAndType("Hello")
        unlock()

        assertThat(state.secondsLeft).isEqualTo(TIMEOUT_SECONDS)
        advanceTimeBy(4_000)
        runCurrent()
        assertThat(state.secondsLeft).isEqualTo(TIMEOUT_SECONDS - 4)
        advanceTimeBy(TIMEOUT_SECONDS * 1_000L)
        runCurrent()
        assertThat(state.isUnlocked).isFalse()
    }

    @Test
    fun `the keystore refuses after the window ends`() = test {
        createKeyAndType("Hello")
        unlock()
        advanceTimeBy((TIMEOUT_SECONDS + 1) * 1_000L)
        runCurrent()

        viewModel.onEncrypt()
        runCurrent()

        assertThat(state.error).isEqualTo(AuthTimeoutError.KEY_LOCKED)
        assertThat(state.attempts.first()).isEqualTo(
            Attempt(TimedOperation.ENCRYPT, isAllowed = false, secondsSinceUnlock = TIMEOUT_SECONDS + 1L),
        )
    }

    @Test
    fun `a new unlock restarts the window`() = test {
        createKeyAndType("Hello")
        unlock()
        advanceTimeBy((TIMEOUT_SECONDS + 1) * 1_000L)
        runCurrent()

        unlock()
        viewModel.onEncrypt()
        runCurrent()

        assertThat(state.secondsLeft).isEqualTo(TIMEOUT_SECONDS)
        assertThat(state.error).isNull()
    }

    @Test
    fun `decrypts back inside the window`() = test {
        createKeyAndType("Attack at dawn")
        unlock()
        viewModel.onEncrypt()
        runCurrent()

        viewModel.onDecrypt()
        runCurrent()

        assertThat(state.decrypted).isEqualTo("Attack at dawn")
        assertThat(state.attempts.first().operation).isEqualTo(TimedOperation.DECRYPT)
    }

    @Test
    fun `keeps only the latest attempts`() = test {
        createKeyAndType("Hello")
        repeat(AuthTimeoutViewModel.MAX_ATTEMPTS + 2) {
            viewModel.onEncrypt()
            runCurrent()
        }

        assertThat(state.attempts).hasSize(AuthTimeoutViewModel.MAX_ATTEMPTS)
    }

    @Test
    fun `canceling the prompt is not a failure`() = test {
        createKeyAndType("Hello")
        viewModel.onUnlock()

        viewModel.onAuthFailed(canceled = true, message = "Authentication canceled by user.")

        assertThat(state.isAwaitingAuth).isFalse()
        assertThat(state.error).isEqualTo(AuthTimeoutError.CANCELED)
        assertThat(state.authMessage).isEmpty()
    }

    @Test
    fun `a prompt error keeps the system message`() = test {
        createKeyAndType("Hello")
        viewModel.onUnlock()

        viewModel.onAuthFailed(canceled = false, message = "Too many attempts.")

        assertThat(state.error).isEqualTo(AuthTimeoutError.AUTH_FAILED)
        assertThat(state.authMessage).isEqualTo("Too many attempts.")
    }

    @Test
    fun `deleting the key clears ciphertext and attempts`() = test {
        createKeyAndType("Hello")
        unlock()
        viewModel.onEncrypt()
        runCurrent()

        viewModel.onDeleteKey()
        runCurrent()

        assertThat(state.hasKey).isFalse()
        assertThat(state.sealed).isEmpty()
        assertThat(state.attempts).isEmpty()
    }

    @Test
    fun `encrypting without a key asks for one`() = test {
        viewModel.onInputChange("Hello")

        viewModel.onEncrypt()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(AuthTimeoutError.NO_KEY)
    }

    private fun TestScope.createKeyAndType(text: String) {
        viewModel.onCreateKey()
        viewModel.onInputChange(text)
        runCurrent()
    }

    private fun TestScope.unlock() {
        crypto.unlock()
        viewModel.onUnlocked()
        runCurrent()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        runCurrent()
        block()
    }

    private class WindowCrypto(private val now: () -> Long, private val timeoutSeconds: Int) : TimedCrypto {
        private val real = AesGcmTimedCrypto()
        private var unlockedAt: Long? = null

        fun unlock() {
            unlockedAt = now()
        }

        override fun encrypt(key: SecretKey, plaintext: ByteArray): SealedMessage = checked { real.encrypt(key, plaintext) }

        override fun decrypt(key: SecretKey, message: SealedMessage): ByteArray = checked { real.decrypt(key, message) }

        private fun <T> checked(operation: () -> T): T {
            val start = unlockedAt
            if (start == null || now() - start > timeoutSeconds * 1_000L) {
                throw KeyLockedException(InvalidKeyException("User not authenticated"))
            }
            return operation()
        }
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

    private companion object {
        const val TIMEOUT_SECONDS = 15
    }
}
