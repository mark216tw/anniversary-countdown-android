package com.example.anniversarycountdown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryRepository
import com.example.anniversarycountdown.data.AnniversaryCategory
import com.example.anniversarycountdown.data.AnniversaryColor
import com.example.anniversarycountdown.data.AppSettings
import com.example.anniversarycountdown.data.AppThemeColor
import com.example.anniversarycountdown.data.DisplayMode
import com.example.anniversarycountdown.data.LeapDayRule
import com.example.anniversarycountdown.data.RepeatRule
import com.example.anniversarycountdown.data.SettingsRepository
import com.example.anniversarycountdown.notification.ReminderNotificationManager
import com.example.anniversarycountdown.notification.ReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnniversaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnniversaryRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val _notificationsAvailable: MutableStateFlow<Boolean> = MutableStateFlow(
        ReminderNotificationManager.notificationsAvailable(application),
    )

    val notificationsAvailable: StateFlow<Boolean> = _notificationsAvailable.asStateFlow()
    private val _anniversariesLoaded = MutableStateFlow(false)
    val anniversariesLoaded: StateFlow<Boolean> = _anniversariesLoaded.asStateFlow()
    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    val anniversaries = repository.anniversaries
        .onEach { _anniversariesLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val settings = settingsRepository.settings
        .onEach { _settingsLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    init {
        ReminderNotificationManager.createChannel(application)
        viewModelScope.launch { ReminderScheduler(application).rescheduleAll() }
    }

    fun save(
        existing: Anniversary?,
        name: String,
        date: LocalDate,
        time: LocalTime?,
        repeatRule: RepeatRule,
        leapDayRule: LeapDayRule,
        category: AnniversaryCategory,
        colorArgb: Int,
        fixedZoneId: String?,
        reminderEnabled: Boolean,
    ) {
        val anniversary = Anniversary(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            dateEpochDay = date.toEpochDay(),
            hour = time?.hour,
            minute = time?.minute,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: System.currentTimeMillis(),
            repeatRule = repeatRule,
            leapDayRule = leapDayRule,
            category = category,
            color = existing?.color ?: AnniversaryColor.ROSE,
            customColorArgb = colorArgb or 0xFF000000.toInt(),
            fixedZoneId = fixedZoneId,
            reminderEnabled = reminderEnabled,
        )
        viewModelScope.launch { repository.upsert(anniversary) }
    }

    fun delete(anniversary: Anniversary) {
        viewModelScope.launch { repository.delete(anniversary.id) }
    }

    fun setThemeColor(themeColor: AppThemeColor) {
        viewModelScope.launch { settingsRepository.setThemeColor(themeColor) }
    }

    fun setCustomThemeColor(colorArgb: Int) {
        viewModelScope.launch { settingsRepository.setCustomThemeColor(colorArgb) }
    }

    fun setDisplayMode(displayMode: DisplayMode) {
        viewModelScope.launch { settingsRepository.setDisplayMode(displayMode) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRemindersEnabled(enabled) }
    }

    fun setAdvanceReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAdvanceReminderEnabled(enabled) }
    }

    fun setAdvanceReminderDays(days: Int) {
        viewModelScope.launch { settingsRepository.setAdvanceReminderDays(days) }
    }

    fun setSameDayReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSameDayReminderEnabled(enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.setReminderTime(hour, minute) }
    }

    fun refreshNotificationAvailability() {
        val available = ReminderNotificationManager.notificationsAvailable(getApplication())
        if (_notificationsAvailable.value != available) {
            _notificationsAvailable.value = available
            viewModelScope.launch { com.example.anniversarycountdown.widget.AnniversaryWidgetUpdater.updateAll(getApplication()) }
        }
    }

    fun sendTestNotification() {
        ReminderNotificationManager.showTest(getApplication())
    }

}
