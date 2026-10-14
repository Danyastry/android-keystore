package com.danyastry.keystore.feature.biometric.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
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
import com.danyastry.keystore.feature.biometric.AndroidBiometricStatus
import com.danyastry.keystore.feature.biometric.BiometricAvailability
import com.danyastry.keystore.feature.biometric.R
import com.danyastry.keystore.feature.biometric.isCancellation

@Composable
fun BiometricRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val viewModel: BiometricViewModel = viewModel { BiometricViewModel(AndroidBiometricStatus(context.applicationContext)) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val title = stringResource(R.string.biometric_prompt_title)
    val encryptSubtitle = stringResource(R.string.biometric_prompt_encrypt)
    val decryptSubtitle = stringResource(R.string.biometric_prompt_decrypt)
    val cancel = stringResource(R.string.biometric_prompt_cancel)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onRefresh() }

    LaunchedEffect(viewModel, activity) {
        viewModel.authRequests.collect { request ->
            val subtitle = when (request.operation) {
                BiometricOperation.ENCRYPT -> encryptSubtitle
                BiometricOperation.DECRYPT -> decryptSubtitle
            }
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancel)
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .build()
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.onAuthenticated(request)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.onAuthFailed(canceled = isCancellation(errorCode), message = errString.toString())
                }
            }
            BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
                .authenticate(info, BiometricPrompt.CryptoObject(request.cipher))
        }
    }

    BiometricScreen(
        state = state,
        onOpenSettings = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
        onCreateKey = viewModel::onCreateKey,
        onDeleteKey = viewModel::onDeleteKey,
        onInputChange = viewModel::onInputChange,
        onEncrypt = viewModel::onEncrypt,
        onEncryptWithoutAuth = viewModel::onEncryptWithoutAuth,
        onDecrypt = viewModel::onDecrypt,
        modifier = modifier,
    )
}

@Composable
fun BiometricScreen(
    state: BiometricUiState,
    onOpenSettings: () -> Unit,
    onCreateKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onInputChange: (String) -> Unit,
    onEncrypt: () -> Unit,
    onEncryptWithoutAuth: () -> Unit,
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
        KeyCard(state = state, onCreateKey = onCreateKey, onDeleteKey = onDeleteKey)
        CipherTextField(
            value = state.input,
            onValueChange = onInputChange,
            label = stringResource(R.string.biometric_input_label),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEncrypt, enabled = state.canEncrypt) {
                Text(stringResource(R.string.biometric_encrypt))
            }
            OutlinedButton(onClick = onEncryptWithoutAuth, enabled = state.canEncrypt) {
                Text(stringResource(R.string.biometric_encrypt_without_auth))
            }
        }
        state.error?.let { error ->
            Text(
                text = error.message(state.authMessage),
                style = MaterialTheme.typography.bodyMedium,
                color = if (error == BiometricError.CANCELED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        SealedCard(state = state, onDecrypt = onDecrypt)
    }
}

@Composable
private fun SealedCard(state: BiometricUiState, onDecrypt: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.biometric_sealed_label),
                style = MaterialTheme.typography.labelLarge,
            )
            SelectionContainer {
                Text(
                    text = state.sealed.ifEmpty { stringResource(R.string.biometric_sealed_empty) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
            Button(onClick = onDecrypt, enabled = state.canDecrypt, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.biometric_decrypt))
            }
            if (state.decrypted.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.biometric_decrypted, state.decrypted),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(availability: BiometricAvailability?, onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.biometric_status_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    when (availability) {
                        null -> R.string.biometric_status_checking
                        BiometricAvailability.AVAILABLE -> R.string.biometric_status_available
                        BiometricAvailability.NONE_ENROLLED -> R.string.biometric_status_none_enrolled
                        BiometricAvailability.NO_HARDWARE -> R.string.biometric_status_no_hardware
                        BiometricAvailability.UNAVAILABLE -> R.string.biometric_status_unavailable
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (availability == BiometricAvailability.NONE_ENROLLED) {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.biometric_open_settings))
                }
            }
        }
    }
}

@Composable
private fun KeyCard(state: BiometricUiState, onCreateKey: () -> Unit, onDeleteKey: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.biometric_key_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(if (state.hasKey) R.string.biometric_key_ready else R.string.biometric_key_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateKey, enabled = state.canCreateKey) {
                    Text(stringResource(R.string.biometric_create_key))
                }
                OutlinedButton(onClick = onDeleteKey, enabled = state.hasKey) {
                    Text(stringResource(R.string.biometric_delete_key))
                }
            }
        }
    }
}

@Composable
private fun BiometricError.message(authMessage: String): String = when (this) {
    BiometricError.NO_KEY -> stringResource(R.string.biometric_error_no_key)
    BiometricError.KEY_CREATION_FAILED -> stringResource(R.string.biometric_error_key_creation_failed)
    BiometricError.KEY_INVALIDATED -> stringResource(R.string.biometric_error_key_invalidated)
    BiometricError.KEY_REFUSED -> stringResource(R.string.biometric_error_key_refused)
    BiometricError.CANCELED -> stringResource(R.string.biometric_error_canceled)
    BiometricError.AUTH_FAILED -> stringResource(R.string.biometric_error_auth_failed, authMessage)
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("Biometric screen must be hosted in a FragmentActivity")
}

@Preview(showBackground = true)
@Composable
private fun BiometricScreenPreview() {
    KeystoreTheme {
        BiometricScreen(
            state = BiometricUiState(
                availability = BiometricAvailability.AVAILABLE,
                hasKey = true,
                input = "Attack at dawn",
                sealed = "q83vEjRWeJCrze8SNFZ4kKvN7xI0VniQ",
                error = BiometricError.KEY_REFUSED,
            ),
            onOpenSettings = {},
            onCreateKey = {},
            onDeleteKey = {},
            onInputChange = {},
            onEncrypt = {},
            onEncryptWithoutAuth = {},
            onDecrypt = {},
        )
    }
}
