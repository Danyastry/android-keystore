package com.danyastry.keystore.feature.keyimport.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.feature.keyimport.AndroidKeyImporter
import com.danyastry.keystore.feature.keyimport.ImportCipher
import com.danyastry.keystore.feature.keyimport.KeyHex
import com.danyastry.keystore.feature.keyimport.KeyImporter
import java.security.GeneralSecurityException
import java.security.ProviderException
import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeyImportViewModel(
    private val importer: KeyImporter = AndroidKeyImporter(),
    private val newHex: () -> String = { KeyHex.random() },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(KeyImportUiState())
    val state: StateFlow<KeyImportUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    fun onGenerate() {
        _state.update { it.copy(originalHex = newHex(), error = null) }
    }

    fun onHexChange(hex: String) {
        _state.update { it.copy(originalHex = hex, error = null) }
    }

    fun onImport() {
        val bytes = KeyHex.parseOrNull(_state.value.originalHex) ?: return
        viewModelScope.launch {
            val failed = withContext(dispatcher) {
                try {
                    importer.import(KEY_ALIAS, bytes)
                    false
                } catch (_: GeneralSecurityException) {
                    true
                } catch (_: ProviderException) {
                    true
                } finally {
                    bytes.fill(0)
                }
            }
            _state.update { it.copy(error = if (failed) KeyImportError.IMPORT_FAILED else null) }
            refresh()
        }
    }

    fun onForgetOriginal() {
        _state.update { it.copy(originalHex = "", error = null) }
    }

    fun onDeleteImported() {
        viewModelScope.launch {
            withContext(dispatcher) { importer.delete(KEY_ALIAS) }
            _state.update { it.copy(sealed = "", decrypted = "", decryptedBy = null, error = null) }
            refresh()
        }
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input) }
    }

    fun onEncryptWithKeystore() {
        val plaintext = _state.value.input.encodeToByteArray()
        viewModelScope.launch {
            val sealed = withContext(dispatcher) { importer.get(KEY_ALIAS)?.let { ImportCipher.encrypt(it, plaintext).toBase64() } }
            _state.update {
                if (sealed == null) {
                    it.copy(error = KeyImportError.NO_IMPORTED_KEY)
                } else {
                    it.copy(sealed = sealed, decrypted = "", decryptedBy = null, error = null)
                }
            }
        }
    }

    fun onDecryptWithKeystore() = decrypt(Reader.KEYSTORE, KeyImportError.NO_IMPORTED_KEY) { importer.get(KEY_ALIAS) }

    fun onDecryptWithOriginal() {
        val hex = _state.value.originalHex
        decrypt(Reader.ORIGINAL, KeyImportError.NO_ORIGINAL) { KeyHex.parseOrNull(hex)?.let(ImportCipher::softwareKey) }
    }

    private fun decrypt(reader: Reader, missingKey: KeyImportError, keyOf: () -> SecretKey?) {
        val message = EncryptedMessage.fromBase64OrNull(_state.value.sealed)
        if (message == null) {
            _state.update { it.copy(error = KeyImportError.INVALID_MESSAGE) }
            return
        }
        viewModelScope.launch {
            val (plaintext, error) = withContext(dispatcher) {
                val key = keyOf() ?: return@withContext null to missingKey
                try {
                    ImportCipher.decrypt(key, message).decodeToString() to null
                } catch (_: GeneralSecurityException) {
                    null to KeyImportError.WRONG_KEY
                }
            }
            _state.update {
                if (plaintext == null) {
                    it.copy(decrypted = "", decryptedBy = null, error = error)
                } else {
                    it.copy(decrypted = plaintext, decryptedBy = reader, error = null)
                }
            }
        }
    }

    private suspend fun refresh() {
        val (details, isHidden) = withContext(dispatcher) {
            importer.details(KEY_ALIAS) to (importer.get(KEY_ALIAS)?.encoded == null)
        }
        _state.update { it.copy(imported = details, isImportedMaterialHidden = details != null && isHidden) }
    }

    companion object {
        const val KEY_ALIAS = "key-import-demo"
    }
}
