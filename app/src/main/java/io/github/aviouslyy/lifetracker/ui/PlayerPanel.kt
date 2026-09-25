package io.github.aviouslyy.lifetracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import io.github.aviouslyy.lifetracker.Counter
import io.github.aviouslyy.lifetracker.GameState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Highlight { None, Cycling, Chosen }

private const val SMALL_STEP = 1
private const val BIG_STEP = 10
private const val DELTA_VISIBLE_MS = 1500L

/**
 * One player's area, modelled on Carbon: tap above the life total to gain 1 and below it to lose 1,
 * press and hold for 10. Swipe down (or tap the name) to open this player's counters.
 *
 * [rotation] turns the whole panel (0, 90, 180 or 270 degrees) so it faces the player's seat.
 */
@Composable
fun PlayerPanel(
    state: GameState,
    seat: Int,
    rotation: Int,
    highlight: Highlight,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = state.players[seat]
    val eliminated = state.isEliminated(seat)
    val baseColor = playerColor(player.colorIndex)
    val color by animateColorAsState(
        targetValue = if (eliminated) lerp(baseColor, Color.Black, 0.65f) else baseColor,
        animationSpec = tween(500),
        label = "panelColor",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (highlight == Highlight.None) 0.dp else 5.dp,
        animationSpec = tween(120),
        label = "highlightBorder",
    )

    var sheetOpen by remember(state.gameId) { mutableStateOf(false) }

    // Running total of recent life changes ("-3"), hidden shortly after the last one.
    var delta by remember(state.gameId) { mutableIntStateOf(0) }
    var deltaVisible by remember(state.gameId) { mutableStateOf(false) }
    var deltaChanges by remember(state.gameId) { mutableIntStateOf(0) }
    LaunchedEffect(state.gameId, deltaChanges) {
        if (deltaChanges > 0) {
            delay(DELTA_VISIBLE_MS)
            deltaVisible = false
        }
    }
    val showDelta = { amount: Int ->
        if (amount != 0) {
            if (!deltaVisible) delta = 0
            delta += amount
            deltaVisible = true
            deltaChanges++
        }
    }
    val changeLife = { amount: Int ->
        state.changeLife(seat, amount)
        showDelta(amount)
    }

    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .rotateLayout(rotation)
            .clip(shape)
            .background(color)
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, Color.White, shape) else Modifier)
    ) {
        AnimatedContent(
            targetState = sheetOpen,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically { -it } + fadeIn()) togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith (slideOutVertically { -it } + fadeOut())
                }
            },
            label = "sheet",
            modifier = Modifier.fillMaxSize(),
        ) { open ->
            if (open) {
                PlayerSheet(
                    state = state,
                    seat = seat,
                    onLifeChanged = showDelta,
                    onEdit = onEdit,
                    onClose = { sheetOpen = false },
                )
            } else {
                LifeView(
                    state = state,
                    seat = seat,
                    eliminated = eliminated,
                    highlight = highlight,
                    delta = delta,
                    showDelta = deltaVisible && delta != 0,
                    onLifeChange = changeLife,
                    onOpenSheet = { sheetOpen = true },
                )
            }
        }
    }
}

@Composable
private fun LifeView(
    state: GameState,
    seat: Int,
    eliminated: Boolean,
    highlight: Highlight,
    delta: Int,
    showDelta: Boolean,
    onLifeChange: (Int) -> Unit,
    onOpenSheet: () -> Unit,
) {
    val player = state.players[seat]
    val currentOnOpenSheet by rememberUpdatedState(onOpenSheet)
    val deltaAlpha by animateFloatAsState(
        targetValue = if (showDelta) 1f else 0f,
        animationSpec = tween(if (showDelta) 80 else 400),
        label = "deltaAlpha",
    )
    val status = when {
        highlight == Highlight.Chosen -> "GOES FIRST"
        eliminated -> "OUT"
        else -> null
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Carbon's gesture: pull down on a player to reach their counters.
            .pointerInput(Unit) {
                val threshold = 40.dp.toPx()
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                    },
                    onDragEnd = { if (dragged > threshold) currentOnOpenSheet() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val digitHeight = minOf(maxHeight * 0.36f, maxWidth * 0.3f)
        val lifeSize = with(LocalDensity.current) { digitHeight.toSp() }
        val glyphOffset = digitHeight * 0.62f + 14.dp

        Column(Modifier.fillMaxSize()) {
            TapZone(
                onTap = { onLifeChange(SMALL_STEP) },
                onLongPress = { onLifeChange(BIG_STEP) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            TapZone(
                onTap = { onLifeChange(-SMALL_STEP) },
                onLongPress = { onLifeChange(-BIG_STEP) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }

        Glyph("+", Modifier.offset(y = -glyphOffset))
        if (status == null) Glyph("−", Modifier.offset(y = glyphOffset))

        Text(
            text = player.life.toString(),
            color = Color.White,
            fontSize = lifeSize,
            fontWeight = FontWeight.Bold,
            style = NumberStyle,
        )

        Text(
            text = if (delta >= 0) "+$delta" else "−${-delta}",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            style = NumberStyle,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .alpha(deltaAlpha),
        )

        CounterBadges(
            state = state,
            seat = seat,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp),
        )

        Text(
            text = player.name,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .widthIn(max = maxWidth * 0.4f)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenSheet)
                .background(Color.Black.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        if (status != null) {
            Text(
                text = status,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

/** Small read-only badges beside the life total for every counter that is in use. */
@Composable
private fun CounterBadges(state: GameState, seat: Int, modifier: Modifier = Modifier) {
    val player = state.players[seat]
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.monarch == seat) Badge("Monarch")
        if (player.citysBlessing) Badge("Blessing")
        Counter.entries.forEach { counter ->
            val value = player.counter(counter)
            if (value > 0) Badge("${counter.short} $value")
        }
        player.commanderDamage.toSortedMap().forEach { (source, value) ->
            if (value > 0 && source < state.playerCount) {
                Badge("Cmdr $value", dot = playerColor(state.players[source].colorIndex))
            }
        }
    }
}

@Composable
private fun Badge(text: String, dot: Color? = null) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.22f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (dot != null) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dot)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            )
        }
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, style = NumberStyle)
    }
}

@Composable
private fun Glyph(symbol: String, modifier: Modifier = Modifier) {
    Text(
        text = symbol,
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 22.sp,
        fontWeight = FontWeight.Light,
        style = NumberStyle,
        modifier = modifier,
    )
}

@Composable
private fun TapZone(
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
                        scope.launch { flash.snapTo(0.1f) }
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
            }
    )
}

val NumberStyle = TextStyle(
    fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

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
