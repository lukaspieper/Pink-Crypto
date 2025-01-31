/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.ext.query

@Throws(NoSuchElementException::class, IllegalArgumentException::class)
internal fun TypedRealm.findCipherFolderEntity(folderId: String): RealmCipherFolderEntity {
    return query<RealmCipherFolderEntity>("${RealmCipherFolderEntity.REALM_FIELD_ID} == '$folderId'")
        .find()
        .single()
}

internal fun TypedRealm.findCipherFileEntityOrNull(cipherFileEntityId: String): RealmCipherFileEntity? {
    return query<RealmCipherFileEntity>("${RealmCipherFileEntity.REALM_FIELD_ID} == '$cipherFileEntityId'")
        .find()
        .singleOrNull()
}
