package com.danyastry.keystore.feature.rsaoaep.ui

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
import com.danyastry.keystore.core.ui.components.CipherMode
import com.danyastry.keystore.core.ui.components.CipherTextField
import com.danyastry.keystore.core.ui.components.ModeSelector
import com.danyastry.keystore.core.ui.components.ResultCard
import com.danyastry.keystore.core.ui.components.inputLabelRes
import com.danyastry.keystore.core.ui.components.outputLabelRes
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.rsaoaep.R
import com.danyastry.keystore.feature.rsaoaep.RsaOaepCipher

@Composable
fun RsaOaepRoute(
    modifier: Modifier = Modifier,
    viewModel: RsaOaepViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RsaOaepScreen(
        state = state,
        onCreateKey = viewModel::onCreateKey,
        onDeleteKey = viewModel::onDeleteKey,
        onModeChange = viewModel::onModeChange,
        onInputChange = viewModel::onInputChange,
        onUseOutputAsInput = viewModel::onUseOutputAsInput,
        modifier = modifier,
    )
}

@Composable
fun RsaOaepScreen(
    state: RsaOaepUiState,
    onCreateKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onModeChange: (CipherMode) -> Unit,
    onInputChange: (String) -> Unit,
    onUseOutputAsInput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KeyCard(state = state, onCreateKey = onCreateKey, onDeleteKey = onDeleteKey)
        ModeSelector(selected = state.mode, onSelect = onModeChange)
        CipherTextField(
            value = state.input,
            onValueChange = onInputChange,
            label = stringResource(state.mode.inputLabelRes),
            isError = state.error != null,
            supportingText = when {
                state.error != null -> stringResource(state.error.messageRes, RsaOaepCipher.MAX_PLAINTEXT_BYTES)
                state.mode == CipherMode.ENCRYPT -> stringResource(R.string.rsaoaep_bytes, state.inputBytes, RsaOaepCipher.MAX_PLAINTEXT_BYTES)
                else -> stringResource(R.string.rsaoaep_hint_decrypt)
            },
        )
        ResultCard(
            label = stringResource(state.mode.outputLabelRes),
            output = state.output,
            onUseOutputAsInput = onUseOutputAsInput,
        )
        Text(
            text = stringResource(R.string.rsaoaep_why_small),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeyCard(state: RsaOaepUiState, onCreateKey: () -> Unit, onDeleteKey: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.rsaoaep_key_title),
                style = MaterialTheme.typography.titleSmall,
            )
            if (state.hasKey) {
                Text(
                    text = stringResource(R.string.rsaoaep_public_key),
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
                        if (state.isPrivateKeyHidden) R.string.rsaoaep_private_hidden else R.string.rsaoaep_private_visible,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.rsaoaep_key_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateKey, enabled = !state.hasKey) {
                    Text(stringResource(R.string.rsaoaep_create_key))
                }
                OutlinedButton(onClick = onDeleteKey, enabled = state.hasKey) {
                    Text(stringResource(R.string.rsaoaep_delete_key))
                }
            }
        }
    }
}

private const val PUBLIC_KEY_LINES = 3

private val RsaOaepError.messageRes: Int
    get() = when (this) {
        RsaOaepError.NO_KEY -> R.string.rsaoaep_error_no_key
        RsaOaepError.TOO_LONG -> R.string.rsaoaep_error_too_long
        RsaOaepError.INVALID_MESSAGE -> R.string.rsaoaep_error_invalid
        RsaOaepError.DECRYPTION_FAILED -> R.string.rsaoaep_error_failed
    }

@Preview(showBackground = true)
@Composable
private fun RsaOaepScreenPreview() {
    KeystoreTheme {
        RsaOaepScreen(
            state = RsaOaepUiState(hasKey = true, publicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA", isPrivateKeyHidden = true, input = "Hello", inputBytes = 5),
            onCreateKey = {},
            onDeleteKey = {},
            onModeChange = {},
            onInputChange = {},
            onUseOutputAsInput = {},
        )
    }
}
