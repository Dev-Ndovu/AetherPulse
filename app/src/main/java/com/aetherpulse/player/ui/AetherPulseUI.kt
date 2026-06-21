package com.aetherpulse.player.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherpulse.player.data.PlaylistEntity
import com.aetherpulse.player.data.Track
import com.aetherpulse.player.ui.theme.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    tracks: List<Track>,
    folders: Map<String, List<Track>>,
    playlists: List<PlaylistEntity>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackSelect: (Track) -> Unit,
    onTogglePlay: () -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "ÆTHER//PULSE", 
                        color = NeonCyan, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        },
        bottomBar = {
            // Elegant Control Deck fixed at the bottom
            PlaybackControlDeck(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onTogglePlay = onTogglePlay
            )
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Neon Nav Engine Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabs = listOf("TRACKS", "FOLDERS", "PLAYLISTS")
                tabs.forEachIndexed { index, title ->
                    TextButton(
                        onClick = { selectedTab = index },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            color = if (selectedTab == index) NeonCyan else TextSecondary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab View Router Switching Layouts Dynamically
            when (selectedTab) {
                0 -> TrackListLayout(tracks, onTrackSelect)
                1 -> FolderListLayout(folders, onTrackSelect)
                2 -> PlaylistLayout(playlists, onCreateClick = { showPlaylistDialog = true })
            }
        }
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("NEW PLAYLIST CORE", color = NeonCyan) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Core Identifier Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = PulseViolet
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showPlaylistDialog = false
                        }
                    }
                ) { Text("INITIALIZE") }
            },
            containerColor = DeepSpaceGrey
        )
    }
}

@Composable
fun TrackListLayout(tracks: List<Track>, onTrackSelect: (Track) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tracks) { track ->
            TrackGlassCard(track, onClick = { onTrackSelect(track) })
        }
    }
}

@Composable
fun FolderListLayout(folders: Map<String, List<Track>>, onTrackSelect: (Track) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        folders.forEach { (path, tracksInFolder) ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassSurface, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(path.substringAfterLast("/"), color = PulseViolet, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${tracksInFolder.size} Audio Elements Inside", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    tracksInFolder.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrackSelect(track) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(track.title, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistLayout(playlists: List<PlaylistEntity>, onCreateClick: () -> Unit) {
    Column {
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = PulseViolet),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GENERATE NEW CUSTOM REEFER", color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(playlists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassSurface, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(playlist.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.List, contentDescription = null, tint = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun TrackGlassCard(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
        }
        Text(
            text = String.format("%d:%02d", (track.duration / 1000) / 60, (track.duration / 1000) % 60),
            color = NeonCyan,
            fontSize = 12.sp
        )
    }
}

@Composable
fun PlaybackControlDeck(currentTrack: Track?, isPlaying: Boolean, onTogglePlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSpaceGrey)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(currentTrack?.title ?: "No System Signal", color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(currentTrack?.artist ?: "Deck Offline", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.background(Brush.horizontalGradient(listOf(NeonCyan, PulseViolet)), RoundedCornerShape(50.dp))
            ) {
                Text(if (isPlaying) "║" else "▶", color = ObsidianBlack, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // High-Fidelity Audio Dynamic Oscillator Wave
        SciFiWaveOscillator(isPlaying = isPlaying)
    }
}

@Composable
fun SciFiWaveOscillator(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "OscillatorWave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * java.lang.Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1200 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp)
    ) {
        val width = size.width
        val height = size.height
        val wavePath = Path()
        
        val amplitude = if (isPlaying) height * 0.4f else height * 0.08f
        val frequency = 0.02f

        wavePath.moveTo(0f, height / 2)

        for (x in 0..width.toInt() step 5) {
            val y = (sin((x * frequency) + waveOffset) * amplitude) + (height / 2)
            wavePath.lineTo(x.toFloat(), y)
        }

        drawPath(
            path = wavePath,
            color = NeonCyan,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Secondary aesthetic phase sync trace line
        val auxiliaryPath = Path()
        auxiliaryPath.moveTo(0f, height / 2)
        for (x in 0..width.toInt() step 5) {
            val y = (sin((x * frequency) - waveOffset + 180f) * (amplitude * 0.5f)) + (height / 2)
            auxiliaryPath.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = auxiliaryPath,
            color = PulseViolet,
            style = Stroke(width = 1.dp.toPx(), miter = 1f)
        )
    }
}
