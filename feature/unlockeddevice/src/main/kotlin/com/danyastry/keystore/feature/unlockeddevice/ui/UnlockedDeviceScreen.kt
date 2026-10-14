package com.danyastry.keystore.feature.unlockeddevice.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.unlockeddevice.AndroidDeviceLock
import com.danyastry.keystore.feature.unlockeddevice.ProbeKey
import com.danyastry.keystore.feature.unlockeddevice.ProbeResult
import com.danyastry.keystore.feature.unlockeddevice.R

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun UnlockedDeviceRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: UnlockedDeviceViewModel = viewModel { UnlockedDeviceViewModel(AndroidDeviceLock(context.applicationContext)) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onRefresh() }

    UnlockedDeviceScreen(
        state = state,
        delaySeconds = UnlockedDeviceViewModel.DELAY_SECONDS,
        onOpenSettings = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
        onCreateKeys = viewModel::onCreateKeys,
        onDeleteKeys = viewModel::onDeleteKeys,
        onTestNow = viewModel::onTestNow,
        onTestLater = viewModel::onTestLater,
        onCancelTest = viewModel::onCancelTest,
        modifier = modifier,
    )
}

@Composable
fun UnlockedDeviceScreen(
    state: UnlockedDeviceUiState,
    delaySeconds: Int,
    onOpenSettings: () -> Unit,
    onCreateKeys: () -> Unit,
    onDeleteKeys: () -> Unit,
    onTestNow: () -> Unit,
    onTestLater: () -> Unit,
    onCancelTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusCard(hasScreenLock = state.hasScreenLock, onOpenSettings = onOpenSettings)
        KeysCard(state = state, onCreateKeys = onCreateKeys, onDeleteKeys = onDeleteKeys)
        TestCard(
            state = state,
            delaySeconds = delaySeconds,
            onTestNow = onTestNow,
            onTestLater = onTestLater,
            onCancelTest = onCancelTest,
        )
        RoundsCard(rounds = state.rounds)
    }
}

@Composable
private fun StatusCard(hasScreenLock: Boolean?, onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.unlockeddevice_status_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    when (hasScreenLock) {
                        null -> R.string.unlockeddevice_status_checking
                        true -> R.string.unlockeddevice_status_secure
                        false -> R.string.unlockeddevice_status_not_secure
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasScreenLock == false) {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.unlockeddevice_open_settings))
                }
            }
        }
    }
}

@Composable
private fun KeysCard(state: UnlockedDeviceUiState, onCreateKeys: () -> Unit, onDeleteKeys: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.unlockeddevice_keys_title),
                style = MaterialTheme.typography.titleSmall,
            )
            val lines = when {
                state.isKeyCreationFailed -> listOf(R.string.unlockeddevice_keys_failed)
                state.hasKeys -> listOf(R.string.unlockeddevice_key_regular, R.string.unlockeddevice_key_unlocked_only)
                else -> listOf(R.string.unlockeddevice_keys_missing)
            }
            lines.forEach { line ->
                Text(
                    text = stringResource(line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isKeyCreationFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateKeys, enabled = state.canCreateKeys) {
                    Text(stringResource(R.string.unlockeddevice_create_keys))
                }
                OutlinedButton(onClick = onDeleteKeys, enabled = state.hasKeys) {
                    Text(stringResource(R.string.unlockeddevice_delete_keys))
                }
            }
        }
    }
}

@Composable
private fun TestCard(
    state: UnlockedDeviceUiState,
    delaySeconds: Int,
    onTestNow: () -> Unit,
    onTestLater: () -> Unit,
    onCancelTest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.unlockeddevice_test_title),
                style = MaterialTheme.typography.titleSmall,
            )
            if (state.isCountingDown) {
                Text(
                    text = stringResource(R.string.unlockeddevice_countdown, state.secondsUntilTest),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(onClick = onCancelTest, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.unlockeddevice_cancel))
                }
            } else {
                Text(
                    text = stringResource(R.string.unlockeddevice_test_hint, delaySeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onTestLater, enabled = state.canTest) {
                        Text(stringResource(R.string.unlockeddevice_test_later, delaySeconds))
                    }
                    OutlinedButton(onClick = onTestNow, enabled = state.canTest) {
                        Text(stringResource(R.string.unlockeddevice_test_now))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundsCard(rounds: List<ProbeRound>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.unlockeddevice_rounds_title),
                style = MaterialTheme.typography.labelLarge,
            )
            if (rounds.isEmpty()) {
                Text(
                    text = stringResource(R.string.unlockeddevice_rounds_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            rounds.forEach { round ->
                Column {
                    Text(
                        text = stringResource(
                            if (round.wasDeviceLocked) R.string.unlockeddevice_round_locked else R.string.unlockeddevice_round_unlocked,
                            round.number,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    ProbeKey.entries.forEach { key ->
                        round.results[key]?.let { result ->
                            Text(
                                text = result.label(key),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result is ProbeResult.Allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProbeResult.label(key: ProbeKey): String = when (this) {
    ProbeResult.Allowed -> stringResource(
        when (key) {
            ProbeKey.REGULAR -> R.string.unlockeddevice_result_regular_allowed
            ProbeKey.UNLOCKED_ONLY -> R.string.unlockeddevice_result_unlocked_only_allowed
        },
    )
    is ProbeResult.Refused -> stringResource(
        when (key) {
            ProbeKey.REGULAR -> R.string.unlockeddevice_result_regular_refused
            ProbeKey.UNLOCKED_ONLY -> R.string.unlockeddevice_result_unlocked_only_refused
        },
        reason,
    )
}

@Preview(showBackground = true)
@Composable
private fun UnlockedDeviceScreenPreview() {
    KeystoreTheme {
        UnlockedDeviceScreen(
            state = UnlockedDeviceUiState(
                hasScreenLock = true,
                hasKeys = true,
                rounds = listOf(
                    ProbeRound(
                        number = 2,
                        wasDeviceLocked = true,
                        results = mapOf(
                            ProbeKey.REGULAR to ProbeResult.Allowed,
                            ProbeKey.UNLOCKED_ONLY to ProbeResult.Refused("UserNotAuthenticatedException"),
                        ),
                    ),
                    ProbeRound(
                        number = 1,
                        wasDeviceLocked = false,
                        results = mapOf(ProbeKey.REGULAR to ProbeResult.Allowed, ProbeKey.UNLOCKED_ONLY to ProbeResult.Allowed),
                    ),
                ),
            ),
            delaySeconds = 10,
            onOpenSettings = {},
            onCreateKeys = {},
            onDeleteKeys = {},
            onTestNow = {},
            onTestLater = {},
            onCancelTest = {},
        )
    }
}
