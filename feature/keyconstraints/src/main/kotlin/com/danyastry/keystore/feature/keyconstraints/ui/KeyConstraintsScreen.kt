package com.danyastry.keystore.feature.keyconstraints.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.keystore.core.ui.RequirementBadge
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.keyconstraints.R
import com.danyastry.keystore.feature.keyconstraints.UseResult

@Composable
fun KeyConstraintsRoute(
    modifier: Modifier = Modifier,
    viewModel: KeyConstraintsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    KeyConstraintsScreen(
        state = state,
        onCreateWindowKey = viewModel::onCreateWindowKey,
        onUseWindowKey = viewModel::onUseWindowKey,
        onDeleteWindowKey = viewModel::onDeleteWindowKey,
        onCreateLimitedKey = viewModel::onCreateLimitedKey,
        onUseLimitedKey = viewModel::onUseLimitedKey,
        onDeleteLimitedKey = viewModel::onDeleteLimitedKey,
        modifier = modifier,
    )
}

@Composable
fun KeyConstraintsScreen(
    state: KeyConstraintsUiState,
    onCreateWindowKey: () -> Unit,
    onUseWindowKey: () -> Unit,
    onDeleteWindowKey: () -> Unit,
    onCreateLimitedKey: () -> Unit,
    onUseLimitedKey: () -> Unit,
    onDeleteLimitedKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WindowCard(window = state.window, onCreate = onCreateWindowKey, onUse = onUseWindowKey, onDelete = onDeleteWindowKey)
        LimitCard(limit = state.limit, onCreate = onCreateLimitedKey, onUse = onUseLimitedKey, onDelete = onDeleteLimitedKey)
    }
}

@Composable
private fun WindowCard(window: WindowState, onCreate: () -> Unit, onUse: () -> Unit, onDelete: () -> Unit) {
    ConstraintCard(
        title = stringResource(R.string.keyconstraints_window_title),
        description = stringResource(
            R.string.keyconstraints_window_description,
            KeyConstraintsViewModel.VALID_FROM_SECONDS.toInt(),
            KeyConstraintsViewModel.VALID_UNTIL_SECONDS.toInt(),
        ),
        status = when (window.phase) {
            null -> stringResource(R.string.keyconstraints_window_missing)
            WindowPhase.NOT_YET_VALID -> stringResource(R.string.keyconstraints_window_not_yet_valid, window.secondsToNextPhase.toInt())
            WindowPhase.VALID -> stringResource(R.string.keyconstraints_window_valid, window.secondsToNextPhase.toInt())
            WindowPhase.EXPIRED -> stringResource(R.string.keyconstraints_window_expired)
        },
        isStatusGood = window.phase == WindowPhase.VALID,
        isCreationFailed = window.isCreationFailed,
        canUse = window.hasKey,
        onCreate = onCreate,
        onUse = onUse,
        onDelete = onDelete,
        attempts = window.attempts.map {
            stringResource(R.string.keyconstraints_window_attempt, it.secondsAfterCreation.toInt(), it.result.label()) to it.result
        },
    )
}

@Composable
private fun LimitCard(limit: LimitState, onCreate: () -> Unit, onUse: () -> Unit, onDelete: () -> Unit) {
    ConstraintCard(
        title = stringResource(R.string.keyconstraints_limit_title),
        description = stringResource(R.string.keyconstraints_limit_description, KeyConstraintsViewModel.MAX_USES),
        status = when {
            !limit.isSupported -> stringResource(R.string.keyconstraints_limit_unsupported)
            limit.hasKey -> stringResource(R.string.keyconstraints_limit_uses_left, limit.usesLeft)
            limit.usesMade > 0 -> stringResource(R.string.keyconstraints_limit_gone)
            else -> stringResource(R.string.keyconstraints_limit_missing)
        },
        isStatusGood = limit.hasKey,
        isCreationFailed = limit.isCreationFailed,
        canCreate = limit.isSupported,
        canUse = limit.hasKey || limit.usesMade > 0,
        onCreate = onCreate,
        onUse = onUse,
        onDelete = onDelete,
        attempts = limit.attempts.map { stringResource(R.string.keyconstraints_limit_attempt, it.number, it.result.label()) to it.result },
        badge = { RequirementBadge(minSdk = KeyConstraintsViewModel.LIMIT_MIN_SDK, isSupported = limit.isSupported) },
    )
}

@Composable
private fun ConstraintCard(
    title: String,
    description: String,
    status: String,
    isStatusGood: Boolean,
    isCreationFailed: Boolean,
    canUse: Boolean,
    onCreate: () -> Unit,
    onUse: () -> Unit,
    onDelete: () -> Unit,
    attempts: List<Pair<String, UseResult>>,
    canCreate: Boolean = true,
    badge: @Composable () -> Unit = {},
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                badge()
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                color = if (isStatusGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (isCreationFailed) {
                Text(
                    text = stringResource(R.string.keyconstraints_creation_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreate, enabled = canCreate) {
                    Text(stringResource(R.string.keyconstraints_create))
                }
                OutlinedButton(onClick = onUse, enabled = canUse) {
                    Text(stringResource(R.string.keyconstraints_use))
                }
                OutlinedButton(onClick = onDelete, enabled = canUse) {
                    Text(stringResource(R.string.keyconstraints_delete))
                }
            }
            attempts.forEach { (text, result) ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result is UseResult.Allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun UseResult.label(): String = when (this) {
    UseResult.Allowed -> stringResource(R.string.keyconstraints_result_allowed)
    is UseResult.Refused -> stringResource(R.string.keyconstraints_result_refused, reason)
    UseResult.Missing -> stringResource(R.string.keyconstraints_result_missing)
}

@Preview(showBackground = true)
@Composable
private fun KeyConstraintsScreenPreview() {
    KeystoreTheme {
        KeyConstraintsScreen(
            state = KeyConstraintsUiState(
                window = WindowState(
                    hasKey = true,
                    secondsSinceCreation = 14,
                    attempts = listOf(
                        WindowAttempt(12, UseResult.Allowed),
                        WindowAttempt(4, UseResult.Refused("KeyNotYetValidException")),
                    ),
                ),
                limit = LimitState(
                    isSupported = true,
                    hasKey = false,
                    usesMade = 4,
                    attempts = listOf(LimitAttempt(4, UseResult.Missing), LimitAttempt(3, UseResult.Allowed)),
                ),
            ),
            onCreateWindowKey = {},
            onUseWindowKey = {},
            onDeleteWindowKey = {},
            onCreateLimitedKey = {},
            onUseLimitedKey = {},
            onDeleteLimitedKey = {},
        )
    }
}
