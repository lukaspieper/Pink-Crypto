/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.constants.FixedValues
import de.lukaspieper.truvark.common.domain.IdGenerator
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import io.realm.kotlin.Realm

internal class CipherFolderEntityCreator(
    private val realm: Realm,
    private val idGenerator: IdGenerator
) {

    /**
     * Creates a new [CipherFolderEntity] with a random name for the physical directory and the [displayName] in a
     * [Realm] instance. **No physical directory is created.** If a [parentCipherFolderEntity] is provided, the new
     * [CipherFolderEntity] will be a child of the parent.
     */
    @Throws(IllegalArgumentException::class, NoSuchElementException::class)
    suspend fun createFolder(displayName: String, parentCipherFolderEntity: CipherFolderEntity) {
        realm.write {
            val parentFolderEntity = when (parentCipherFolderEntity) {
                is RealmCipherFolderEntity -> findLatest(parentCipherFolderEntity)
                else -> null
            }

            var repeat: Boolean
            do {
                repeat = false

                val directoryName = idGenerator.createStringId(FixedValues.FILENAME_LENGTH)

                val cipherFolderEntity = RealmCipherFolderEntity().apply {
                    id = directoryName
                    this.displayName = displayName
                    parentFolder = parentFolderEntity
                }

                try {
                    copyToRealm(cipherFolderEntity)
                } catch (e: Exception) {
                    logcat(LogPriority.WARN) { e.asLog() }
                    repeat = true
                }
            } while (repeat)
        }
    }
}
