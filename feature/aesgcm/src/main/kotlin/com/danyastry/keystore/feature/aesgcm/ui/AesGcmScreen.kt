package com.danyastry.keystore.feature.aesgcm.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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
import com.danyastry.keystore.feature.aesgcm.R

@Composable
fun AesGcmRoute(
    modifier: Modifier = Modifier,
    viewModel: AesGcmViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AesGcmScreen(
        state = state,
        onCreateKey = viewModel::onCreateKey,
        onDeleteKey = viewModel::onDeleteKey,
        onModeChange = viewModel::onModeChange,
        onInputChange = viewModel::onInputChange,
        onUseOutputAsInput = viewModel::onUseOutputAsInput,
        onFlipBit = viewModel::onFlipBit,
        modifier = modifier,
    )
}

@Composable
fun AesGcmScreen(
    state: AesGcmUiState,
    onCreateKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onModeChange: (CipherMode) -> Unit,
    onInputChange: (String) -> Unit,
    onUseOutputAsInput: () -> Unit,
    onFlipBit: () -> Unit,
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
            supportingText = stringResource(state.error?.messageRes ?: state.mode.hintRes),
        )
        ResultCard(
            label = stringResource(state.mode.outputLabelRes),
            output = state.output,
            onUseOutputAsInput = onUseOutputAsInput,
        )
        if (state.ivHex.isNotEmpty()) {
            Text(
                text = stringResource(R.string.aesgcm_iv, state.ivHex),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.mode == CipherMode.DECRYPT) {
            OutlinedButton(onClick = onFlipBit, enabled = state.ivHex.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.aesgcm_flip_bit))
            }
        }
    }
}

@Composable
private fun KeyCard(state: AesGcmUiState, onCreateKey: () -> Unit, onDeleteKey: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.aesgcm_key_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    when {
                        !state.hasKey -> R.string.aesgcm_key_missing
                        state.isKeyMaterialHidden -> R.string.aesgcm_key_hidden
                        else -> R.string.aesgcm_key_visible
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateKey, enabled = !state.hasKey) {
                    Text(stringResource(R.string.aesgcm_create_key))
                }
                OutlinedButton(onClick = onDeleteKey, enabled = state.hasKey) {
                    Text(stringResource(R.string.aesgcm_delete_key))
                }
            }
        }
    }
}

private val AesGcmError.messageRes: Int
    get() = when (this) {
        AesGcmError.NO_KEY -> R.string.aesgcm_error_no_key
        AesGcmError.INVALID_MESSAGE -> R.string.aesgcm_error_invalid
        AesGcmError.TAMPERED -> R.string.aesgcm_error_tampered
    }

private val CipherMode.hintRes: Int
    get() = when (this) {
        CipherMode.ENCRYPT -> R.string.aesgcm_hint_encrypt
        CipherMode.DECRYPT -> R.string.aesgcm_hint_decrypt
    }

@Preview(showBackground = true)
@Composable
private fun AesGcmScreenPreview() {
    KeystoreTheme {
        AesGcmScreen(
            state = AesGcmUiState(hasKey = true, isKeyMaterialHidden = true, input = "Hello", output = "q83vEjRWeJA=", ivHex = "9f2c11d0a4b7e3f6c1d2e3f4"),
            onCreateKey = {},
            onDeleteKey = {},
            onModeChange = {},
            onInputChange = {},
            onUseOutputAsInput = {},
            onFlipBit = {},
        )
    }
}
