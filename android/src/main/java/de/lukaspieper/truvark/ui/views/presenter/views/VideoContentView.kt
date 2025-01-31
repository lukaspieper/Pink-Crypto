/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.presenter.views

import android.media.MediaDataSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import de.lukaspieper.truvark.ui.controls.mediaplayer.MediaPlayerState
import de.lukaspieper.truvark.ui.controls.mediaplayer.MediaView

@Composable
fun VideoContentView(
    mediaDataSource: MediaDataSource,
    isTopBarVisible: State<Boolean>,
    switchTopBarVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val mediaPlayerState by remember(mediaDataSource) {
        mutableStateOf(
            MediaPlayerState(
                mediaDataSource = mediaDataSource,
                coroutineScope = coroutineScope,
                areControlsVisible = isTopBarVisible
            )
        )
    }

    var centerX by remember { mutableFloatStateOf(0F) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { centerX = it.width / 2F }
            .simpleTapGesture(switchTopBarVisibility, mediaPlayerState, centerX)
    ) {
        MediaView(
            state = mediaPlayerState,
            modifier = modifier
        )
    }
}

private fun Modifier.simpleTapGesture(
    switchTopBarVisibility: () -> Unit,
    playerState: MediaPlayerState? = null,
    centerX: Float? = null
): Modifier {
    return this.pointerInput(centerX) {
        detectTapGestures(
            onDoubleTap = {
                if (playerState != null && centerX != null) {
                    if (it.x > centerX) {
                        playerState.forward()
                    } else {
                        playerState.rewind()
                    }
                }
            },
            onTap = {
                switchTopBarVisibility()
            }
        )
    }
}
