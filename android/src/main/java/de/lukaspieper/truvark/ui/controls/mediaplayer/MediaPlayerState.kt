/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.controls.mediaplayer

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.view.SurfaceHolder
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import de.lukaspieper.truvark.common.logging.LogPriority.DEBUG
import de.lukaspieper.truvark.common.logging.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class MediaPlayerState(
    private val mediaDataSource: MediaDataSource,
    private val coroutineScope: CoroutineScope,
    val areControlsVisible: State<Boolean>
) {
    private var pollMediaPositionJob: Job? = null
    private var seekMode = MediaPlayer.SEEK_CLOSEST_SYNC

    private var isPlayerReleased = false

    val player = MediaPlayer().apply {
        setDataSource(mediaDataSource)
        isLooping = true

        setOnPreparedListener {
            logcat(DEBUG) { "MediaPlayer: onPrepared" }
            setScreenOnWhilePlaying(true)
            seekTo(0L, MediaPlayer.SEEK_PREVIOUS_SYNC)
            mediaDuration = duration.milliseconds

            if (mediaDuration < 5.minutes) {
                seekMode = MediaPlayer.SEEK_CLOSEST
            }

            pollMediaPositionJob = coroutineScope.launch {
                snapshotFlow { areControlsVisible.value }.collectLatest { visible ->
                    while (visible) {
                        mediaPosition = currentPosition.milliseconds
                        delay(100)
                    }
                }
            }
        }

        setOnVideoSizeChangedListener { _, width, height ->
            mediaWith = width
            mediaHeight = height
            aspectRatio = width / max(1f, height.toFloat())
        }
    }

    var isPlaying by mutableStateOf(false)
        private set

    var mediaPosition by mutableStateOf(Duration.ZERO)
        private set

    var mediaDuration by mutableStateOf(Duration.ZERO)
        private set

    var aspectRatio by mutableFloatStateOf(0f)
        private set

    var mediaWith by mutableIntStateOf(0)
        private set

    var mediaHeight by mutableIntStateOf(0)
        private set

    val surfaceCallback by mutableStateOf(
        object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder) {
                logcat(DEBUG) { "MediaPlayer: surfaceCreated" }
                player.setDisplay(holder)
                player.prepareAsync()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // Intentionally empty.
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                logcat(DEBUG) { "MediaPlayer: surfaceDestroyed" }
                pollMediaPositionJob?.cancel()
                // TODO: Is there a way to re-initialize the player? E.g. after switching back from another app.
                player.release()
                isPlayerReleased = true
            }
        }
    )

    fun play() {
        if (!isPlayerReleased) {
            player.start()
            isPlaying = true
        }
    }

    fun pause() {
        if (!isPlayerReleased) {
            player.pause()
            isPlaying = false
        }
    }

    fun forward() {
        if (!isPlayerReleased) {
            seekTo(player.currentPosition + 10_000F)
        }
    }

    fun rewind() {
        if (!isPlayerReleased) {
            seekTo(player.currentPosition - 10_000F)
        }
    }

    fun seekTo(position: Float) {
        if (!isPlayerReleased) {
            position.toLong().let { milliseconds ->
                mediaPosition = milliseconds.milliseconds
                player.seekTo(milliseconds, seekMode)
            }
        }
    }
}
