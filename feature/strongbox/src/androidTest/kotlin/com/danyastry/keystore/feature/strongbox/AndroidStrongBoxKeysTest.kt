package com.danyastry.keystore.feature.strongbox

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.keystore.KeystoreEntries
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidStrongBoxKeysTest {

    private val entries = KeystoreEntries()
    private val keys = AndroidStrongBoxKeys(InstrumentationRegistry.getInstrumentation().targetContext, entries)

    @After
    fun tearDown() {
        keys.deleteAll()
    }

    @Test
    fun regularKeyIsAlwaysCreated() {
        val creation = keys.create(KeyTarget.REGULAR)

        assertThat(creation).isInstanceOf(KeyCreation.Created::class.java)
        assertThat((creation as KeyCreation.Created).location).isNotEqualTo(KeyLocation.STRONGBOX)
    }

    @Test
    fun strongBoxKeyLandsInStrongBoxWhenTheChipExists() {
        assumeTrue(keys.isStrongBoxDeclared())

        assertThat(keys.create(KeyTarget.STRONGBOX)).isEqualTo(KeyCreation.Created(KeyLocation.STRONGBOX))
    }

    @Test
    fun strongBoxKeyIsRefusedWithoutTheChip() {
        assumeFalse(keys.isStrongBoxDeclared())

        assertThat(keys.create(KeyTarget.STRONGBOX)).isEqualTo(KeyCreation.Unavailable)
        assertThat(entries.contains(AndroidStrongBoxKeys.aliasOf(KeyTarget.STRONGBOX))).isFalse()
    }

    @Test
    fun measuresAnExistingKeyAndSkipsAMissingOne() {
        keys.create(KeyTarget.REGULAR)

        assertThat(keys.measure(KeyTarget.REGULAR, rounds = 3)).isNotNull()
        assertThat(keys.measure(KeyTarget.STRONGBOX, rounds = 3)).isNull()
    }

    @Test
    fun deleteAllRemovesBothAliases() {
        keys.create(KeyTarget.REGULAR)
        keys.create(KeyTarget.STRONGBOX)

        keys.deleteAll()

        KeyTarget.entries.forEach { assertThat(entries.contains(AndroidStrongBoxKeys.aliasOf(it))).isFalse() }
    }
}
