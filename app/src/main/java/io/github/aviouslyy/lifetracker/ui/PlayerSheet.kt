package io.github.aviouslyy.lifetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aviouslyy.lifetracker.Counter
import io.github.aviouslyy.lifetracker.GameState
import io.github.aviouslyy.lifetracker.Marker

/** A tracked value inside the sheet: its label, optional color dot, current value and how to change it. */
private class SheetRow(
    val label: String,
    val value: Int,
    val dot: Color? = null,
    val onChange: (Int) -> Unit,
)

private class SheetToggle(val label: String, val on: Boolean, val onClick: () -> Unit)

/**
 * A player's counters, drawn inside their own panel so it faces them like the life total does.
 * Commander damage is tracked separately for every opponent and also comes off the life total.
 */
@Composable
fun PlayerSheet(
    state: GameState,
    seat: Int,
    onLifeChanged: (Int) -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    val player = state.players[seat]
    val rows = buildList {
        Counter.entries.forEach { counter ->
            add(SheetRow(counter.label, player.counter(counter)) { state.changeCounter(seat, counter, it) })
        }
        for (source in 0 until state.playerCount) {
            if (source == seat) continue
            val opponent = state.players[source]
            add(
                SheetRow(
                    label = "Cmdr · ${opponent.name}",
                    value = player.commanderDamageFrom(source),
                    dot = playerColor(opponent.colorIndex),
                ) { onLifeChanged(state.changeCommanderDamage(seat, source, it)) }
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = player.name.uppercase(),
                color = Color.White,
                style = LabelStyle.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            RoundIconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit name and color", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            RoundIconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // Sideways panels are wide and short, so use two columns when there is room.
            val columns = if (maxWidth >= 380.dp) 2 else 1
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val toggles = Marker.entries.map { marker ->
                    SheetToggle(marker.label, state.holder(marker) == seat) { state.toggleMarker(marker, seat) }
                } + SheetToggle("City's blessing", player.citysBlessing) { state.toggleCitysBlessing(seat) }
                val perLine = if (columns == 2) toggles.size else 2
                toggles.chunked(perLine).forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        line.forEach { Toggle(it.label, it.on, Modifier.weight(1f), it.onClick) }
                        repeat(perLine - line.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                rows.chunked(columns).forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        line.forEach { row -> CounterRow(row, Modifier.weight(1f)) }
                        if (line.size < columns) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Asks who [marker] goes to next, drawn inside the current holder's panel so it faces them.
 * Picking a player (or nobody) moves it and closes the picker.
 */
@Composable
fun PassMarkerSheet(
    state: GameState,
    seat: Int,
    marker: Marker,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PASS THE ${marker.label.uppercase()} TO",
                color = Color.White,
                style = LabelStyle.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            RoundIconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val columns = if (maxWidth >= 380.dp) 3 else 2
            val choices: List<Int?> = (0 until state.playerCount).filter { it != seat } + null
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                choices.chunked(columns).forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        line.forEach { target ->
                            PlayerChoice(
                                label = target?.let { state.players[it].name } ?: "Nobody",
                                dot = target?.let { playerColor(state.players[it].colorIndex) },
                                modifier = Modifier.weight(1f),
                            ) {
                                state.giveMarker(marker, target)
                                onClose()
                            }
                        }
                        repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerChoice(label: String, dot: Color?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dot)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label,
            color = if (dot != null) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CounterRow(row: SheetRow, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.dot != null) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(row.dot)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = row.label,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StepButton("−") { row.onChange(-1) }
        Text(
            text = row.value.toString(),
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            style = NumberStyle,
            modifier = Modifier.width(40.dp),
        )
        StepButton("+") { row.onChange(1) }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, style = NumberStyle)
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (on) Gold else Color.Black.copy(alpha = 0.22f))
            .border(1.dp, if (on) Gold else Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (on) OnGold else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
