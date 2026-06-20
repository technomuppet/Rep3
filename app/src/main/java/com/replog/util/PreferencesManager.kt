package com.replog.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    }

    val useKg: Flow<Boolean> = store.data.map { it[Keys.USE_KG] ?: true }
    val isFirstLaunch: Flow<Boolean> = store.data.map { it[Keys.FIRST_LAUNCH] ?: true }
    val restSeconds: Flow<Int> = store.data.map { it[Keys.REST_SECONDS] ?: 90 }
    val activeSessionId: Flow<Int?> = store.data.map { it[Keys.ACTIVE_SESSION_ID]?.takeIf { id -> id > 0 } }
    val onboardingComplete: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val bodyweightGoal: Flow<Double?> = store.data.map { it[Keys.BODYWEIGHT_GOAL]?.takeIf { value -> value > 0.0 } }
    val customKgPlates: Flow<String> = store.data.map { it[Keys.CUSTOM_KG_PLATES] ?: PlateCalculator.formatPlates(PlateCalculator.metricPlates) }
    val customLbPlates: Flow<String> = store.data.map { it[Keys.CUSTOM_LB_PLATES] ?: PlateCalculator.formatPlates(PlateCalculator.imperialPlates) }

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
}
