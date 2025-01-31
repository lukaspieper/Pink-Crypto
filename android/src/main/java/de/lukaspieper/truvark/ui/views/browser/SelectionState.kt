/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.browser

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity

class SelectionState(
    initialSelectedFolderIds: Set<String> = emptySet(),
    initialSelectedFileIds: Set<String> = emptySet(),
) {
    var relocationSourceFolder by mutableStateOf<CipherFolderEntity?>(null)
        private set

    var selectedFolderIds by mutableStateOf(initialSelectedFolderIds)
        private set

    var selectedFileIds by mutableStateOf(initialSelectedFileIds)
        private set

    val numberOfSelections by derivedStateOf { selectedFolderIds.size + selectedFileIds.size }

    val mode by derivedStateOf {
        when {
            relocationSourceFolder != null -> SelectionMode.RELOCATION
            numberOfSelections > 0 -> SelectionMode.SELECTION
            else -> SelectionMode.NONE
        }
    }

    /**
     * Switches the selection mode to [SelectionMode.NONE].
     */
    fun disableSelectionMode() {
        selectedFolderIds = emptySet()
        selectedFileIds = emptySet()
        relocationSourceFolder = null
    }

    fun enableRelocationMode(sourceFolder: CipherFolderEntity) {
        relocationSourceFolder = sourceFolder
    }

    fun selectFolders(folderIds: Set<String>) {
        if (folderIds.isEmpty()) return
        selectedFolderIds = selectedFolderIds + folderIds
    }

    fun deselectFolders(folderIds: Set<String>) {
        if (folderIds.isEmpty()) return
        selectedFolderIds = selectedFolderIds - folderIds
    }

    fun selectFiles(fileIds: Set<String>) {
        if (fileIds.isEmpty()) return
        selectedFileIds = selectedFileIds + fileIds
    }

    fun deselectFiles(fileIds: Set<String>) {
        if (fileIds.isEmpty()) return
        selectedFileIds = selectedFileIds - fileIds
    }

    fun switchFolderSelection(folderId: String) {
        selectedFolderIds = when {
            selectedFolderIds.contains(folderId) -> selectedFolderIds - folderId
            else -> selectedFolderIds + folderId
        }
    }

    fun switchFileSelection(fileId: String) {
        selectedFileIds = when {
            selectedFileIds.contains(fileId) -> selectedFileIds - fileId
            else -> selectedFileIds + fileId
        }
    }

    enum class SelectionMode {
        NONE,
        SELECTION,
        RELOCATION
    }
}
