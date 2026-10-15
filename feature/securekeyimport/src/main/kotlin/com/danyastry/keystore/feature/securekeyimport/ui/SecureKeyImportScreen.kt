package com.danyastry.keystore.feature.securekeyimport.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.danyastry.keystore.core.keystore.KeyDetails
import com.danyastry.keystore.core.keystore.KeyKind
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.keystore.KeyOrigin
import com.danyastry.keystore.core.keystore.KeyPurpose
import com.danyastry.keystore.core.ui.components.CipherTextField
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.securekeyimport.R

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun SecureKeyImportRoute(modifier: Modifier = Modifier) {
    val viewModel: SecureKeyImportViewModel = viewModel { SecureKeyImportViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecureKeyImportScreen(
        state = state,
        onCreateWrappingKey = viewModel::onCreateWrappingKey,
        onWrap = viewModel::onWrapOnServer,
        onTamper = viewModel::onTamper,
        onImport = viewModel::onImport,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onServerSend,
        onDecrypt = viewModel::onDeviceDecrypt,
        onReset = viewModel::onReset,
        modifier = modifier,
    )
}

@Composable
fun SecureKeyImportScreen(
    state: SecureKeyImportUiState,
    onCreateWrappingKey: () -> Unit,
    onWrap: () -> Unit,
    onTamper: () -> Unit,
    onImport: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onDecrypt: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.securekeyimport_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepCard(title = stringResource(R.string.securekeyimport_step_device), hint = stringResource(R.string.securekeyimport_device_hint)) {
            Button(onClick = onCreateWrappingKey, enabled = !state.hasWrappingKey) {
                Text(stringResource(R.string.securekeyimport_create_wrapping_key))
            }
            if (state.hasWrappingKey) MonoText(stringResource(R.string.securekeyimport_public_key, state.wrappingPublicKey))
        }
        StepCard(title = stringResource(R.string.securekeyimport_step_server), hint = stringResource(R.string.securekeyimport_server_hint)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onWrap, enabled = state.hasWrappingKey) {
                    Text(stringResource(R.string.securekeyimport_wrap))
                }
                OutlinedButton(onClick = onTamper, enabled = state.canTamper) {
                    Text(stringResource(R.string.securekeyimport_tamper))
                }
            }
            state.wrapped?.let { wrapped ->
                MonoText(stringResource(R.string.securekeyimport_server_key, state.serverKey))
                MonoText(pluralStringResource(R.plurals.securekeyimport_field_transport_key, wrapped.transportKeyBytes, wrapped.transportKeyBytes))
                MonoText(pluralStringResource(R.plurals.securekeyimport_field_iv, wrapped.ivBytes, wrapped.ivBytes))
                MonoText(pluralStringResource(R.plurals.securekeyimport_field_description, wrapped.descriptionBytes, wrapped.descriptionBytes))
                MonoText(pluralStringResource(R.plurals.securekeyimport_field_encrypted_key, wrapped.encryptedKeyBytes, wrapped.encryptedKeyBytes))
                MonoText(pluralStringResource(R.plurals.securekeyimport_field_tag, wrapped.tagBytes, wrapped.tagBytes))
                Text(
                    text = pluralStringResource(R.plurals.securekeyimport_total, wrapped.totalBytes, wrapped.totalBytes),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (wrapped.isTampered) {
                    Text(
                        text = stringResource(R.string.securekeyimport_tampered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        StepCard(title = stringResource(R.string.securekeyimport_step_import), hint = stringResource(R.string.securekeyimport_import_hint)) {
            Button(onClick = onImport, enabled = state.canImport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.securekeyimport_import))
            }
            state.imported?.let { imported ->
                Text(
                    text = stringResource(
                        R.string.securekeyimport_imported,
                        imported.sizeBits,
                        stringResource(imported.origin.labelRes),
                        stringResource(imported.location.labelRes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.isImportedMaterialHidden) {
                    Text(
                        text = stringResource(R.string.securekeyimport_imported_hidden),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(R.string.securekeyimport_origin_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StepCard(title = stringResource(R.string.securekeyimport_step_message), hint = null) {
            CipherTextField(
                value = state.input,
                onValueChange = onInputChange,
                label = stringResource(R.string.securekeyimport_input_label),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSend, enabled = state.canSend) {
                    Text(stringResource(R.string.securekeyimport_send))
                }
                OutlinedButton(onClick = onDecrypt, enabled = state.canDecrypt) {
                    Text(stringResource(R.string.securekeyimport_decrypt))
                }
            }
            SelectionContainer {
                MonoText(state.serverMessage.ifEmpty { stringResource(R.string.securekeyimport_message_empty) })
            }
            if (state.deviceRead.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.securekeyimport_device_read, state.deviceRead),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        TextButton(onClick = onReset) {
            Text(stringResource(R.string.securekeyimport_reset))
        }
    }
}

@Composable
private fun StepCard(title: String, hint: String?, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun MonoText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
}

private val SecureImportError.messageRes: Int
    get() = when (this) {
        SecureImportError.WRAPPING_KEY_FAILED -> R.string.securekeyimport_error_wrapping_key_failed
        SecureImportError.IMPORT_REFUSED -> R.string.securekeyimport_error_import_refused
        SecureImportError.CANNOT_DECRYPT -> R.string.securekeyimport_error_cannot_decrypt
    }

private val KeyOrigin.labelRes: Int
    get() = when (this) {
        KeyOrigin.GENERATED -> R.string.securekeyimport_origin_generated
        KeyOrigin.IMPORTED -> R.string.securekeyimport_origin_imported
        KeyOrigin.SECURELY_IMPORTED -> R.string.securekeyimport_origin_securely_imported
        KeyOrigin.UNKNOWN -> R.string.securekeyimport_origin_unknown
    }

private val KeyLocation.labelRes: Int
    get() = when (this) {
        KeyLocation.STRONGBOX -> R.string.securekeyimport_location_strongbox
        KeyLocation.TRUSTED_ENVIRONMENT -> R.string.securekeyimport_location_tee
        KeyLocation.SECURE_HARDWARE -> R.string.securekeyimport_location_secure_hardware
        KeyLocation.SOFTWARE -> R.string.securekeyimport_location_software
        KeyLocation.UNKNOWN -> R.string.securekeyimport_location_unknown
    }

@Preview(showBackground = true)
@Composable
private fun SecureKeyImportScreenPreview() {
    KeystoreTheme {
        SecureKeyImportScreen(
            state = SecureKeyImportUiState(
                wrappingPublicKey = "4e:1a:90:c3:77:02:bd:5f",
                serverKey = "a1:0f:6c:e2:39:d8:41:7b",
                wrapped = WrappedSummary(256, 12, 43, 32, 16, 381),
                imported = KeyDetails(
                    alias = "secure-import-demo",
                    algorithm = "AES",
                    sizeBits = 256,
                    kind = KeyKind.SECRET,
                    location = KeyLocation.TRUSTED_ENVIRONMENT,
                    purposes = setOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT),
                    origin = KeyOrigin.SECURELY_IMPORTED,
                    requiresUserAuthentication = false,
                ),
                isImportedMaterialHidden = true,
                input = "Attack at dawn",
                deviceRead = "Attack at dawn",
            ),
            onCreateWrappingKey = {},
            onWrap = {},
            onTamper = {},
            onImport = {},
            onInputChange = {},
            onSend = {},
            onDecrypt = {},
            onReset = {},
        )
    }
}
