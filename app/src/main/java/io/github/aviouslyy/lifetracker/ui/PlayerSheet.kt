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

/** A tracked value inside the sheet: its label, optional color dot, current value and how to change it. */
private class SheetRow(
    val label: String,
    val value: Int,
    val dot: Color? = null,
    val onChange: (Int) -> Unit,
)

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
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = player.name,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Toggle("Monarch", state.monarch == seat, Modifier.weight(1f)) { state.toggleMonarch(seat) }
                    Toggle("City's blessing", player.citysBlessing, Modifier.weight(1f)) { state.toggleCitysBlessing(seat) }
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

@Composable
private fun CounterRow(row: SheetRow, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.2f))
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
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
            .background(Color.White.copy(alpha = 0.14f))
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) Color.White else Color.Black.copy(alpha = 0.2f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (on) Color.Black else Color.White,
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
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
