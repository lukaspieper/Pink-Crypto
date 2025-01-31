/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.launcher

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.authenticateWithClass3Biometrics
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import de.lukaspieper.truvark.Page
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import de.lukaspieper.truvark.ui.controls.PasswordField
import de.lukaspieper.truvark.ui.controls.SafeDrawingScaffold
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.preview.PreviewHost
import de.lukaspieper.truvark.ui.theme.paddings
import de.lukaspieper.truvark.ui.views.launcher.LauncherViewModel.LauncherState.DIRECTORY_SELECTION
import de.lukaspieper.truvark.ui.views.launcher.LauncherViewModel.LauncherState.DONE
import de.lukaspieper.truvark.ui.views.launcher.LauncherViewModel.LauncherState.NONE
import de.lukaspieper.truvark.ui.views.launcher.LauncherViewModel.LauncherState.PROCESSING
import de.lukaspieper.truvark.ui.views.launcher.LauncherViewModel.LauncherState.VAULT_CREATION

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LauncherPage(
    navigateAndClearBackStack: (Page) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current as FragmentActivity

    LaunchedEffect(viewModel.state, navigateAndClearBackStack) {
        if (viewModel.state == DONE) {
            navigateAndClearBackStack(Page.Browser)
        }
    }

    val notificationPermissionState = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        }

        else -> null
    }

    LauncherView(
        notificationPermissionState = notificationPermissionState,
        state = viewModel.state,
        updateState = { viewModel.state = it },
        vaultDisplayName = viewModel.vaultConfig?.displayName ?: "",
        biometricUnlockingSupported = viewModel.supportsBiometricUnlocking,
        unlockingErrorText = viewModel.unlockingErrorText,
        unlockVaultWithPassword = viewModel::unlockVaultWithPassword,
        showBiometricPrompt = { showBiometricPrompt(activity, viewModel) },
        setupDialog = {
            SetupDialog(
                state = viewModel.state,
                updateState = { viewModel.state = it },
                inspectDirectory = viewModel::inspectDirectory,
                createVault = viewModel::createVault
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LauncherView(
    notificationPermissionState: PermissionState?,
    state: LauncherViewModel.LauncherState,
    vaultDisplayName: String,
    biometricUnlockingSupported: Boolean,
    unlockingErrorText: Int?,
    updateState: (LauncherViewModel.LauncherState) -> Unit,
    unlockVaultWithPassword: (ByteArray) -> Unit,
    showBiometricPrompt: () -> Unit,
    setupDialog: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    SafeDrawingScaffold(
        largeTopAppBarTitle = stringResource(R.string.app_name),
        modifier = modifier,
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier.sizeIn(maxWidth = 550.dp)
            ) {
                if (notificationPermissionState?.status is PermissionStatus.Denied) {
                    NotificationPermissionView(notificationPermissionState)
                } else {
                    if (vaultDisplayName.isNotBlank()) {
                        VaultUnlockCardView(
                            vaultDisplayName = vaultDisplayName,
                            biometricUnlockingSupported = biometricUnlockingSupported,
                            unlockingErrorText = unlockingErrorText,
                            unlockVaultWithPassword = unlockVaultWithPassword,
                            showBiometricPrompt = showBiometricPrompt
                        )
                    } else {
                        NoVaultCardView()
                    }
                }
            }

            if (notificationPermissionState?.status !is PermissionStatus.Denied) {
                OutlinedButton(
                    onClick = { updateState(DIRECTORY_SELECTION) },
                    modifier = Modifier
                        .align(CenterHorizontally)
                        .padding(top = 32.dp)
                ) {
                    Icon(Icons.Outlined.FolderOpen, null)
                    Text(
                        text = stringResource(R.string.create_or_open_vault),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (state in listOf(DIRECTORY_SELECTION, VAULT_CREATION, PROCESSING)) {
                    setupDialog()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NotificationPermissionView(notificationPermissionState: PermissionState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var requestPermissionCounter by rememberSaveable { mutableIntStateOf(0) }
    var permissionRequestCompleted by rememberSaveable { mutableStateOf(false) }

    with(notificationPermissionState) {
        LaunchedEffect(status) {
            if (requestPermissionCounter > 0) {
                permissionRequestCompleted = true
            }
        }

        Column(
            modifier = modifier.padding(all = MaterialTheme.paddings.large),
            verticalArrangement = spacedBy(MaterialTheme.paddings.medium)
        ) {
            Text(
                text = stringResource(R.string.notification_permission_description),
                textAlign = TextAlign.Justify,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    if (requestPermissionCounter > 1 || (permissionRequestCompleted && !status.shouldShowRationale)) {
                        context.startActivity(
                            Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    } else {
                        launchPermissionRequest()
                        requestPermissionCounter++
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        requestPermissionCounter > 1 || (permissionRequestCompleted && !status.shouldShowRationale) -> {
                            stringResource(R.string.open_app_settings)
                        }

                        else -> stringResource(R.string.grant_permission)
                    }
                )
            }
        }
    }
}

@Composable
private fun NoVaultCardView(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = spacedBy(MaterialTheme.paddings.large),
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(MaterialTheme.paddings.large)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_locker),
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .sizeIn(minHeight = 80.dp, maxWidth = 80.dp)
        )
        Text(
            text = stringResource(R.string.no_existing_vault_info),
            textAlign = TextAlign.Justify,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun VaultUnlockCardView(
    vaultDisplayName: String,
    biometricUnlockingSupported: Boolean,
    unlockingErrorText: Int?,
    unlockVaultWithPassword: (ByteArray) -> Unit,
    showBiometricPrompt: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(100.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_locker),
            contentDescription = null,
            modifier = Modifier.padding(MaterialTheme.paddings.extraLarge)
        )
        Text(
            text = vaultDisplayName,
            modifier = Modifier.padding(end = MaterialTheme.paddings.large),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineLarge
        )
    }

    Column(
        verticalArrangement = spacedBy(MaterialTheme.paddings.medium),
        modifier = Modifier.padding(MaterialTheme.paddings.large)
    ) {
        PasswordUnlockView(unlockVaultWithPassword, unlockingErrorText)

        if (biometricUnlockingSupported) {
            Button(
                onClick = showBiometricPrompt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.biometric_unlocking),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(Icons.Default.Fingerprint, null)
            }
        }
    }
}

@Composable
private fun PasswordUnlockView(
    unlockWithPassword: (ByteArray) -> Unit,
    @StringRes errorMessageResource: Int?
) {
    if (errorMessageResource != null && errorMessageResource != R.string.incorrect_password) {
        Surface(
            shape = CardDefaults.shape,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(errorMessageResource),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(MaterialTheme.paddings.small)
            )
        }
    }

    Row(
        horizontalArrangement = spacedBy(MaterialTheme.paddings.medium),
        modifier = Modifier.fillMaxWidth()
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        var password by rememberSaveable { mutableStateOf("") }
        PasswordField(
            value = password,
            onValueChange = { password = it },
            onKeyboardDone = {
                keyboardController?.hide()
                unlockWithPassword(password.toByteArray())
            },
            passwordIsIncorrect = errorMessageResource == R.string.incorrect_password,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = {
                keyboardController?.hide()
                unlockWithPassword(password.toByteArray())
            },
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .height(TextFieldDefaults.MinHeight)
        ) {
            Icon(Icons.Default.LockOpen, null)
        }
    }
}

private fun showBiometricPrompt(activity: FragmentActivity, viewModel: LauncherViewModel) {
    val callback = object : AuthPromptCallback() {

        override fun onAuthenticationError(activity: FragmentActivity?, errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(activity, errorCode, errString)
            logcat(LogPriority.WARN) { "Biometric unlocking failed: $errorCode '$errString'" }

            val errorCodesCausedByUser = listOf(
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_CANCELED
            )
            if (!errorCodesCausedByUser.contains(errorCode)) {
                viewModel.disableBiometricUnlockingBecauseOfError()
            }
        }

        override fun onAuthenticationSucceeded(
            activity: FragmentActivity?,
            result: BiometricPrompt.AuthenticationResult
        ) {
            super.onAuthenticationSucceeded(activity, result)

            result.cryptoObject?.cipher?.let { cipher ->
                viewModel.unlockWithCipher(cipher)
            }
        }
    }

    try {
        val cryptoObject = viewModel.getCryptoObject()
        activity.authenticateWithClass3Biometrics(
            crypto = cryptoObject,
            title = activity.getString(R.string.biometric_unlocking),
            negativeButtonText = activity.getString(R.string.cancel),
            callback = callback
        )
    } catch (e: Exception) {
        logcat("LauncherPage", LogPriority.ERROR) { e.asLog() }
        viewModel.disableBiometricUnlockingBecauseOfError()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@PagePreviews
@Composable
private fun NoNotificationPermissionPreview() = PreviewHost {
    LauncherView(
        notificationPermissionState = object : PermissionState {
            override val permission: String = ""
            override val status: PermissionStatus = PermissionStatus.Denied(false)

            override fun launchPermissionRequest() {
                // Previews do not need implementations.
            }
        },
        state = NONE,
        vaultDisplayName = "",
        biometricUnlockingSupported = false,
        unlockingErrorText = null,
        updateState = {},
        unlockVaultWithPassword = {},
        showBiometricPrompt = {},
        setupDialog = {}
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@PagePreviews
@Composable
private fun NoVaultSelectedPreview() = PreviewHost {
    LauncherView(
        notificationPermissionState = null,
        state = NONE,
        vaultDisplayName = "",
        biometricUnlockingSupported = false,
        unlockingErrorText = null,
        updateState = {},
        unlockVaultWithPassword = {},
        showBiometricPrompt = {},
        setupDialog = {}
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@PagePreviews
@Composable
private fun VaultSelectedPreview() = PreviewHost {
    LauncherView(
        notificationPermissionState = null,
        state = NONE,
        vaultDisplayName = "Vault",
        biometricUnlockingSupported = true,
        unlockingErrorText = null,
        updateState = {},
        unlockVaultWithPassword = {},
        showBiometricPrompt = {},
        setupDialog = {}
    )
}
