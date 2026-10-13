package com.danyastry.keystore.feature.strongbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.feature.strongbox.KeyCreation
import com.danyastry.keystore.feature.strongbox.KeyTarget
import com.danyastry.keystore.feature.strongbox.StrongBoxKeys
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StrongBoxUiState(
    val isStrongBoxDeclared: Boolean? = null,
    val keys: Map<KeyTarget, KeyCreation> = emptyMap(),
    val timings: Map<KeyTarget, Duration> = emptyMap(),
    val isBusy: Boolean = false,
) {
    val canMeasure: Boolean
        get() = keys.values.any { it is KeyCreation.Created }
}

class StrongBoxViewModel(
    private val keys: StrongBoxKeys,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(StrongBoxUiState())
    val state: StateFlow<StrongBoxUiState> = _state.asStateFlow()

    init {
        work { _state.update { it.copy(isStrongBoxDeclared = keys.isStrongBoxDeclared()) } }
    }

    fun onCreateKeys() = work {
        keys.deleteAll()
        val created = KeyTarget.entries.associateWith { keys.create(it) }
        _state.update { it.copy(keys = created, timings = emptyMap()) }
    }

    fun onMeasure() = work {
        val timings = KeyTarget.entries.mapNotNull { target -> keys.measure(target, ROUNDS)?.let { target to it } }.toMap()
        _state.update { it.copy(timings = timings) }
    }

    fun onDeleteKeys() = work {
        keys.deleteAll()
        _state.update { it.copy(keys = emptyMap(), timings = emptyMap()) }
    }

    private fun work(action: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            withContext(dispatcher) { action() }
            _state.update { it.copy(isBusy = false) }
        }
    }

    companion object {
        const val ROUNDS = 20
    }
}
