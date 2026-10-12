package com.danyastry.keystore.feature.rsaoaep.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.ui.components.CipherMode
import com.danyastry.keystore.feature.rsaoaep.AndroidKeyStoreRsaKeys
import com.danyastry.keystore.feature.rsaoaep.KeyPairStore
import com.danyastry.keystore.feature.rsaoaep.RsaOaepCipher
import java.security.GeneralSecurityException
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

class RsaOaepViewModel(
    private val keys: KeyPairStore = AndroidKeyStoreRsaKeys(),
    private val cipher: RsaOaepCipher = RsaOaepCipher(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(RsaOaepUiState())
    val state: StateFlow<RsaOaepUiState> = _state.asStateFlow()

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

    private fun compute(state: RsaOaepUiState): RsaOaepUiState {
        val keyPair = keys.get(KEY_ALIAS)
        val plaintextBytes = state.input.encodeToByteArray()
        val base = state.copy(
            hasKey = keyPair != null,
            publicKeyBase64 = keyPair?.public?.encoded?.let(Base64.getEncoder()::encodeToString).orEmpty(),
            isPrivateKeyHidden = keyPair?.private?.encoded == null,
            inputBytes = if (state.mode == CipherMode.ENCRYPT) plaintextBytes.size else 0,
            output = "",
            error = null,
        )
        if (state.input.isEmpty()) return base
        if (keyPair == null) return base.copy(error = RsaOaepError.NO_KEY)
        return when (state.mode) {
            CipherMode.ENCRYPT -> {
                if (plaintextBytes.size > RsaOaepCipher.MAX_PLAINTEXT_BYTES) return base.copy(error = RsaOaepError.TOO_LONG)
                base.copy(output = Base64.getEncoder().encodeToString(cipher.encrypt(plaintextBytes, keyPair.public)))
            }
            CipherMode.DECRYPT -> {
                val ciphertext = decodeOrNull(state.input) ?: return base.copy(error = RsaOaepError.INVALID_MESSAGE)
                try {
                    base.copy(output = cipher.decrypt(ciphertext, keyPair.private).decodeToString())
                } catch (_: GeneralSecurityException) {
                    base.copy(error = RsaOaepError.DECRYPTION_FAILED)
                }
            }
        }
    }

    private fun decodeOrNull(text: String): ByteArray? =
        runCatching { Base64.getDecoder().decode(text.filterNot { it.isWhitespace() }) }.getOrNull()
            ?.takeIf { it.size == RsaOaepCipher.KEY_SIZE_BITS / Byte.SIZE_BITS }

    companion object {
        const val KEY_ALIAS = "rsa-oaep-demo"
    }
}
