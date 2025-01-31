/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.controls.mediaplayer

import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.lukaspieper.truvark.ui.theme.DarkColorScheme
import de.lukaspieper.truvark.ui.theme.paddings
import kotlin.time.Duration

@Composable
fun MediaView(
    state: MediaPlayerState,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current

    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(state.surfaceCallback)
            }
        },
        update = { view ->
            view.holder.setFixedSize(
                state.mediaWith,
                state.mediaHeight
            )
        },
        modifier = modifier.then(
            when (state.aspectRatio) {
                0f -> Modifier
                else -> {
                    val screenAspectRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp
                    Modifier.aspectRatio(
                        ratio = state.aspectRatio,
                        matchHeightConstraintsFirst = state.aspectRatio < screenAspectRatio
                    )
                }
            }
        )
    )

    AnimatedVisibility(
        visible = state.areControlsVisible.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        MediaViewController(state)
    }
}

@Composable
private fun MediaViewController(state: MediaPlayerState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.30f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.fillMaxWidth())
        PlaybackControl(state)

        TimelineControl(
            modifier = Modifier.padding(bottom = MaterialTheme.paddings.extraLarge),
            state = state
        )
    }
}

@Composable
private fun PlaybackControl(state: MediaPlayerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(
            modifier = Modifier
                .size(BigIconButtonSize)
                .padding(10.dp),
            onClick = state::rewind
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Rounded.Replay10,
                contentDescription = null,
                tint = DarkColorScheme.onBackground
            )
        }
        IconButton(
            modifier = Modifier.size(BigIconButtonSize),
            onClick = { if (state.isPlaying) state.pause() else state.play() }
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = DarkColorScheme.onBackground
            )
        }
        IconButton(
            modifier = Modifier
                .size(BigIconButtonSize)
                .padding(10.dp),
            onClick = state::forward
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Rounded.Forward10,
                contentDescription = null,
                tint = DarkColorScheme.onBackground
            )
        }
    }
}

@Composable
private fun TimelineControl(
    state: MediaPlayerState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.mediaPosition.prettyVideoTimestamp(),
                color = DarkColorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                modifier = Modifier
                    .weight(1F)
                    .height(2.dp)
                    .padding(horizontal = 4.dp),
                value = state.mediaPosition.inWholeMilliseconds.toFloat(),
                valueRange = 0f..state.mediaDuration.inWholeMilliseconds.toFloat(),
                onValueChange = state::seekTo
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.mediaDuration.prettyVideoTimestamp(),
                color = DarkColorScheme.onBackground
            )
        }
    }
}

private fun Duration.prettyVideoTimestamp(): String {
    toComponents { minutes, seconds, _ ->
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

val BigIconButtonSize = 48.dp
