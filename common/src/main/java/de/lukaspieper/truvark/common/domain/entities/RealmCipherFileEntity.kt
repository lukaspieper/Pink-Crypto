/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.entities

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey

@PersistedName("EncryptedFiles")
internal class RealmCipherFileEntity : RealmObject, CipherFileEntity {

    @PrimaryKey
    @PersistedName(REALM_FIELD_ID)
    override var id: String = ""

    @PersistedName(REALM_FIELD_ORIGINAL_NAME)
    override var name: String = ""

    @PersistedName(REALM_FIELD_FILE_EXTENSION)
    override var fileExtension: String = ""

    @PersistedName(REALM_FIELD_MIME_TYPE)
    override var mimeType: String = ""

    @PersistedName(REALM_FIELD_FILE_SIZE)
    override var fileSize: Long = 0L

    @PersistedName(REALM_FIELD_THUMBNAIL)
    override var thumbnail: ByteArray? = null

    @PersistedName(REALM_FIELD_FOLDER)
    override var folder: RealmCipherFolderEntity? = null

    @PersistedName(REALM_FIELD_CREATION_DATE)
    var creationDate: RealmInstant = RealmInstant.now()

    @PersistedName(REALM_FIELD_MEDIA_DURATION_SECONDS)
    override var mediaDurationSeconds: Long? = null

    companion object {
        const val REALM_FIELD_ID = "id"
        const val REALM_FIELD_ORIGINAL_NAME = "originalName"
        const val REALM_FIELD_FILE_EXTENSION = "fileExtension"
        const val REALM_FIELD_MIME_TYPE = "mimeType"
        const val REALM_FIELD_FILE_SIZE = "fileSize"
        const val REALM_FIELD_THUMBNAIL = "thumbnail"
        const val REALM_FIELD_FOLDER = "folder"
        const val REALM_FIELD_CREATION_DATE = "creationDate"
        const val REALM_FIELD_MEDIA_DURATION_SECONDS = "mediaDurationSeconds"
    }
}
