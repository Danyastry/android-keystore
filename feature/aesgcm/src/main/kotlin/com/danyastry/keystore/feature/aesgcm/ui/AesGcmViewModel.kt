package com.danyastry.keystore.feature.aesgcm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.core.ui.components.CipherMode
import com.danyastry.keystore.feature.aesgcm.AesGcmCipher
import com.danyastry.keystore.feature.aesgcm.AesGcmKeys
import com.danyastry.keystore.feature.aesgcm.EncryptedMessage
import java.security.GeneralSecurityException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AesGcmViewModel(
    private val keys: SecretKeyStore = AesGcmKeys.store(),
    private val cipher: AesGcmCipher = AesGcmCipher(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(AesGcmUiState())
    val state: StateFlow<AesGcmUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun onCreateKey() = runKeyAction { keys.create(KEY_ALIAS) }

    fun onDeleteKey() = runKeyAction { keys.delete(KEY_ALIAS) }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input) }
        refresh()
    }

    fun onModeChange(mode: CipherMode) {
        _state.update { it.copy(mode = mode) }
        refresh()
    }

    fun onUseOutputAsInput() {
        _state.update { it.copy(input = it.output, mode = it.mode.toggled()) }
        refresh()
    }

    fun onFlipBit() {
        val message = EncryptedMessage.fromBase64OrNull(_state.value.input) ?: return
        val tampered = message.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        onInputChange(EncryptedMessage(message.iv, tampered).toBase64())
    }

    private fun runKeyAction(action: () -> Unit) {
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

    private fun compute(state: AesGcmUiState): AesGcmUiState {
        val key = keys.get(KEY_ALIAS)
        val base = state.copy(hasKey = key != null, isKeyMaterialHidden = key?.encoded == null, output = "", ivHex = "", error = null)
        if (state.input.isEmpty()) return base
        if (key == null) return base.copy(error = AesGcmError.NO_KEY)
        return when (state.mode) {
            CipherMode.ENCRYPT -> {
                val message = cipher.encrypt(state.input.encodeToByteArray(), key)
                base.copy(output = message.toBase64(), ivHex = message.iv.toHex())
            }
            CipherMode.DECRYPT -> {
                val message = EncryptedMessage.fromBase64OrNull(state.input) ?: return base.copy(error = AesGcmError.INVALID_MESSAGE)
                try {
                    base.copy(output = cipher.decrypt(message, key).decodeToString(), ivHex = message.iv.toHex())
                } catch (_: GeneralSecurityException) {
                    base.copy(error = AesGcmError.TAMPERED, ivHex = message.iv.toHex())
                }
            }
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    companion object {
        const val KEY_ALIAS = "aes-gcm-demo"
    }
}
