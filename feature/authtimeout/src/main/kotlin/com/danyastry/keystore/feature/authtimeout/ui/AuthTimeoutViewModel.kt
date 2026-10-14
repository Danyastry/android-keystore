package com.danyastry.keystore.feature.authtimeout.ui

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.feature.authtimeout.AesGcmTimedCrypto
import com.danyastry.keystore.feature.authtimeout.AuthTimeoutKeys
import com.danyastry.keystore.feature.authtimeout.CredentialStatus
import com.danyastry.keystore.feature.authtimeout.KeyLockedException
import com.danyastry.keystore.feature.authtimeout.SealedMessage
import com.danyastry.keystore.feature.authtimeout.TimedCrypto
import java.security.GeneralSecurityException
import java.security.ProviderException
import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.R)
class AuthTimeoutViewModel(
    private val status: CredentialStatus,
    private val keys: SecretKeyStore = AuthTimeoutKeys.store(),
    private val crypto: TimedCrypto = AesGcmTimedCrypto(),
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val timeoutSeconds: Int = AuthTimeoutKeys.TIMEOUT_SECONDS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthTimeoutUiState())
    val state: StateFlow<AuthTimeoutUiState> = _state.asStateFlow()

    private val _unlockRequests = Channel<Unit>(Channel.CONFLATED)
    val unlockRequests: Flow<Unit> = _unlockRequests.receiveAsFlow()

    private var unlockedAt: Long? = null
    private var countdown: Job? = null

    init {
        onRefresh()
    }

    fun onRefresh() {
        viewModelScope.launch { refresh() }
    }

    fun onCreateKey() = work {
        try {
            keys.create(KEY_ALIAS)
            null
        } catch (_: GeneralSecurityException) {
            AuthTimeoutError.KEY_CREATION_FAILED
        } catch (_: ProviderException) {
            AuthTimeoutError.KEY_CREATION_FAILED
        }
    }

    fun onDeleteKey() = work {
        keys.delete(KEY_ALIAS)
        _state.update { it.copy(sealed = "", decrypted = "", attempts = emptyList()) }
        null
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input) }
    }

    fun onUnlock() {
        _state.update { it.copy(isAwaitingAuth = true, error = null, authMessage = "") }
        _unlockRequests.trySend(Unit)
    }

    fun onUnlocked() {
        unlockedAt = clock()
        _state.update { it.copy(isAwaitingAuth = false, error = null) }
        countdown?.cancel()
        countdown = viewModelScope.launch {
            do {
                val left = secondsLeft()
                _state.update { it.copy(secondsLeft = left) }
                delay(TICK_MILLIS)
            } while (left > 0)
        }
    }

    fun onAuthFailed(canceled: Boolean, message: String) {
        _state.update {
            it.copy(
                isAwaitingAuth = false,
                error = if (canceled) AuthTimeoutError.CANCELED else AuthTimeoutError.AUTH_FAILED,
                authMessage = if (canceled) "" else message,
            )
        }
    }

    fun onEncrypt() {
        val plaintext = _state.value.input.encodeToByteArray()
        attempt(TimedOperation.ENCRYPT) { key ->
            val sealed = crypto.encrypt(key, plaintext)
            _state.update { it.copy(sealed = sealed.toBase64(), decrypted = "") }
        }
    }

    fun onDecrypt() {
        val message = SealedMessage.fromBase64OrNull(_state.value.sealed) ?: return
        attempt(TimedOperation.DECRYPT) { key ->
            val plaintext = crypto.decrypt(key, message)
            _state.update { it.copy(decrypted = plaintext.decodeToString()) }
        }
    }

    private fun attempt(operation: TimedOperation, action: (SecretKey) -> Unit) = work {
        val key = keys.get(KEY_ALIAS) ?: return@work AuthTimeoutError.NO_KEY
        val secondsSinceUnlock = unlockedAt?.let { (clock() - it) / MILLIS_PER_SECOND }
        try {
            action(key)
            record(Attempt(operation, isAllowed = true, secondsSinceUnlock = secondsSinceUnlock))
            null
        } catch (_: KeyLockedException) {
            record(Attempt(operation, isAllowed = false, secondsSinceUnlock = secondsSinceUnlock))
            AuthTimeoutError.KEY_LOCKED
        }
    }

    private fun record(attempt: Attempt) {
        _state.update { it.copy(attempts = (listOf(attempt) + it.attempts).take(MAX_ATTEMPTS)) }
    }

    private fun secondsLeft(): Int {
        val start = unlockedAt ?: return 0
        val remaining = timeoutSeconds * MILLIS_PER_SECOND - (clock() - start)
        return ((remaining + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceAtLeast(0).toInt()
    }

    private fun work(action: () -> AuthTimeoutError?) {
        _state.update { it.copy(error = null, authMessage = "") }
        viewModelScope.launch {
            val error = withContext(dispatcher) { action() }
            _state.update { it.copy(error = error) }
            refresh()
        }
    }

    private suspend fun refresh() {
        val (availability, hasKey) = withContext(dispatcher) { status.check() to (keys.get(KEY_ALIAS) != null) }
        _state.update { it.copy(availability = availability, hasKey = hasKey) }
    }

    companion object {
        const val KEY_ALIAS = "auth-timeout-demo"
        const val MAX_ATTEMPTS = 5
        private const val TICK_MILLIS = 1_000L
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
