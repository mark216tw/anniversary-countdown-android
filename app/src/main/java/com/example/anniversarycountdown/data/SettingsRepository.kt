package com.example.anniversarycountdown.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
                themeColor = preferences[THEME_COLOR]
                    ?.let { runCatching { AppThemeColor.valueOf(it) }.getOrNull() }
                    ?: AppThemeColor.BERRY,
                darkMode = preferences[DARK_MODE] ?: false,
            )
        }

    suspend fun setThemeColor(themeColor: AppThemeColor) {
        context.settingsDataStore.edit { it[THEME_COLOR] = themeColor.name }
        AnniversaryWidgetUpdater.updateAll(context)
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[DARK_MODE] = enabled }
        AnniversaryWidgetUpdater.updateAll(context)
    }

    private companion object {
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
}
