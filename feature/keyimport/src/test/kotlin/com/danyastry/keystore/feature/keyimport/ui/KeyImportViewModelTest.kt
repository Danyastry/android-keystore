package com.danyastry.keystore.feature.keyimport.ui

import com.danyastry.keystore.core.keystore.EncryptedMessage
import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.core.keystore.KeyKind
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.keystore.KeyOrigin
import com.danyastry.keystore.core.keystore.KeyPurpose
import com.danyastry.keystore.feature.keyimport.ImportCipher
import com.danyastry.keystore.feature.keyimport.KeyImporter
import com.google.common.truth.Truth.assertThat
import java.security.KeyStoreException
import javax.crypto.SecretKey
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
class KeyImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val importer = InMemoryImporter()
    private lateinit var viewModel: KeyImportViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = KeyImportViewModel(importer = importer, newHex = { HEX }, dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with nothing to import`() = test {
        assertThat(state.imported).isNull()
        assertThat(state.canImport).isFalse()
    }

    @Test
    fun `generate fills in a valid key`() = test {
        viewModel.onGenerate()

        assertThat(state.originalHex).isEqualTo(HEX)
        assertThat(state.canImport).isTrue()
    }

    @Test
    fun `invalid hex is flagged and cannot be imported`() = test {
        viewModel.onHexChange("1234")

        assertThat(state.isOriginalInvalid).isTrue()
        assertThat(state.canImport).isFalse()
    }

    @Test
    fun `importing shows the key as imported`() = test {
        importKey()

        assertThat(importer.importedBytes).isEqualTo(HEX_BYTES)
        assertThat(state.imported?.origin).isEqualTo(KeyOrigin.IMPORTED)
        assertThat(state.imported?.sizeBits).isEqualTo(256)
        assertThat(state.canImport).isFalse()
    }

    @Test
    fun `key material of the fake key is visible`() = test {
        importKey()

        assertThat(state.isImportedMaterialHidden).isFalse()
    }

    @Test
    fun `the bytes handed to the importer are wiped afterwards`() = test {
        importKey()

        assertThat(importer.handedBytes!!.all { it == 0.toByte() }).isTrue()
    }

    @Test
    fun `a refused import is reported`() = test {
        importer.failImport = true

        importKey()

        assertThat(state.error).isEqualTo(KeyImportError.IMPORT_FAILED)
        assertThat(state.imported).isNull()
    }

    @Test
    fun `the original bytes decrypt what the keystore key encrypted`() = test {
        importKey()
        encrypt("Attack at dawn")

        viewModel.onDecryptWithOriginal()
        advanceUntilIdle()

        assertThat(EncryptedMessage.fromBase64OrNull(state.sealed)).isNotNull()
        assertThat(state.decryptedBy).isEqualTo(Reader.ORIGINAL)
        assertThat(state.decrypted).isEqualTo("Attack at dawn")
    }

    @Test
    fun `the keystore key decrypts its own message`() = test {
        importKey()
        encrypt("Attack at dawn")

        viewModel.onDecryptWithKeystore()
        advanceUntilIdle()

        assertThat(state.decryptedBy).isEqualTo(Reader.KEYSTORE)
        assertThat(state.decrypted).isEqualTo("Attack at dawn")
    }

    @Test
    fun `after forgetting the original only the keystore can decrypt`() = test {
        importKey()
        encrypt("Attack at dawn")

        viewModel.onForgetOriginal()
        viewModel.onDecryptWithOriginal()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(KeyImportError.NO_ORIGINAL)
        assertThat(state.decryptedBy).isNull()
    }

    @Test
    fun `a different original cannot decrypt`() = test {
        importKey()
        encrypt("Attack at dawn")

        viewModel.onHexChange("ff".repeat(32))
        viewModel.onDecryptWithOriginal()
        advanceUntilIdle()

        assertThat(state.error).isEqualTo(KeyImportError.WRONG_KEY)
    }

    @Test
    fun `deleting the imported key clears the message`() = test {
        importKey()
        encrypt("Attack at dawn")

        viewModel.onDeleteImported()
        advanceUntilIdle()

        assertThat(state.imported).isNull()
        assertThat(state.sealed).isEmpty()
        assertThat(state.canEncrypt).isFalse()
    }

    private fun TestScope.importKey() {
        viewModel.onGenerate()
        viewModel.onImport()
        advanceUntilIdle()
    }

    private fun TestScope.encrypt(text: String) {
        viewModel.onInputChange(text)
        viewModel.onEncryptWithKeystore()
        advanceUntilIdle()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }

    private class InMemoryImporter : KeyImporter {
        private var key: SecretKey? = null
        var importedBytes: ByteArray? = null
        var handedBytes: ByteArray? = null
        var failImport = false

        override fun import(alias: String, keyBytes: ByteArray) {
            handedBytes = keyBytes
            if (failImport) throw KeyStoreException("Keystore refused")
            importedBytes = keyBytes.copyOf()
            key = ImportCipher.softwareKey(keyBytes.copyOf())
        }

        override fun get(alias: String): SecretKey? = key

        override fun details(alias: String): KeyDetails? = key?.let {
            KeyDetails(
                alias = alias,
                algorithm = "AES",
                sizeBits = it.encoded.size * Byte.SIZE_BITS,
                kind = KeyKind.SECRET,
                location = KeyLocation.TRUSTED_ENVIRONMENT,
                purposes = setOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT),
                origin = KeyOrigin.IMPORTED,
                requiresUserAuthentication = false,
            )
        }

        override fun delete(alias: String) {
            key = null
        }
    }

    private companion object {
        const val HEX = "603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4"
        val HEX_BYTES = ByteArray(32) { HEX.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }
}
