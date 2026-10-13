package com.danyastry.keystore.feature.attestation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyastry.keystore.core.keystore.KeystoreEntries
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAttestorTest {

    private val entries = KeystoreEntries()
    private val attestor = AndroidAttestor(entries)

    @After
    fun tearDown() {
        entries.delete(AndroidAttestor.ALIAS)
    }

    @Test
    fun producesASignedChainWithOurChallenge() {
        val report = attestor.attest()

        assertThat(report.chainSubjects.size).isAtLeast(2)
        assertThat(report.isChainSigned).isTrue()
        assertThat(report.isChallengeMatching).isTrue()
        assertThat(report.attestationLevel).isNotEqualTo(AttestationLevel.UNKNOWN)
        assertThat(report.attestationVersion).isGreaterThan(0)
    }

    @Test
    fun anotherChallengeDoesNotMatch() {
        attestor.attest()
        val chain = entries.certificateChain(AndroidAttestor.ALIAS)

        val report = AndroidAttestor.report(chain, ByteArray(AndroidAttestor.CHALLENGE_SIZE))

        assertThat(report.isChallengeMatching).isFalse()
    }

    @Test
    fun aChainInTheWrongOrderIsNotSigned() {
        attestor.attest()
        val chain = entries.certificateChain(AndroidAttestor.ALIAS)

        assertThat(AndroidAttestor.isSignedChain(chain)).isTrue()
        assertThat(AndroidAttestor.isSignedChain(chain.reversed())).isFalse()
    }
}
