package com.example.anniversarycountdown.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.anniversarycountdown.widget.AnniversaryWidgetUpdater
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppSettings(
                displayMode = preferences[DISPLAY_MODE]
                    ?.let { runCatching { DisplayMode.valueOf(it) }.getOrDefault(DisplayMode.SYSTEM) }
                    ?: preferences[DARK_MODE]?.let { if (it) DisplayMode.DARK else DisplayMode.LIGHT }
                    ?: DisplayMode.SYSTEM,
                themeColor = preferences[THEME_COLOR]
                    ?.let { runCatching { AppThemeColor.valueOf(it) }.getOrNull() }
                    ?: AppThemeColor.BERRY,
                customThemeColorArgb = preferences[CUSTOM_THEME_COLOR_ARGB],
            )
        }

    suspend fun setThemeColor(themeColor: AppThemeColor) {
        context.settingsDataStore.edit {
            it[THEME_COLOR] = themeColor.name
            it.remove(CUSTOM_THEME_COLOR_ARGB)
        }
        AnniversaryWidgetUpdater.updateAll(context)
    }

    suspend fun setCustomThemeColor(colorArgb: Int) {
        context.settingsDataStore.edit {
            it[CUSTOM_THEME_COLOR_ARGB] = colorArgb or 0xFF000000.toInt()
        }
        AnniversaryWidgetUpdater.updateAll(context)
    }

    suspend fun setDisplayMode(displayMode: DisplayMode) {
        context.settingsDataStore.edit { it[DISPLAY_MODE] = displayMode.name }
        AnniversaryWidgetUpdater.updateAll(context)
    }

    private companion object {
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val CUSTOM_THEME_COLOR_ARGB = intPreferencesKey("custom_theme_color_argb")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
}
