/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.browser

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.lukaspieper.truvark.Page
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.common.domain.entities.CipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.ui.controls.SafeDrawingScaffold
import de.lukaspieper.truvark.ui.preview.BooleanPreviewParameterProvider
import de.lukaspieper.truvark.ui.preview.PagePreviews
import de.lukaspieper.truvark.ui.preview.PreviewHost
import de.lukaspieper.truvark.ui.preview.PreviewSampleData
import de.lukaspieper.truvark.ui.theme.paddings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun BrowserPage(
    navigate: (Page) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    BackHandler(enabled = viewModel.isRootLevel.not()) {
        viewModel.navigateToParentFolder()
    }

    LaunchedEffect(Unit) {
        viewModel.checkForVaultNameUpdates()
    }

    BrowserView(
        folderHierarchyLevel = viewModel.currentFolderHierarchyLevel,
        selectionState = viewModel.selectionState,
        isRootLevel = viewModel.isRootLevel,
        isListLayoutState = viewModel.isListLayout,
        createFolder = viewModel::createCipherFolderEntity,
        renameFolder = viewModel::renameCipherFolderEntity,
        encryptUris = viewModel::encryptUris,
        decryptSelectedCipherEntities = viewModel::decryptSelectedCipherEntities,
        deleteSelectedCipherEntities = viewModel::deleteSelectedCipherEntities,
        relocateSelectedCipherEntities = viewModel::relocateSelectedCipherEntities,
        updateIsListLayout = viewModel::updateIsListLayout,
        navigateToSettings = { navigate(Page.SettingsHome) },
        navigateToFilePresenter = { cipherFileEntity ->
            navigate(
                Page.Presenter(
                    viewModel.currentFolderHierarchyLevel.folder.id,
                    cipherFileEntity.id
                )
            )
        },
        navigateToFolder = viewModel::navigateToFolder,
        navigateToParentFolder = viewModel::navigateToParentFolder,
        modifier = modifier
    )
}

@Composable
private fun BrowserView(
    folderHierarchyLevel: BrowserViewModel.FolderHierarchyLevel,
    selectionState: SelectionState,
    isRootLevel: Boolean,
    isListLayoutState: Flow<Boolean>,
    createFolder: suspend (String) -> Boolean,
    renameFolder: suspend (String) -> Boolean,
    encryptUris: (List<Uri>, Boolean) -> Unit,
    decryptSelectedCipherEntities: () -> Unit,
    deleteSelectedCipherEntities: () -> Unit,
    relocateSelectedCipherEntities: () -> Unit,
    updateIsListLayout: (Boolean) -> Unit,
    navigateToSettings: () -> Unit,
    navigateToFilePresenter: (CipherFileEntity) -> Unit,
    navigateToFolder: (String, CipherFolderEntity) -> Unit,
    navigateToParentFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListLayout by isListLayoutState.collectAsStateWithLifecycle(false)
    var visibleDialog by rememberSaveable { mutableStateOf(BrowserDialogs.NONE) }

    SafeDrawingScaffold(
        modifier = modifier,
        largeTopAppBarTitle = folderHierarchyLevel.folder.displayName,
        largeTopAppBarActions = {
            IconButton(
                onClick = { updateIsListLayout(isListLayout.not()) },
                content = {
                    if (isListLayout) {
                        Icon(Icons.Default.ViewModule, null)
                    } else {
                        Icon(Icons.AutoMirrored.Default.ViewList, null)
                    }
                }
            )

            if (isRootLevel) {
                IconButton(
                    onClick = navigateToSettings,
                    content = { Icon(Icons.Default.Settings, null) }
                )
            } else {
                IconButton(
                    onClick = { visibleDialog = BrowserDialogs.RENAME_FOLDER },
                    content = { Icon(Icons.Default.Edit, null) }
                )
            }
        },
        largeTopAppBarNavigationIcon = {
            if (!isRootLevel) {
                IconButton(
                    onClick = navigateToParentFolder,
                    content = { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }
                )
            }
        },
        bottomOverlay = {
            when (selectionState.mode) {
                SelectionState.SelectionMode.NONE -> {
                    FloatingActionsButtons(
                        isEncryptionAllowed = isRootLevel.not(),
                        showEncryptFilesDialog = { visibleDialog = BrowserDialogs.ENCRYPT },
                        showNewFolderDialog = { visibleDialog = BrowserDialogs.NEW_FOLDER }
                    )
                }

                SelectionState.SelectionMode.SELECTION -> {
                    val numberOfCipherEntities = remember(folderHierarchyLevel) {
                        folderHierarchyLevel.folders.size + folderHierarchyLevel.files.size
                    }

                    SelectionModeBar(
                        numberOfSelections = selectionState.numberOfSelections,
                        numberOfCipherEntities = numberOfCipherEntities,
                        disableSelectionMode = selectionState::disableSelectionMode,
                        enableRelocationMode = { selectionState.enableRelocationMode(folderHierarchyLevel.folder) },
                        selectAll = {
                            selectionState.selectFolders(folderHierarchyLevel.folderIds)
                            selectionState.selectFiles(folderHierarchyLevel.fileIds)
                        },
                        showDeleteSelectionDialog = { visibleDialog = BrowserDialogs.DELETE_SELECTION },
                        decryptSelectedCipherEntities = decryptSelectedCipherEntities
                    )
                }

                SelectionState.SelectionMode.RELOCATION -> {
                    // User should not be able to move entities to their current location and must not be able to move
                    // files to the root of the vault. The root only supports folders.
                    val isRelocationAllowed = remember(isRootLevel, selectionState, folderHierarchyLevel) {
                        folderHierarchyLevel.folder != selectionState.relocationSourceFolder &&
                                (!isRootLevel || selectionState.selectedFileIds.isEmpty())
                    }

                    RelocationModeBar(
                        numberOfSelectedFolders = selectionState.selectedFolderIds.size,
                        numberOfSelectedFiles = selectionState.selectedFileIds.size,
                        isRelocationAllowed = isRelocationAllowed,
                        disableSelectionMode = selectionState::disableSelectionMode,
                        relocateSelectedCipherEntities = relocateSelectedCipherEntities
                    )
                }
            }
        }
    ) { paddingValues ->
        CipherEntityGrid(
            folderHierarchyLevel = folderHierarchyLevel,
            selectionState = selectionState,
            isListLayout = isListLayout,
            onFileClick = navigateToFilePresenter,
            onFolderClick = navigateToFolder,
            contentPadding = paddingValues
        )
    }

    when (visibleDialog) {
        BrowserDialogs.NONE -> {
            // Show nothing.
        }

        BrowserDialogs.NEW_FOLDER -> NewFolderDialog(
            hideDialog = { visibleDialog = BrowserDialogs.NONE },
            createFolder = createFolder
        )

        BrowserDialogs.RENAME_FOLDER -> RenameFolderDialog(
            hideDialog = { visibleDialog = BrowserDialogs.NONE },
            folderName = folderHierarchyLevel.folder.displayName,
            renameFolder = renameFolder
        )

        BrowserDialogs.ENCRYPT -> EncryptFilesDialog(
            hideDialog = { visibleDialog = BrowserDialogs.NONE },
            encryptUris = encryptUris
        )

        BrowserDialogs.DELETE_SELECTION -> DeleteSelectionDialog(
            hideDialog = { visibleDialog = BrowserDialogs.NONE },
            deleteSelectedCipherEntities = deleteSelectedCipherEntities
        )
    }
}

@Composable
private fun FloatingActionsButtons(
    isEncryptionAllowed: Boolean,
    showEncryptFilesDialog: () -> Unit,
    showNewFolderDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = spacedBy(MaterialTheme.paddings.medium),
        horizontalAlignment = Alignment.End,
        modifier = modifier,
    ) {
        if (isEncryptionAllowed) {
            FloatingActionButton(
                onClick = showEncryptFilesDialog,
                content = { Icon(Icons.Outlined.FileOpen, null) }
            )
        }

        FloatingActionButton(
            onClick = showNewFolderDialog,
            content = { Icon(Icons.Outlined.CreateNewFolder, null) }
        )
    }
}

@Composable
private fun SelectionModeBar(
    numberOfSelections: Int,
    numberOfCipherEntities: Int,
    disableSelectionMode: () -> Unit,
    enableRelocationMode: () -> Unit,
    selectAll: () -> Unit,
    showDeleteSelectionDialog: () -> Unit,
    decryptSelectedCipherEntities: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = FloatingActionButtonDefaults.shape,
        modifier = modifier.requiredHeight(56.dp)
    ) {
        Row(
            horizontalArrangement = spacedBy(MaterialTheme.paddings.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = MaterialTheme.paddings.extraSmall)
        ) {
            IconButton(
                onClick = disableSelectionMode,
                content = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary) },
            )
            Text(
                text = "$numberOfSelections/$numberOfCipherEntities"
            )
            IconButton(
                onClick = selectAll,
                content = { Icon(Icons.Default.SelectAll, null) }
            )
            IconButton(
                onClick = decryptSelectedCipherEntities,
                content = { Icon(Icons.Default.LockOpen, null) }
            )
            IconButton(
                onClick = enableRelocationMode,
                content = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) }
            )
            IconButton(
                onClick = showDeleteSelectionDialog,
                content = { Icon(Icons.Default.Delete, null) }
            )
        }
    }
}

@Composable
fun RelocationModeBar(
    numberOfSelectedFolders: Int,
    numberOfSelectedFiles: Int,
    isRelocationAllowed: Boolean,
    disableSelectionMode: () -> Unit,
    relocateSelectedCipherEntities: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = FloatingActionButtonDefaults.shape,
        modifier = modifier
            .sizeIn(maxWidth = 450.dp)
            .width(IntrinsicSize.Max)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.paddings.extraSmall)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = disableSelectionMode,
                    content = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary) },
                )

                Text(
                    text = stringResource(
                        R.string.folders_and_files_selected,
                        numberOfSelectedFolders,
                        numberOfSelectedFiles
                    ),
                    modifier = Modifier.padding(end = MaterialTheme.paddings.extraSmall)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (numberOfSelectedFiles > 0) {
                    Text(
                        text = stringResource(R.string.note_relocate_files_to_root),
                        fontStyle = FontStyle.Italic,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        modifier = Modifier
                            .padding(start = MaterialTheme.paddings.medium, end = MaterialTheme.paddings.extraSmall)
                            .weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    enabled = isRelocationAllowed,
                    onClick = relocateSelectedCipherEntities,
                    content = { Text(stringResource(R.string.move_here)) }
                )
            }
        }
    }
}

@PagePreviews
@Composable
private fun NonRootFolderPreview(
    @PreviewParameter(BooleanPreviewParameterProvider::class) isListLayout: Boolean
) = PreviewHost {
    BrowserView(
        folderHierarchyLevel = PreviewSampleData.folderHierarchyLevel,
        selectionState = SelectionState(),
        isRootLevel = false,
        isListLayoutState = flowOf(isListLayout),
        createFolder = { false },
        renameFolder = { false },
        encryptUris = { _, _ -> },
        decryptSelectedCipherEntities = {},
        deleteSelectedCipherEntities = {},
        relocateSelectedCipherEntities = {},
        updateIsListLayout = {},
        navigateToSettings = {},
        navigateToFilePresenter = {},
        navigateToFolder = { _, _ -> },
        navigateToParentFolder = {}
    )
}
