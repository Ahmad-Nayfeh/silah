package io.github.ahmadnayfeh.silah.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ahmadnayfeh.silah.domain.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** What the user controls from the settings screen. Included in backups. */
@Serializable
data class Settings(
    val notifyMinute: Int = 19 * 60,
    /** ISO day numbers (Monday = 1 … Sunday = 7). */
    val notifyDays: Set<Int> = DayOfWeek.entries.map { it.value }.toSet(),
    val quietStartMinute: Int = 22 * 60 + 30,
    val quietEndMinute: Int = 8 * 60,
    val autoRhythm: Boolean = true,
    val manualPerWeek: Int = 3,
    val tagFamily: String = "العائلة",
    val tagFriends: String = "الأصدقاء",
    val tagOther: String = "آخرون",
    val theme: ThemeMode = ThemeMode.DARK,
) {
    val allowedDays: Set<DayOfWeek> get() = notifyDays.map { DayOfWeek.of(it) }.toSet()

    fun tagName(tag: Tag): String = when (tag) {
        Tag.FAMILY -> tagFamily
        Tag.FRIENDS -> tagFriends
        Tag.OTHER -> tagOther
    }

    /** True when [minuteOfDay] falls inside the night quiet period (which may cross midnight). */
    fun isQuiet(minuteOfDay: Int): Boolean {
        val s = quietStartMinute
        val e = quietEndMinute
        return when {
            s == e -> false
            s < e -> minuteOfDay in s until e
            else -> minuteOfDay >= s || minuteOfDay < e
        }
    }
}

/** Internal bookkeeping that is not part of backups. */
data class AppState(
    val pickDay: Long? = null,
    val pickPerson: Long? = null,
    /** The day the reminder notification is "live" (should be showing until handled). */
    val liveDay: Long? = null,
    /** The day the notification already made a sound; re-posts that day stay silent. */
    val alertedDay: Long? = null,
    /** The day the daily alarm was last handled, so a missed alarm can be caught up. */
    val handledDay: Long? = null,
    /** Person whose reminder was put away by the quiet period, to bring back in the morning. */
    val carryPerson: Long? = null,
    val threadPromptPerson: Long? = null,
    val threadPromptAt: Long? = null,
    val onboarded: Boolean = false,
)

class SettingsStore(private val store: DataStore<Preferences>) {

    private object K {
        val notifyMinute = intPreferencesKey("notify_minute")
        val notifyDays = stringPreferencesKey("notify_days")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val autoRhythm = booleanPreferencesKey("auto_rhythm")
        val manualPerWeek = intPreferencesKey("manual_per_week")
        val tagFamily = stringPreferencesKey("tag_family")
        val tagFriends = stringPreferencesKey("tag_friends")
        val tagOther = stringPreferencesKey("tag_other")
        val theme = stringPreferencesKey("theme")

        val pickDay = longPreferencesKey("pick_day")
        val pickPerson = longPreferencesKey("pick_person")
        val liveDay = longPreferencesKey("live_day")
        val alertedDay = longPreferencesKey("alerted_day")
        val handledDay = longPreferencesKey("handled_day")
        val carryPerson = longPreferencesKey("carry_person")
        val promptPerson = longPreferencesKey("prompt_person")
        val promptAt = longPreferencesKey("prompt_at")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        val d = Settings()
        Settings(
            notifyMinute = p[K.notifyMinute] ?: d.notifyMinute,
            notifyDays = p[K.notifyDays]?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet() ?: d.notifyDays,
            quietStartMinute = p[K.quietStart] ?: d.quietStartMinute,
            quietEndMinute = p[K.quietEnd] ?: d.quietEndMinute,
            autoRhythm = p[K.autoRhythm] ?: d.autoRhythm,
            manualPerWeek = p[K.manualPerWeek] ?: d.manualPerWeek,
            tagFamily = p[K.tagFamily] ?: d.tagFamily,
            tagFriends = p[K.tagFriends] ?: d.tagFriends,
            tagOther = p[K.tagOther] ?: d.tagOther,
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
        )
    }

    val state: Flow<AppState> = store.data.map { p ->
        AppState(
            pickDay = p[K.pickDay],
            pickPerson = p[K.pickPerson],
            liveDay = p[K.liveDay],
            alertedDay = p[K.alertedDay],
            handledDay = p[K.handledDay],
            carryPerson = p[K.carryPerson],
            threadPromptPerson = p[K.promptPerson],
            threadPromptAt = p[K.promptAt],
            onboarded = p[K.onboarded] ?: false,
        )
    }

    suspend fun currentSettings(): Settings = settings.first()
    suspend fun currentState(): AppState = state.first()

    suspend fun saveSettings(s: Settings) {
        store.edit { p ->
            p[K.notifyMinute] = s.notifyMinute
            p[K.notifyDays] = s.notifyDays.sorted().joinToString(",")
            p[K.quietStart] = s.quietStartMinute
            p[K.quietEnd] = s.quietEndMinute
            p[K.autoRhythm] = s.autoRhythm
            p[K.manualPerWeek] = s.manualPerWeek
            p[K.tagFamily] = s.tagFamily
            p[K.tagFriends] = s.tagFriends
            p[K.tagOther] = s.tagOther
            p[K.theme] = s.theme.name
        }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) = saveSettings(transform(currentSettings()))

    suspend fun updateState(transform: (AppState) -> AppState) {
        store.edit { p ->
            val old = AppState(
                pickDay = p[K.pickDay], pickPerson = p[K.pickPerson], liveDay = p[K.liveDay],
                alertedDay = p[K.alertedDay], handledDay = p[K.handledDay], carryPerson = p[K.carryPerson],
                threadPromptPerson = p[K.promptPerson], threadPromptAt = p[K.promptAt],
                onboarded = p[K.onboarded] ?: false,
            )
            val s = transform(old)
            fun set(key: Preferences.Key<Long>, v: Long?) {
                if (v == null) p.remove(key) else p[key] = v
            }
            set(K.pickDay, s.pickDay)
            set(K.pickPerson, s.pickPerson)
            set(K.liveDay, s.liveDay)
            set(K.alertedDay, s.alertedDay)
            set(K.handledDay, s.handledDay)
            set(K.carryPerson, s.carryPerson)
            set(K.promptPerson, s.threadPromptPerson)
            set(K.promptAt, s.threadPromptAt)
            p[K.onboarded] = s.onboarded
        }
    }

    /** Wipes settings and state (used by "erase all data"). */
    suspend fun clear() {
        store.edit { it.clear() }
    }
}
