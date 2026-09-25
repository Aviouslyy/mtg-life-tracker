package io.github.aviouslyy.lifetracker.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlin.random.Random
import kotlinx.coroutines.delay

private val StartingLifeOptions = listOf(20, 30, 40)
private const val MAX_NAME_LENGTH = 20

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
        Surface(shape = RoundedCornerShape(28.dp), color = DialogSurface, contentColor = Color.White) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Game", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    GameTimer(state.startedAt)
                }

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
                }
                Text(
                    "Changing players or starting life starts a new game.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )

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
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().height(if (result == null) 0.dp else 44.dp),
                        )
                    }
                    Chip("Pick who goes first", Modifier.fillMaxWidth(), onClick = onPickFirstPlayer)
                }

                Chip("History", Modifier.fillMaxWidth(), onClick = onShowHistory)

                Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("New game", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("Edit player") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerColors.indices.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { i ->
                                ColorSwatch(
                                    color = PlayerColors[i],
                                    selected = i == colorIndex,
                                    onClick = { colorIndex = i },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = save) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color)
            .then(if (selected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun Segmented(options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Muted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.toString(),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Muted)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

/** Time since the game started, ticking every second. */
@Composable
private fun GameTimer(startedAt: Long) {
    val now by produceState(System.currentTimeMillis(), startedAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    Text(
        text = formatDuration(now - startedAt),
        fontSize = 18.sp,
        color = Color.White.copy(alpha = 0.6f),
        style = NumberStyle,
    )
}

@Composable
fun HistoryDialog(state: GameState, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = DialogSurface, contentColor = Color.White) {
            Column(Modifier.padding(vertical = 24.dp)) {
                Text(
                    "History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(12.dp))
                if (state.history.isEmpty()) {
                    Text(
                        "Nothing has happened yet this game.",
                        color = Color.White.copy(alpha = 0.6f),
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
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDuration(entry.time - state.startedAt),
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.5f),
            style = NumberStyle,
            modifier = Modifier.width(52.dp),
        )
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(playerColor(player.colorIndex))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(player.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                state.describe(entry.kind),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (entry.amount > 0) "+${entry.amount}" else "−${-entry.amount}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                style = NumberStyle,
            )
            Text(
                text = "→ ${entry.valueAfter}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                style = NumberStyle,
            )
        }
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
