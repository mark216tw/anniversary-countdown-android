package com.example.anniversarycountdown.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.anniversarycountdown.data.AppThemeColor

data class ThemePalette(
    val primary: Color,
    val lightContainer: Color,
    val darkContainer: Color,
    val lightBackground: Color,
    val darkBackground: Color,
)

fun themePalette(themeColor: AppThemeColor): ThemePalette = when (themeColor) {
    AppThemeColor.BERRY -> ThemePalette(
        Color(0xFFB73E62), Color(0xFFFFD9E2), Color(0xFF7D2944), Color(0xFFFFF8F7), Color(0xFF1D1014),
    )
    AppThemeColor.TANGERINE -> ThemePalette(
        Color(0xFFE06435), Color(0xFFFFDBCC), Color(0xFF8A3518), Color(0xFFFFF8F4), Color(0xFF20100A),
    )
    AppThemeColor.SUNFLOWER -> ThemePalette(
        Color(0xFF8A6A00), Color(0xFFFFE37B), Color(0xFF685000), Color(0xFFFFFAED), Color(0xFF1D1808),
    )
    AppThemeColor.CLOVER -> ThemePalette(
        Color(0xFF328457), Color(0xFFB7F2CD), Color(0xFF175E3B), Color(0xFFF5FBF6), Color(0xFF0C1C13),
    )
    AppThemeColor.LAGOON -> ThemePalette(
        Color(0xFF167C91), Color(0xFFB8EAF4), Color(0xFF005B6D), Color(0xFFF3FAFC), Color(0xFF071B20),
    )
    AppThemeColor.VIOLET -> ThemePalette(
        Color(0xFF7952B3), Color(0xFFE9DDFF), Color(0xFF563389), Color(0xFFFBF8FF), Color(0xFF181121),
    )
}

@Composable
fun AnniversaryCountdownTheme(
    themeColor: AppThemeColor,
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = themePalette(themeColor)
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = palette.lightContainer,
            onPrimary = palette.darkContainer,
            primaryContainer = palette.darkContainer,
            onPrimaryContainer = palette.lightContainer,
            secondary = palette.lightContainer.copy(alpha = 0.82f),
            secondaryContainer = palette.darkContainer.copy(alpha = 0.78f),
            background = palette.darkBackground,
            surface = palette.darkBackground,
            surfaceVariant = Color(0xFF4C4447),
            outline = Color(0xFFA39599),
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.lightContainer,
            onPrimaryContainer = palette.darkContainer,
            secondary = palette.primary.copy(alpha = 0.82f),
            secondaryContainer = palette.lightContainer.copy(alpha = 0.72f),
            background = palette.lightBackground,
            surface = palette.lightBackground,
            surfaceVariant = palette.lightContainer.copy(alpha = 0.72f),
            outline = Color(0xFF817478),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
