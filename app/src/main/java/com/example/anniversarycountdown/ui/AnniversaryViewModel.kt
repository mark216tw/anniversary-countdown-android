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
import com.example.anniversarycountdown.data.LeapDayRule
import com.example.anniversarycountdown.data.RepeatRule
import com.example.anniversarycountdown.data.SettingsRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnniversaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnniversaryRepository(application)
    private val settingsRepository = SettingsRepository(application)

    val anniversaries = repository.anniversaries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun save(
        existing: Anniversary?,
        name: String,
        date: LocalDate,
        time: LocalTime?,
        repeatRule: RepeatRule,
        leapDayRule: LeapDayRule,
        category: AnniversaryCategory,
        color: AnniversaryColor,
        fixedZoneId: String?,
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
            color = color,
            fixedZoneId = fixedZoneId,
        )
        viewModelScope.launch { repository.upsert(anniversary) }
    }

    fun delete(anniversary: Anniversary) {
        viewModelScope.launch { repository.delete(anniversary.id) }
    }

    fun setThemeColor(themeColor: AppThemeColor) {
        viewModelScope.launch { settingsRepository.setThemeColor(themeColor) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkMode(enabled) }
    }
}
