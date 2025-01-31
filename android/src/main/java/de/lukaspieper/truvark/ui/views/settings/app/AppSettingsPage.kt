/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.settings.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.ui.controls.LabeledSwitch
import de.lukaspieper.truvark.ui.preview.DetailPanePreviewHost
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.theme.paddings

@Composable
fun AppSettingsPage(
    modifier: Modifier = Modifier,
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    val imagesFitScreen = viewModel.imagesFitScreen.collectAsStateWithLifecycle(false)
    val isLoggingEnabled = viewModel.isLoggingEnabled.collectAsStateWithLifecycle(false)

    AppSettingsView(
        imagesFitScreen = imagesFitScreen.value,
        updateImagesFitScreen = viewModel::applyImagesFitScreen,
        isLoggingEnabled = isLoggingEnabled.value,
        updateIsLoggingEnabled = viewModel::applyLogging,
        modifier = modifier
    )
}

@Composable
private fun AppSettingsView(
    imagesFitScreen: Boolean,
    updateImagesFitScreen: (Boolean) -> Unit,
    isLoggingEnabled: Boolean,
    updateIsLoggingEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LabeledSwitch(
            text = stringResource(R.string.fit_images_to_match_screen),
            checked = imagesFitScreen,
            onCheckedChange = updateImagesFitScreen,
            modifier = Modifier.padding(horizontal = MaterialTheme.paddings.small)
        )

        LabeledSwitch(
            text = stringResource(R.string.logging),
            checked = isLoggingEnabled,
            onCheckedChange = updateIsLoggingEnabled,
            modifier = Modifier.padding(horizontal = MaterialTheme.paddings.small)
        )
    }
}

@PagePreviews
@Composable
private fun AppSettingsViewPreview() = DetailPanePreviewHost { contentPadding ->
    AppSettingsView(
        imagesFitScreen = false,
        updateImagesFitScreen = {},
        isLoggingEnabled = false,
        updateIsLoggingEnabled = {},
        modifier = Modifier.padding(contentPadding)
    )
}
