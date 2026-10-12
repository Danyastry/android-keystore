package com.danyastry.keystore.feature.ecdsa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.keystore.core.ui.components.CipherTextField
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.ecdsa.R

@Composable
fun EcdsaRoute(
    modifier: Modifier = Modifier,
    viewModel: EcdsaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EcdsaScreen(
        state = state,
        onCreateKey = viewModel::onCreateKey,
        onDeleteKey = viewModel::onDeleteKey,
        onMessageChange = viewModel::onMessageChange,
        onSignatureChange = viewModel::onSignatureChange,
        onSign = viewModel::onSign,
        modifier = modifier,
    )
}

@Composable
fun EcdsaScreen(
    state: EcdsaUiState,
    onCreateKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSignatureChange: (String) -> Unit,
    onSign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KeyCard(state = state, onCreateKey = onCreateKey, onDeleteKey = onDeleteKey)
        CipherTextField(
            value = state.message,
            onValueChange = onMessageChange,
            label = stringResource(R.string.ecdsa_message),
            supportingText = stringResource(R.string.ecdsa_message_hint),
        )
        Button(onClick = onSign, enabled = state.hasKey, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ecdsa_sign))
        }
        CipherTextField(
            value = state.signature,
            onValueChange = onSignatureChange,
            label = stringResource(R.string.ecdsa_signature),
            supportingText = if (state.signatureBytes > 0) stringResource(R.string.ecdsa_signature_size, state.signatureBytes) else null,
        )
        state.verification?.let { VerificationCard(it) }
    }
}

@Composable
private fun KeyCard(state: EcdsaUiState, onCreateKey: () -> Unit, onDeleteKey: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.ecdsa_key_title),
                style = MaterialTheme.typography.titleSmall,
            )
            if (state.hasKey) {
                Text(
                    text = stringResource(R.string.ecdsa_public_key),
                    style = MaterialTheme.typography.labelLarge,
                )
                SelectionContainer {
                    Text(
                        text = state.publicKeyBase64,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = PUBLIC_KEY_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(
                        if (state.isPrivateKeyHidden) R.string.ecdsa_private_hidden else R.string.ecdsa_private_visible,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.ecdsa_key_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateKey, enabled = !state.hasKey) {
                    Text(stringResource(R.string.ecdsa_create_key))
                }
                OutlinedButton(onClick = onDeleteKey, enabled = state.hasKey) {
                    Text(stringResource(R.string.ecdsa_delete_key))
                }
            }
        }
    }
}

@Composable
private fun VerificationCard(verification: Verification) {
    val colors = MaterialTheme.colorScheme
    val isValid = verification == Verification.VALID
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isValid) colors.primaryContainer else colors.errorContainer,
            contentColor = if (isValid) colors.onPrimaryContainer else colors.onErrorContainer,
        ),
    ) {
        Text(
            text = stringResource(verification.messageRes),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private const val PUBLIC_KEY_LINES = 3

private val Verification.messageRes: Int
    get() = when (this) {
        Verification.VALID -> R.string.ecdsa_valid
        Verification.INVALID -> R.string.ecdsa_invalid
        Verification.NOT_BASE64 -> R.string.ecdsa_not_base64
    }

@Preview(showBackground = true)
@Composable
private fun EcdsaScreenPreview() {
    KeystoreTheme {
        EcdsaScreen(
            state = EcdsaUiState(hasKey = true, publicKeyBase64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE", isPrivateKeyHidden = true, signature = "MEUCIQD", signatureBytes = 71, verification = Verification.VALID),
            onCreateKey = {},
            onDeleteKey = {},
            onMessageChange = {},
            onSignatureChange = {},
            onSign = {},
        )
    }
}
