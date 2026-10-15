package com.danyastry.keystore.feature.securekeyimport.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.feature.securekeyimport.AndroidSecureImporter
import com.danyastry.keystore.feature.securekeyimport.SecureImportUnavailableException
import com.danyastry.keystore.feature.securekeyimport.SecureImporter
import com.danyastry.keystore.feature.securekeyimport.SecureKeyWrapper
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.ProviderException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.P)
class SecureKeyImportViewModel(
    private val importer: SecureImporter = AndroidSecureImporter(),
    private val random: SecureRandom = SecureRandom(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(SecureKeyImportUiState())
    val state: StateFlow<SecureKeyImportUiState> = _state.asStateFlow()

    private var serverSecret: ByteArray? = null
    private var wrappedBytes: ByteArray? = null

    init {
        viewModelScope.launch {
            withContext(dispatcher) { importer.deleteAll() }
        }
    }

    fun onCreateWrappingKey() {
        viewModelScope.launch {
            val publicKey = withContext(dispatcher) {
                try {
                    importer.createWrappingKey()
                } catch (_: GeneralSecurityException) {
                    null
                } catch (_: ProviderException) {
                    null
                }
            }
            _state.update {
                if (publicKey == null) {
                    it.copy(error = SecureImportError.WRAPPING_KEY_FAILED)
                } else {
                    it.copy(wrappingPublicKey = fingerprint(publicKey.encoded), error = null)
                }
            }
        }
    }

    fun onWrapOnServer() {
        viewModelScope.launch {
            val (secret, wrapped) = withContext(dispatcher) {
                val publicKey = importer.wrappingKey() ?: return@withContext null
                val secret = ByteArray(SERVER_KEY_BYTES).also(random::nextBytes)
                secret to SecureKeyWrapper.wrap(secret, publicKey, random)
            } ?: return@launch
            serverSecret = secret
            wrappedBytes = wrapped.bytes
            _state.update {
                it.copy(
                    serverKey = fingerprint(secret),
                    wrapped = WrappedSummary(
                        transportKeyBytes = wrapped.encryptedTransportKey.size,
                        ivBytes = wrapped.iv.size,
                        descriptionBytes = wrapped.keyDescription.size,
                        encryptedKeyBytes = wrapped.encryptedKey.size,
                        tagBytes = wrapped.tag.size,
                        totalBytes = wrapped.bytes.size,
                    ),
                    error = null,
                )
            }
        }
    }

    fun onTamper() {
        val bytes = wrappedBytes ?: return
        bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 1).toByte()
        _state.update { it.copy(wrapped = it.wrapped?.copy(isTampered = true)) }
    }

    fun onImport() {
        val bytes = wrappedBytes ?: return
        viewModelScope.launch {
            val error = withContext(dispatcher) {
                try {
                    importer.importWrapped(bytes.copyOf())
                    null
                } catch (_: SecureImportUnavailableException) {
                    SecureImportError.UNSUPPORTED
                } catch (_: GeneralSecurityException) {
                    SecureImportError.IMPORT_REFUSED
                } catch (_: ProviderException) {
                    SecureImportError.IMPORT_REFUSED
                }
            }
            val (details, isHidden) = withContext(dispatcher) { importer.importedDetails() to (importer.importedKey()?.encoded == null) }
            _state.update {
                it.copy(
                    imported = details,
                    isImportedMaterialHidden = details != null && isHidden,
                    serverMessage = "",
                    deviceRead = "",
                    error = error,
                )
            }
        }
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input) }
    }

    fun onServerSend() {
        val secret = serverSecret ?: return
        val plaintext = _state.value.input.encodeToByteArray()
        viewModelScope.launch {
            val message = withContext(dispatcher) {
                val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(secret, "AES")) }
                EncryptedMessage(cipher.iv, cipher.doFinal(plaintext)).toBase64()
            }
            _state.update { it.copy(serverMessage = message, deviceRead = "", error = null) }
        }
    }

    fun onDeviceDecrypt() {
        val message = EncryptedMessage.fromBase64OrNull(_state.value.serverMessage) ?: return
        viewModelScope.launch {
            val plaintext = withContext(dispatcher) {
                val key = importer.importedKey() ?: return@withContext null
                try {
                    Cipher.getInstance(TRANSFORMATION).run {
                        init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(EncryptedMessage.TAG_SIZE * Byte.SIZE_BITS, message.iv))
                        doFinal(message.ciphertext).decodeToString()
                    }
                } catch (_: GeneralSecurityException) {
                    null
                }
            }
            _state.update {
                if (plaintext == null) it.copy(deviceRead = "", error = SecureImportError.CANNOT_DECRYPT) else it.copy(deviceRead = plaintext, error = null)
            }
        }
    }

    fun onReset() {
        serverSecret?.fill(0)
        serverSecret = null
        wrappedBytes = null
        viewModelScope.launch {
            withContext(dispatcher) { importer.deleteAll() }
            _state.update { SecureKeyImportUiState(input = it.input) }
        }
    }

    private fun fingerprint(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).take(FINGERPRINT_BYTES).joinToString(":") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    companion object {
        const val SERVER_KEY_BYTES = 32
        private const val FINGERPRINT_BYTES = 8
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
