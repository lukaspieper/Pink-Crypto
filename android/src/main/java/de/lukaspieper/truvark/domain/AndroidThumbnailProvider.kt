/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.request.videoFramePercent
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.domain.ThumbnailProvider
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import java.io.ByteArrayOutputStream

class AndroidThumbnailProvider(private val context: Context) : ThumbnailProvider {

    companion object {
        const val THUMBNAIL_QUALITY = 50
        const val THUMBNAIL_SIZE = 280
    }

    private val imageLoader = ImageLoader.Builder(context)
        .components {
            add(SvgDecoder.Factory())
            add(VideoFrameDecoder.Factory())
        }
        .diskCachePolicy(CachePolicy.DISABLED)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()

    override suspend fun createThumbnail(file: FileInfo): ByteArray? {
        try {
            val thumbnail = createThumbnailFromFile(file)
            return compressBitmapToByteArray(thumbnail)
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { e.asLog() }
        }

        return null
    }

    private suspend fun createThumbnailFromFile(file: FileInfo): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(file.uri)
            .allowConversionToBitmap(true)
            .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
            .videoFramePercent(0.07)
            .build()

        val result = imageLoader.execute(request)
        when (result) {
            is SuccessResult -> return (result.drawable as BitmapDrawable).bitmap
            is ErrorResult -> throw result.throwable
        }
    }

    private fun compressBitmapToByteArray(thumbnail: Bitmap): ByteArray {
        ByteArrayOutputStream().use { byteArrayBitmapStream ->
            thumbnail.compress(Bitmap.CompressFormat.WEBP, THUMBNAIL_QUALITY, byteArrayBitmapStream)
            return byteArrayBitmapStream.toByteArray()
        }
    }
}
