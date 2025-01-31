/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import de.lukaspieper.truvark.common.domain.entities.CipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.ui.preview.BooleanPreviewParameterProvider
import de.lukaspieper.truvark.ui.preview.ElementPreviews
import de.lukaspieper.truvark.ui.preview.PreviewHost
import de.lukaspieper.truvark.ui.preview.PreviewSampleData
import de.lukaspieper.truvark.ui.theme.paddings
import de.lukaspieper.truvark.ui.views.browser.SelectionState.SelectionMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

@Composable
fun CipherEntityGrid(
    folderHierarchyLevel: BrowserViewModel.FolderHierarchyLevel,
    selectionState: SelectionState,
    isListLayout: Boolean,
    onFileClick: (CipherFileEntity) -> Unit,
    onFolderClick: (String, CipherFolderEntity) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val gridCells = remember(isListLayout) {
        if (isListLayout) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 150.dp)
    }

    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    var isDragging by remember { mutableStateOf(false) }
    val autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(autoScrollSpeed) {
        if (autoScrollSpeed != 0f) {
            while (isActive) {
                gridState.scrollBy(autoScrollSpeed)
                delay(10)
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = gridCells,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.paddings.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.paddings.small),
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize()
            .cipherEntityGridDragHandler(
                lazyGridState = gridState,
                folderHierarchyLevel = folderHierarchyLevel,
                selectionState = selectionState,
                autoScrollThreshold = autoScrollThreshold,
                updateAutoScrollSpeed = { autoScrollSpeed = it },
                updateIsDragging = { isDragging = it }
            )
    ) {
        items(folderHierarchyLevel.folders, key = { it.id }) { cipherFolderEntity ->
            val isSelected by remember(selectionState.selectedFolderIds) {
                derivedStateOf { selectionState.selectedFolderIds.contains(cipherFolderEntity.id) }
            }

            // cipherFolderEntity may got updated (e.g. renamed)
            val clickableModifier = remember(selectionState.mode, isDragging, isSelected, cipherFolderEntity) {
                if (isDragging) return@remember Modifier
                // User should not be able to select subfolder of selected folder as destination.
                if (isSelected && selectionState.mode == SelectionMode.RELOCATION) return@remember Modifier

                Modifier.clickable {
                    when (selectionState.mode) {
                        SelectionMode.SELECTION -> selectionState.switchFolderSelection(cipherFolderEntity.id)
                        else -> onFolderClick(folderHierarchyLevel.folder.id, cipherFolderEntity)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .requiredHeight(56.dp)
                    .animateItem()
                    .clip(CardDefaults.shape)
                    .then(clickableModifier)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.paddings.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.aspectRatio(1F)
                    ) {
                        Surface(
                            shape = CardDefaults.shape,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(MaterialTheme.paddings.extraSmall)
                        ) {
                            Icon(
                                if (isSelected) Icons.Default.CheckCircle else Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.requiredSize(28.dp)
                            )
                        }
                    }

                    Text(
                        text = cipherFolderEntity.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item(span = { GridItemSpan(currentLineSpan = maxCurrentLineSpan) }) {
        }

        items(folderHierarchyLevel.files, key = { it.id }) { cipherFileEntity ->
            val isSelected by remember(selectionState.selectedFileIds) {
                derivedStateOf { selectionState.selectedFileIds.contains(cipherFileEntity.id) }
            }

            val clickableModifier = remember(selectionState.mode, isDragging) {
                if (isDragging) return@remember Modifier

                when (selectionState.mode) {
                    SelectionMode.NONE -> Modifier.clickable { onFileClick(cipherFileEntity) }
                    SelectionMode.SELECTION -> Modifier.clickable {
                        selectionState.switchFileSelection(cipherFileEntity.id)
                    }

                    SelectionMode.RELOCATION -> Modifier
                }
            }

            val thumbnail = remember {
                ImageRequest.Builder(context)
                    .data(cipherFileEntity.thumbnail)
                    .memoryCacheKey(cipherFileEntity.id)
                    .build()
            }

            Card(
                modifier = Modifier
                    .animateItem()
                    .clip(CardDefaults.shape)
                    .then(clickableModifier)
                    .then(if (isListLayout) Modifier else Modifier.aspectRatio(1F))
            ) {
                if (isListLayout) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.paddings.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var isError by remember { mutableStateOf(false) }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.requiredSize(56.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = thumbnail,
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                onError = { isError = true }
                            )

                            if (isError || isSelected) {
                                Surface(
                                    shape = CardDefaults.shape,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(MaterialTheme.paddings.extraSmall)
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.CheckCircle else Icons.AutoMirrored.Default.InsertDriveFile,
                                        contentDescription = null,
                                        modifier = Modifier.requiredSize(28.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = cipherFileEntity.fullName(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.paddings.large)) {
                                Text(
                                    text = cipherFileEntity.mimeType,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                )

                                cipherFileEntity.mediaDurationSeconds?.let { mediaDurationSeconds ->
                                    Text(
                                        text = mediaDurationSeconds.seconds.toString(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box {
                        SubcomposeAsyncImage(
                            model = thumbnail,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(MaterialTheme.paddings.large)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Default.InsertDriveFile,
                                        contentDescription = null,
                                        modifier = Modifier.defaultMinSize(64.dp, 64.dp)
                                    )
                                    Spacer(modifier = Modifier.size(MaterialTheme.paddings.medium))

                                    Text(
                                        text = cipherFileEntity.fullName(),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        )

                        if (isSelected) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.requiredSize(56.dp)
                            ) {
                                Surface(
                                    shape = CardDefaults.shape,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(MaterialTheme.paddings.extraSmall)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.requiredSize(28.dp)
                                    )
                                }
                            }
                        }

                        cipherFileEntity.mediaDurationSeconds?.let { mediaDurationSeconds ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(
                                        bottom = MaterialTheme.paddings.extraSmall,
                                        end = MaterialTheme.paddings.extraSmall
                                    )
                            ) {
                                Text(
                                    text = mediaDurationSeconds.seconds.toString(),
                                    modifier = Modifier.padding(MaterialTheme.paddings.extraSmall)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.cipherEntityGridDragHandler(
    lazyGridState: LazyGridState,
    folderHierarchyLevel: BrowserViewModel.FolderHierarchyLevel,
    selectionState: SelectionState,
    autoScrollThreshold: Float,
    updateAutoScrollSpeed: (Float) -> Unit,
    updateIsDragging: (Boolean) -> Unit
): Modifier {
    if (selectionState.mode == SelectionMode.RELOCATION) return this

    return this.pointerInput(folderHierarchyLevel, selectionState) {
        fun LazyGridState.gridItemKeyAtPosition(hitPoint: Offset): LazyGridItemInfo? {
            val paddingAwareHitPoint = hitPoint.copy(
                y = hitPoint.y + lazyGridState.layoutInfo.viewportStartOffset
            )

            return layoutInfo.visibleItemsInfo.find { itemInfo ->
                itemInfo.size.toIntRect().contains(paddingAwareHitPoint.round() - itemInfo.offset)
            }
        }

        val cipherEntitiesSize = folderHierarchyLevel.folders.size + folderHierarchyLevel.files.size
        val allGridEntitiesIds = folderHierarchyLevel.folderIds.toList()
            .plus(List(lazyGridState.layoutInfo.totalItemsCount - cipherEntitiesSize) { null })
            .plus(folderHierarchyLevel.fileIds)

        var initial: LazyGridItemInfo? = null
        var previous: LazyGridItemInfo? = null

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                lazyGridState.gridItemKeyAtPosition(offset)?.let { current ->
                    initial = current
                    previous = current
                    updateIsDragging(true)

                    val id = current.key as? String
                    when {
                        folderHierarchyLevel.folderIds.contains(id) -> selectionState.selectFolders(setOf(id!!))
                        folderHierarchyLevel.fileIds.contains(id) -> selectionState.selectFiles(setOf(id!!))
                    }
                }
            },
            onDragCancel = {
                initial = null
                updateAutoScrollSpeed(0f)
                updateIsDragging(false)
            },
            onDragEnd = {
                initial = null
                updateAutoScrollSpeed(0f)
                updateIsDragging(false)
            },
            onDrag = { change, _ ->
                if (initial != null) {
                    val distFromBottom = lazyGridState.layoutInfo.viewportSize.height - change.position.y
                    val distFromTop = change.position.y
                    val newAutoScrollSpeed = when {
                        distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                        distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                        else -> 0f
                    }
                    updateAutoScrollSpeed(newAutoScrollSpeed)

                    lazyGridState.gridItemKeyAtPosition(change.position)?.let { current ->
                        if (previous != current) {
                            val currentIndices = when {
                                initial!!.index < current.index -> initial!!.index..current.index
                                else -> current.index..initial!!.index
                            }
                            val previousIndices = when {
                                initial!!.index < previous!!.index -> initial!!.index..previous!!.index
                                else -> previous!!.index..initial!!.index
                            }

                            val folderIds = HashSet<String>()
                            val fileIds = HashSet<String>()

                            // Items to select
                            (currentIndices - previousIndices).mapNotNull { allGridEntitiesIds.getOrNull(it) }.forEach {
                                when {
                                    folderHierarchyLevel.folderIds.contains(it) -> folderIds.add(it)
                                    folderHierarchyLevel.fileIds.contains(it) -> fileIds.add(it)
                                }
                            }
                            selectionState.selectFolders(folderIds)
                            selectionState.selectFiles(fileIds)

                            folderIds.clear()
                            fileIds.clear()

                            // Items to deselect
                            (previousIndices - currentIndices).mapNotNull { allGridEntitiesIds.getOrNull(it) }.forEach {
                                when {
                                    folderHierarchyLevel.folderIds.contains(it) -> folderIds.add(it)
                                    folderHierarchyLevel.fileIds.contains(it) -> fileIds.add(it)
                                }
                            }
                            selectionState.deselectFolders(folderIds)
                            selectionState.deselectFiles(fileIds)

                            previous = current
                        }
                    }
                }
            }
        )
    }
}

@ElementPreviews
@Composable
private fun CipherEntityGridSelectionPreviews(
    @PreviewParameter(BooleanPreviewParameterProvider::class) isListLayout: Boolean
) = PreviewHost {
    CipherEntityGrid(
        folderHierarchyLevel = PreviewSampleData.folderHierarchyLevel,
        selectionState = SelectionState(
            initialSelectedFolderIds = setOf("folder1", "folder3", "folder4"),
            initialSelectedFileIds = setOf("file0", "file3", "file4", "file6", "file11", "file18", "file21")
        ),
        isListLayout = isListLayout,
        onFileClick = {},
        onFolderClick = { _, _ -> },
        contentPadding = PaddingValues(MaterialTheme.paddings.large),
        modifier = Modifier
    )
}
