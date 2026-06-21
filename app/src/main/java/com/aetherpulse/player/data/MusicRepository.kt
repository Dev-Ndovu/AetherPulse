package com.aetherpulse.player.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    suspend fun scanAudioFiles(): List<Track> = withContext(Dispatchers.IO) {
        val trackList = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA // Essential for determining absolute folders
        )

        // Select only music/audio files that aren't ringtones or system clips
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Track"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val absolutePath = cursor.getString(dataColumn) ?: ""

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                // Parse the directory structure safely out of the system paths
                val file = File(absolutePath)
                val folderPath = file.parent ?: "/Root"
                val folderName = file.parentFile?.name ?: "Internal Storage"

                if (duration > 5000) { // Filter out any short notification sounds under 5 seconds
                    trackList.add(
                        Track(
                            id = contentUri,
                            title = title,
                            artist = artist,
                            duration = duration,
                            size = size,
                            folderPath = folderPath,
                            folderName = folderName
                        )
                    )
                }
            }
        }
        return@withContext trackList
    }

    // Helper logic that instantly maps scanned files into a tidy folder tree mapping
    fun groupTracksByFolder(tracks: List<Track>): Map<String, List<Track>> {
        return tracks.groupBy { it.folderPath }
    }
}
