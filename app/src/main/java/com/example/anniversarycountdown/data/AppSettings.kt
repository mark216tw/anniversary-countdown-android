package com.example.anniversarycountdown.data

enum class AppThemeColor { BERRY, TANGERINE, SUNFLOWER, CLOVER, LAGOON, VIOLET }

data class AppSettings(
    val themeColor: AppThemeColor = AppThemeColor.BERRY,
    val darkMode: Boolean = false,
)
