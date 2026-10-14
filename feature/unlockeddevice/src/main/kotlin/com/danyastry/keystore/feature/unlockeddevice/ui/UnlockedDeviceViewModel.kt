package com.danyastry.keystore.feature.unlockeddevice.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.feature.unlockeddevice.AesGcmKeyProbe
import com.danyastry.keystore.feature.unlockeddevice.DeviceLock
import com.danyastry.keystore.feature.unlockeddevice.KeyProbe
import com.danyastry.keystore.feature.unlockeddevice.ProbeKey
import com.danyastry.keystore.feature.unlockeddevice.ProbeResult
import com.danyastry.keystore.feature.unlockeddevice.UnlockedDeviceKeys
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

@RequiresApi(Build.VERSION_CODES.P)
class UnlockedDeviceViewModel(
    private val device: DeviceLock,
    private val keys: Map<ProbeKey, SecretKeyStore> = ProbeKey.entries.associateWith(UnlockedDeviceKeys::store),
    private val probe: KeyProbe = AesGcmKeyProbe(),
    private val delaySeconds: Int = DELAY_SECONDS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockedDeviceUiState())
    val state: StateFlow<UnlockedDeviceUiState> = _state.asStateFlow()

    private var countdown: Job? = null

    init {
        onRefresh()
    }

    fun onRefresh() {
        viewModelScope.launch { refresh() }
    }

    fun onCreateKeys() {
        viewModelScope.launch {
            val failed = withContext(dispatcher) {
                try {
                    keys.forEach { (key, store) -> store.create(UnlockedDeviceKeys.aliasOf(key)) }
                    false
                } catch (_: GeneralSecurityException) {
                    true
                } catch (_: ProviderException) {
                    true
                }
            }
            _state.update { it.copy(isKeyCreationFailed = failed) }
            refresh()
        }
    }

    fun onDeleteKeys() {
        countdown?.cancel()
        viewModelScope.launch {
            withContext(dispatcher) { keys.forEach { (key, store) -> store.delete(UnlockedDeviceKeys.aliasOf(key)) } }
            _state.update { it.copy(rounds = emptyList(), secondsUntilTest = 0, isKeyCreationFailed = false) }
            refresh()
        }
    }

    fun onTestNow() {
        viewModelScope.launch { runRound() }
    }

    fun onTestLater() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (left in delaySeconds downTo 1) {
                _state.update { it.copy(secondsUntilTest = left) }
                delay(TICK_MILLIS)
            }
            _state.update { it.copy(secondsUntilTest = 0) }
            runRound()
        }
    }

    fun onCancelTest() {
        countdown?.cancel()
        _state.update { it.copy(secondsUntilTest = 0) }
    }

    private suspend fun runRound() {
        _state.update { it.copy(isTesting = true) }
        val round = withContext(dispatcher) {
            val wasDeviceLocked = device.isLocked()
            val results = keys.mapNotNull { (key, store) ->
                store.get(UnlockedDeviceKeys.aliasOf(key))?.let { key to probe.probe(it) }
            }.toMap()
            ProbeRound(number = 0, wasDeviceLocked = wasDeviceLocked, results = results)
        }
        _state.update { state ->
            val number = (state.rounds.firstOrNull()?.number ?: 0) + 1
            state.copy(isTesting = false, rounds = (listOf(round.copy(number = number)) + state.rounds).take(MAX_ROUNDS))
        }
    }

    private suspend fun refresh() {
        val (hasScreenLock, hasKeys) = withContext(dispatcher) {
            device.hasScreenLock() to keys.all { (key, store) -> store.get(UnlockedDeviceKeys.aliasOf(key)) != null }
        }
        _state.update { it.copy(hasScreenLock = hasScreenLock, hasKeys = hasKeys) }
    }

    companion object {
        const val DELAY_SECONDS = 10
        const val MAX_ROUNDS = 5
        private const val TICK_MILLIS = 1_000L
    }
}
