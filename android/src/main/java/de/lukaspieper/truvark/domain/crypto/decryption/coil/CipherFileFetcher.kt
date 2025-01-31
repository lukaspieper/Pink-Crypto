/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.domain.crypto.decryption.coil

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.domain.vault.Vault
import de.lukaspieper.truvark.domain.crypto.decryption.DecryptingFileHandle
import okio.buffer

class CipherFileFetcher(
    private val fileInfo: FileInfo,
    private val context: Context,
    private val vault: Vault
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // TODO: Make this FileSystem-agnostic, e.g. by splitting file access and decryption and adding a method
        //  returning a FileHandle
        require(fileInfo.uri is Uri)

        val decryptingFileHandle = DecryptingFileHandle(context.contentResolver, vault, fileInfo.uri as Uri)
        val imageSource = ImageSource(decryptingFileHandle.singleSource().buffer(), context)

        return SourceResult(
            source = imageSource,
            mimeType = decryptingFileHandle.header.mimeType,
            dataSource = DataSource.DISK
        )
    }

    class Factory(
        private val context: Context,
        private val vault: Vault
    ) : Fetcher.Factory<FileInfo> {

        override fun create(data: FileInfo, options: Options, imageLoader: ImageLoader): Fetcher {
            return CipherFileFetcher(data, context, vault)
        }
    }
}
