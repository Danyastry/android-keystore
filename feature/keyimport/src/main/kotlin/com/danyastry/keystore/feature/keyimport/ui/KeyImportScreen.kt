package com.danyastry.keystore.feature.keyimport.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.core.keystore.KeyKind
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.keystore.KeyOrigin
import com.danyastry.keystore.core.keystore.KeyPurpose
import com.danyastry.keystore.core.ui.components.CipherTextField
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.keyimport.R

@Composable
fun KeyImportRoute(
    modifier: Modifier = Modifier,
    viewModel: KeyImportViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    KeyImportScreen(
        state = state,
        onHexChange = viewModel::onHexChange,
        onGenerate = viewModel::onGenerate,
        onForgetOriginal = viewModel::onForgetOriginal,
        onImport = viewModel::onImport,
        onDeleteImported = viewModel::onDeleteImported,
        onInputChange = viewModel::onInputChange,
        onEncrypt = viewModel::onEncryptWithKeystore,
        onDecryptWithKeystore = viewModel::onDecryptWithKeystore,
        onDecryptWithOriginal = viewModel::onDecryptWithOriginal,
        modifier = modifier,
    )
}

@Composable
fun KeyImportScreen(
    state: KeyImportUiState,
    onHexChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onForgetOriginal: () -> Unit,
    onImport: () -> Unit,
    onDeleteImported: () -> Unit,
    onInputChange: (String) -> Unit,
    onEncrypt: () -> Unit,
    onDecryptWithKeystore: () -> Unit,
    onDecryptWithOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OriginalCard(state = state, onHexChange = onHexChange, onGenerate = onGenerate, onForgetOriginal = onForgetOriginal, onImport = onImport)
        ImportedCard(state = state, onDeleteImported = onDeleteImported)
        CipherTextField(
            value = state.input,
            onValueChange = onInputChange,
            label = stringResource(R.string.keyimport_input_label),
        )
        Button(onClick = onEncrypt, enabled = state.canEncrypt, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.keyimport_encrypt))
        }
        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        MessageCard(state = state, onDecryptWithKeystore = onDecryptWithKeystore, onDecryptWithOriginal = onDecryptWithOriginal)
    }
}

@Composable
private fun OriginalCard(
    state: KeyImportUiState,
    onHexChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onForgetOriginal: () -> Unit,
    onImport: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.keyimport_original_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.keyimport_original_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.originalHex,
                onValueChange = onHexChange,
                label = { Text(stringResource(R.string.keyimport_original_label)) },
                isError = state.isOriginalInvalid,
                supportingText = if (state.isOriginalInvalid) {
                    { Text(stringResource(R.string.keyimport_original_invalid)) }
                } else {
                    null
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onGenerate) {
                    Text(stringResource(R.string.keyimport_generate))
                }
                OutlinedButton(onClick = onForgetOriginal, enabled = state.originalHex.isNotEmpty()) {
                    Text(stringResource(R.string.keyimport_forget))
                }
            }
            Button(onClick = onImport, enabled = state.canImport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.keyimport_import))
            }
        }
    }
}

@Composable
private fun ImportedCard(state: KeyImportUiState, onDeleteImported: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.keyimport_imported_title), style = MaterialTheme.typography.titleSmall)
            val imported = state.imported
            if (imported == null) {
                Text(
                    text = stringResource(R.string.keyimport_imported_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.keyimport_imported_details,
                        imported.sizeBits,
                        stringResource(imported.origin.labelRes),
                        stringResource(imported.location.labelRes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(if (state.isImportedMaterialHidden) R.string.keyimport_imported_hidden else R.string.keyimport_imported_visible),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isImportedMaterialHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.keyimport_imported_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onDeleteImported) {
                    Text(stringResource(R.string.keyimport_delete))
                }
            }
        }
    }
}

@Composable
private fun MessageCard(state: KeyImportUiState, onDecryptWithKeystore: () -> Unit, onDecryptWithOriginal: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.keyimport_sealed_label), style = MaterialTheme.typography.labelLarge)
            SelectionContainer {
                Text(
                    text = state.sealed.ifEmpty { stringResource(R.string.keyimport_sealed_empty) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDecryptWithKeystore, enabled = state.canDecryptWithKeystore) {
                    Text(stringResource(R.string.keyimport_decrypt_keystore))
                }
                OutlinedButton(onClick = onDecryptWithOriginal, enabled = state.sealed.isNotEmpty()) {
                    Text(stringResource(R.string.keyimport_decrypt_original))
                }
            }
            state.decryptedBy?.let { reader ->
                Text(
                    text = stringResource(
                        when (reader) {
                            Reader.KEYSTORE -> R.string.keyimport_decrypted_keystore
                            Reader.ORIGINAL -> R.string.keyimport_decrypted_original
                        },
                        state.decrypted,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private val KeyImportError.messageRes: Int
    get() = when (this) {
        KeyImportError.IMPORT_FAILED -> R.string.keyimport_error_import_failed
        KeyImportError.NO_IMPORTED_KEY -> R.string.keyimport_error_no_imported_key
        KeyImportError.NO_ORIGINAL -> R.string.keyimport_error_no_original
        KeyImportError.INVALID_MESSAGE -> R.string.keyimport_error_invalid_message
        KeyImportError.WRONG_KEY -> R.string.keyimport_error_wrong_key
    }

private val KeyOrigin.labelRes: Int
    get() = when (this) {
        KeyOrigin.GENERATED -> R.string.keyimport_origin_generated
        KeyOrigin.IMPORTED -> R.string.keyimport_origin_imported
        KeyOrigin.SECURELY_IMPORTED -> R.string.keyimport_origin_securely_imported
        KeyOrigin.UNKNOWN -> R.string.keyimport_origin_unknown
    }

private val KeyLocation.labelRes: Int
    get() = when (this) {
        KeyLocation.STRONGBOX -> R.string.keyimport_location_strongbox
        KeyLocation.TRUSTED_ENVIRONMENT -> R.string.keyimport_location_tee
        KeyLocation.SECURE_HARDWARE -> R.string.keyimport_location_secure_hardware
        KeyLocation.SOFTWARE -> R.string.keyimport_location_software
        KeyLocation.UNKNOWN -> R.string.keyimport_location_unknown
    }

@Preview(showBackground = true)
@Composable
private fun KeyImportScreenPreview() {
    KeystoreTheme {
        KeyImportScreen(
            state = KeyImportUiState(
                originalHex = "603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4",
                imported = KeyDetails(
                    alias = "key-import-demo",
                    algorithm = "AES",
                    sizeBits = 256,
                    kind = KeyKind.SECRET,
                    location = KeyLocation.TRUSTED_ENVIRONMENT,
                    purposes = setOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT),
                    origin = KeyOrigin.IMPORTED,
                    requiresUserAuthentication = false,
                ),
                isImportedMaterialHidden = true,
                input = "Attack at dawn",
            ),
            onHexChange = {},
            onGenerate = {},
            onForgetOriginal = {},
            onImport = {},
            onDeleteImported = {},
            onInputChange = {},
            onEncrypt = {},
            onDecryptWithKeystore = {},
            onDecryptWithOriginal = {},
        )
    }
}
