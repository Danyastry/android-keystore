package com.danyastry.keystore.feature.envelope.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.keystore.core.ui.components.CipherTextField
import com.danyastry.keystore.core.ui.components.SegmentedSelector
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.envelope.EnvelopeCipher
import com.danyastry.keystore.feature.envelope.R

@Composable
fun EnvelopeRoute(
    modifier: Modifier = Modifier,
    viewModel: EnvelopeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EnvelopeScreen(
        state = state,
        onCreateKey = viewModel::onCreateKey,
        onDeleteKey = viewModel::onDeleteKey,
        onSourceChange = viewModel::onSourceChange,
        onInputChange = viewModel::onInputChange,
        onRsaOnly = viewModel::onRsaOnly,
        onSeal = viewModel::onSeal,
        onOpen = viewModel::onOpen,
        modifier = modifier,
    )
}

@Composable
fun EnvelopeScreen(
    state: EnvelopeUiState,
    onCreateKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onSourceChange: (PayloadSource) -> Unit,
    onInputChange: (String) -> Unit,
    onRsaOnly: () -> Unit,
    onSeal: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KeyCard(hasKey = state.hasKey, onCreateKey = onCreateKey, onDeleteKey = onDeleteKey)
        SegmentedSelector(
            options = PayloadSource.entries,
            selected = state.source,
            label = { stringResource(it.labelRes) },
            onSelect = onSourceChange,
        )
        if (state.source == PayloadSource.TEXT) {
            CipherTextField(
                value = state.input,
                onValueChange = onInputChange,
                label = stringResource(R.string.envelope_input_label),
            )
        }
        Text(
            text = pluralStringResource(R.plurals.envelope_payload_size, state.payloadBytes, state.payloadBytes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RsaOnlyCard(state = state, onRsaOnly = onRsaOnly)
        EnvelopeCard(state = state, onSeal = onSeal, onOpen = onOpen)
        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun KeyCard(hasKey: Boolean, onCreateKey: () -> Unit, onDeleteKey: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.envelope_key_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(if (hasKey) R.string.envelope_key_ready else R.string.envelope_key_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateKey, enabled = !hasKey) {
                    Text(stringResource(R.string.envelope_create_key))
                }
                OutlinedButton(onClick = onDeleteKey, enabled = hasKey) {
                    Text(stringResource(R.string.envelope_delete_key))
                }
            }
        }
    }
}

@Composable
private fun RsaOnlyCard(state: EnvelopeUiState, onRsaOnly: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.envelope_rsa_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = pluralStringResource(R.plurals.envelope_rsa_hint, EnvelopeCipher.RSA_MAX_PLAINTEXT_BYTES, EnvelopeCipher.RSA_MAX_PLAINTEXT_BYTES),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onRsaOnly, enabled = state.canSeal, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.envelope_rsa_only))
            }
            state.rsaOnly?.let { result ->
                Text(
                    text = if (result.isAllowed) {
                        pluralStringResource(R.plurals.envelope_rsa_allowed, result.payloadBytes, result.payloadBytes)
                    } else {
                        pluralStringResource(R.plurals.envelope_rsa_refused, result.payloadBytes, result.payloadBytes, result.reason)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result.isAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EnvelopeCard(state: EnvelopeUiState, onSeal: () -> Unit, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.envelope_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.envelope_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSeal, enabled = state.canSeal) {
                    Text(stringResource(R.string.envelope_seal))
                }
                OutlinedButton(onClick = onOpen, enabled = state.canOpen) {
                    Text(stringResource(R.string.envelope_open))
                }
            }
            state.sealed?.let { sealed ->
                MonoText(pluralStringResource(R.plurals.envelope_wrapped_key, sealed.wrappedKeyBytes, sealed.wrappedKeyBytes))
                MonoText(pluralStringResource(R.plurals.envelope_iv, sealed.ivBytes, sealed.ivBytes))
                MonoText(pluralStringResource(R.plurals.envelope_ciphertext, sealed.ciphertextBytes, sealed.ciphertextBytes))
                MonoText(pluralStringResource(R.plurals.envelope_total, sealed.totalBytes, sealed.totalBytes))
                MonoText(stringResource(R.string.envelope_preview, sealed.preview))
            }
            state.opened?.let { opened ->
                Text(
                    text = pluralStringResource(R.plurals.envelope_opened, opened.bytes, opened.bytes),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(if (opened.matchesOriginal) R.string.envelope_matches else R.string.envelope_differs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (opened.matchesOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                MonoText(opened.preview)
            }
        }
    }
}

@Composable
private fun MonoText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
}

private val PayloadSource.labelRes: Int
    get() = when (this) {
        PayloadSource.TEXT -> R.string.envelope_source_text
        PayloadSource.SAMPLE -> R.string.envelope_source_sample
    }

private val EnvelopeError.messageRes: Int
    get() = when (this) {
        EnvelopeError.KEY_CREATION_FAILED -> R.string.envelope_error_key_creation_failed
        EnvelopeError.NO_KEY -> R.string.envelope_error_no_key
        EnvelopeError.CANNOT_OPEN -> R.string.envelope_error_cannot_open
    }

@Preview(showBackground = true)
@Composable
private fun EnvelopeScreenPreview() {
    KeystoreTheme {
        EnvelopeScreen(
            state = EnvelopeUiState(
                hasKey = true,
                source = PayloadSource.SAMPLE,
                payloadBytes = 65_536,
                rsaOnly = RsaOnlyResult(65_536, isAllowed = false, reason = "IllegalBlockSizeException"),
                sealed = EnvelopeSummary(256, 12, 65_552, 65_822, "AQBxk3f9Ld0aQm2v7rTz1c9Kp0wX4bH8nY6eS5uJ"),
                opened = OpenedResult(65_536, matchesOriginal = true, preview = "The quick brown fox jumps over the lazy dog. The quick"),
            ),
            onCreateKey = {},
            onDeleteKey = {},
            onSourceChange = {},
            onInputChange = {},
            onRsaOnly = {},
            onSeal = {},
            onOpen = {},
        )
    }
}
