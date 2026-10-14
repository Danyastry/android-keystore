package com.danyastry.keystore.feature.authtimeout.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.keystore.core.ui.components.CipherTextField
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.authtimeout.AndroidCredentialStatus
import com.danyastry.keystore.feature.authtimeout.AuthTimeoutKeys
import com.danyastry.keystore.feature.authtimeout.CredentialAvailability
import com.danyastry.keystore.feature.authtimeout.R
import com.danyastry.keystore.feature.authtimeout.isCancellation

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AuthTimeoutRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val viewModel: AuthTimeoutViewModel = viewModel { AuthTimeoutViewModel(AndroidCredentialStatus(context.applicationContext)) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val title = stringResource(R.string.authtimeout_prompt_title)
    val subtitle = stringResource(R.string.authtimeout_prompt_subtitle)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onRefresh() }

    LaunchedEffect(viewModel, activity) {
        viewModel.unlockRequests.collect {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(AndroidCredentialStatus.AUTHENTICATORS)
                .build()
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.onAuthFailed(canceled = isCancellation(errorCode), message = errString.toString())
                }
            }
            BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
        }
    }

    AuthTimeoutScreen(
        state = state,
        timeoutSeconds = AuthTimeoutKeys.TIMEOUT_SECONDS,
        onOpenSettings = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
        onCreateKey = viewModel::onCreateKey,
        onDeleteKey = viewModel::onDeleteKey,
        onUnlock = viewModel::onUnlock,
        onInputChange = viewModel::onInputChange,
        onEncrypt = viewModel::onEncrypt,
        onDecrypt = viewModel::onDecrypt,
        modifier = modifier,
    )
}

@Composable
fun AuthTimeoutScreen(
    state: AuthTimeoutUiState,
    timeoutSeconds: Int,
    onOpenSettings: () -> Unit,
    onCreateKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onUnlock: () -> Unit,
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
        StatusCard(availability = state.availability, onOpenSettings = onOpenSettings)
        KeyCard(state = state, timeoutSeconds = timeoutSeconds, onCreateKey = onCreateKey, onDeleteKey = onDeleteKey)
        WindowCard(state = state, onUnlock = onUnlock)
        CipherTextField(
            value = state.input,
            onValueChange = onInputChange,
            label = stringResource(R.string.authtimeout_input_label),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEncrypt, enabled = state.canEncrypt) {
                Text(stringResource(R.string.authtimeout_encrypt))
            }
            OutlinedButton(onClick = onDecrypt, enabled = state.canDecrypt) {
                Text(stringResource(R.string.authtimeout_decrypt))
            }
        }
        state.error?.let { error ->
            Text(
                text = error.message(state.authMessage),
                style = MaterialTheme.typography.bodyMedium,
                color = if (error == AuthTimeoutError.CANCELED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        SealedCard(state = state)
        AttemptsCard(attempts = state.attempts)
    }
}

@Composable
private fun StatusCard(availability: CredentialAvailability?, onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.authtimeout_status_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    when (availability) {
                        null -> R.string.authtimeout_status_checking
                        CredentialAvailability.AVAILABLE -> R.string.authtimeout_status_available
                        CredentialAvailability.NO_LOCK_SCREEN -> R.string.authtimeout_status_no_lock_screen
                        CredentialAvailability.UNAVAILABLE -> R.string.authtimeout_status_unavailable
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (availability == CredentialAvailability.NO_LOCK_SCREEN) {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.authtimeout_open_settings))
                }
            }
        }
    }
}

@Composable
private fun KeyCard(state: AuthTimeoutUiState, timeoutSeconds: Int, onCreateKey: () -> Unit, onDeleteKey: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.authtimeout_key_title, timeoutSeconds),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(if (state.hasKey) R.string.authtimeout_key_ready else R.string.authtimeout_key_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateKey, enabled = state.canCreateKey) {
                    Text(stringResource(R.string.authtimeout_create_key))
                }
                OutlinedButton(onClick = onDeleteKey, enabled = state.hasKey) {
                    Text(stringResource(R.string.authtimeout_delete_key))
                }
            }
        }
    }
}

@Composable
private fun WindowCard(state: AuthTimeoutUiState, onUnlock: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (state.isUnlocked) {
                    stringResource(R.string.authtimeout_unlocked, state.secondsLeft)
                } else {
                    stringResource(R.string.authtimeout_locked)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.authtimeout_window_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onUnlock, enabled = state.canUnlock, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.authtimeout_unlock))
            }
        }
    }
}

@Composable
private fun SealedCard(state: AuthTimeoutUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.authtimeout_sealed_label),
                style = MaterialTheme.typography.labelLarge,
            )
            SelectionContainer {
                Text(
                    text = state.sealed.ifEmpty { stringResource(R.string.authtimeout_sealed_empty) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
            if (state.decrypted.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.authtimeout_decrypted, state.decrypted),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun AttemptsCard(attempts: List<Attempt>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.authtimeout_attempts_title),
                style = MaterialTheme.typography.labelLarge,
            )
            if (attempts.isEmpty()) {
                Text(
                    text = stringResource(R.string.authtimeout_attempts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            attempts.forEach { attempt ->
                Text(
                    text = attempt.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (attempt.isAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun Attempt.label(): String {
    val operation = stringResource(
        when (operation) {
            TimedOperation.ENCRYPT -> R.string.authtimeout_operation_encrypt
            TimedOperation.DECRYPT -> R.string.authtimeout_operation_decrypt
        },
    )
    val seconds = secondsSinceUnlock
    return when {
        seconds == null && isAllowed -> stringResource(R.string.authtimeout_attempt_allowed_no_unlock, operation)
        seconds == null -> stringResource(R.string.authtimeout_attempt_refused_no_unlock, operation)
        isAllowed -> stringResource(R.string.authtimeout_attempt_allowed, operation, seconds.toInt())
        else -> stringResource(R.string.authtimeout_attempt_refused, operation, seconds.toInt())
    }
}

@Composable
private fun AuthTimeoutError.message(authMessage: String): String = when (this) {
    AuthTimeoutError.NO_KEY -> stringResource(R.string.authtimeout_error_no_key)
    AuthTimeoutError.KEY_CREATION_FAILED -> stringResource(R.string.authtimeout_error_key_creation_failed)
    AuthTimeoutError.KEY_LOCKED -> stringResource(R.string.authtimeout_error_key_locked)
    AuthTimeoutError.CANCELED -> stringResource(R.string.authtimeout_error_canceled)
    AuthTimeoutError.AUTH_FAILED -> stringResource(R.string.authtimeout_error_auth_failed, authMessage)
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("Unlock window screen must be hosted in a FragmentActivity")
}

@Preview(showBackground = true)
@Composable
private fun AuthTimeoutScreenPreview() {
    KeystoreTheme {
        AuthTimeoutScreen(
            state = AuthTimeoutUiState(
                availability = CredentialAvailability.AVAILABLE,
                hasKey = true,
                input = "Attack at dawn",
                secondsLeft = 9,
                attempts = listOf(
                    Attempt(TimedOperation.ENCRYPT, isAllowed = false, secondsSinceUnlock = 17),
                    Attempt(TimedOperation.ENCRYPT, isAllowed = true, secondsSinceUnlock = 6),
                ),
            ),
            timeoutSeconds = 15,
            onOpenSettings = {},
            onCreateKey = {},
            onDeleteKey = {},
            onUnlock = {},
            onInputChange = {},
            onEncrypt = {},
            onDecrypt = {},
        )
    }
}
