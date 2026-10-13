package com.danyastry.keystore.feature.keyinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.keyinfo.R

@Composable
fun KeyInfoRoute(
    modifier: Modifier = Modifier,
    viewModel: KeyInfoViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    KeyInfoScreen(
        state = state,
        onRefresh = viewModel::onRefresh,
        onCreateSampleKey = viewModel::onCreateSampleKey,
        modifier = modifier,
    )
}

@Composable
fun KeyInfoScreen(
    state: KeyInfoUiState,
    onRefresh: () -> Unit,
    onCreateSampleKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.keyinfo_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateSampleKey) {
                    Text(stringResource(R.string.keyinfo_create_sample))
                }
                OutlinedButton(onClick = onRefresh) {
                    Text(stringResource(R.string.keyinfo_refresh))
                }
            }
        }
        if (!state.isLoading && state.keys.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.keyinfo_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.keys, key = { it.alias }) { details -> KeyCard(details) }
    }
}

@Composable
private fun KeyCard(details: KeyDetails) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = details.alias,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f),
                )
                LocationBadge(details.location)
            }
            Detail(R.string.keyinfo_algorithm, stringResource(R.string.keyinfo_algorithm_value, details.algorithm, details.sizeBits, stringResource(details.kind.labelRes)))
            Detail(R.string.keyinfo_purposes, details.purposes.sortedBy { it.ordinal }.joinToString(", ") { it.label })
            Detail(R.string.keyinfo_origin, stringResource(details.origin.labelRes))
            Detail(
                R.string.keyinfo_user_auth,
                stringResource(if (details.requiresUserAuthentication) R.string.keyinfo_yes else R.string.keyinfo_no),
            )
            Text(
                text = stringResource(details.location.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Detail(labelRes: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun LocationBadge(location: KeyLocation) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (location) {
        KeyLocation.STRONGBOX, KeyLocation.TRUSTED_ENVIRONMENT, KeyLocation.SECURE_HARDWARE -> colors.primaryContainer to colors.onPrimaryContainer
        KeyLocation.SOFTWARE -> colors.errorContainer to colors.onErrorContainer
        KeyLocation.UNKNOWN -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Badge(text = stringResource(location.labelRes), container = container, content = content)
}

@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private val KeyLocation.labelRes: Int
    get() = when (this) {
        KeyLocation.STRONGBOX -> R.string.keyinfo_location_strongbox
        KeyLocation.TRUSTED_ENVIRONMENT -> R.string.keyinfo_location_tee
        KeyLocation.SECURE_HARDWARE -> R.string.keyinfo_location_secure_hardware
        KeyLocation.SOFTWARE -> R.string.keyinfo_location_software
        KeyLocation.UNKNOWN -> R.string.keyinfo_location_unknown
    }

private val KeyLocation.descriptionRes: Int
    get() = when (this) {
        KeyLocation.STRONGBOX -> R.string.keyinfo_location_strongbox_description
        KeyLocation.TRUSTED_ENVIRONMENT -> R.string.keyinfo_location_tee_description
        KeyLocation.SECURE_HARDWARE -> R.string.keyinfo_location_secure_hardware_description
        KeyLocation.SOFTWARE -> R.string.keyinfo_location_software_description
        KeyLocation.UNKNOWN -> R.string.keyinfo_location_unknown_description
    }

private val KeyKind.labelRes: Int
    get() = when (this) {
        KeyKind.SECRET -> R.string.keyinfo_kind_secret
        KeyKind.PRIVATE -> R.string.keyinfo_kind_private
    }

private val KeyOrigin.labelRes: Int
    get() = when (this) {
        KeyOrigin.GENERATED -> R.string.keyinfo_origin_generated
        KeyOrigin.IMPORTED -> R.string.keyinfo_origin_imported
        KeyOrigin.SECURELY_IMPORTED -> R.string.keyinfo_origin_securely_imported
        KeyOrigin.UNKNOWN -> R.string.keyinfo_origin_unknown
    }

private val KeyPurpose.label: String
    get() = name.lowercase().replace('_', ' ')

@Preview(showBackground = true)
@Composable
private fun KeyInfoScreenPreview() {
    KeystoreTheme {
        KeyInfoScreen(
            state = KeyInfoUiState(
                isLoading = false,
                keys = listOf(
                    KeyDetails("aes-gcm-demo", "AES", 256, KeyKind.SECRET, KeyLocation.TRUSTED_ENVIRONMENT, setOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT), KeyOrigin.GENERATED, false),
                ),
            ),
            onRefresh = {},
            onCreateSampleKey = {},
        )
    }
}
