package com.danyastry.keystore.feature.attestation

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.danyastry.keystore.core.keystore.KeystoreEntries
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec

data class AttestationReport(
    val chainSubjects: List<String>,
    val isChainSigned: Boolean,
    val attestationVersion: Int,
    val attestationLevel: AttestationLevel,
    val keymasterVersion: Int,
    val keymasterLevel: AttestationLevel,
    val isChallengeMatching: Boolean,
)

fun interface Attestor {
    fun attest(): AttestationReport
}

class AndroidAttestor(
    private val entries: KeystoreEntries = KeystoreEntries(),
    private val random: SecureRandom = SecureRandom(),
) : Attestor {

    override fun attest(): AttestationReport {
        val challenge = ByteArray(CHALLENGE_SIZE).also(random::nextBytes)
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KeystoreEntries.ANDROID_KEY_STORE).apply {
            initialize(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAttestationChallenge(challenge)
                    .build(),
            )
        }.generateKeyPair()
        return report(entries.certificateChain(ALIAS), challenge)
    }

    companion object {
        const val ALIAS = "attestation-demo"
        const val CHALLENGE_SIZE = 32

        fun report(chain: List<X509Certificate>, challenge: ByteArray): AttestationReport {
            val extension = requireNotNull(chain.first().getExtensionValue(KeyDescription.OID)) {
                "The leaf certificate has no attestation extension"
            }
            val description = KeyDescription.parse(extension)
            return AttestationReport(
                chainSubjects = chain.map { it.subjectX500Principal.name },
                isChainSigned = isSignedChain(chain),
                attestationVersion = description.attestationVersion,
                attestationLevel = description.attestationLevel,
                keymasterVersion = description.keymasterVersion,
                keymasterLevel = description.keymasterLevel,
                isChallengeMatching = description.challenge.contentEquals(challenge),
            )
        }

        fun isSignedChain(chain: List<X509Certificate>): Boolean = chain.indices.all { index ->
            val issuer = chain.getOrNull(index + 1) ?: chain[index]
            try {
                chain[index].verify(issuer.publicKey)
                true
            } catch (_: GeneralSecurityException) {
                false
            }
        }
    }
}
