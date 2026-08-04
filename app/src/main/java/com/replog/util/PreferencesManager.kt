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
import com.replog.util.legal.LegalAcceptance
import com.replog.util.profile.UserProfile
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
        val COACH_LAST_GEN_DAY = longPreferencesKey("coach_last_gen_day")
        val COACH_LAST_GEN_SESSION_COUNT = intPreferencesKey("coach_last_gen_session_count")
        val PROFILE_GOAL = stringPreferencesKey("profile_goal")
        val PROFILE_LEVEL = stringPreferencesKey("profile_level")
        val PROFILE_EQUIPMENT = stringPreferencesKey("profile_equipment")
        val PROFILE_DAYS = intPreferencesKey("profile_days_per_week")
        val PROFILE_STYLE = stringPreferencesKey("profile_style")
        // Export location: persisted SAF tree URI + human-readable label. When the
        // URI is blank, exports fall back to the public Downloads/RepLog folder.
        val EXPORT_TREE_URI = stringPreferencesKey("export_tree_uri")
        val EXPORT_FOLDER_LABEL = stringPreferencesKey("export_folder_label")
        // Sprint 5 P1: which field to auto-focus after completing a set ("weight" | "reps").
        val AUTO_FOCUS_FIELD = stringPreferencesKey("auto_focus_field")

        // Sprint 12: user identity (profile). Display name is the only required field.
        val PROFILE_DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        val PROFILE_DOB_EPOCH_DAY = longPreferencesKey("profile_dob_epoch_day")
        val PROFILE_HEIGHT_CM = doublePreferencesKey("profile_height_cm")
        val PROFILE_WEIGHT_KG = doublePreferencesKey("profile_weight_kg")

        // Sprint 12: versioned legal acceptance (latest record).
        val LEGAL_ACCEPTED_SIGNATURE = stringPreferencesKey("legal_accepted_signature")
        val LEGAL_DISCLAIMER_VERSION = stringPreferencesKey("legal_disclaimer_version")
        val LEGAL_TERMS_VERSION = stringPreferencesKey("legal_terms_version")
        val LEGAL_PRIVACY_VERSION = stringPreferencesKey("legal_privacy_version")
        val LEGAL_ACCEPTED_AT = longPreferencesKey("legal_accepted_at")
        val LEGAL_ACCEPTED_APP_VERSION = stringPreferencesKey("legal_accepted_app_version")
        val LEGAL_ACCEPTED_NAME = stringPreferencesKey("legal_accepted_name")
        val LEGAL_COMPLETED = booleanPreferencesKey("legal_completed")
        // Append-only acceptance history, one record per line; fields tab-separated.
        val LEGAL_HISTORY = stringPreferencesKey("legal_acceptance_history")
        // Sprint 12: pre-workout fuel-up reminder fatigue suppression.
        val PRE_WORKOUT_REMINDER_SKIPS = intPreferencesKey("pre_workout_reminder_skips")
        val PRE_WORKOUT_REMINDER_SNOOZED_UNTIL = longPreferencesKey("pre_workout_reminder_snoozed_until")
        val PRE_WORKOUT_REMINDER_PENDING = booleanPreferencesKey("pre_workout_reminder_pending")
    }

    val useKg: Flow<Boolean> = store.data.map { it[Keys.USE_KG] ?: true }
    val isFirstLaunch: Flow<Boolean> = store.data.map { it[Keys.FIRST_LAUNCH] ?: true }
    val restSeconds: Flow<Int> = store.data.map { it[Keys.REST_SECONDS] ?: 90 }
    val activeSessionId: Flow<Int?> = store.data.map { it[Keys.ACTIVE_SESSION_ID]?.takeIf { id -> id > 0 } }
    val onboardingComplete: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val preWorkoutReminderSkips: Flow<Int> = store.data.map { it[Keys.PRE_WORKOUT_REMINDER_SKIPS] ?: 0 }
    val preWorkoutReminderSnoozedUntil: Flow<Long?> = store.data.map {
        it[Keys.PRE_WORKOUT_REMINDER_SNOOZED_UNTIL]?.takeIf { timestamp -> timestamp > 0L }
    }
    val preWorkoutReminderPending: Flow<Boolean> = store.data.map { it[Keys.PRE_WORKOUT_REMINDER_PENDING] ?: false }
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

    /** Persisted SAF tree URI for exports; blank means "use Downloads/RepLog". */
    val exportTreeUri: Flow<String?> = store.data.map { it[Keys.EXPORT_TREE_URI]?.takeIf { uri -> uri.isNotBlank() } }
    /** Human-readable label for the export folder shown in Settings. */
    val exportFolderLabel: Flow<String> = store.data.map { it[Keys.EXPORT_FOLDER_LABEL] ?: "Downloads/RepLog" }
    suspend fun setExportFolder(treeUri: String?, label: String) {
        store.edit {
            if (treeUri.isNullOrBlank()) it.remove(Keys.EXPORT_TREE_URI) else it[Keys.EXPORT_TREE_URI] = treeUri
            it[Keys.EXPORT_FOLDER_LABEL] = label
        }
    }

    suspend fun setUseKg(value: Boolean) { store.edit { it[Keys.USE_KG] = value } }
    suspend fun setFirstLaunchComplete() { store.edit { it[Keys.FIRST_LAUNCH] = false } }
    suspend fun setRestSeconds(value: Int) { store.edit { it[Keys.REST_SECONDS] = value.coerceIn(15, 600) } }

    /** Increment the pre-workout reminder dismissal counter (used for fatigue suppression). */
    suspend fun recordPreWorkoutReminderDismissal() {
        store.edit { it[Keys.PRE_WORKOUT_REMINDER_SKIPS] = (it[Keys.PRE_WORKOUT_REMINDER_SKIPS] ?: 0) + 1 }
    }

    /** Mark that another screen created a session and Active Workout should show the reminder. */
    suspend fun requestPreWorkoutReminder() {
        store.edit { it[Keys.PRE_WORKOUT_REMINDER_PENDING] = true }
    }

    /** Consume a pending request exactly once when Active Workout resumes the session. */
    suspend fun consumePreWorkoutReminderRequest(): Boolean {
        var requested = false
        store.edit {
            requested = it[Keys.PRE_WORKOUT_REMINDER_PENDING] == true
            it.remove(Keys.PRE_WORKOUT_REMINDER_PENDING)
        }
        return requested
    }

    /** Clear persisted suppression state, useful for user-facing reminder reset controls. */
    suspend fun clearPreWorkoutReminderSuppression() {
        store.edit {
            it.remove(Keys.PRE_WORKOUT_REMINDER_SKIPS)
            it.remove(Keys.PRE_WORKOUT_REMINDER_SNOOZED_UNTIL)
        }
    }

    /** Snooze the reminder; its dismissal count is reset when the snooze expires. */
    suspend fun snoozePreWorkoutReminder(
        nowMillis: Long = System.currentTimeMillis(),
        durationMillis: Long
    ) {
        store.edit {
            it[Keys.PRE_WORKOUT_REMINDER_SNOOZED_UNTIL] = nowMillis + durationMillis
        }
    }

    /** Atomically clear an expired snooze and its accumulated dismissal count. */
    suspend fun resetPreWorkoutReminderIfSnoozeExpired(nowMillis: Long): Boolean {
        var reset = false
        store.edit {
            val snoozedUntil = it[Keys.PRE_WORKOUT_REMINDER_SNOOZED_UNTIL]
            if (snoozedUntil != null && snoozedUntil <= nowMillis) {
                it.remove(Keys.PRE_WORKOUT_REMINDER_SNOOZED_UNTIL)
                it.remove(Keys.PRE_WORKOUT_REMINDER_SKIPS)
                reset = true
            }
        }
        return reset
    }
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

    // --- Training profile (onboarding personalization) ---
    val profileGoal: Flow<String?> = store.data.map { it[Keys.PROFILE_GOAL] }
    val profileLevel: Flow<String?> = store.data.map { it[Keys.PROFILE_LEVEL] }
    val profileEquipment: Flow<String?> = store.data.map { it[Keys.PROFILE_EQUIPMENT] }
    val profileDaysPerWeek: Flow<Int?> = store.data.map { it[Keys.PROFILE_DAYS] }
    val profileStyle: Flow<String?> = store.data.map { it[Keys.PROFILE_STYLE] }
    /** Sprint 5 P1: "weight" (default) or "reps" - the field focused after a completed set. */
    val autoFocusField: Flow<String> = store.data.map { it[Keys.AUTO_FOCUS_FIELD] ?: "weight" }
    suspend fun setAutoFocusField(value: String) { store.edit { it[Keys.AUTO_FOCUS_FIELD] = value } }
    suspend fun setTrainingProfile(goal: String, level: String, equipment: String, daysPerWeek: Int, style: String = "NO_PREFERENCE") {
        store.edit {
            it[Keys.PROFILE_GOAL] = goal
            it[Keys.PROFILE_LEVEL] = level
            it[Keys.PROFILE_EQUIPMENT] = equipment
            it[Keys.PROFILE_DAYS] = daysPerWeek
            it[Keys.PROFILE_STYLE] = style
        }
    }

    // --- Smart Coach: recompute-only-when-necessary cache markers ---
    val coachLastGenDay: Flow<Long> = store.data.map { it[Keys.COACH_LAST_GEN_DAY] ?: 0L }
    val coachLastGenSessionCount: Flow<Int> = store.data.map { it[Keys.COACH_LAST_GEN_SESSION_COUNT] ?: -1 }
    suspend fun setCoachGenerationMarker(dayEpoch: Long, sessionCount: Int) {
        store.edit {
            it[Keys.COACH_LAST_GEN_DAY] = dayEpoch
            it[Keys.COACH_LAST_GEN_SESSION_COUNT] = sessionCount
        }
    }

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

    // --- Sprint 12: user profile (DataStore; never Room) ---
    /** Emits null until a non-blank display name has been stored. */
    val userProfile: Flow<UserProfile?> = store.data.map { p ->
        val name = p[Keys.PROFILE_DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: return@map null
        UserProfile(
            displayName = name,
            dateOfBirthEpochDay = p[Keys.PROFILE_DOB_EPOCH_DAY]?.takeIf { it > 0 },
            heightCm = p[Keys.PROFILE_HEIGHT_CM]?.takeIf { it > 0.0 },
            weightKg = p[Keys.PROFILE_WEIGHT_KG]?.takeIf { it > 0.0 },
            useKg = p[Keys.USE_KG] ?: true,
            experienceLevel = p[Keys.PROFILE_LEVEL],
            primaryGoal = p[Keys.PROFILE_GOAL],
            weeklyFrequency = p[Keys.PROFILE_DAYS],
            equipment = p[Keys.PROFILE_EQUIPMENT]
        )
    }

    /** Convenience: the display name for greetings (null before onboarding). */
    val displayName: Flow<String?> = store.data.map { it[Keys.PROFILE_DISPLAY_NAME]?.takeIf { n -> n.isNotBlank() } }

    /** Whether a usable profile (non-blank display name) exists. */
    val hasProfile: Flow<Boolean> = store.data.map { !it[Keys.PROFILE_DISPLAY_NAME].isNullOrBlank() }

    /** Persist the whole profile atomically. Units + training prefs reuse existing keys. */
    suspend fun setUserProfile(profile: UserProfile) {
        store.edit { p ->
            p[Keys.PROFILE_DISPLAY_NAME] = profile.displayName.trim()
            if (profile.dateOfBirthEpochDay != null && profile.dateOfBirthEpochDay > 0) p[Keys.PROFILE_DOB_EPOCH_DAY] = profile.dateOfBirthEpochDay else p.remove(Keys.PROFILE_DOB_EPOCH_DAY)
            if (profile.heightCm != null && profile.heightCm > 0.0) p[Keys.PROFILE_HEIGHT_CM] = profile.heightCm else p.remove(Keys.PROFILE_HEIGHT_CM)
            if (profile.weightKg != null && profile.weightKg > 0.0) p[Keys.PROFILE_WEIGHT_KG] = profile.weightKg else p.remove(Keys.PROFILE_WEIGHT_KG)
            p[Keys.USE_KG] = profile.useKg
            profile.experienceLevel?.let { p[Keys.PROFILE_LEVEL] = it }
            profile.primaryGoal?.let { p[Keys.PROFILE_GOAL] = it }
            profile.weeklyFrequency?.let { p[Keys.PROFILE_DAYS] = it }
            profile.equipment?.let { p[Keys.PROFILE_EQUIPMENT] = it }
        }
    }

    // --- Sprint 12: versioned legal acceptance (DataStore) ---
    val legalAcceptance: Flow<LegalAcceptance?> = store.data.map { p ->
        val sig = p[Keys.LEGAL_ACCEPTED_SIGNATURE]?.takeIf { it.isNotBlank() } ?: return@map null
        LegalAcceptance(
            acceptedSignature = sig,
            disclaimerVersion = p[Keys.LEGAL_DISCLAIMER_VERSION].orEmpty(),
            termsVersion = p[Keys.LEGAL_TERMS_VERSION].orEmpty(),
            privacyVersion = p[Keys.LEGAL_PRIVACY_VERSION].orEmpty(),
            acceptedAtEpochMillis = p[Keys.LEGAL_ACCEPTED_AT] ?: 0L,
            appVersion = p[Keys.LEGAL_ACCEPTED_APP_VERSION].orEmpty(),
            displayName = p[Keys.LEGAL_ACCEPTED_NAME].orEmpty(),
            completed = p[Keys.LEGAL_COMPLETED] ?: false
        )
    }

    /** Full acceptance history, newest first. Stored line-delimited, tab-separated. */
    val legalAcceptanceHistory: Flow<List<LegalAcceptance>> = store.data.map { p ->
        decodeLegalHistory(p[Keys.LEGAL_HISTORY].orEmpty())
    }

    /** Record a legal acceptance: writes the latest record and appends to history. */
    suspend fun recordLegalAcceptance(record: LegalAcceptance) {
        store.edit { p ->
            p[Keys.LEGAL_ACCEPTED_SIGNATURE] = record.acceptedSignature
            p[Keys.LEGAL_DISCLAIMER_VERSION] = record.disclaimerVersion
            p[Keys.LEGAL_TERMS_VERSION] = record.termsVersion
            p[Keys.LEGAL_PRIVACY_VERSION] = record.privacyVersion
            p[Keys.LEGAL_ACCEPTED_AT] = record.acceptedAtEpochMillis
            p[Keys.LEGAL_ACCEPTED_APP_VERSION] = record.appVersion
            p[Keys.LEGAL_ACCEPTED_NAME] = record.displayName
            p[Keys.LEGAL_COMPLETED] = record.completed
            val existing = decodeLegalHistory(p[Keys.LEGAL_HISTORY].orEmpty())
            val updated = (listOf(record) + existing).take(50)
            p[Keys.LEGAL_HISTORY] = encodeLegalHistory(updated)
        }
    }

    private fun encodeLegalHistory(items: List<LegalAcceptance>): String =
        items.joinToString("\n") { r ->
            listOf(
                r.acceptedSignature, r.disclaimerVersion, r.termsVersion, r.privacyVersion,
                r.acceptedAtEpochMillis.toString(), r.appVersion,
                r.displayName.replace("\t", " ").replace("\n", " "), r.completed.toString()
            ).joinToString("\t")
        }

    private fun decodeLegalHistory(raw: String): List<LegalAcceptance> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val f = line.split("\t")
            if (f.size < 8) return@mapNotNull null
            runCatching {
                LegalAcceptance(
                    acceptedSignature = f[0],
                    disclaimerVersion = f[1],
                    termsVersion = f[2],
                    privacyVersion = f[3],
                    acceptedAtEpochMillis = f[4].toLongOrNull() ?: 0L,
                    appVersion = f[5],
                    displayName = f[6],
                    completed = f[7].toBooleanStrictOrNull() ?: false
                )
            }.getOrNull()
        }
    }
}
