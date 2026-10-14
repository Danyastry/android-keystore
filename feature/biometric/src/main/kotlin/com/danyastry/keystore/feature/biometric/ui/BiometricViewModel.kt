package com.danyastry.keystore.feature.biometric.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.feature.biometric.AesGcmBiometricCrypto
import com.danyastry.keystore.feature.biometric.BiometricCrypto
import com.danyastry.keystore.feature.biometric.BiometricKeys
import com.danyastry.keystore.feature.biometric.BiometricStatus
import com.danyastry.keystore.feature.biometric.KeyInvalidatedException
import java.security.GeneralSecurityException
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BiometricViewModel(
    private val status: BiometricStatus,
    private val keys: SecretKeyStore = BiometricKeys.store(),
    private val crypto: BiometricCrypto = AesGcmBiometricCrypto(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(BiometricUiState())
    val state: StateFlow<BiometricUiState> = _state.asStateFlow()

    private val _authRequests = Channel<AuthRequest>(Channel.BUFFERED)
    val authRequests: Flow<AuthRequest> = _authRequests.receiveAsFlow()

    init {
        onRefresh()
    }

    fun onRefresh() {
        viewModelScope.launch { refresh() }
    }

    fun onCreateKey() = work {
        try {
            keys.create(KEY_ALIAS)
            null
        } catch (_: GeneralSecurityException) {
            BiometricError.KEY_CREATION_FAILED
        } catch (_: ProviderException) {
            BiometricError.KEY_CREATION_FAILED
        }
    }

    fun onDeleteKey() = work {
        keys.delete(KEY_ALIAS)
        _state.update { it.copy(sealed = "", decrypted = "") }
        null
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input) }
    }

    fun onEncrypt() {
        val plaintext = _state.value.input.encodeToByteArray()
        requestAuth(BiometricOperation.ENCRYPT, plaintext) { crypto.encryptionCipher(it) }
    }

    fun onDecrypt() {
        val message = EncryptedMessage.fromBase64OrNull(_state.value.sealed) ?: return
        requestAuth(BiometricOperation.DECRYPT, message.ciphertext) { crypto.decryptionCipher(it, message.iv) }
    }

    fun onEncryptWithoutAuth() = work {
        val key = keys.get(KEY_ALIAS) ?: return@work BiometricError.NO_KEY
        try {
            val sealed = crypto.seal(crypto.encryptionCipher(key), _state.value.input.encodeToByteArray())
            _state.update { it.copy(sealed = sealed.toBase64(), decrypted = "") }
            null
        } catch (_: KeyInvalidatedException) {
            BiometricError.KEY_INVALIDATED
        } catch (_: GeneralSecurityException) {
            BiometricError.KEY_REFUSED
        }
    }

    fun onAuthenticated(request: AuthRequest) = work {
        try {
            when (request.operation) {
                BiometricOperation.ENCRYPT -> {
                    val sealed = crypto.seal(request.cipher, request.payload)
                    _state.update { it.copy(sealed = sealed.toBase64(), decrypted = "") }
                }
                BiometricOperation.DECRYPT -> {
                    val plaintext = crypto.open(request.cipher, request.payload)
                    _state.update { it.copy(decrypted = plaintext.decodeToString()) }
                }
            }
            null
        } catch (_: GeneralSecurityException) {
            BiometricError.KEY_REFUSED
        }
    }

    fun onAuthFailed(canceled: Boolean, message: String) {
        _state.update {
            it.copy(
                isAwaitingAuth = false,
                error = if (canceled) BiometricError.CANCELED else BiometricError.AUTH_FAILED,
                authMessage = if (canceled) "" else message,
            )
        }
    }

    private fun requestAuth(operation: BiometricOperation, payload: ByteArray, cipherFor: (SecretKey) -> Cipher) {
        _state.update { it.copy(isAwaitingAuth = true, error = null, authMessage = "") }
        viewModelScope.launch {
            when (val prepared = withContext(dispatcher) { prepare(cipherFor) }) {
                is Prepared.Ready -> _authRequests.send(AuthRequest(operation, prepared.cipher, payload))
                is Prepared.Failed -> _state.update { it.copy(isAwaitingAuth = false, error = prepared.error) }
            }
        }
    }

    private fun prepare(cipherFor: (SecretKey) -> Cipher): Prepared {
        val key = keys.get(KEY_ALIAS) ?: return Prepared.Failed(BiometricError.NO_KEY)
        return try {
            Prepared.Ready(cipherFor(key))
        } catch (_: KeyInvalidatedException) {
            Prepared.Failed(BiometricError.KEY_INVALIDATED)
        } catch (_: GeneralSecurityException) {
            Prepared.Failed(BiometricError.KEY_REFUSED)
        }
    }

    private fun work(action: () -> BiometricError?) {
        _state.update { it.copy(error = null, authMessage = "") }
        viewModelScope.launch {
            val error = withContext(dispatcher) { action() }
            _state.update { it.copy(isAwaitingAuth = false, error = error) }
            refresh()
        }
    }

    private suspend fun refresh() {
        val (availability, hasKey) = withContext(dispatcher) { status.check() to (keys.get(KEY_ALIAS) != null) }
        _state.update { it.copy(availability = availability, hasKey = hasKey) }
    }

    private sealed interface Prepared {
        class Ready(val cipher: Cipher) : Prepared
        class Failed(val error: BiometricError) : Prepared
    }

    companion object {
        const val KEY_ALIAS = "biometric-demo"
    }
}
