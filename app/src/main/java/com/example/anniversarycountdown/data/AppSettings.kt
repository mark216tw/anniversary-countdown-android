package com.example.anniversarycountdown.data

enum class DisplayMode { SYSTEM, LIGHT, DARK }

enum class AppThemeColor(val argb: Int) {
    BERRY(0xFFA63C4A.toInt()),
    TANGERINE(0xFF974700.toInt()),
    SUNFLOWER(0xFF765B00.toInt()),
    CLOVER(0xFF386A20.toInt()),
    LAGOON(0xFF00658B.toInt()),
    VIOLET(0xFF6555C7.toInt()),
}

val ADVANCE_REMINDER_DAY_OPTIONS = listOf(1, 3, 7, 14, 30)

data class AppSettings(
    val displayMode: DisplayMode = DisplayMode.SYSTEM,
    val themeColor: AppThemeColor = AppThemeColor.BERRY,
    val customThemeColorArgb: Int? = null,
    val remindersEnabled: Boolean = true,
    val advanceReminderEnabled: Boolean = true,
    val advanceReminderDays: Set<Int> = setOf(7),
    val sameDayReminderEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
) {
    val themeSeedArgb: Int
        get() = customThemeColorArgb ?: themeColor.argb

    val hasEnabledReminderRule: Boolean
        get() = advanceReminderEnabled || sameDayReminderEnabled
}
