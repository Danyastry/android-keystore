package com.danyastry.keystore.feature.ecdh.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.core.keystore.KeyPairStore
import com.danyastry.keystore.feature.ecdh.Agreement
import com.danyastry.keystore.feature.ecdh.EcdhKeys
import com.danyastry.keystore.feature.ecdh.Fingerprint
import com.danyastry.keystore.feature.ecdh.KeystoreAgreement
import com.danyastry.keystore.feature.ecdh.SessionCipher
import com.danyastry.keystore.feature.ecdh.SessionKeys
import com.danyastry.keystore.feature.ecdh.SoftwareAgreement
import java.security.GeneralSecurityException
import java.security.KeyPair
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

@RequiresApi(Build.VERSION_CODES.S)
class EcdhViewModel(
    private val deviceKeys: KeyPairStore = EcdhKeys.store(),
    private val deviceAgreement: Agreement = KeystoreAgreement(),
    private val peerAgreement: Agreement = SoftwareAgreement(),
    private val newPeer: () -> KeyPair = EcdhKeys::softwareKeyPair,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(EcdhUiState())
    val state: StateFlow<EcdhUiState> = _state.asStateFlow()

    private var peer: KeyPair? = null
    private var deviceSession: SecretKey? = null
    private var peerSession: SecretKey? = null

    init {
        viewModelScope.launch {
            refreshDeviceKey()
            replacePeer()
        }
    }

    fun onCreateDeviceKey() {
        viewModelScope.launch {
            val failed = withContext(dispatcher) {
                try {
                    deviceKeys.create(DEVICE_ALIAS)
                    false
                } catch (_: GeneralSecurityException) {
                    true
                } catch (_: ProviderException) {
                    true
                }
            }
            resetAgreement()
            refreshDeviceKey()
            _state.update { it.copy(error = if (failed) EcdhError.KEY_CREATION_FAILED else null) }
        }
    }

    fun onDeleteDeviceKey() {
        viewModelScope.launch {
            withContext(dispatcher) { deviceKeys.delete(DEVICE_ALIAS) }
            resetAgreement()
            _state.update { it.copy(sealed = "", peerDecrypted = "", error = null) }
            refreshDeviceKey()
        }
    }

    fun onNewPeer() {
        viewModelScope.launch { replacePeer() }
    }

    fun onAgree() {
        val currentPeer = peer ?: return
        viewModelScope.launch {
            val device = withContext(dispatcher) { deviceKeys.get(DEVICE_ALIAS) }
            if (device == null) {
                _state.update { it.copy(error = EcdhError.NO_DEVICE_KEY) }
                return@launch
            }
            val (deviceSecret, peerSecret) = withContext(dispatcher) {
                deviceAgreement.sharedSecret(device.private, currentPeer.public) to
                    peerAgreement.sharedSecret(currentPeer.private, device.public)
            }
            val deviceKey = SessionKeys.derive(deviceSecret)
            deviceSession = deviceKey
            peerSession = SessionKeys.derive(peerSecret)
            _state.update {
                it.copy(
                    deviceSecret = Fingerprint.of(deviceSecret),
                    peerSecret = Fingerprint.of(peerSecret),
                    sessionKey = Fingerprint.of(deviceKey.encoded),
                    peerDecrypted = "",
                    error = null,
                )
            }
        }
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input) }
    }

    fun onEncryptOnDevice() {
        val key = deviceSession
        if (key == null) {
            _state.update { it.copy(error = EcdhError.NOT_AGREED) }
            return
        }
        val plaintext = _state.value.input.encodeToByteArray()
        viewModelScope.launch {
            val sealed = withContext(dispatcher) { SessionCipher.encrypt(key, plaintext).toBase64() }
            _state.update { it.copy(sealed = sealed, peerDecrypted = "", error = null) }
        }
    }

    fun onDecryptOnPeer() {
        val key = peerSession
        val message = EncryptedMessage.fromBase64OrNull(_state.value.sealed)
        if (key == null || message == null) {
            _state.update { it.copy(error = if (key == null) EcdhError.NOT_AGREED else EcdhError.INVALID_MESSAGE) }
            return
        }
        viewModelScope.launch {
            val plaintext = withContext(dispatcher) {
                try {
                    SessionCipher.decrypt(key, message).decodeToString()
                } catch (_: GeneralSecurityException) {
                    null
                }
            }
            _state.update {
                if (plaintext == null) {
                    it.copy(peerDecrypted = "", error = EcdhError.PEER_CANNOT_DECRYPT)
                } else {
                    it.copy(peerDecrypted = plaintext, error = null)
                }
            }
        }
    }

    private suspend fun replacePeer() {
        val pair = withContext(dispatcher) { newPeer() }
        peer = pair
        resetAgreement()
        _state.update { it.copy(peerPublicKey = Fingerprint.of(pair.public.encoded), peerDecrypted = "", error = null) }
    }

    private fun resetAgreement() {
        deviceSession = null
        peerSession = null
        _state.update { it.copy(deviceSecret = "", peerSecret = "", sessionKey = "") }
    }

    private suspend fun refreshDeviceKey() {
        val device = withContext(dispatcher) { deviceKeys.get(DEVICE_ALIAS) }
        _state.update {
            it.copy(hasDeviceKey = device != null, devicePublicKey = device?.public?.encoded?.let(Fingerprint::of).orEmpty())
        }
    }

    companion object {
        const val DEVICE_ALIAS = "ecdh-demo"
    }
}
