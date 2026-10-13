package com.danyastry.keystore.feature.keyinfo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.feature.keyinfo.AndroidKeyInspector
import com.danyastry.keystore.feature.keyinfo.KeyInspector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class KeyInfoUiState(
    val isLoading: Boolean = true,
    val keys: List<KeyDetails> = emptyList(),
)

class KeyInfoViewModel(
    private val inspector: KeyInspector = AndroidKeyInspector(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(KeyInfoUiState())
    val state: StateFlow<KeyInfoUiState> = _state.asStateFlow()

    init {
        onRefresh()
    }

    fun onRefresh() = load {}

    fun onCreateSampleKey() = load { inspector.createSampleKey() }

    private fun load(action: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val keys = withContext(dispatcher) {
                action()
                inspector.keys()
            }
            _state.update { KeyInfoUiState(isLoading = false, keys = keys) }
        }
    }
}
