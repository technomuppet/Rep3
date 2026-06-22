package com.replog.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.replog.util.timer.RestPresets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.replogDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.replogDataStore

    private object Keys {
        val USE_KG = booleanPreferencesKey("use_kg")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val REST_SECONDS = intPreferencesKey("rest_seconds")
        val ACTIVE_SESSION_ID = intPreferencesKey("active_session_id")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val BODYWEIGHT_GOAL = doublePreferencesKey("bodyweight_goal")
        val CUSTOM_KG_PLATES = stringPreferencesKey("custom_kg_plates")
        val CUSTOM_LB_PLATES = stringPreferencesKey("custom_lb_plates")
        val REST_COMPOUND = intPreferencesKey("rest_compound")
        val REST_ISOLATION = intPreferencesKey("rest_isolation")
        val REST_BODYWEIGHT = intPreferencesKey("rest_bodyweight")
        val REST_CUSTOM_JSON = stringPreferencesKey("rest_custom_json")
        val REST_END_AT = longPreferencesKey("rest_end_at")
        val REST_STARTED_AT = longPreferencesKey("rest_started_at")
        val REST_PLANNED_SECONDS = intPreferencesKey("rest_planned_seconds")
        val REST_SESSION_EXERCISE_ID = intPreferencesKey("rest_session_exercise_id")
        val REST_SET_ID = intPreferencesKey("rest_set_id")
        val REST_SOUND_ENABLED = booleanPreferencesKey("rest_sound_enabled")
        val REST_VIBRATE_ENABLED = booleanPreferencesKey("rest_vibrate_enabled")
        val REST_AUTO_START = booleanPreferencesKey("rest_auto_start")
        val PROG_SUGGESTED_COUNT = intPreferencesKey("prog_suggested_count")
        val PROG_ACCEPTED_COUNT = intPreferencesKey("prog_accepted_count")
    }

    val useKg: Flow<Boolean> = store.data.map { it[Keys.USE_KG] ?: true }
    val isFirstLaunch: Flow<Boolean> = store.data.map { it[Keys.FIRST_LAUNCH] ?: true }
    val restSeconds: Flow<Int> = store.data.map { it[Keys.REST_SECONDS] ?: 90 }
    val activeSessionId: Flow<Int?> = store.data.map { it[Keys.ACTIVE_SESSION_ID]?.takeIf { id -> id > 0 } }
    val onboardingComplete: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val bodyweightGoal: Flow<Double?> = store.data.map { it[Keys.BODYWEIGHT_GOAL]?.takeIf { value -> value > 0.0 } }
    val customKgPlates: Flow<String> = store.data.map { it[Keys.CUSTOM_KG_PLATES] ?: PlateCalculator.formatPlates(PlateCalculator.metricPlates) }
    val customLbPlates: Flow<String> = store.data.map { it[Keys.CUSTOM_LB_PLATES] ?: PlateCalculator.formatPlates(PlateCalculator.imperialPlates) }

    private fun parseCustomMap(s: String): Map<Int, Int> = try {
        if (s.isBlank()) emptyMap() else s.split(",").mapNotNull {
            val kv = it.split(":"); if (kv.size == 2) kv[0].toIntOrNull()?.let { k -> k to (kv[1].toIntOrNull() ?: 90) } else null
        }.toMap()
    } catch (_: Exception) { emptyMap() }
    private fun encodeCustomMap(m: Map<Int, Int>): String = m.entries.joinToString(",") { "${it.key}:${it.value}" }

    val restPresets: Flow<RestPresets> = store.data.map { p ->
        RestPresets(
            compoundSeconds = p[Keys.REST_COMPOUND] ?: 180,
            isolationSeconds = p[Keys.REST_ISOLATION] ?: 90,
            bodyweightSeconds = p[Keys.REST_BODYWEIGHT] ?: 60,
            customByExerciseId = parseCustomMap(p[Keys.REST_CUSTOM_JSON].orEmpty())
        )
    }
    val restSoundEnabled: Flow<Boolean> = store.data.map { it[Keys.REST_SOUND_ENABLED] ?: true }
    val restVibrateEnabled: Flow<Boolean> = store.data.map { it[Keys.REST_VIBRATE_ENABLED] ?: true }
    val restAutoStart: Flow<Boolean> = store.data.map { it[Keys.REST_AUTO_START] ?: true }

    data class RestTimerPersist(
        val endAt: Long,
        val startedAt: Long,
        val plannedSeconds: Int,
        val sessionExerciseId: Int?,
        val setId: Int?
    )
    val restTimerPersist: Flow<RestTimerPersist?> = store.data.map { p ->
        val end = p[Keys.REST_END_AT] ?: return@map null
        if (end == 0L) return@map null
        RestTimerPersist(
            endAt = end,
            startedAt = p[Keys.REST_STARTED_AT] ?: end,
            plannedSeconds = p[Keys.REST_PLANNED_SECONDS] ?: 90,
            sessionExerciseId = p[Keys.REST_SESSION_EXERCISE_ID]?.takeIf { it > 0 },
            setId = p[Keys.REST_SET_ID]?.takeIf { it > 0 }
        )
    }

    data class ProgressionStats(val suggested: Int, val accepted: Int, val rate: Float)
    val progressionStats: Flow<ProgressionStats> = store.data.map { p ->
        val s = p[Keys.PROG_SUGGESTED_COUNT] ?: 0
        val a = p[Keys.PROG_ACCEPTED_COUNT] ?: 0
        ProgressionStats(s, a, if (s > 0) a.toFloat() / s else 0f)
    }

    suspend fun setUseKg(value: Boolean) { store.edit { it[Keys.USE_KG] = value } }
    suspend fun setFirstLaunchComplete() { store.edit { it[Keys.FIRST_LAUNCH] = false } }
    suspend fun setRestSeconds(value: Int) { store.edit { it[Keys.REST_SECONDS] = value.coerceIn(15, 600) } }
    suspend fun setCustomKgPlates(value: String) { store.edit { it[Keys.CUSTOM_KG_PLATES] = value } }
    suspend fun setCustomLbPlates(value: String) { store.edit { it[Keys.CUSTOM_LB_PLATES] = value } }
    suspend fun setBodyweightGoal(value: Double?) {
        store.edit { preferences ->
            if (value == null || value <= 0.0) preferences.remove(Keys.BODYWEIGHT_GOAL) else preferences[Keys.BODYWEIGHT_GOAL] = value
        }
    }
    suspend fun setActiveSessionId(value: Int?) {
        store.edit { preferences ->
            if (value == null || value <= 0) preferences.remove(Keys.ACTIVE_SESSION_ID) else preferences[Keys.ACTIVE_SESSION_ID] = value
        }
    }
    suspend fun setOnboardingComplete(value: Boolean = true) { store.edit { it[Keys.ONBOARDING_COMPLETE] = value } }

    suspend fun setRestPresets(presets: RestPresets) {
        store.edit {
            it[Keys.REST_COMPOUND] = presets.compoundSeconds.coerceIn(15, 600)
            it[Keys.REST_ISOLATION] = presets.isolationSeconds.coerceIn(15, 600)
            it[Keys.REST_BODYWEIGHT] = presets.bodyweightSeconds.coerceIn(15, 600)
            it[Keys.REST_CUSTOM_JSON] = encodeCustomMap(presets.customByExerciseId.filterValues { v -> v in 15..600 })
            it[Keys.REST_SECONDS] = presets.isolationSeconds
        }
    }
    suspend fun setRestSoundEnabled(v: Boolean) = store.edit { it[Keys.REST_SOUND_ENABLED] = v }
    suspend fun setRestVibrateEnabled(v: Boolean) = store.edit { it[Keys.REST_VIBRATE_ENABLED] = v }
    suspend fun setRestAutoStart(v: Boolean) = store.edit { it[Keys.REST_AUTO_START] = v }

    suspend fun persistRestTimer(endAt: Long, startedAt: Long, plannedSeconds: Int, sessionExerciseId: Int?, setId: Int?) {
        store.edit {
            if (endAt == 0L) {
                it.remove(Keys.REST_END_AT); it.remove(Keys.REST_STARTED_AT)
                it.remove(Keys.REST_PLANNED_SECONDS); it.remove(Keys.REST_SESSION_EXERCISE_ID); it.remove(Keys.REST_SET_ID)
            } else {
                it[Keys.REST_END_AT] = endAt
                it[Keys.REST_STARTED_AT] = startedAt
                it[Keys.REST_PLANNED_SECONDS] = plannedSeconds
                if (sessionExerciseId != null) it[Keys.REST_SESSION_EXERCISE_ID] = sessionExerciseId else it.remove(Keys.REST_SESSION_EXERCISE_ID)
                if (setId != null) it[Keys.REST_SET_ID] = setId else it.remove(Keys.REST_SET_ID)
            }
        }
    }

    suspend fun trackProgressionSuggested(count: Int = 1) {
        store.edit { it[Keys.PROG_SUGGESTED_COUNT] = (it[Keys.PROG_SUGGESTED_COUNT] ?: 0) + count }
    }
    suspend fun trackProgressionAccepted() {
        store.edit { it[Keys.PROG_ACCEPTED_COUNT] = (it[Keys.PROG_ACCEPTED_COUNT] ?: 0) + 1 }
    }
}
