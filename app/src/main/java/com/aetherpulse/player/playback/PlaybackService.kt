package com.aetherpulse.player.playback

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var crossfadeJob: Job? = null
    private val crossfadeDurationMs = 4000L // 4 second premium blend

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        exoPlayer = ExoPlayer.Builder(this)
            .setHandleAudioAttributes(true, true)
            .build()

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .build()

        // Set up our loop monitor to detect when a song is finishing to start crossfading
        setupCrossfadeMonitor()
    }

    private fun setupCrossfadeMonitor() {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Reset volume state when starting a new track cleanly
                exoPlayer?.volume = 1.0f
            }
        })

        // Poll the playback progress periodically to detect the end of a track
        serviceScope.launch {
            while (isActive) {
                delay(500)
                val player = exoPlayer ?: continue
                
                if (player.isPlaying && player.duration > 0) {
                    val remainingTime = player.duration - player.currentPosition
                    
                    // If we're within our crossfade threshold and have an upcoming track in queue
                    if (remainingTime <= crossfadeDurationMs && player.hasNextMediaItem() && crossfadeJob == null) {
                        triggerCrossfade()
                    }
                }
            }
        }
    }

    private fun triggerCrossfade() {
        crossfadeJob = serviceScope.launch {
            val player = exoPlayer ?: return@launch
            val steps = 20
            val delayStep = crossfadeDurationMs / steps

            // Smoothly ramp down current audio volume
            for (i in steps downTo 0) {
                player.volume = i.toFloat() / steps
                delay(delayStep)
            }

            // Instantly skip over to the next song in the queue line
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()

                // Smoothly ramp up the new audio track volume
                for (i in 0..steps) {
                    player.volume = i.toFloat() / steps
                    delay(delayStep)
                }
                player.volume = 1.0f
            }
            crossfadeJob = null
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        crossfadeJob?.cancel()
        exoPlayer?.release()
        mediaSession?.release()
        exoPlayer = null
        mediaSession = null
        super.onDestroy()
    }
}
