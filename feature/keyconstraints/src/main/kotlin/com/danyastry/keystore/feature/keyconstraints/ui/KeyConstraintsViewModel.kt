package com.danyastry.keystore.feature.keyconstraints.ui

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.feature.keyconstraints.AndroidConstrainedKeys
import com.danyastry.keystore.feature.keyconstraints.ConstrainedKey
import com.danyastry.keystore.feature.keyconstraints.ConstrainedKeys
import java.security.GeneralSecurityException
import java.security.ProviderException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeyConstraintsViewModel(
    private val keys: ConstrainedKeys = AndroidConstrainedKeys(),
    private val clock: () -> Long = System::currentTimeMillis,
    deviceSdk: Int = Build.VERSION.SDK_INT,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(KeyConstraintsUiState(limit = LimitState(isSupported = deviceSdk >= LIMIT_MIN_SDK)))
    val state: StateFlow<KeyConstraintsUiState> = _state.asStateFlow()

    private var windowCreatedAt: Long? = null
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            withContext(dispatcher) { ConstrainedKey.entries.forEach(keys::delete) }
        }
    }

    fun onCreateWindowKey() {
        viewModelScope.launch {
            val createdAt = clock()
            val failed = withContext(dispatcher) {
                keys.delete(ConstrainedKey.WINDOW)
                attempt { keys.createWindowKey(createdAt + VALID_FROM_SECONDS * MILLIS, createdAt + VALID_UNTIL_SECONDS * MILLIS) }
            }
            windowCreatedAt = if (failed) null else createdAt
            _state.update { it.copy(window = WindowState(hasKey = !failed, secondsSinceCreation = if (failed) null else 0, isCreationFailed = failed)) }
            if (!failed) startTicker()
        }
    }

    fun onUseWindowKey() {
        val createdAt = windowCreatedAt ?: return
        viewModelScope.launch {
            val result = withContext(dispatcher) { keys.use(ConstrainedKey.WINDOW) }
            val attempt = WindowAttempt(secondsAfterCreation = (clock() - createdAt) / MILLIS, result = result)
            _state.update { it.copy(window = it.window.copy(attempts = (listOf(attempt) + it.window.attempts).take(MAX_ATTEMPTS))) }
        }
    }

    fun onDeleteWindowKey() {
        ticker?.cancel()
        windowCreatedAt = null
        viewModelScope.launch {
            withContext(dispatcher) { keys.delete(ConstrainedKey.WINDOW) }
            _state.update { it.copy(window = WindowState()) }
        }
    }

    fun onCreateLimitedKey() {
        viewModelScope.launch {
            val (failed, exists) = withContext(dispatcher) {
                keys.delete(ConstrainedKey.LIMITED)
                val failed = attempt { keys.createLimitedKey(MAX_USES) }
                failed to keys.exists(ConstrainedKey.LIMITED)
            }
            _state.update { it.copy(limit = it.limit.copy(hasKey = exists, usesMade = 0, attempts = emptyList(), isCreationFailed = failed)) }
        }
    }

    fun onUseLimitedKey() {
        viewModelScope.launch {
            val (result, exists) = withContext(dispatcher) { keys.use(ConstrainedKey.LIMITED) to keys.exists(ConstrainedKey.LIMITED) }
            _state.update { state ->
                val limit = state.limit
                val number = limit.usesMade + 1
                state.copy(
                    limit = limit.copy(
                        hasKey = exists,
                        usesMade = number,
                        attempts = (listOf(LimitAttempt(number, result)) + limit.attempts).take(MAX_ATTEMPTS),
                    ),
                )
            }
        }
    }

    fun onDeleteLimitedKey() {
        viewModelScope.launch {
            withContext(dispatcher) { keys.delete(ConstrainedKey.LIMITED) }
            _state.update { it.copy(limit = LimitState(isSupported = it.limit.isSupported)) }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val createdAt = windowCreatedAt ?: break
                val seconds = (clock() - createdAt) / MILLIS
                _state.update { it.copy(window = it.window.copy(secondsSinceCreation = seconds)) }
                if (seconds >= VALID_UNTIL_SECONDS) break
                delay(MILLIS)
            }
        }
    }

    private fun attempt(action: () -> Unit): Boolean =
        try {
            action()
            false
        } catch (_: GeneralSecurityException) {
            true
        } catch (_: ProviderException) {
            true
        } catch (_: UnsupportedOperationException) {
            true
        }

    companion object {
        const val VALID_FROM_SECONDS = 10L
        const val VALID_UNTIL_SECONDS = 30L
        const val MAX_USES = 3
        const val MAX_ATTEMPTS = 6
        const val LIMIT_MIN_SDK = Build.VERSION_CODES.S
        private const val MILLIS = 1_000L
    }
}
