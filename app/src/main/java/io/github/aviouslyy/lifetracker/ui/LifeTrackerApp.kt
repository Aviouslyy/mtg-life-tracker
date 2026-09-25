package io.github.aviouslyy.lifetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.aviouslyy.lifetracker.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val Gap = 6.dp

@Composable
fun LifeTrackerApp(state: GameState) {
    var showMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var highlighted by remember { mutableStateOf<Int?>(null) }
    var cycling by remember { mutableStateOf(false) }
    var pickJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun clearHighlight() {
        pickJob?.cancel()
        highlighted = null
        cycling = false
    }

    // Spin the highlight around the table, slowing down until it lands on a random player.
    fun pickFirstPlayer() {
        clearHighlight()
        pickJob = scope.launch {
            val count = state.playerCount
            val winner = Random.nextInt(count)
            val steps = count * maxOf(3, 12 / count) + winner
            cycling = true
            for (step in 0..steps) {
                highlighted = step % count
                delay(50L + step * 6L)
            }
            cycling = false
            delay(3500)
            highlighted = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(Gap)
    ) {
        PlayerGrid(state.playerCount) { index, rotation, modifier ->
            PlayerPanel(
                state = state,
                seat = index,
                rotation = rotation,
                highlight = when {
                    highlighted != index -> Highlight.None
                    cycling -> Highlight.Cycling
                    else -> Highlight.Chosen
                },
                onEdit = { editing = index },
                modifier = modifier,
            )
        }

        Box(
            Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .background(Background)
                .border(2.dp, Muted, CircleShape)
                .clickable { showMenu = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Game menu",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }

    if (showMenu) {
        GameMenuDialog(
            state = state,
            onNewGame = {
                clearHighlight()
                state.newGame()
                showMenu = false
            },
            onPickFirstPlayer = {
                showMenu = false
                pickFirstPlayer()
            },
            onShowHistory = {
                showMenu = false
                showHistory = true
            },
            onSettingsChanged = ::clearHighlight,
            onDismiss = { showMenu = false },
        )
    }

    if (showHistory) {
        HistoryDialog(state = state, onDismiss = { showHistory = false })
    }

    editing?.let { index ->
        EditPlayerDialog(
            player = state.players[index],
            onSave = { name, colorIndex ->
                state.updatePlayer(index, name, colorIndex)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/**
 * Two players sit across from each other. With more, the phone lies between two rows of players,
 * so panels on the left face left and panels on the right face right.
 */
@Composable
private fun PlayerGrid(
    count: Int,
    panel: @Composable (index: Int, rotation: Int, modifier: Modifier) -> Unit,
) {
    if (count == 2) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Gap)) {
            panel(0, 180, Modifier.weight(1f).fillMaxWidth())
            panel(1, 0, Modifier.weight(1f).fillMaxWidth())
        }
        return
    }

    val leftCount = (count + 1) / 2
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Gap)) {
            for (i in 0 until leftCount) panel(i, 90, Modifier.weight(1f).fillMaxWidth())
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Gap)) {
            for (i in leftCount until count) panel(i, 270, Modifier.weight(1f).fillMaxWidth())
        }
    }
}
