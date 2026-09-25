package io.github.aviouslyy.lifetracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aviouslyy.lifetracker.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Highlight { None, Cycling, Chosen }

private const val SMALL_STEP = 1
private const val BIG_STEP = 10
private const val DELTA_VISIBLE_MS = 1500L

/**
 * One player's area. Tap the left half to lose 1 life and the right half to gain 1;
 * press and hold for 10. Tap the name to rename the player or change their color.
 *
 * [rotation] turns the whole panel (0, 90, 180 or 270 degrees) so it faces the player's seat.
 */
@Composable
fun PlayerPanel(
    player: Player,
    rotation: Int,
    gameId: Int,
    highlight: Highlight,
    onLifeChange: (Int) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = playerColor(player.colorIndex)
    val color by animateColorAsState(
        targetValue = if (player.life <= 0) lerp(baseColor, Color.Black, 0.6f) else baseColor,
        label = "panelColor",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (highlight == Highlight.None) 0.dp else 5.dp,
        animationSpec = tween(120),
        label = "highlightBorder",
    )

    // Running total of recent changes ("-3"), hidden shortly after the last tap.
    var delta by remember(gameId) { mutableIntStateOf(0) }
    var deltaVisible by remember(gameId) { mutableStateOf(false) }
    var deltaTaps by remember(gameId) { mutableIntStateOf(0) }
    LaunchedEffect(gameId, deltaTaps) {
        if (deltaTaps > 0) {
            delay(DELTA_VISIBLE_MS)
            deltaVisible = false
        }
    }
    val change = { amount: Int ->
        onLifeChange(amount)
        if (!deltaVisible) delta = 0
        delta += amount
        deltaVisible = true
        deltaTaps++
    }

    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .rotateLayout(rotation)
            .clip(shape)
            .background(color)
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, Color.White, shape) else Modifier)
    ) {
        Row(Modifier.fillMaxSize()) {
            TapZone(
                symbol = "−",
                alignment = Alignment.CenterStart,
                onTap = { change(-SMALL_STEP) },
                onLongPress = { change(-BIG_STEP) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            TapZone(
                symbol = "+",
                alignment = Alignment.CenterEnd,
                onTap = { change(SMALL_STEP) },
                onLongPress = { change(BIG_STEP) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        LifeTotal(life = player.life, delta = delta, showDelta = deltaVisible && delta != 0)

        Text(
            text = player.name,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onEdit)
                .background(Color.Black.copy(alpha = 0.18f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )

        if (highlight == Highlight.Chosen) {
            Text(
                text = "GOES FIRST",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun LifeTotal(life: Int, delta: Int, showDelta: Boolean) {
    val deltaAlpha by animateFloatAsState(
        targetValue = if (showDelta) 1f else 0f,
        animationSpec = tween(if (showDelta) 80 else 400),
        label = "deltaAlpha",
    )

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val digitHeight = minOf(maxHeight * 0.42f, maxWidth * 0.3f)
        val lifeSize = with(LocalDensity.current) { digitHeight.toSp() }
        val noPadding = TextStyle(
            fontFeatureSettings = "tnum",
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )

        Text(
            text = life.toString(),
            color = Color.White,
            fontSize = lifeSize,
            fontWeight = FontWeight.Bold,
            style = noPadding,
        )
        Text(
            text = if (delta >= 0) "+$delta" else "−${-delta}",
            color = Color.White,
            fontSize = lifeSize * 0.24f,
            fontWeight = FontWeight.SemiBold,
            style = noPadding,
            modifier = Modifier
                .offset(y = -digitHeight * 0.72f)
                .alpha(deltaAlpha),
        )
    }
}

@Composable
private fun TapZone(
    symbol: String,
    alignment: Alignment,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val flash = remember { Animatable(0f) }

    Box(
        modifier
            .drawBehind { drawRect(Color.White.copy(alpha = flash.value)) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { flash.snapTo(0.12f) }
                        tryAwaitRelease()
                        scope.launch { flash.animateTo(0f, tween(300)) }
                    },
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnTap()
                    },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnLongPress()
                    },
                )
            },
        contentAlignment = alignment,
    ) {
        Text(
            text = symbol,
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 36.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(horizontal = 22.dp),
        )
    }
}

/**
 * Rotates content by a multiple of 90 degrees. Unlike [Modifier.rotate], a quarter turn also swaps
 * the width and height the content is laid out with, so a tall slot gets wide, sideways content.
 */
fun Modifier.rotateLayout(degrees: Int): Modifier {
    if (degrees % 180 == 0) return rotate(degrees.toFloat())
    return this
        .layout { measurable, constraints ->
            val placeable = measurable.measure(
                Constraints(
                    minWidth = constraints.minHeight,
                    maxWidth = constraints.maxHeight,
                    minHeight = constraints.minWidth,
                    maxHeight = constraints.maxWidth,
                )
            )
            layout(placeable.height, placeable.width) {
                placeable.place(
                    x = -(placeable.width - placeable.height) / 2,
                    y = -(placeable.height - placeable.width) / 2,
                )
            }
        }
        .rotate(degrees.toFloat())
}
