/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.preview

import de.lukaspieper.truvark.common.domain.entities.CipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.ui.views.browser.BrowserViewModel

object PreviewSampleData {

    private val cipherFolderEntities: List<CipherFolderEntity> = List(5) {
        object : CipherFolderEntity {
            override val id: String = "folder$it"
            override val displayName: String = "Personal Folder $it"
        }
    }

    val cipherFileEntities: List<CipherFileEntity> = List(30) {
        object : CipherFileEntity {
            override val id: String = "file$it"
            override val thumbnail: ByteArray? = null
            override val folder: CipherFolderEntity? = null
            override var name: String = "Top Secret File $it"
            override var fileExtension: String = "file"
            override var mimeType: String = "application/octet-stream"
            override var fileSize: Long = 0L
            override val mediaDurationSeconds: Long? = if (it % 7 == 0) 62L else null
        }
    }

    val folderHierarchyLevel: BrowserViewModel.FolderHierarchyLevel
        get() = BrowserViewModel.FolderHierarchyLevel(
            folder = object : CipherFolderEntity {
                override val id: String = "" // This is the root folder id, usually contains only folders, no files.
                override val displayName: String = "Vault"
            },
            folders = cipherFolderEntities,
            folderIds = cipherFolderEntities.map { it.id }.toSet(),
            files = cipherFileEntities,
            fileIds = cipherFileEntities.map { it.id }.toSet()
        )
}
