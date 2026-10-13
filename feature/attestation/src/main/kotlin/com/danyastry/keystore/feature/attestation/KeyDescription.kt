package com.danyastry.keystore.feature.attestation

enum class AttestationLevel { SOFTWARE, TRUSTED_ENVIRONMENT, STRONGBOX, UNKNOWN }

class KeyDescription(
    val attestationVersion: Int,
    val attestationLevel: AttestationLevel,
    val keymasterVersion: Int,
    val keymasterLevel: AttestationLevel,
    val challenge: ByteArray,
) {
    companion object {
        const val OID = "1.3.6.1.4.1.11129.2.1.17"

        fun parse(extensionValue: ByteArray): KeyDescription {
            val wrapped = DerReader(extensionValue).read()
            val sequence = DerReader(wrapped.asBytes()).read()
            require(sequence.tag == DerElement.TAG_SEQUENCE) { "KeyDescription must be a SEQUENCE" }
            val fields = sequence.children()
            require(fields.size >= 5) { "KeyDescription has ${fields.size} fields, expected at least 5" }
            return KeyDescription(
                attestationVersion = fields[0].asInt(),
                attestationLevel = levelOf(fields[1].asInt()),
                keymasterVersion = fields[2].asInt(),
                keymasterLevel = levelOf(fields[3].asInt()),
                challenge = fields[4].asBytes(),
            )
        }

        fun levelOf(value: Int): AttestationLevel = when (value) {
            0 -> AttestationLevel.SOFTWARE
            1 -> AttestationLevel.TRUSTED_ENVIRONMENT
            2 -> AttestationLevel.STRONGBOX
            else -> AttestationLevel.UNKNOWN
        }
    }
}
