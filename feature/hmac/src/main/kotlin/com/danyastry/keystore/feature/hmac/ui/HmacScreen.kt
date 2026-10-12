package com.danyastry.keystore.feature.hmac.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.keystore.core.ui.components.CipherTextField
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.hmac.R

@Composable
fun HmacRoute(
    modifier: Modifier = Modifier,
    viewModel: HmacViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HmacScreen(
        state = state,
        onCreateKey = viewModel::onCreateKey,
        onDeleteKey = viewModel::onDeleteKey,
        onMessageChange = viewModel::onMessageChange,
        onTagChange = viewModel::onTagChange,
        onComputeTag = viewModel::onComputeTag,
        modifier = modifier,
    )
}

@Composable
fun HmacScreen(
    state: HmacUiState,
    onCreateKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onMessageChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onComputeTag: () -> Unit,
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
            label = stringResource(R.string.hmac_message),
            supportingText = stringResource(R.string.hmac_message_hint),
        )
        Button(onClick = onComputeTag, enabled = state.hasKey, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.hmac_compute))
        }
        CipherTextField(
            value = state.tag,
            onValueChange = onTagChange,
            label = stringResource(R.string.hmac_tag),
            supportingText = stringResource(R.string.hmac_tag_hint),
        )
        state.check?.let { CheckCard(it) }
    }
}

@Composable
private fun KeyCard(state: HmacUiState, onCreateKey: () -> Unit, onDeleteKey: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.hmac_key_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    when {
                        !state.hasKey -> R.string.hmac_key_missing
                        state.isKeyMaterialHidden -> R.string.hmac_key_hidden
                        else -> R.string.hmac_key_visible
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateKey, enabled = !state.hasKey) {
                    Text(stringResource(R.string.hmac_create_key))
                }
                OutlinedButton(onClick = onDeleteKey, enabled = state.hasKey) {
                    Text(stringResource(R.string.hmac_delete_key))
                }
            }
        }
    }
}

@Composable
private fun CheckCard(check: TagCheck) {
    val colors = MaterialTheme.colorScheme
    val isValid = check == TagCheck.VALID
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isValid) colors.primaryContainer else colors.errorContainer,
            contentColor = if (isValid) colors.onPrimaryContainer else colors.onErrorContainer,
        ),
    ) {
        Text(
            text = stringResource(check.messageRes),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private val TagCheck.messageRes: Int
    get() = when (this) {
        TagCheck.VALID -> R.string.hmac_valid
        TagCheck.INVALID -> R.string.hmac_invalid
        TagCheck.NOT_HEX -> R.string.hmac_not_hex
    }

@Preview(showBackground = true)
@Composable
private fun HmacScreenPreview() {
    KeystoreTheme {
        HmacScreen(
            state = HmacUiState(hasKey = true, isKeyMaterialHidden = true, tag = "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", check = TagCheck.VALID),
            onCreateKey = {},
            onDeleteKey = {},
            onMessageChange = {},
            onTagChange = {},
            onComputeTag = {},
        )
    }
}
