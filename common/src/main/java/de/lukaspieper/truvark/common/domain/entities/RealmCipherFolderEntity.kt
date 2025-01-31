/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.entities

import io.realm.kotlin.ext.backlinks
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey

@PersistedName("Folders")
internal class RealmCipherFolderEntity : RealmObject, CipherFolderEntity {

    @PrimaryKey
    @PersistedName(REALM_FIELD_ID)
    override var id: String = ""

    @PersistedName(REALM_FIELD_DISPLAY_NAME)
    override var displayName: String = ""

    @PersistedName(REALM_FIELD_PARENT_FOLDER)
    var parentFolder: RealmCipherFolderEntity? = null

    @PersistedName(REALM_FIELD_CREATION_DATE)
    var creationDate: RealmInstant = RealmInstant.now()

    val files: RealmResults<RealmCipherFileEntity> by backlinks(RealmCipherFileEntity::folder)

    val subfolders: RealmResults<RealmCipherFolderEntity> by backlinks(
        RealmCipherFolderEntity::parentFolder
    )

    companion object {
        const val REALM_FIELD_ID = "cryptoName"
        const val REALM_FIELD_DISPLAY_NAME = "displayName"
        const val REALM_FIELD_PARENT_FOLDER = "parentFolder"
        const val REALM_FIELD_CREATION_DATE = "creationDate"
    }
}
