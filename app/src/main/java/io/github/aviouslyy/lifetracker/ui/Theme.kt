package io.github.aviouslyy.lifetracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.aviouslyy.lifetracker.R

// Onyx: near-black surfaces, jewel-tone players and a single champagne-gold accent.
val Background = Color(0xFF09090C)
val DialogSurface = Color(0xFF15151B)
val Muted = Color(0xFF23232B)
val Hairline = Color.White.copy(alpha = 0.08f)
val TextSecondary = Color.White.copy(alpha = 0.55f)

val Gold = Color(0xFFD8B36A)
val GoldLight = Color(0xFFF1DCA7)
val GoldDeep = Color(0xFF9C7636)
val OnGold = Color(0xFF1C1508)
val GoldRing = Brush.sweepGradient(listOf(GoldLight, Gold, GoldDeep, Gold, GoldLight))

val Gain = Color(0xFF8FD6A8)
val Loss = Color(0xFFF19A9A)

val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
)

/** Numbers use tabular figures so totals don't shift sideways as digits change. */
val NumberStyle = TextStyle(
    fontFamily = Outfit,
    fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/** Small uppercase labels with wide tracking. */
val LabelStyle = TextStyle(
    fontFamily = Outfit,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 1.8.sp,
)

class Jewel(val name: String, val color: Color) {
    /** Lit from above: a touch lighter at the top, deeper at the bottom. */
    val brush: Brush = Brush.verticalGradient(
        listOf(lerp(color, Color.White, 0.10f), color, lerp(color, Color.Black, 0.35f))
    )
}

val PlayerColors = listOf(
    Jewel("Sapphire", Color(0xFF2C5FA8)),
    Jewel("Ruby", Color(0xFFA8323E)),
    Jewel("Emerald", Color(0xFF1E7F57)),
    Jewel("Amethyst", Color(0xFF6A45A6)),
    Jewel("Amber", Color(0xFFB8741F)),
    Jewel("Jade", Color(0xFF167C78)),
    Jewel("Garnet", Color(0xFF9E3A63)),
    Jewel("Slate", Color(0xFF4A5566)),
)

fun jewel(index: Int): Jewel = PlayerColors[index.mod(PlayerColors.size)]

fun playerColor(index: Int): Color = jewel(index).color

private fun TextStyle.outfit() = copy(fontFamily = Outfit)

@Composable
fun LifeTrackerTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = OnGold,
            surface = DialogSurface,
            onSurface = Color.White,
            onSurfaceVariant = TextSecondary,
            surfaceContainerHigh = DialogSurface,
            outline = Color.White.copy(alpha = 0.2f),
            background = Background,
        ),
        typography = Typography(
            displayLarge = base.displayLarge.outfit(),
            displayMedium = base.displayMedium.outfit(),
            displaySmall = base.displaySmall.outfit(),
            headlineLarge = base.headlineLarge.outfit(),
            headlineMedium = base.headlineMedium.outfit(),
            headlineSmall = base.headlineSmall.outfit(),
            titleLarge = base.titleLarge.outfit(),
            titleMedium = base.titleMedium.outfit(),
            titleSmall = base.titleSmall.outfit(),
            bodyLarge = base.bodyLarge.outfit(),
            bodyMedium = base.bodyMedium.outfit(),
            bodySmall = base.bodySmall.outfit(),
            labelLarge = base.labelLarge.outfit(),
            labelMedium = base.labelMedium.outfit(),
            labelSmall = base.labelSmall.outfit(),
        ),
        content = content,
    )
}
