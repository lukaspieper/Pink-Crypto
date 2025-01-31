/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.browser

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.common.domain.entities.CipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.common.domain.vault.Vault
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import de.lukaspieper.truvark.data.io.AndroidFileSystem
import de.lukaspieper.truvark.data.preferences.PersistentPreferences
import de.lukaspieper.truvark.work.WorkScheduler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val vault: Vault,
    private val fileSystem: AndroidFileSystem,
    private val preferences: PersistentPreferences
) : ViewModel() {
    private val stack = ArrayDeque<FolderHierarchyLevel>()
    private var updateFolderHierarchyLevelJob: Job

    var currentFolderHierarchyLevel by mutableStateOf(
        runBlocking { FolderHierarchyLevel(vault.findCipherFolderEntity("")) }
    )
        private set

    val selectionState: SelectionState = SelectionState()

    var isRootLevel by mutableStateOf(true)
        private set

    val isListLayout = preferences.isListLayout

    init {
        stack.addLast(currentFolderHierarchyLevel)
        updateFolderHierarchyLevelJob = updateFolderHierarchyLevel()
    }

    fun navigateToFolder(currentFolderId: String, folder: CipherFolderEntity) {
        synchronized(stack) {
            // Prevent parallel folders from being added to the navigation stack, happens with Multi-Touch.
            if (currentFolderId != currentFolderHierarchyLevel.folder.id) {
                return
            }

            updateFolderHierarchyLevelJob.cancel()

            val folderHierarchyLevel = FolderHierarchyLevel(folder)
            stack.addLast(folderHierarchyLevel)
            currentFolderHierarchyLevel = folderHierarchyLevel
            isRootLevel = false
        }

        updateFolderHierarchyLevelJob = updateFolderHierarchyLevel()
    }

    fun navigateToParentFolder() {
        if (stack.size > 1) {
            synchronized(stack) {
                updateFolderHierarchyLevelJob.cancel()
                if (selectionState.mode != SelectionState.SelectionMode.RELOCATION) {
                    selectionState.disableSelectionMode()
                }

                stack.removeLast()
                currentFolderHierarchyLevel = stack.last()
                isRootLevel = (stack.size == 1)
            }

            updateFolderHierarchyLevelJob = updateFolderHierarchyLevel()
        }
    }

    @OptIn(FlowPreview::class)
    private fun updateFolderHierarchyLevel(): Job {
        return viewModelScope.launch {
            launch {
                vault.findCipherFileEntitySubFolders(currentFolderHierarchyLevel.folder.id)
                    .debounce(250)
                    .collect { folders ->
                        val folderIds = folders.map { it.id }.toSet()

                        withContext(Dispatchers.Main) {
                            updatePeekOfStack {
                                currentFolderHierarchyLevel.copy(
                                    folders = folders,
                                    folderIds = folderIds
                                )
                            }
                        }
                    }
            }

            launch {
                vault.findCipherFileEntitiesForFolder(currentFolderHierarchyLevel.folder.id)
                    .debounce(250)
                    .collect { files ->
                        val fileIds = files.map { it.id }.toSet()

                        withContext(Dispatchers.Main) {
                            updatePeekOfStack {
                                currentFolderHierarchyLevel.copy(
                                    files = files,
                                    fileIds = fileIds
                                )
                            }
                        }
                    }
            }
        }
    }

    fun checkForVaultNameUpdates() {
        if (isRootLevel && currentFolderHierarchyLevel.folder.displayName != vault.displayName) {
            updatePeekOfStack {
                currentFolderHierarchyLevel.copy(
                    folder = runBlocking { vault.findCipherFolderEntity("") }
                )
            }
        }
    }

    private fun updatePeekOfStack(updatePeek: () -> FolderHierarchyLevel) {
        synchronized(stack) {
            val folderHierarchyLevel = updatePeek()

            // Update peek of stack
            stack.removeLast()
            stack.addLast(folderHierarchyLevel)
            currentFolderHierarchyLevel = folderHierarchyLevel

            // No need to update the job, because the parent folder id is the same.
        }
    }

    suspend fun createCipherFolderEntity(displayName: String): Boolean {
        try {
            vault.createFolder(displayName, currentFolderHierarchyLevel.folder)
            return true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
            return false
        }
    }

    suspend fun renameCipherFolderEntity(newDisplayName: String): Boolean {
        try {
            vault.renameFolder(currentFolderHierarchyLevel.folder, newDisplayName)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
            return false
        }

        updatePeekOfStack {
            currentFolderHierarchyLevel.copy(
                folder = runBlocking { vault.findCipherFolderEntity(currentFolderHierarchyLevel.folder.id) }
            )
        }
        return true
    }

    fun encryptUris(uris: List<Uri>, deleteSourceFiles: Boolean) {
        vault.scheduleEncryption(
            metadata = WorkScheduler.AndroidSchedulerMetadata(R.string.encrypting_files),
            destination = currentFolderHierarchyLevel.folder,
            sources = uris.reversed().map { { fileSystem.fileInfo(it) } },
            deleteSources = deleteSourceFiles
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun decryptSelectedCipherEntities() {
        GlobalScope.launch {
            vault.scheduleDecryption(
                metadata = WorkScheduler.AndroidSchedulerMetadata(
                    notificationTitle = R.string.decrypting_files,
                    notificationFinishTitle = R.string.decrypted_files,
                    notificationActionText = R.string.open_directory,
                    notificationAction = Intent(Intent.ACTION_VIEW, vault.fileSystem.decryptionRootDirectory.uri as Uri)
                ),
                parentFolder = currentFolderHierarchyLevel.folder,
                files = selectionState.selectedFileIds.map { vault.findCipherFileEntity(it) },
                folders = selectionState.selectedFolderIds.map { vault.findCipherFolderEntity(it) }
            )

            selectionState.disableSelectionMode()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun deleteSelectedCipherEntities() {
        GlobalScope.launch {
            vault.scheduleDeletion(
                metadata = WorkScheduler.AndroidSchedulerMetadata(R.string.deleting_files),
                files = selectionState.selectedFileIds.map { vault.findCipherFileEntity(it) },
                folders = selectionState.selectedFolderIds.map { vault.findCipherFolderEntity(it) }
            )

            selectionState.disableSelectionMode()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun relocateSelectedCipherEntities() {
        GlobalScope.launch {
            vault.scheduleRelocation(
                metadata = WorkScheduler.AndroidSchedulerMetadata(R.string.relocating_files),
                destination = currentFolderHierarchyLevel.folder,
                files = selectionState.selectedFileIds.map { vault.findCipherFileEntity(it) },
                folders = selectionState.selectedFolderIds.map { vault.findCipherFolderEntity(it) }
            )

            selectionState.disableSelectionMode()
        }
    }

    fun updateIsListLayout(isListLayout: Boolean) {
        viewModelScope.launch {
            preferences.saveIsListLayout(isListLayout)
        }
    }

    @Immutable
    data class FolderHierarchyLevel(
        val folder: CipherFolderEntity,
        val folders: List<CipherFolderEntity> = emptyList(),
        val folderIds: Set<String> = emptySet(),
        val files: List<CipherFileEntity> = emptyList(),
        val fileIds: Set<String> = emptySet()
    )
}
