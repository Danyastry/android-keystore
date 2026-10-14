package com.danyastry.keystore.feature.ecdh.ui

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.keystore.core.ui.components.CipherTextField
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.ecdh.R

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun EcdhRoute(modifier: Modifier = Modifier) {
    val viewModel: EcdhViewModel = viewModel { EcdhViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EcdhScreen(
        state = state,
        onCreateDeviceKey = viewModel::onCreateDeviceKey,
        onDeleteDeviceKey = viewModel::onDeleteDeviceKey,
        onNewPeer = viewModel::onNewPeer,
        onAgree = viewModel::onAgree,
        onInputChange = viewModel::onInputChange,
        onEncrypt = viewModel::onEncryptOnDevice,
        onDecrypt = viewModel::onDecryptOnPeer,
        modifier = modifier,
    )
}

@Composable
fun EcdhScreen(
    state: EcdhUiState,
    onCreateDeviceKey: () -> Unit,
    onDeleteDeviceKey: () -> Unit,
    onNewPeer: () -> Unit,
    onAgree: () -> Unit,
    onInputChange: (String) -> Unit,
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PartyCard(
            title = stringResource(R.string.ecdh_device_title),
            description = stringResource(if (state.hasDeviceKey) R.string.ecdh_device_ready else R.string.ecdh_device_missing),
            publicKey = state.devicePublicKey,
        ) {
            Button(onClick = onCreateDeviceKey, enabled = !state.hasDeviceKey) {
                Text(stringResource(R.string.ecdh_create_key))
            }
            OutlinedButton(onClick = onDeleteDeviceKey, enabled = state.hasDeviceKey) {
                Text(stringResource(R.string.ecdh_delete_key))
            }
        }
        PartyCard(
            title = stringResource(R.string.ecdh_peer_title),
            description = stringResource(R.string.ecdh_peer_description),
            publicKey = state.peerPublicKey,
        ) {
            OutlinedButton(onClick = onNewPeer) {
                Text(stringResource(R.string.ecdh_new_peer))
            }
        }
        AgreementCard(state = state, onAgree = onAgree)
        CipherTextField(
            value = state.input,
            onValueChange = onInputChange,
            label = stringResource(R.string.ecdh_input_label),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEncrypt, enabled = state.canEncrypt) {
                Text(stringResource(R.string.ecdh_encrypt))
            }
            OutlinedButton(onClick = onDecrypt, enabled = state.canDecrypt) {
                Text(stringResource(R.string.ecdh_decrypt))
            }
        }
        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        MessageCard(state = state)
    }
}

@Composable
private fun PartyCard(title: String, description: String, publicKey: String, actions: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (publicKey.isNotEmpty()) {
                MonoText(stringResource(R.string.ecdh_public_key, publicKey))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}

@Composable
private fun AgreementCard(state: EcdhUiState, onAgree: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.ecdh_agree_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.ecdh_agree_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAgree, enabled = state.canAgree, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ecdh_agree))
            }
            if (state.isAgreed) {
                MonoText(stringResource(R.string.ecdh_device_secret, state.deviceSecret))
                MonoText(stringResource(R.string.ecdh_peer_secret, state.peerSecret))
                Text(
                    text = stringResource(if (state.secretsMatch) R.string.ecdh_secrets_match else R.string.ecdh_secrets_differ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.secretsMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                MonoText(stringResource(R.string.ecdh_session_key, state.sessionKey))
                Text(
                    text = stringResource(R.string.ecdh_secret_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MessageCard(state: EcdhUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.ecdh_sealed_label), style = MaterialTheme.typography.labelLarge)
            SelectionContainer {
                MonoText(state.sealed.ifEmpty { stringResource(R.string.ecdh_sealed_empty) })
            }
            if (state.peerDecrypted.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.ecdh_peer_decrypted, state.peerDecrypted),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun MonoText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
}

private val EcdhError.messageRes: Int
    get() = when (this) {
        EcdhError.KEY_CREATION_FAILED -> R.string.ecdh_error_key_creation_failed
        EcdhError.NO_DEVICE_KEY -> R.string.ecdh_error_no_device_key
        EcdhError.NOT_AGREED -> R.string.ecdh_error_not_agreed
        EcdhError.INVALID_MESSAGE -> R.string.ecdh_error_invalid_message
        EcdhError.PEER_CANNOT_DECRYPT -> R.string.ecdh_error_peer_cannot_decrypt
    }

@Preview(showBackground = true)
@Composable
private fun EcdhScreenPreview() {
    KeystoreTheme {
        EcdhScreen(
            state = EcdhUiState(
                hasDeviceKey = true,
                devicePublicKey = "3a:91:0c:5e:77:12:d4:8b",
                peerPublicKey = "c2:08:fe:41:9a:6d:30:e5",
                deviceSecret = "5b:e1:02:7c:aa:93:4f:10",
                peerSecret = "5b:e1:02:7c:aa:93:4f:10",
                sessionKey = "9d:44:b7:20:6e:c1:08:f3",
                input = "Attack at dawn",
            ),
            onCreateDeviceKey = {},
            onDeleteDeviceKey = {},
            onNewPeer = {},
            onAgree = {},
            onInputChange = {},
            onEncrypt = {},
            onDecrypt = {},
        )
    }
}
