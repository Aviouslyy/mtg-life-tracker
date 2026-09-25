package io.github.aviouslyy.lifetracker

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Extra counters a player can track next to their life total. */
enum class Counter(val label: String, val short: String) {
    Poison("Poison", "Poison"),
    Energy("Energy", "Energy"),
    Experience("Experience", "Exp"),
    CommanderTax("Commander tax", "Tax"),
    Storm("Storm", "Storm"),
}

data class Player(
    val name: String,
    val life: Int,
    val colorIndex: Int,
    val counters: Map<Counter, Int> = emptyMap(),
    /** Commander damage taken, keyed by the seat index of the player whose commander dealt it. */
    val commanderDamage: Map<Int, Int> = emptyMap(),
    val citysBlessing: Boolean = false,
) {
    fun counter(counter: Counter): Int = counters[counter] ?: 0
    fun commanderDamageFrom(seat: Int): Int = commanderDamage[seat] ?: 0
}

/**
 * One line in the game log. Rapid taps on the same thing are merged, so three quick taps
 * read as a single "-3" rather than "-1 -1 -1".
 */
data class HistoryEntry(
    val seat: Int,
    /** "life", a [Counter] name, or "cmd:<source seat>". */
    val kind: String,
    val amount: Int,
    val valueAfter: Int,
    val time: Long,
)

/**
 * The whole game, backed by SharedPreferences so it survives the app being closed.
 *
 * All [MAX_PLAYERS] seats are always kept, so switching the player count keeps names and colors.
 */
class GameState(private val prefs: SharedPreferences, private val clock: () -> Long = System::currentTimeMillis) {

    var playerCount by mutableIntStateOf(prefs.getInt(KEY_COUNT, 2).coerceIn(MIN_PLAYERS, MAX_PLAYERS))
        private set

    var startingLife by mutableIntStateOf(prefs.getInt(KEY_START, 20))
        private set

    /** Seat of the player who is the monarch, if anyone. */
    var monarch by mutableStateOf(prefs.getInt(KEY_MONARCH, -1).takeIf { it >= 0 })
        private set

    var startedAt by mutableLongStateOf(prefs.getLong(KEY_STARTED_AT, clock()))
        private set

    /** Changes on every new game so the UI can drop transient state such as the recent-change badge. */
    var gameId by mutableIntStateOf(0)
        private set

    val players = mutableStateListOf<Player>().apply {
        for (i in 0 until MAX_PLAYERS) add(loadPlayer(i))
    }

    val history = mutableStateListOf<HistoryEntry>().apply { addAll(loadHistory()) }

    fun isEliminated(seat: Int): Boolean {
        val p = players[seat]
        return p.life <= 0 ||
            p.counter(Counter.Poison) >= LETHAL_POISON ||
            p.commanderDamage.values.any { it >= LETHAL_COMMANDER_DAMAGE }
    }

    fun changeLife(seat: Int, amount: Int) {
        val player = players[seat]
        val life = (player.life + amount).coerceIn(MIN_LIFE, MAX_LIFE)
        players[seat] = player.copy(life = life)
        log(seat, KIND_LIFE, amount, life)
        save()
    }

    fun changeCounter(seat: Int, counter: Counter, amount: Int) {
        val player = players[seat]
        val value = (player.counter(counter) + amount).coerceIn(0, MAX_COUNTER)
        if (value == player.counter(counter)) return
        players[seat] = player.copy(counters = player.counters + (counter to value))
        log(seat, counter.name, value - player.counter(counter), value)
        save()
    }

    /** Commander damage is also life loss, so this moves the life total the other way. Returns the life change. */
    fun changeCommanderDamage(seat: Int, sourceSeat: Int, amount: Int): Int {
        val player = players[seat]
        val before = player.commanderDamageFrom(sourceSeat)
        val value = (before + amount).coerceIn(0, MAX_COUNTER)
        val applied = value - before
        if (applied == 0) return 0
        val life = (player.life - applied).coerceIn(MIN_LIFE, MAX_LIFE)
        players[seat] = player.copy(life = life, commanderDamage = player.commanderDamage + (sourceSeat to value))
        log(seat, "$KIND_COMMANDER$sourceSeat", applied, value)
        save()
        return -applied
    }

    fun toggleMonarch(seat: Int) {
        monarch = if (monarch == seat) null else seat
        save()
    }

    fun toggleCitysBlessing(seat: Int) {
        players[seat] = players[seat].copy(citysBlessing = !players[seat].citysBlessing)
        save()
    }

    fun updatePlayer(seat: Int, name: String, colorIndex: Int) {
        players[seat] = players[seat].copy(name = name, colorIndex = colorIndex)
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
            players[i] = players[i].copy(
                life = startingLife,
                counters = emptyMap(),
                commanderDamage = emptyMap(),
                citysBlessing = false,
            )
        }
        monarch = null
        history.clear()
        startedAt = clock()
        gameId++
        save()
    }

    /** Human-readable name for a history entry's [HistoryEntry.kind]. */
    fun describe(kind: String): String = when {
        kind == KIND_LIFE -> "Life"
        kind.startsWith(KIND_COMMANDER) -> {
            val source = kind.removePrefix(KIND_COMMANDER).toIntOrNull()
            "Commander damage from " + (source?.let { players.getOrNull(it)?.name } ?: "?")
        }
        else -> runCatching { Counter.valueOf(kind).label }.getOrDefault(kind)
    }

    private fun log(seat: Int, kind: String, amount: Int, valueAfter: Int) {
        val now = clock()
        val last = history.lastOrNull()
        if (last != null && last.seat == seat && last.kind == kind && now - last.time < MERGE_WINDOW_MS) {
            val merged = last.amount + amount
            if (merged == 0) {
                history.removeAt(history.lastIndex)
            } else {
                history[history.lastIndex] = last.copy(amount = merged, valueAfter = valueAfter, time = now)
            }
        } else {
            history.add(HistoryEntry(seat, kind, amount, valueAfter, now))
            if (history.size > MAX_HISTORY) history.removeAt(0)
        }
    }

    private fun loadPlayer(i: Int): Player {
        val counters = Counter.entries
            .associateWith { prefs.getInt("p${i}_c_${it.name}", 0) }
            .filterValues { it != 0 }
        val commander = (0 until MAX_PLAYERS)
            .associateWith { prefs.getInt("p${i}_cmd_$it", 0) }
            .filterValues { it != 0 }
        return Player(
            name = prefs.getString("p${i}_name", null) ?: "Player ${i + 1}",
            life = prefs.getInt("p${i}_life", startingLife),
            colorIndex = prefs.getInt("p${i}_color", i),
            counters = counters,
            commanderDamage = commander,
            citysBlessing = prefs.getBoolean("p${i}_blessing", false),
        )
    }

    private fun loadHistory(): List<HistoryEntry> = runCatching {
        val array = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            HistoryEntry(o.getInt("s"), o.getString("k"), o.getInt("a"), o.getInt("v"), o.getLong("t"))
        }
    }.getOrDefault(emptyList())

    private fun save() {
        val historyJson = JSONArray()
        history.forEach { e ->
            historyJson.put(
                JSONObject().put("s", e.seat).put("k", e.kind).put("a", e.amount).put("v", e.valueAfter).put("t", e.time)
            )
        }
        prefs.edit {
            putInt(KEY_COUNT, playerCount)
            putInt(KEY_START, startingLife)
            putInt(KEY_MONARCH, monarch ?: -1)
            putLong(KEY_STARTED_AT, startedAt)
            putString(KEY_HISTORY, historyJson.toString())
            players.forEachIndexed { i, p ->
                putString("p${i}_name", p.name)
                putInt("p${i}_life", p.life)
                putInt("p${i}_color", p.colorIndex)
                putBoolean("p${i}_blessing", p.citysBlessing)
                Counter.entries.forEach { putInt("p${i}_c_${it.name}", p.counter(it)) }
                for (source in 0 until MAX_PLAYERS) putInt("p${i}_cmd_$source", p.commanderDamageFrom(source))
            }
        }
    }

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 6
        const val LETHAL_POISON = 10
        const val LETHAL_COMMANDER_DAMAGE = 21
        private const val MIN_LIFE = -999
        private const val MAX_LIFE = 9999
        private const val MAX_COUNTER = 999
        private const val MAX_HISTORY = 300
        private const val MERGE_WINDOW_MS = 3000L
        private const val KIND_LIFE = "life"
        private const val KIND_COMMANDER = "cmd:"
        private const val KEY_COUNT = "player_count"
        private const val KEY_START = "starting_life"
        private const val KEY_MONARCH = "monarch"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_HISTORY = "history"
    }
}
