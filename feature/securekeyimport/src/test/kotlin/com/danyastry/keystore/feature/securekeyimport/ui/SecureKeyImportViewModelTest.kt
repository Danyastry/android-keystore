package com.danyastry.keystore.feature.securekeyimport.ui

import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.core.keystore.KeyKind
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.keystore.KeyOrigin
import com.danyastry.keystore.core.keystore.KeyPurpose
import com.danyastry.keystore.feature.securekeyimport.SecureImportUnavailableException
import com.danyastry.keystore.feature.securekeyimport.SecureImporter
import com.danyastry.keystore.feature.securekeyimport.SoftwareUnwrapper
import com.google.common.truth.Truth.assertThat
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStoreException
import java.security.PublicKey
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SecureKeyImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val importer = SoftwareImporter()
    private lateinit var viewModel: SecureKeyImportViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SecureKeyImportViewModel(importer = importer, dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts clean and clears old keys`() = test {
        assertThat(importer.deleteAllCalls).isEqualTo(1)
        assertThat(state.hasWrappingKey).isFalse()
        assertThat(state.canImport).isFalse()
    }

    @Test
    fun `creating the wrapping key shows its public key`() = test {
        createWrappingKey()

        assertThat(state.wrappingPublicKey).matches("([0-9a-f]{2}:){7}[0-9a-f]{2}")
    }

    @Test
    fun `a refused wrapping key is reported`() = test {
        importer.failWrappingKey = true

        createWrappingKey()

        assertThat(state.error).isEqualTo(SecureImportError.WRAPPING_KEY_FAILED)
        assertThat(state.hasWrappingKey).isFalse()
    }

    @Test
    fun `the server builds a wrapper with the spec field sizes`() = test {
        createWrappingKey()

        wrap()

        val wrapped = state.wrapped!!
        assertThat(wrapped.transportKeyBytes).isEqualTo(256)
        assertThat(wrapped.ivBytes).isEqualTo(12)
        assertThat(wrapped.encryptedKeyBytes).isEqualTo(32)
        assertThat(wrapped.tagBytes).isEqualTo(16)
        assertThat(wrapped.totalBytes).isGreaterThan(256 + 12 + 32 + 16)
        assertThat(state.serverKey).isNotEmpty()
        assertThat(state.canImport).isTrue()
    }

    @Test
    fun `the device imports the key the server wrapped`() = test {
        createWrappingKey()
        wrap()

        import()

        assertThat(state.error).isNull()
        assertThat(state.imported?.origin).isEqualTo(KeyOrigin.SECURELY_IMPORTED)
        assertThat(state.imported?.sizeBits).isEqualTo(256)
    }

    @Test
    fun `the device reads what the server encrypted`() = test {
        createWrappingKey()
        wrap()
        import()
        viewModel.onInputChange("Attack at dawn")

        viewModel.onServerSend()
        advanceUntilIdle()
        viewModel.onDeviceDecrypt()
        advanceUntilIdle()

        assertThat(state.serverMessage).isNotEmpty()
        assertThat(state.deviceRead).isEqualTo("Attack at dawn")
    }

    @Test
    fun `a flipped bit makes the import fail`() = test {
        createWrappingKey()
        wrap()

        viewModel.onTamper()
        import()

        assertThat(state.wrapped?.isTampered).isTrue()
        assertThat(state.canTamper).isFalse()
        assertThat(state.error).isEqualTo(SecureImportError.IMPORT_REFUSED)
        assertThat(state.imported).isNull()
    }

    @Test
    fun `a wrapper for the old wrapping key is refused after a new one is made`() = test {
        createWrappingKey()
        wrap()
        importer.replaceWrappingKey()

        import()

        assertThat(state.error).isEqualTo(SecureImportError.IMPORT_REFUSED)
    }

    @Test
    fun `a keystore without secure import support is reported as unsupported`() = test {
        createWrappingKey()
        wrap()
        importer.isSecureImportSupported = false

        import()

        assertThat(state.error).isEqualTo(SecureImportError.UNSUPPORTED)
        assertThat(state.imported).isNull()
    }

    @Test
    fun `start over clears everything but the message text`() = test {
        createWrappingKey()
        wrap()
        import()
        viewModel.onInputChange("Hello")

        viewModel.onReset()
        advanceUntilIdle()

        assertThat(state).isEqualTo(SecureKeyImportUiState(input = "Hello"))
        assertThat(importer.wrappingKey()).isNull()
    }

    private fun TestScope.createWrappingKey() {
        viewModel.onCreateWrappingKey()
        advanceUntilIdle()
    }

    private fun TestScope.wrap() {
        viewModel.onWrapOnServer()
        advanceUntilIdle()
    }

    private fun TestScope.import() {
        viewModel.onImport()
        advanceUntilIdle()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class SoftwareImporter : SecureImporter {
        private var wrapping: KeyPair? = null
        private var imported: SecretKey? = null
        var failWrappingKey = false
        var isSecureImportSupported = true
        var deleteAllCalls = 0

        fun replaceWrappingKey() {
            wrapping = newPair()
        }

        override fun createWrappingKey(): PublicKey {
            if (failWrappingKey) throw KeyStoreException("Keystore refused")
            return newPair().also { wrapping = it }.public
        }

        override fun wrappingKey(): PublicKey? = wrapping?.public

        override fun importWrapped(wrappedKey: ByteArray) {
            imported = null
            if (!isSecureImportSupported) throw SecureImportUnavailableException(KeyStoreException("Secure Key Import not available"))
            val pair = wrapping ?: throw KeyStoreException("No wrapping key")
            imported = SecretKeySpec(SoftwareUnwrapper.unwrap(wrappedKey, pair.private), "AES")
        }

        override fun importedKey(): SecretKey? = imported

        override fun importedDetails(): KeyDetails? = imported?.let {
            KeyDetails(
                alias = "secure-import-demo",
                algorithm = "AES",
                sizeBits = it.encoded.size * Byte.SIZE_BITS,
                kind = KeyKind.SECRET,
                location = KeyLocation.TRUSTED_ENVIRONMENT,
                purposes = setOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT),
                origin = KeyOrigin.SECURELY_IMPORTED,
                requiresUserAuthentication = false,
            )
        }

        override fun deleteImported() {
            imported = null
        }

        override fun deleteAll() {
            deleteAllCalls++
            imported = null
            wrapping = null
        }

        private fun newPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    }
}
