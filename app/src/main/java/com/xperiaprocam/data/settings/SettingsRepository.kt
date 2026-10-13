package com.xperiaprocam.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xperiaprocam.domain.model.AppSettings
import com.xperiaprocam.domain.model.CaptureLook
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val GRID = booleanPreferencesKey("show_grid")
        val LEVEL = booleanPreferencesKey("show_level")
        val HAPTICS = booleanPreferencesKey("haptic_feedback")
        val TIMER = intPreferencesKey("default_timer")
        val LOOK = stringPreferencesKey("capture_look")
        val RAW = booleanPreferencesKey("raw_capture")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            showGrid = p[Keys.GRID] ?: true,
            showLevel = p[Keys.LEVEL] ?: false,
            hapticFeedback = p[Keys.HAPTICS] ?: true,
            defaultTimerSeconds = p[Keys.TIMER] ?: 0,
            look = p[Keys.LOOK]?.let { runCatching { CaptureLook.valueOf(it) }.getOrNull() }
                ?: CaptureLook.NATURAL,
            rawCapture = p[Keys.RAW] ?: false,
        )
    }

    suspend fun setShowGrid(v: Boolean) = context.dataStore.edit { it[Keys.GRID] = v }
    suspend fun setShowLevel(v: Boolean) = context.dataStore.edit { it[Keys.LEVEL] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[Keys.HAPTICS] = v }
    suspend fun setDefaultTimer(v: Int) = context.dataStore.edit { it[Keys.TIMER] = v }
    suspend fun setLook(v: CaptureLook) = context.dataStore.edit { it[Keys.LOOK] = v.name }
    suspend fun setRawCapture(v: Boolean) = context.dataStore.edit { it[Keys.RAW] = v }
}
