package com.danyastry.keystore.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.keystore.core.ui.FeatureCategory
import com.danyastry.keystore.core.ui.FeatureEntry
import com.danyastry.keystore.core.ui.FeatureInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class FeatureCatalogTest {

    @Test
    fun `empty catalog has no sections`() {
        val catalog = FeatureCatalog(emptyList(), deviceSdk = 35)

        assertThat(catalog.isEmpty).isTrue()
        assertThat(catalog.sections).isEmpty()
    }

    @Test
    fun `groups features into sections ordered by category`() {
        val aes = feature("aes-gcm", "AES-GCM", FeatureCategory.ENCRYPTION)
        val ecdsa = feature("ecdsa", "ECDSA", FeatureCategory.SIGNING)
        val strongBox = feature("strongbox", "StrongBox", FeatureCategory.HARDWARE)

        val catalog = FeatureCatalog(listOf(strongBox, ecdsa, aes), deviceSdk = 35)

        assertThat(catalog.sections.map { it.category }).containsExactly(
            FeatureCategory.ENCRYPTION,
            FeatureCategory.SIGNING,
            FeatureCategory.HARDWARE,
        ).inOrder()
    }

    @Test
    fun `sorts features inside a section by name`() {
        val rsa = feature("rsa-oaep", "RSA-OAEP", FeatureCategory.ENCRYPTION)
        val aes = feature("aes-gcm", "AES-GCM", FeatureCategory.ENCRYPTION)

        val catalog = FeatureCatalog(listOf(rsa, aes), deviceSdk = 35)

        assertThat(catalog.sections.single().entries).containsExactly(aes, rsa).inOrder()
    }

    @Test
    fun `features newer than the device are listed but not supported`() {
        val ecdh = feature("ecdh", "ECDH", FeatureCategory.ENCRYPTION, minSdk = 31)

        val catalog = FeatureCatalog(listOf(ecdh), deviceSdk = 30)

        assertThat(catalog.sections.single().entries).containsExactly(ecdh)
        assertThat(catalog.isSupported(ecdh)).isFalse()
        assertThat(catalog.find("ecdh")).isNull()
    }

    @Test
    fun `finds supported features by id`() {
        val ecdh = feature("ecdh", "ECDH", FeatureCategory.ENCRYPTION, minSdk = 31)

        val catalog = FeatureCatalog(listOf(ecdh), deviceSdk = 31)

        assertThat(catalog.isSupported(ecdh)).isTrue()
        assertThat(catalog.find("ecdh")).isSameInstanceAs(ecdh)
        assertThat(catalog.find("unknown")).isNull()
    }

    @Test
    fun `rejects duplicate ids`() {
        val first = feature("aes-gcm", "AES-GCM", FeatureCategory.ENCRYPTION)
        val second = feature("aes-gcm", "AES", FeatureCategory.ENCRYPTION)

        val error = assertThrows(IllegalArgumentException::class.java) { FeatureCatalog(listOf(first, second), deviceSdk = 35) }
        assertThat(error).hasMessageThat().contains("aes-gcm")
    }

    @Test
    fun `registry has unique ids`() {
        FeatureCatalog(FeatureRegistry.features, deviceSdk = 35)
    }

    private fun feature(id: String, name: String, category: FeatureCategory, minSdk: Int = 26): FeatureEntry =
        object : FeatureEntry {
            override val info = FeatureInfo(id = id, name = name, category = category, summary = "$name summary", minSdk = minSdk)

            @Composable
            override fun Content(modifier: Modifier) = Unit
        }
}
