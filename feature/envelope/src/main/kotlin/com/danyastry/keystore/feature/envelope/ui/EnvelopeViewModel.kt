package com.danyastry.keystore.feature.envelope.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.KeyPairStore
import com.danyastry.keystore.feature.envelope.Envelope
import com.danyastry.keystore.feature.envelope.EnvelopeCipher
import com.danyastry.keystore.feature.envelope.EnvelopeKeys
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnvelopeViewModel(
    private val keys: KeyPairStore = EnvelopeKeys.store(),
    private val cipher: EnvelopeCipher = EnvelopeCipher(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(EnvelopeUiState())
    val state: StateFlow<EnvelopeUiState> = _state.asStateFlow()

    private var envelopeBytes: ByteArray? = null
    private var sealedPayload: ByteArray? = null

    init {
        viewModelScope.launch { refresh() }
    }

    fun onCreateKey() {
        viewModelScope.launch {
            val failed = withContext(dispatcher) {
                try {
                    keys.create(KEY_ALIAS)
                    false
                } catch (_: GeneralSecurityException) {
                    true
                } catch (_: ProviderException) {
                    true
                }
            }
            _state.update { it.copy(error = if (failed) EnvelopeError.KEY_CREATION_FAILED else null) }
            refresh()
        }
    }

    fun onDeleteKey() {
        viewModelScope.launch {
            withContext(dispatcher) { keys.delete(KEY_ALIAS) }
            _state.update { it.copy(opened = null, error = null) }
            refresh()
        }
    }

    fun onSourceChange(source: PayloadSource) {
        _state.update { it.copy(source = source, payloadBytes = payload(source, it.input).size, rsaOnly = null, error = null) }
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input, payloadBytes = payload(it.source, input).size, rsaOnly = null) }
    }

    fun onRsaOnly() {
        val snapshot = _state.value
        val plaintext = payload(snapshot.source, snapshot.input)
        viewModelScope.launch {
            val result = withContext(dispatcher) {
                val pair = keys.get(KEY_ALIAS) ?: return@withContext null
                try {
                    cipher.rsaOnly(plaintext, pair.public)
                    RsaOnlyResult(plaintext.size, isAllowed = true)
                } catch (e: GeneralSecurityException) {
                    RsaOnlyResult(plaintext.size, isAllowed = false, reason = e.javaClass.simpleName)
                }
            }
            _state.update { if (result == null) it.copy(error = EnvelopeError.NO_KEY) else it.copy(rsaOnly = result, error = null) }
        }
    }

    fun onSeal() {
        val snapshot = _state.value
        val plaintext = payload(snapshot.source, snapshot.input)
        viewModelScope.launch {
            val envelope = withContext(dispatcher) { keys.get(KEY_ALIAS)?.let { cipher.seal(plaintext, it.public) } }
            if (envelope == null) {
                _state.update { it.copy(error = EnvelopeError.NO_KEY) }
                return@launch
            }
            val bytes = envelope.toBytes()
            envelopeBytes = bytes
            sealedPayload = plaintext
            _state.update { it.copy(sealed = summaryOf(envelope, bytes), opened = null, error = null) }
        }
    }

    fun onOpen() {
        val bytes = envelopeBytes ?: return
        val original = sealedPayload ?: return
        viewModelScope.launch {
            val opened = withContext(dispatcher) {
                val pair = keys.get(KEY_ALIAS) ?: return@withContext null
                val envelope = Envelope.fromBytesOrNull(bytes) ?: return@withContext null
                try {
                    val plaintext = cipher.open(envelope, pair.private)
                    OpenedResult(plaintext.size, plaintext.contentEquals(original), plaintext.decodeToString().take(PREVIEW_CHARS))
                } catch (_: GeneralSecurityException) {
                    null
                }
            }
            _state.update { if (opened == null) it.copy(opened = null, error = EnvelopeError.CANNOT_OPEN) else it.copy(opened = opened, error = null) }
        }
    }

    private fun summaryOf(envelope: Envelope, bytes: ByteArray) = EnvelopeSummary(
        wrappedKeyBytes = envelope.wrappedKey.size,
        ivBytes = envelope.message.iv.size,
        ciphertextBytes = envelope.message.ciphertext.size,
        totalBytes = bytes.size,
        preview = Base64.getEncoder().encodeToString(bytes.copyOf(minOf(bytes.size, PREVIEW_BYTES))),
    )

    private fun payload(source: PayloadSource, input: String): ByteArray = when (source) {
        PayloadSource.TEXT -> input.encodeToByteArray()
        PayloadSource.SAMPLE -> SAMPLE
    }

    private suspend fun refresh() {
        val hasKey = withContext(dispatcher) { keys.get(KEY_ALIAS) != null }
        _state.update { it.copy(hasKey = hasKey) }
    }

    companion object {
        const val KEY_ALIAS = "envelope-demo"
        const val SAMPLE_BYTES = 64 * 1024
        private const val PREVIEW_CHARS = 60
        private const val PREVIEW_BYTES = 48
        private const val SAMPLE_LINE = "The quick brown fox jumps over the lazy dog. "
        private val SAMPLE: ByteArray = SAMPLE_LINE.repeat(SAMPLE_BYTES / SAMPLE_LINE.length + 1).take(SAMPLE_BYTES).encodeToByteArray()
    }
}
