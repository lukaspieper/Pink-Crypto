/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.domain.crypto.decryption.coil

import android.content.ContentResolver
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Dimension
import com.google.accompanist.drawablepainter.DrawablePainter
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.domain.vault.Vault
import de.lukaspieper.truvark.domain.crypto.decryption.DecryptingFileHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.saket.telephoto.subsamplingimage.ImageBitmapOptions
import me.saket.telephoto.subsamplingimage.SubSamplingImageSource
import me.saket.telephoto.zoomable.ZoomableImageSource
import me.saket.telephoto.zoomable.ZoomableImageSource.ResolveResult
import kotlin.math.roundToInt
import kotlin.time.Duration
import coil.size.Size as CoilSize

/**
 * A [ZoomableImageSource] for Telephoto to load encrypted images with support for subsampling while keeping fallback to
 * Coil for other image types like GIFs, SVGs, etc.
 */
internal class CipherZoomableImageSource(
    private val model: FileInfo,
    private val mimeType: String,
    private val vault: Vault,
    private val contentResolver: ContentResolver,
    private val imageLoader: ImageLoader,
) : ZoomableImageSource {

    @Composable
    override fun resolve(canvasSize: Flow<Size>): ResolveResult {
        val context = LocalContext.current
        val resolver = remember(this) {
            Resolver(
                request = ImageRequest.Builder(context)
                    .data(model)
                    .size { canvasSize.first().toCoilSize() }
                    .build(),
                mimeType = mimeType,
                imageLoader = imageLoader,
                vault = vault,
                contentResolver = contentResolver
            )
        }
        return resolver.resolved
    }

    private fun Size.toCoilSize() = CoilSize(
        width = if (width.isFinite()) Dimension(width.roundToInt()) else Dimension.Undefined,
        height = if (height.isFinite()) Dimension(height.roundToInt()) else Dimension.Undefined
    )
}

private class Resolver(
    private val request: ImageRequest,
    private val mimeType: String,
    private val imageLoader: ImageLoader,
    private val vault: Vault,
    private val contentResolver: ContentResolver,
) : RememberObserver {
    private var scope: CoroutineScope? = null
    private val subSamplingMimeTypes = listOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/heif",
        "image/heic",
    )

    var resolved: ResolveResult by mutableStateOf(
        ResolveResult(delegate = null)
    )

    private suspend fun work() {
        val result = imageLoader.execute(request)
        val imageSource = result.toSubSamplingImageSource()

        resolved = resolved.copy(
            delegate = if (result is SuccessResult && imageSource != null) {
                ZoomableImageSource.SubSamplingDelegate(
                    source = imageSource,
                    imageOptions = ImageBitmapOptions(from = (result.drawable as BitmapDrawable).bitmap)
                )
            } else {
                ZoomableImageSource.PainterDelegate(
                    painter = result.drawable?.asPainter()
                )
            }
        )
    }

    private fun ImageResult.toSubSamplingImageSource(): SubSamplingImageSource? {
        if (this !is SuccessResult) return null

        // TODO: Make this FileSystem-agnostic, e.g. by splitting file access and decryption and adding a method
        //  returning a FileHandle
        (request.data as? FileInfo)?.let { fileInfo ->
            if (fileInfo.uri is Uri && subSamplingMimeTypes.contains(mimeType)) {
                return SubSamplingImageSource.rawSource(
                    source = { DecryptingFileHandle(contentResolver, vault, fileInfo.uri as Uri).singleSource() },
                )
            }
        }

        return null
    }

    private fun Drawable.asPainter(): Painter {
        return DrawablePainter(mutate())
    }

    //region RememberWorker and extension

    override fun onRemembered() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope!!.launch { work() }
    }

    override fun onAbandoned() {
        scope?.cancel()
    }

    override fun onForgotten() {
        scope?.cancel()
    }

    private fun ResolveResult.copy(
        delegate: ZoomableImageSource.ImageDelegate? = this.delegate,
        crossfadeDuration: Duration = this.crossfadeDuration,
        placeholder: Painter? = this.placeholder,
    ) = ResolveResult(
        delegate = delegate,
        crossfadeDuration = crossfadeDuration,
        placeholder = placeholder,
    )

    //endregion
}
