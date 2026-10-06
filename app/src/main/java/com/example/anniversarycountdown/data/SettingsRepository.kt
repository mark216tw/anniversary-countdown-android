package com.example.anniversarycountdown.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.anniversarycountdown.widget.AnniversaryWidgetUpdater
import com.example.anniversarycountdown.notification.ReminderScheduler
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
                remindersEnabled = preferences[REMINDERS_ENABLED] ?: true,
                advanceReminderEnabled = preferences[ADVANCE_REMINDER_ENABLED] ?: true,
                advanceReminderDays = (preferences[ADVANCE_REMINDER_DAYS] ?: 7).coerceIn(1, 365),
                sameDayReminderEnabled = preferences[SAME_DAY_REMINDER_ENABLED] ?: true,
                reminderHour = (preferences[REMINDER_HOUR] ?: 9).coerceIn(0, 23),
                reminderMinute = (preferences[REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
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

    suspend fun setRemindersEnabled(enabled: Boolean) = updateReminderSettings {
        it[REMINDERS_ENABLED] = enabled
    }

    suspend fun setAdvanceReminderEnabled(enabled: Boolean) = updateReminderSettings {
        it[ADVANCE_REMINDER_ENABLED] = enabled
    }

    suspend fun setAdvanceReminderDays(days: Int) = updateReminderSettings {
        it[ADVANCE_REMINDER_DAYS] = days.coerceIn(1, 365)
    }

    suspend fun setSameDayReminderEnabled(enabled: Boolean) = updateReminderSettings {
        it[SAME_DAY_REMINDER_ENABLED] = enabled
    }

    suspend fun setReminderTime(hour: Int, minute: Int) = updateReminderSettings {
        it[REMINDER_HOUR] = hour.coerceIn(0, 23)
        it[REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }

    private suspend fun updateReminderSettings(
        transform: (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
    ) {
        context.settingsDataStore.edit(transform)
        ReminderScheduler(context).rescheduleAll()
        AnniversaryWidgetUpdater.updateAll(context)
    }

    private companion object {
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val CUSTOM_THEME_COLOR_ARGB = intPreferencesKey("custom_theme_color_argb")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val ADVANCE_REMINDER_ENABLED = booleanPreferencesKey("advance_reminder_enabled")
        val ADVANCE_REMINDER_DAYS = intPreferencesKey("advance_reminder_days")
        val SAME_DAY_REMINDER_ENABLED = booleanPreferencesKey("same_day_reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }
}
