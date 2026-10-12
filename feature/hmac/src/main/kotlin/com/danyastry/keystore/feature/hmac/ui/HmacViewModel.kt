package com.danyastry.keystore.feature.hmac.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.feature.hmac.HmacKeys
import com.danyastry.keystore.feature.hmac.HmacSigner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HmacViewModel(
    private val keys: SecretKeyStore = HmacKeys.store(),
    private val signer: HmacSigner = HmacSigner(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(HmacUiState())
    val state: StateFlow<HmacUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun onCreateKey() = runAction { keys.create(KEY_ALIAS) }

    fun onDeleteKey() = runAction {
        keys.delete(KEY_ALIAS)
        _state.update { it.copy(tag = "") }
    }

    fun onMessageChange(message: String) {
        _state.update { it.copy(message = message) }
        refresh()
    }

    fun onTagChange(tag: String) {
        _state.update { it.copy(tag = tag) }
        refresh()
    }

    fun onComputeTag() = runAction {
        val key = keys.get(KEY_ALIAS) ?: return@runAction
        val tag = signer.tag(_state.value.message.encodeToByteArray(), key)
        _state.update { it.copy(tag = tag.toHex()) }
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

    private fun compute(state: HmacUiState): HmacUiState {
        val key = keys.get(KEY_ALIAS)
        val base = state.copy(hasKey = key != null, isKeyMaterialHidden = key?.encoded == null, check = null)
        if (key == null || state.tag.isBlank()) return base
        val tag = state.tag.filterNot { it.isWhitespace() }.hexToBytesOrNull() ?: return base.copy(check = TagCheck.NOT_HEX)
        val isValid = signer.verify(state.message.encodeToByteArray(), tag, key)
        return base.copy(check = if (isValid) TagCheck.VALID else TagCheck.INVALID)
    }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        val digits = map { Character.digit(it, 16) }
        if (digits.any { it < 0 }) return null
        return ByteArray(length / 2) { ((digits[it * 2] shl 4) or digits[it * 2 + 1]).toByte() }
    }

    companion object {
        const val KEY_ALIAS = "hmac-demo"
    }
}
