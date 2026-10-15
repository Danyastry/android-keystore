package com.danyastry.keystore.feature.envelope.ui

enum class PayloadSource { TEXT, SAMPLE }

enum class EnvelopeError { KEY_CREATION_FAILED, NO_KEY, CANNOT_OPEN }

data class RsaOnlyResult(val payloadBytes: Int, val isAllowed: Boolean, val reason: String = "")

data class EnvelopeSummary(val wrappedKeyBytes: Int, val ivBytes: Int, val ciphertextBytes: Int, val totalBytes: Int, val preview: String)

data class OpenedResult(val bytes: Int, val matchesOriginal: Boolean, val preview: String)

data class EnvelopeUiState(
    val hasKey: Boolean = false,
    val source: PayloadSource = PayloadSource.TEXT,
    val input: String = "",
    val payloadBytes: Int = 0,
    val rsaOnly: RsaOnlyResult? = null,
    val sealed: EnvelopeSummary? = null,
    val opened: OpenedResult? = null,
    val error: EnvelopeError? = null,
) {
    val hasPayload: Boolean
        get() = payloadBytes > 0

    val canSeal: Boolean
        get() = hasKey && hasPayload

    val canOpen: Boolean
        get() = hasKey && sealed != null
}
