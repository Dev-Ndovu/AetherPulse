package com.aetherpulse.player

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aetherpulse.player.data.MusicRepository
import com.aetherpulse.player.data.PlaylistEntity
import com.aetherpulse.player.data.Track
import com.aetherpulse.player.playback.PlaybackService
import com.aetherpulse.player.ui.MainDashboard
import com.aetherpulse.player.ui.theme.AetherPulseTheme
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var musicRepository: MusicRepository
    private var mediaController: MediaController? = null

    // Stateful bridge tracking layers
    private var tracks by mutableStateOf<List<Track>>(emptyList())
    private var folders by mutableStateOf<Map<String, List<Track>>>(emptyMap())
    private var playlists by mutableStateOf<List<PlaylistEntity>>(emptyList())
    private var currentTrack by mutableStateOf<Track?>(null)
    private var isPlaying by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        musicRepository = MusicRepository(this)
        val database = (application as AetherPulseApp).database

        // Safely pull playlist entities out of local cache streams
        lifecycleScope.launch {
            database.playlistDao().getAllPlaylists().collect {
                playlists = it
            }
        }

        // Direct security permission trigger sequence
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                loadAudioData()
            }
        }

        setContent {
            AetherPulseTheme {
                MainDashboard(
                    tracks = tracks,
                    folders = folders,
                    playlists = playlists,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onTrackSelect = { selectedTrack ->
                        playAudioTrack(selectedTrack)
                    },
                    onTogglePlay = {
                        togglePlaybackState()
                    },
                    onCreatePlaylist = { name ->
                        lifecycleScope.launch {
                            database.playlistDao().createPlaylist(PlaylistEntity(name = name))
                        }
                    }
                )
            }
        }

        // Fire permission request safely depending on system version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        initializeMediaController()
    }

    private fun loadAudioData() {
        lifecycleScope.launch {
            val scannedTracks = musicRepository.scanAudioFiles()
            tracks = scannedTracks
            folders = musicRepository.groupTracksByFolder(scannedTracks)
        }
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    val currentUri = mediaItem?.mediaId
                    currentTrack = tracks.find { it.id == currentUri }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    private fun playAudioTrack(track: Track) {
        val controller = mediaController ?: return
        currentTrack = track
        
        // Feed the Media3 system the selected song item mapping sequence
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.id)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    private fun togglePlaybackState() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    override fun onDestroy() {
        mediaController?.release()
        super.onDestroy()
    }
}
