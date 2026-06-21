package com.aetherpulse.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberpunkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = PulseViolet,
    background = ObsidianBlack,
    surface = DeepSpaceGrey,
    onPrimary = ObsidianBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun AetherPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyberpunkColorScheme,
        content = content
    )
}
