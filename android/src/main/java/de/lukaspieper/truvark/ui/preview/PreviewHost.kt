/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.preview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.lukaspieper.truvark.ui.controls.SafeDrawingListDetailPaneScaffold
import de.lukaspieper.truvark.ui.extensions.plus
import de.lukaspieper.truvark.ui.extensions.safeDrawingEnd
import de.lukaspieper.truvark.ui.theme.AppTheme
import de.lukaspieper.truvark.ui.theme.paddings

@Composable
fun PreviewHost(
    modifier: Modifier = Modifier.fillMaxSize(),
    backgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    AppTheme {
        Surface(
            color = backgroundColor ?: MaterialTheme.colorScheme.surface,
            modifier = modifier
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DetailPanePreviewHost(
    modifier: Modifier = Modifier.fillMaxSize(),
    backgroundColor: Color? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    PreviewHost(
        modifier = modifier,
        backgroundColor = backgroundColor
    ) {
        val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Nothing>(
            initialDestinationHistory = listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail))
        )

        SafeDrawingListDetailPaneScaffold(
            scaffoldNavigator = scaffoldNavigator,
            listPaneTopAppBarTitle = "List Pane",
            listPaneContent = { },
            listPaneTopAppBarNavigationIcon = {
                IconButton(
                    onClick = { },
                    content = { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }
                )
            },
            detailPaneTopAppBarTitle = "Detail Pane",
            detailPaneContent = {
                content(
                    WindowInsets.safeDrawingEnd.asPaddingValues() + PaddingValues(MaterialTheme.paddings.large)
                )
            },
            detailPaneTopAppBarNavigationIcon = {
                IconButton(
                    onClick = { },
                    content = { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }
                )
            },
        )
    }
}
