package com.danyastry.keystore.feature.biometric.ui

import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.core.keystore.SecretKeyStore
import com.danyastry.keystore.feature.biometric.AesGcmBiometricCrypto
import com.danyastry.keystore.feature.biometric.BiometricAvailability
import com.danyastry.keystore.feature.biometric.BiometricCrypto
import com.danyastry.keystore.feature.biometric.KeyInvalidatedException
import com.google.common.truth.Truth.assertThat
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.util.Collections
import java.util.IdentityHashMap
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val keys = InMemorySecretKeys()
    private val crypto = GatedCrypto()
    private var availability = BiometricAvailability.AVAILABLE
    private lateinit var viewModel: BiometricViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = BiometricViewModel(status = { availability }, keys = keys, crypto = crypto, dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts without a key and reports availability`() = test {
        assertThat(state.availability).isEqualTo(BiometricAvailability.AVAILABLE)
        assertThat(state.hasKey).isFalse()
        assertThat(state.canCreateKey).isTrue()
    }

    @Test
    fun `cannot create a key without an enrolled fingerprint`() = test {
        availability = BiometricAvailability.NONE_ENROLLED
        viewModel.onRefresh()
        advanceUntilIdle()

        assertThat(state.canCreateKey).isFalse()
    }

    @Test
    fun `refresh picks up a fingerprint enrolled in settings`() = test {
        availability = BiometricAvailability.NONE_ENROLLED
        viewModel.onRefresh()
        advanceUntilIdle()

        availability = BiometricAvailability.AVAILABLE
        viewModel.onRefresh()
        advanceUntilIdle()

        assertThat(state.canCreateKey).isTrue()
    }

    @Test
    fun `a refused key creation is reported`() = test {
        keys.failCreation = true

        viewModel.onCreateKey()
        advanceUntilIdle()

        assertThat(state.hasKey).isFalse()
        assertThat(state.error).isEqualTo(BiometricError.KEY_CREATION_FAILED)
    }

    @Test
    fun `encrypting asks for a fingerprint with a cipher`() = test {
        createKeyAndType("Attack at dawn")

        viewModel.onEncrypt()
        val request = viewModel.authRequests.first()

        assertThat(request.operation).isEqualTo(BiometricOperation.ENCRYPT)
        assertThat(request.payload.decodeToString()).isEqualTo("Attack at dawn")
        assertThat(state.isAwaitingAuth).isTrue()
        assertThat(state.canEncrypt).isFalse()
    }

    @Test
    fun `encrypts after the fingerprint is accepted`() = test {
        createKeyAndType("Attack at dawn")

        authenticate { viewModel.onEncrypt() }

        assertThat(state.isAwaitingAuth).isFalse()
        assertThat(state.error).isNull()
        assertThat(EncryptedMessage.fromBase64OrNull(state.sealed)).isNotNull()
    }

    @Test
    fun `decrypts back after a second fingerprint`() = test {
        createKeyAndType("Attack at dawn")
        authenticate { viewModel.onEncrypt() }

        val request = authenticate { viewModel.onDecrypt() }

        assertThat(request.operation).isEqualTo(BiometricOperation.DECRYPT)
        assertThat(state.decrypted).isEqualTo("Attack at dawn")
    }

    @Test
    fun `the keystore refuses to encrypt without a fingerprint`() = test {
        createKeyAndType("Attack at dawn")

        viewModel.onEncryptWithoutAuth()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(BiometricError.KEY_REFUSED)
        assertThat(state.sealed).isEmpty()
    }

    @Test
    fun `canceling the prompt is not shown as a failure`() = test {
        createKeyAndType("Attack at dawn")
        viewModel.onEncrypt()
        viewModel.authRequests.first()

        viewModel.onAuthFailed(canceled = true, message = "Fingerprint operation canceled by user.")

        assertThat(state.isAwaitingAuth).isFalse()
        assertThat(state.error).isEqualTo(BiometricError.CANCELED)
        assertThat(state.authMessage).isEmpty()
    }

    @Test
    fun `a prompt error keeps the system message`() = test {
        createKeyAndType("Attack at dawn")
        viewModel.onEncrypt()
        viewModel.authRequests.first()

        viewModel.onAuthFailed(canceled = false, message = "Too many attempts.")

        assertThat(state.error).isEqualTo(BiometricError.AUTH_FAILED)
        assertThat(state.authMessage).isEqualTo("Too many attempts.")
    }

    @Test
    fun `a new fingerprint invalidates the key`() = test {
        createKeyAndType("Attack at dawn")
        crypto.invalidated = true

        viewModel.onEncrypt()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(BiometricError.KEY_INVALIDATED)
        assertThat(state.isAwaitingAuth).isFalse()
    }

    @Test
    fun `deleting the key clears the old ciphertext`() = test {
        createKeyAndType("Attack at dawn")
        authenticate { viewModel.onEncrypt() }

        viewModel.onDeleteKey()
        advanceUntilIdle()

        assertThat(state.hasKey).isFalse()
        assertThat(state.sealed).isEmpty()
        assertThat(state.canDecrypt).isFalse()
    }

    @Test
    fun `encrypting without a key asks for one`() = test {
        viewModel.onInputChange("Hello")

        viewModel.onEncrypt()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(BiometricError.NO_KEY)
    }

    private suspend fun TestScope.createKeyAndType(text: String) {
        viewModel.onCreateKey()
        viewModel.onInputChange(text)
        advanceUntilIdle()
    }

    private suspend fun TestScope.authenticate(start: () -> Unit): AuthRequest {
        start()
        val request = viewModel.authRequests.first()
        crypto.authenticate(request.cipher)
        viewModel.onAuthenticated(request)
        advanceUntilIdle()
        return request
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class GatedCrypto : BiometricCrypto {
        private val real = AesGcmBiometricCrypto()
        private val authenticated: MutableSet<Cipher> = Collections.newSetFromMap(IdentityHashMap())
        var invalidated = false

        fun authenticate(cipher: Cipher) {
            authenticated += cipher
        }

        override fun encryptionCipher(key: SecretKey): Cipher = check { real.encryptionCipher(key) }

        override fun decryptionCipher(key: SecretKey, iv: ByteArray): Cipher = check { real.decryptionCipher(key, iv) }

        override fun seal(cipher: Cipher, plaintext: ByteArray): EncryptedMessage = use(cipher) { real.seal(cipher, plaintext) }

        override fun open(cipher: Cipher, ciphertext: ByteArray): ByteArray = use(cipher) { real.open(cipher, ciphertext) }

        private fun check(create: () -> Cipher): Cipher {
            if (invalidated) throw KeyInvalidatedException(InvalidKeyException("Key permanently invalidated"))
            return create()
        }

        private fun <T> use(cipher: Cipher, operation: () -> T): T {
            if (!authenticated.remove(cipher)) throw IllegalBlockSizeException("Key user not authenticated")
            return operation()
        }
    }

    private class InMemorySecretKeys : SecretKeyStore {
        private val keys = mutableMapOf<String, SecretKey>()
        var failCreation = false

        override fun get(alias: String): SecretKey? = keys[alias]

        override fun create(alias: String): SecretKey {
            if (failCreation) throw InvalidAlgorithmParameterException("At least one biometric must be enrolled")
            return KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also { keys[alias] = it }
        }

        override fun delete(alias: String) {
            keys.remove(alias)
        }
    }
}
