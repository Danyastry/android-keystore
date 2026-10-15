package com.danyastry.keystore.feature.keyimport

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyastry.keystore.core.keystore.KeyKind
import com.danyastry.keystore.core.keystore.KeyOrigin
import com.danyastry.keystore.core.keystore.KeyPurpose
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeyImporterTest {

    private val importer = AndroidKeyImporter()
    private val keyBytes = KeyHex.parseOrNull("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4")!!

    @After
    fun tearDown() {
        importer.delete(ALIAS)
    }

    @Test
    fun importedKeyIsMarkedAsImported() {
        importer.import(ALIAS, keyBytes)

        val details = importer.details(ALIAS)!!
        assertThat(details.origin).isEqualTo(KeyOrigin.IMPORTED)
        assertThat(details.kind).isEqualTo(KeyKind.SECRET)
        assertThat(details.sizeBits).isEqualTo(256)
        assertThat(details.purposes).containsExactly(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT)
    }

    @Test
    fun importedKeyMaterialCannotBeReadBack() {
        importer.import(ALIAS, keyBytes)

        assertThat(importer.get(ALIAS)!!.encoded).isNull()
    }

    @Test
    fun originalBytesDecryptWhatTheKeystoreKeyEncrypted() {
        importer.import(ALIAS, keyBytes)

        val message = ImportCipher.encrypt(importer.get(ALIAS)!!, "Attack at dawn".encodeToByteArray())

        assertThat(ImportCipher.decrypt(ImportCipher.softwareKey(keyBytes), message).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun keystoreKeyDecryptsWhatTheOriginalEncrypted() {
        importer.import(ALIAS, keyBytes)

        val message = ImportCipher.encrypt(ImportCipher.softwareKey(keyBytes), "Attack at dawn".encodeToByteArray())

        assertThat(ImportCipher.decrypt(importer.get(ALIAS)!!, message).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun deleteRemovesTheImportedKey() {
        importer.import(ALIAS, keyBytes)

        importer.delete(ALIAS)

        assertThat(importer.get(ALIAS)).isNull()
        assertThat(importer.details(ALIAS)).isNull()
    }

    private companion object {
        const val ALIAS = "key-import-test"
    }
}
