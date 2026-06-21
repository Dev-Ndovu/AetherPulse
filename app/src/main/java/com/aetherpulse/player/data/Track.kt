package com.aetherpulse.player.data

import android.net.Uri

data class Track(
    val id: String,         // Storage content URI / file path
    val title: String,
    val artist: String,
    val duration: Long,     // Milliseconds
    val size: Long,         // Bytes
    val folderPath: String, // Full folder path for matching directory structures
    val folderName: String  // Simple directory name (e.g., "Download")
)
