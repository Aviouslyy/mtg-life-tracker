package io.github.aviouslyy.lifetracker.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.aviouslyy.lifetracker.GameState
import io.github.aviouslyy.lifetracker.HistoryEntry
import io.github.aviouslyy.lifetracker.Player
import io.github.aviouslyy.lifetracker.R
import kotlin.random.Random

private val StartingLifeOptions = listOf(20, 30, 40)
private const val MAX_NAME_LENGTH = 20
private val DialogShape = RoundedCornerShape(28.dp)

/** [id] makes a repeated identical result (two 7s in a row) still animate. */
private data class RollResult(val id: Int, val text: String)

@Composable
fun GameMenuDialog(
    state: GameState,
    onNewGame: () -> Unit,
    onPickFirstPlayer: () -> Unit,
    onShowHistory: () -> Unit,
    onSettingsChanged: () -> Unit,
    onDismiss: () -> Unit,
) {
    var roll by remember { mutableStateOf<RollResult?>(null) }
    fun showRoll(text: String) {
        roll = RollResult((roll?.id ?: 0) + 1, text)
    }

    Dialog(onDismissRequest = onDismiss) {
        BrandSurface {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Wordmark()
                HorizontalDivider(color = Hairline)

                Labeled("Players") {
                    Segmented(
                        options = (GameState.MIN_PLAYERS..GameState.MAX_PLAYERS).toList(),
                        selected = state.playerCount,
                        onSelect = {
                            onSettingsChanged()
                            state.changePlayerCount(it)
                        },
                    )
                }
                Labeled("Starting life") {
                    Segmented(
                        options = StartingLifeOptions,
                        selected = state.startingLife,
                        onSelect = {
                            onSettingsChanged()
                            state.changeStartingLife(it)
                        },
                    )
                    Text(
                        "Changing either starts a new game.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }

                Labeled("Random") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("d6", Modifier.weight(1f)) { showRoll("d6 · ${Random.nextInt(1, 7)}") }
                        Chip("d20", Modifier.weight(1f)) { showRoll("d20 · ${Random.nextInt(1, 21)}") }
                        Chip("Coin", Modifier.weight(1f)) {
                            showRoll(if (Random.nextBoolean()) "Heads" else "Tails")
                        }
                    }
                    Crossfade(targetState = roll, label = "roll") { result ->
                        Text(
                            text = result?.text ?: "",
                            color = Gold,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            style = NumberStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (result == null) 0.dp else 44.dp)
                                .padding(top = 6.dp),
                        )
                    }
                    Chip("Pick who goes first", Modifier.fillMaxWidth(), onClick = onPickFirstPlayer)
                }

                Chip("Game history", Modifier.fillMaxWidth(), onClick = onShowHistory)

                Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("NEW GAME", style = LabelStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/** The Onyx gem and wordmark. */
@Composable
private fun Wordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_mark),
            contentDescription = null,
            modifier = Modifier.size(width = 30.dp, height = 28.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "ONYX",
                color = Gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 7.sp,
            )
            Text("LIFE COUNTER", color = TextSecondary, style = LabelStyle.copy(fontSize = 10.sp, letterSpacing = 2.5.sp))
        }
    }
}

@Composable
fun EditPlayerDialog(
    player: Player,
    onSave: (name: String, colorIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(player.name) }
    var colorIndex by remember { mutableIntStateOf(player.colorIndex) }
    val save = { onSave(name.trim().ifEmpty { player.name }, colorIndex) }

    Dialog(onDismissRequest = onDismiss) {
        BrandSurface {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("PLAYER", color = TextSecondary, style = LabelStyle)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Labeled("Color") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlayerColors.indices.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { i ->
                                    ColorSwatch(
                                        jewel = PlayerColors[i],
                                        selected = i == colorIndex,
                                        onClick = { colorIndex = i },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = save) { Text("Save", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(jewel: Jewel, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // A gold ring sits just outside the selected stone.
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(2.dp, Gold, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(jewel.brush)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = "${jewel.name}, selected", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun HistoryDialog(state: GameState, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        BrandSurface {
            Column(Modifier.padding(vertical = 24.dp)) {
                Text(
                    "GAME HISTORY",
                    color = TextSecondary,
                    style = LabelStyle,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(12.dp))
                if (state.history.isEmpty()) {
                    Text(
                        "Nothing has happened yet this game.",
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 460.dp)) {
                        items(state.history.asReversed()) { entry -> HistoryRow(state, entry) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(end = 16.dp),
                ) { Text("Close") }
            }
        }
    }
}

@Composable
private fun HistoryRow(state: GameState, entry: HistoryEntry) {
    val player = state.players[entry.seat]
    val amountColor = when {
        entry.kind != "life" -> Color.White
        entry.amount > 0 -> Gain
        else -> Loss
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDuration(entry.time - state.startedAt),
            fontSize = 12.sp,
            color = TextSecondary,
            style = NumberStyle,
            modifier = Modifier.width(52.dp),
        )
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(jewel(player.colorIndex).brush)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(player.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(state.describe(entry.kind), fontSize = 12.sp, color = TextSecondary, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (entry.amount > 0) "+${entry.amount}" else "−${-entry.amount}",
                color = amountColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                style = NumberStyle,
            )
            Text(
                text = "→ ${entry.valueAfter}",
                fontSize = 12.sp,
                color = TextSecondary,
                style = NumberStyle,
            )
        }
    }
}

@Composable
private fun BrandSurface(content: @Composable () -> Unit) {
    Surface(
        shape = DialogShape,
        color = DialogSurface,
        contentColor = Color.White,
        border = BorderStroke(1.dp, Hairline),
        content = content,
    )
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label.uppercase(), color = TextSecondary, style = LabelStyle)
        content()
    }
}

@Composable
private fun Segmented(options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Gold else Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.toString(),
                    color = if (isSelected) OnGold else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    style = NumberStyle,
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Muted)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
