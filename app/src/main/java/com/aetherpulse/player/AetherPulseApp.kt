package com.aetherpulse.player

import android.app.Application
import com.aetherpulse.player.data.AppDatabase

class AetherPulseApp : Application() {
    // Single instance of database shared across the whole app context cleanly
    val database by lazy { AppDatabase.getDatabase(this) }
}
