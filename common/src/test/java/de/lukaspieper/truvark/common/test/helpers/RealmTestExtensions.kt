/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.test.helpers

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults

internal fun TypedRealm.findCipherFileEntityByName(name: String): RealmResults<RealmCipherFileEntity> {
    return query<RealmCipherFileEntity>(
        when {
            name.contains('.') -> """
            ${RealmCipherFileEntity.REALM_FIELD_ORIGINAL_NAME} == '${name.substringBeforeLast('.')}' AND
            ${RealmCipherFileEntity.REALM_FIELD_FILE_EXTENSION} == '${name.substringAfterLast('.')}'
            """.trimIndent()

            else -> "${RealmCipherFileEntity.REALM_FIELD_ORIGINAL_NAME} == '$name'"
        }
    ).find()
}
