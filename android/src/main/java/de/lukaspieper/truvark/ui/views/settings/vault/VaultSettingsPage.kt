/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.settings.vault

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.common.constants.FixedValues.MAX_VAULT_NAME_LENGTH
import de.lukaspieper.truvark.ui.preview.DetailPanePreviewHost
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.theme.paddings
import de.lukaspieper.truvark.ui.views.settings.vault.VaultSettingsViewModel.BiometricSetupResult

@Composable
fun VaultSettingsPage(
    modifier: Modifier = Modifier,
    viewModel: VaultSettingsViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current as FragmentActivity
    val biometricsStatus = remember { viewModel.checkBiometricSupport() }
    val isVaultUsingBiometricUnlocking by viewModel.isVaultUsingBiometricUnlocking
        .collectAsStateWithLifecycle(false)

    VaultSettingsSections(
        vaultName = viewModel.vaultName,
        updateVaultName = viewModel::updateVaultName,
        biometricsStatus = biometricsStatus,
        isVaultUsingBiometricUnlocking = isVaultUsingBiometricUnlocking,
        setupBiometricUnlocking = { password ->
            viewModel.setupBiometricUnlocking(
                password = password,
                authenticateCryptoObject = { cryptoObject ->
                    authenticateCryptoObject(cryptoObject, activity)
                }
            )
        },
        modifier = modifier
    )
}

@Composable
fun VaultSettingsSections(
    vaultName: String,
    updateVaultName: (String) -> Boolean,
    biometricsStatus: Int,
    isVaultUsingBiometricUnlocking: Boolean,
    setupBiometricUnlocking: suspend (ByteArray) -> BiometricSetupResult,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = spacedBy(48.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        VaultName(
            vaultName = vaultName,
            updateVaultName = updateVaultName
        )

        BiometricsView(
            biometricsStatus = biometricsStatus,
            isVaultUsingBiometricUnlocking = isVaultUsingBiometricUnlocking,
            setupBiometricUnlocking = setupBiometricUnlocking
        )
    }
}

@Composable
fun VaultName(
    vaultName: String,
    updateVaultName: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = spacedBy(MaterialTheme.paddings.medium),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.vault_name),
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = stringResource(R.string.vault_name_change_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify
        )

        Row(
            horizontalArrangement = spacedBy(MaterialTheme.paddings.medium),
            modifier = Modifier.fillMaxWidth()
        ) {
            var editableVaultName by rememberSaveable(vaultName) { mutableStateOf(vaultName) }
            var isInputValid by rememberSaveable { mutableStateOf(true) }
            val isVaultNameEdited by remember(vaultName, editableVaultName) {
                derivedStateOf { editableVaultName != vaultName }
            }

            TextField(
                value = editableVaultName,
                onValueChange = {
                    if (it.length <= MAX_VAULT_NAME_LENGTH) {
                        editableVaultName = it
                    }
                },
                label = { Text(stringResource(R.string.vault_name)) },
                singleLine = true,
                isError = isInputValid.not(),
                supportingText = {
                    Text(
                        text = "${editableVaultName.length}/${MAX_VAULT_NAME_LENGTH}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { isInputValid = updateVaultName(editableVaultName) }),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { isInputValid = updateVaultName(editableVaultName) },
                enabled = isVaultNameEdited,
                modifier = Modifier.height(TextFieldDefaults.MinHeight)
            ) {
                Icon(if (isVaultNameEdited) Icons.Default.Save else Icons.Default.Done, null)
            }
        }
    }
}

@PagePreviews
@Composable
private fun VaultSettingsSectionsPreview() = DetailPanePreviewHost { contentPadding ->
    VaultSettingsSections(
        vaultName = "Preview vault",
        updateVaultName = { true },
        biometricsStatus = BiometricManager.BIOMETRIC_SUCCESS,
        isVaultUsingBiometricUnlocking = false,
        setupBiometricUnlocking = { BiometricSetupResult.SUCCESS },
        modifier = Modifier.padding(contentPadding)
    )
}
