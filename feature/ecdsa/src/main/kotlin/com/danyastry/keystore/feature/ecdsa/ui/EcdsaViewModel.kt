package com.danyastry.keystore.feature.ecdsa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.KeyPairStore
import com.danyastry.keystore.feature.ecdsa.EcdsaKeys
import com.danyastry.keystore.feature.ecdsa.EcdsaSigner
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EcdsaViewModel(
    private val keys: KeyPairStore = EcdsaKeys.store(),
    private val signer: EcdsaSigner = EcdsaSigner(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(EcdsaUiState())
    val state: StateFlow<EcdsaUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun onCreateKey() = runAction { keys.create(KEY_ALIAS) }

    fun onDeleteKey() = runAction {
        keys.delete(KEY_ALIAS)
        _state.update { it.copy(signature = "") }
    }

    fun onMessageChange(message: String) {
        _state.update { it.copy(message = message) }
        refresh()
    }

    fun onSignatureChange(signature: String) {
        _state.update { it.copy(signature = signature) }
        refresh()
    }

    fun onSign() = runAction {
        val keyPair = keys.get(KEY_ALIAS) ?: return@runAction
        val signature = signer.sign(_state.value.message.encodeToByteArray(), keyPair.private)
        _state.update { it.copy(signature = Base64.getEncoder().encodeToString(signature)) }
    }

    private fun runAction(action: () -> Unit) {
        viewModelScope.launch {
            withContext(dispatcher) { action() }
            refresh()
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        val snapshot = _state.value
        refreshJob = viewModelScope.launch {
            val result = withContext(dispatcher) { compute(snapshot) }
            _state.update { result }
        }
    }

    private fun compute(state: EcdsaUiState): EcdsaUiState {
        val keyPair = keys.get(KEY_ALIAS)
        val base = state.copy(
            hasKey = keyPair != null,
            publicKeyBase64 = keyPair?.public?.encoded?.let(Base64.getEncoder()::encodeToString).orEmpty(),
            isPrivateKeyHidden = keyPair?.private?.encoded == null,
            signatureBytes = 0,
            verification = null,
        )
        if (keyPair == null || state.signature.isBlank()) return base
        val signature = runCatching { Base64.getDecoder().decode(state.signature.filterNot { it.isWhitespace() }) }.getOrNull()
            ?: return base.copy(verification = Verification.NOT_BASE64)
        val isValid = signer.verify(state.message.encodeToByteArray(), signature, keyPair.public)
        return base.copy(signatureBytes = signature.size, verification = if (isValid) Verification.VALID else Verification.INVALID)
    }

    companion object {
        const val KEY_ALIAS = "ecdsa-demo"
    }
}
