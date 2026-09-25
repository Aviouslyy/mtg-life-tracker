package io.github.aviouslyy.lifetracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0E0E12)
val DialogSurface = Color(0xFF1C1C22)
val Muted = Color(0xFF2A2A32)

val PlayerColors = listOf(
    Color(0xFF2F6FB0), // blue
    Color(0xFFB8392F), // red
    Color(0xFF2E8756), // green
    Color(0xFF7D4FB5), // purple
    Color(0xFFC9772A), // orange
    Color(0xFF1C8C8C), // teal
    Color(0xFFB8456F), // pink
    Color(0xFF4F5B6B), // slate
)

fun playerColor(index: Int): Color = PlayerColors[index.mod(PlayerColors.size)]

@Composable
fun LifeTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFECEAF4),
            onPrimary = Color(0xFF15151A),
            surface = DialogSurface,
            onSurface = Color.White,
            surfaceContainerHigh = DialogSurface,
            background = Background,
        ),
        content = content,
    )
}
