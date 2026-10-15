package com.danyastry.keystore.feature.securekeyimport

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.danyastry.keystore.core.keystore.KeyOrigin
import com.danyastry.keystore.core.keystore.KeyPurpose
import com.danyastry.keystore.core.keystore.KeystoreEntries
import com.google.common.truth.Truth.assertThat
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
class AndroidSecureImporterTest {

    private val importer = AndroidSecureImporter()
    private val secret = ByteArray(32).also(SecureRandom()::nextBytes)

    @After
    fun tearDown() {
        importer.deleteAll()
    }

    @Test
    fun importsAWrappedKeyThatCannotBeReadBack() {
        val wrapped = SecureKeyWrapper.wrap(secret, importer.createWrappingKey())

        importer.importWrapped(wrapped.bytes)

        val details = importer.importedDetails()!!
        assertThat(details.origin).isAnyOf(KeyOrigin.SECURELY_IMPORTED, KeyOrigin.IMPORTED)
        assertThat(details.sizeBits).isEqualTo(256)
        assertThat(details.purposes).containsExactly(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT)
        assertThat(importer.importedKey()!!.encoded).isNull()
    }

    @Test
    fun importedKeyDecryptsWhatTheServerEncrypted() {
        importer.importWrapped(SecureKeyWrapper.wrap(secret, importer.createWrappingKey()).bytes)
        val server = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(secret, "AES")) }
        val ciphertext = server.doFinal("Attack at dawn".encodeToByteArray())

        val device = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, importer.importedKey(), GCMParameterSpec(128, server.iv))
        }

        assertThat(device.doFinal(ciphertext).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun wrappingKeyIsForWrappingOnly() {
        importer.createWrappingKey()

        val details = KeystoreEntries().details(AndroidSecureImporter.WRAPPING_ALIAS)!!

        assertThat(details.purposes).containsExactly(KeyPurpose.WRAP_KEY)
    }

    @Test
    fun keystoreRefusesATamperedBlob() {
        val bytes = SecureKeyWrapper.wrap(secret, importer.createWrappingKey()).bytes
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()

        assertThrows(GeneralSecurityException::class.java) { importer.importWrapped(bytes) }
        assertThat(importer.importedKey()).isNull()
    }

    @Test
    fun keystoreRefusesABlobForAnotherWrappingKey() {
        val otherDevice = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        importer.createWrappingKey()

        assertThrows(GeneralSecurityException::class.java) { importer.importWrapped(SecureKeyWrapper.wrap(secret, otherDevice.public).bytes) }
    }
}
