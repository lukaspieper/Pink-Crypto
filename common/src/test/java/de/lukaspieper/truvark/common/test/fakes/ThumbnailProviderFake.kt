/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.test.fakes

import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.domain.ThumbnailProvider

class ThumbnailProviderFake : ThumbnailProvider {

    override suspend fun createThumbnail(file: FileInfo): ByteArray? {
        return null
    }
}
