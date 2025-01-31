/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.controls

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import de.lukaspieper.truvark.ui.extensions.plus
import de.lukaspieper.truvark.ui.extensions.safeDrawingEnd
import de.lukaspieper.truvark.ui.extensions.safeDrawingEndTop
import de.lukaspieper.truvark.ui.extensions.safeDrawingStart
import de.lukaspieper.truvark.ui.extensions.safeDrawingStartTop
import de.lukaspieper.truvark.ui.extensions.safeDrawingTopAppBar
import de.lukaspieper.truvark.ui.theme.paddings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeDrawingScaffold(
    largeTopAppBarTitle: String,
    modifier: Modifier = Modifier,
    largeTopAppBarActions: @Composable RowScope.() -> Unit = {},
    largeTopAppBarNavigationIcon: @Composable () -> Unit = {},
    bottomOverlay: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier
            .fillMaxSize() // https://stackoverflow.com/a/76916130
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                windowInsets = WindowInsets.safeDrawingTopAppBar,
                title = {
                    Text(
                        text = largeTopAppBarTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    Row {
                        largeTopAppBarActions()
                    }
                },
                navigationIcon = largeTopAppBarNavigationIcon
            )
        },
        content = {
            Box {
                val paddingValues = it + PaddingValues(MaterialTheme.paddings.large)
                content(paddingValues)

                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(paddingValues)
                ) {
                    bottomOverlay()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SafeDrawingListDetailPaneScaffold(
    scaffoldNavigator: ThreePaneScaffoldNavigator<*>,
    listPaneTopAppBarTitle: String,
    listPaneContent: @Composable (PaddingValues) -> Unit,
    detailPaneTopAppBarTitle: String,
    detailPaneContent: @Composable (PaddingValues) -> Unit,
    modifier: Modifier = Modifier,
    listPaneTopAppBarNavigationIcon: @Composable () -> Unit = {},
    detailPaneTopAppBarNavigationIcon: @Composable () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val isTwoPane = remember(scaffoldNavigator.scaffoldState.currentState) {
        with(scaffoldNavigator.scaffoldState.currentState) {
            primary == PaneAdaptedValue.Expanded && secondary == PaneAdaptedValue.Expanded
        }
    }

    BackHandler(scaffoldNavigator.canNavigateBack()) {
        coroutineScope.launch { scaffoldNavigator.navigateBack() }
    }

    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(), // https://stackoverflow.com/a/76916130
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        listPane = {
            AnimatedPane(
                // Match NavHost transitions
                enterTransition = fadeIn(animationSpec = tween(700)),
                exitTransition = fadeOut(animationSpec = tween(700)),
            ) {
                Column {
                    TopAppBar(
                        windowInsets = WindowInsets.safeDrawingStartTop,
                        title = {
                            Text(
                                text = listPaneTopAppBarTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = listPaneTopAppBarNavigationIcon
                    )

                    val padding = when {
                        isTwoPane -> PaddingValues(
                            start = MaterialTheme.paddings.large + MaterialTheme.paddings.extraSmall,
                            top = MaterialTheme.paddings.large,
                            bottom = MaterialTheme.paddings.large
                        )

                        else -> PaddingValues(MaterialTheme.paddings.medium)
                    }
                    listPaneContent(WindowInsets.safeDrawingStart.asPaddingValues() + padding)
                }
            }
        },
        detailPane = {
            AnimatedPane(
                // Match NavHost transitions
                enterTransition = fadeIn(animationSpec = tween(700)),
                exitTransition = fadeOut(animationSpec = tween(700)),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = when {
                        isTwoPane -> MaterialTheme.colorScheme.surfaceContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                ) {
                    Column {
                        TopAppBar(
                            windowInsets = WindowInsets.safeDrawingEndTop,
                            colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
                            title = {
                                Text(
                                    text = detailPaneTopAppBarTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                if (!isTwoPane) {
                                    detailPaneTopAppBarNavigationIcon()
                                }
                            }
                        )

                        detailPaneContent(
                            WindowInsets.safeDrawingEnd.asPaddingValues() + PaddingValues(MaterialTheme.paddings.large)
                        )
                    }
                }
            }
        }
    )
}
