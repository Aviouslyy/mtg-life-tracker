package io.github.aviouslyy.lifetracker

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

data class Player(val name: String, val life: Int, val colorIndex: Int)

/**
 * The whole game, backed by SharedPreferences so it survives the app being closed.
 *
 * All [MAX_PLAYERS] seats are always kept, so switching the player count keeps names and colors.
 */
class GameState(private val prefs: SharedPreferences) {

    var playerCount by mutableIntStateOf(prefs.getInt(KEY_COUNT, 2).coerceIn(MIN_PLAYERS, MAX_PLAYERS))
        private set

    var startingLife by mutableIntStateOf(prefs.getInt(KEY_START, 20))
        private set

    /** Changes on every new game so the UI can drop transient state such as the recent-change badge. */
    var gameId by mutableIntStateOf(0)
        private set

    val players = mutableStateListOf<Player>().apply {
        for (i in 0 until MAX_PLAYERS) {
            add(
                Player(
                    name = prefs.getString("p${i}_name", null) ?: "Player ${i + 1}",
                    life = prefs.getInt("p${i}_life", startingLife),
                    colorIndex = prefs.getInt("p${i}_color", i),
                )
            )
        }
    }

    fun changeLife(index: Int, amount: Int) {
        val player = players[index]
        players[index] = player.copy(life = (player.life + amount).coerceIn(MIN_LIFE, MAX_LIFE))
        save()
    }

    fun updatePlayer(index: Int, name: String, colorIndex: Int) {
        players[index] = players[index].copy(name = name, colorIndex = colorIndex)
        save()
    }

    fun changePlayerCount(count: Int) {
        if (count == playerCount) return
        playerCount = count
        newGame()
    }

    fun changeStartingLife(life: Int) {
        if (life == startingLife) return
        startingLife = life
        newGame()
    }

    fun newGame() {
        for (i in players.indices) {
            players[i] = players[i].copy(life = startingLife)
        }
        gameId++
        save()
    }

    private fun save() {
        prefs.edit {
            putInt(KEY_COUNT, playerCount)
            putInt(KEY_START, startingLife)
            players.forEachIndexed { i, p ->
                putString("p${i}_name", p.name)
                putInt("p${i}_life", p.life)
                putInt("p${i}_color", p.colorIndex)
            }
        }
    }

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 6
        private const val MIN_LIFE = -999
        private const val MAX_LIFE = 9999
        private const val KEY_COUNT = "player_count"
        private const val KEY_START = "starting_life"
    }
}
