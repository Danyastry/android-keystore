package com.danyastry.keystore.feature.attestation.ui

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
import com.danyastry.keystore.core.ui.theme.KeystoreTheme
import com.danyastry.keystore.feature.attestation.AttestationLevel
import com.danyastry.keystore.feature.attestation.AttestationReport
import com.danyastry.keystore.feature.attestation.R

@Composable
fun AttestationRoute(
    modifier: Modifier = Modifier,
    viewModel: AttestationViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AttestationScreen(state = state, onAttest = viewModel::onAttest, modifier = modifier)
}

@Composable
fun AttestationScreen(
    state: AttestationUiState,
    onAttest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.attestation_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAttest, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.attestation_run))
        }
        state.errorMessage?.let {
            Text(
                text = stringResource(R.string.attestation_failed, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.report?.let { report ->
            ChecksCard(report)
            ChainCard(report.chainSubjects)
            Text(
                text = stringResource(R.string.attestation_server_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChecksCard(report: AttestationReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = stringResource(R.string.attestation_what_it_says), style = MaterialTheme.typography.titleSmall)
            Line(R.string.attestation_key_level, stringResource(report.attestationLevel.labelRes))
            Line(R.string.attestation_keymaster, stringResource(R.string.attestation_version_at, report.keymasterVersion, stringResource(report.keymasterLevel.labelRes)))
            Line(R.string.attestation_version, report.attestationVersion.toString())
            Line(R.string.attestation_challenge, stringResource(if (report.isChallengeMatching) R.string.attestation_matches else R.string.attestation_differs))
            Line(R.string.attestation_signatures, stringResource(if (report.isChainSigned) R.string.attestation_signed else R.string.attestation_broken))
        }
    }
}

@Composable
private fun ChainCard(subjects: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.attestation_chain, subjects.size), style = MaterialTheme.typography.titleSmall)
            subjects.forEachIndexed { index, subject ->
                Column {
                    Text(
                        text = stringResource(
                            when (index) {
                                0 -> R.string.attestation_chain_leaf
                                subjects.lastIndex -> R.string.attestation_chain_root
                                else -> R.string.attestation_chain_middle
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Line(labelRes: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f))
    }
}

private val AttestationLevel.labelRes: Int
    get() = when (this) {
        AttestationLevel.SOFTWARE -> R.string.attestation_level_software
        AttestationLevel.TRUSTED_ENVIRONMENT -> R.string.attestation_level_tee
        AttestationLevel.STRONGBOX -> R.string.attestation_level_strongbox
        AttestationLevel.UNKNOWN -> R.string.attestation_level_unknown
    }

@Preview(showBackground = true)
@Composable
private fun AttestationScreenPreview() {
    KeystoreTheme {
        AttestationScreen(
            state = AttestationUiState(
                report = AttestationReport(
                    chainSubjects = listOf("CN=Android Keystore Key", "CN=Android Keystore Software Attestation Intermediate", "CN=Android Keystore Software Attestation Root"),
                    isChainSigned = true,
                    attestationVersion = 100,
                    attestationLevel = AttestationLevel.SOFTWARE,
                    keymasterVersion = 100,
                    keymasterLevel = AttestationLevel.SOFTWARE,
                    isChallengeMatching = true,
                ),
            ),
            onAttest = {},
        )
    }
}
