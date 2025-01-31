/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import de.lukaspieper.truvark.ui.controls.SafeDrawingListDetailPaneScaffold
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.preview.PreviewHost
import de.lukaspieper.truvark.ui.theme.paddings
import de.lukaspieper.truvark.ui.views.settings.app.AppSettingsPage
import de.lukaspieper.truvark.ui.views.settings.licensing.OpenSourceLicensePage
import de.lukaspieper.truvark.ui.views.settings.vault.VaultSettingsPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsHomePage(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsMenuItems = mapOf(
        SettingsMenuItem.Key.VAULT to SettingsMenuItem.Internal(
            Icons.Default.Lock, R.string.vault, R.string.settings_description_vault
        ),
        SettingsMenuItem.Key.APP to SettingsMenuItem.Internal(
            Icons.Default.AppSettingsAlt, R.string.app, R.string.settings_description_app
        ),
        SettingsMenuItem.Key.OSS_LICENSES to SettingsMenuItem.Internal(
            Icons.Default.Copyright, R.string.settings_licensing
        ),
        SettingsMenuItem.Key.SOURCE_CODE to SettingsMenuItem.External(
            Icons.Default.Code, R.string.source_code, null, Uri.parse("https://github.com/lukaspieper/Truvark")
        ),
        SettingsMenuItem.Key.PRIVACY_POLICY to SettingsMenuItem.External(
            Icons.Default.PrivacyTip,
            R.string.privacy_policy,
            null,
            Uri.parse("https://truvark.lukaspieper.de/en/privacy.html")
        )
    )

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<SettingsMenuItem.Key>()
    val selectedMenuItem by remember(scaffoldNavigator.currentDestination) {
        derivedStateOf { settingsMenuItems[scaffoldNavigator.currentDestination?.contentKey] }
    }

    LaunchedEffect(scaffoldNavigator) {
        // Do initial navigation when both panes are visible. Note that primary maps to the detail pane.
        val detailPaneState = scaffoldNavigator.scaffoldState.currentState.primary
        if (scaffoldNavigator.currentDestination?.contentKey == null && detailPaneState == PaneAdaptedValue.Expanded) {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, SettingsMenuItem.Key.VAULT)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    SafeDrawingListDetailPaneScaffold(
        scaffoldNavigator = scaffoldNavigator,
        modifier = modifier,
        listPaneTopAppBarTitle = stringResource(R.string.settings),
        listPaneTopAppBarNavigationIcon = {
            IconButton(
                onClick = navigateBack,
                content = { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }
            )
        },
        listPaneContent = { contentPadding ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.paddings.small),
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = settingsMenuItems.keys.toList(),
                    key = { key -> key }
                ) { key ->
                    val settingsMenuItem by remember(key) { derivedStateOf { settingsMenuItems.getValue(key) } }
                    val isSelected by remember(settingsMenuItem, selectedMenuItem) {
                        derivedStateOf { settingsMenuItem == selectedMenuItem }
                    }
                    val onClick: () -> Unit = {
                        when (settingsMenuItem) {
                            is SettingsMenuItem.Internal -> {
                                coroutineScope.launch {
                                    scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, key)
                                }
                            }

                            is SettingsMenuItem.External -> {
                                try {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        (settingsMenuItem as SettingsMenuItem.External).uri
                                    )
                                    context.startActivity(browserIntent, Bundle.EMPTY)
                                } catch (e: Exception) {
                                    logcat("SettingsHomePage", LogPriority.WARN) { e.asLog() }
                                }
                            }
                        }
                    }

                    SettingsButton(
                        icon = settingsMenuItem.icon,
                        title = stringResource(settingsMenuItem.title),
                        description = settingsMenuItem.description?.let { stringResource(it) },
                        isExternal = settingsMenuItem is SettingsMenuItem.External,
                        isSelected = isSelected,
                        onClick = onClick
                    )
                }
            }
        },
        detailPaneContent = { contentPadding ->
            scaffoldNavigator.currentDestination?.contentKey?.let { settingsMenuItem ->
                when (settingsMenuItem) {
                    SettingsMenuItem.Key.VAULT -> VaultSettingsPage(modifier = Modifier.padding(contentPadding))
                    SettingsMenuItem.Key.APP -> AppSettingsPage(modifier = Modifier.padding(contentPadding))
                    SettingsMenuItem.Key.OSS_LICENSES -> OpenSourceLicensePage(
                        modifier = Modifier.padding(contentPadding)
                    )

                    else -> {
                        // Show nothing.
                    }
                }
            }
        },
        detailPaneTopAppBarTitle = selectedMenuItem?.let { stringResource(it.title) } ?: "",
        detailPaneTopAppBarNavigationIcon = {
            IconButton(
                onClick = { coroutineScope.launch { scaffoldNavigator.navigateBack() } },
                content = { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }
            )
        }
    )
}

@Composable
fun SettingsButton(
    icon: ImageVector,
    title: String,
    description: String?,
    isExternal: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isSelected,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = CardDefaults.shape,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(icon, null)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = MaterialTheme.paddings.large)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isExternal) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
        }
    }
}

@PagePreviews
@Composable
private fun SettingsViewPreview() = PreviewHost {
    SettingsHomePage(navigateBack = {})
}
