package com.danyastry.keystore.feature.attestation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.feature.attestation.AndroidAttestor
import com.danyastry.keystore.feature.attestation.AttestationReport
import com.danyastry.keystore.feature.attestation.Attestor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AttestationUiState(
    val isBusy: Boolean = false,
    val report: AttestationReport? = null,
    val errorMessage: String? = null,
)

class AttestationViewModel(
    private val attestor: Attestor = AndroidAttestor(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(AttestationUiState())
    val state: StateFlow<AttestationUiState> = _state.asStateFlow()

    fun onAttest() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, errorMessage = null) }
            val result = withContext(dispatcher) { runCatching { attestor.attest() } }
            _state.update {
                AttestationUiState(
                    isBusy = false,
                    report = result.getOrNull(),
                    errorMessage = result.exceptionOrNull()?.let { error -> error.message ?: error::class.java.simpleName },
                )
            }
        }
    }
}
