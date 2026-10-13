package com.danyastry.keystore.feature.strongbox.ui

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.keystore.core.keystore.KeyLocation
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.strongbox.AndroidStrongBoxKeys
import com.danyastry.keystore.feature.strongbox.KeyCreation
import com.danyastry.keystore.feature.strongbox.KeyTarget
import com.danyastry.keystore.feature.strongbox.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun StrongBoxRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val viewModel: StrongBoxViewModel = viewModel { StrongBoxViewModel(AndroidStrongBoxKeys(context)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    StrongBoxScreen(
        state = state,
        onCreateKeys = viewModel::onCreateKeys,
        onMeasure = viewModel::onMeasure,
        onDeleteKeys = viewModel::onDeleteKeys,
        modifier = modifier,
    )
}

@Composable
fun StrongBoxScreen(
    state: StrongBoxUiState,
    onCreateKeys: () -> Unit,
    onMeasure: () -> Unit,
    onDeleteKeys: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.strongbox_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.isStrongBoxDeclared?.let { declared ->
            Text(
                text = stringResource(if (declared) R.string.strongbox_declared else R.string.strongbox_not_declared),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreateKeys, enabled = !state.isBusy) {
                Text(stringResource(R.string.strongbox_create))
            }
            OutlinedButton(onClick = onDeleteKeys, enabled = !state.isBusy && state.keys.isNotEmpty()) {
                Text(stringResource(R.string.strongbox_delete))
            }
        }
        KeyTarget.entries.forEach { target ->
            state.keys[target]?.let { creation -> KeyResultCard(target, creation, state.timings[target]) }
        }
        if (state.canMeasure) {
            OutlinedButton(onClick = onMeasure, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.strongbox_measure, StrongBoxViewModel.ROUNDS))
            }
        }
    }
}

@Composable
private fun KeyResultCard(target: KeyTarget, creation: KeyCreation, timing: Duration?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(target.titleRes),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = when (creation) {
                    is KeyCreation.Created -> stringResource(R.string.strongbox_stored_in, stringResource(creation.location.labelRes))
                    KeyCreation.Unavailable -> stringResource(R.string.strongbox_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (creation is KeyCreation.Unavailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            timing?.let {
                Text(
                    text = stringResource(R.string.strongbox_timing, it.inWholeMilliseconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val KeyTarget.titleRes: Int
    get() = when (this) {
        KeyTarget.REGULAR -> R.string.strongbox_regular_key
        KeyTarget.STRONGBOX -> R.string.strongbox_strongbox_key
    }

private val KeyLocation.labelRes: Int
    get() = when (this) {
        KeyLocation.STRONGBOX -> R.string.strongbox_location_strongbox
        KeyLocation.TRUSTED_ENVIRONMENT -> R.string.strongbox_location_tee
        KeyLocation.SECURE_HARDWARE -> R.string.strongbox_location_secure_hardware
        KeyLocation.SOFTWARE -> R.string.strongbox_location_software
        KeyLocation.UNKNOWN -> R.string.strongbox_location_unknown
    }

@Preview(showBackground = true)
@Composable
private fun StrongBoxScreenPreview() {
    KeystoreTheme {
        StrongBoxScreen(
            state = StrongBoxUiState(
                isStrongBoxDeclared = true,
                keys = mapOf(
                    KeyTarget.REGULAR to KeyCreation.Created(KeyLocation.TRUSTED_ENVIRONMENT),
                    KeyTarget.STRONGBOX to KeyCreation.Created(KeyLocation.STRONGBOX),
                ),
                timings = mapOf(KeyTarget.REGULAR to 12.milliseconds, KeyTarget.STRONGBOX to 310.milliseconds),
            ),
            onCreateKeys = {},
            onMeasure = {},
            onDeleteKeys = {},
        )
    }
}
