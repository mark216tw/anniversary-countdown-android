package com.example.anniversarycountdown.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorSettingsTest {
    @Test
    fun newSettingsFollowSystemAndUsePresetColor() {
        val settings = AppSettings()

        assertEquals(DisplayMode.SYSTEM, settings.displayMode)
        assertNull(settings.customThemeColorArgb)
        assertEquals(AppThemeColor.BERRY.argb, settings.themeSeedArgb)
    }

    @Test
    fun presetThemeColorsMatchProductPalette() {
        assertEquals(0xFF765B00.toInt(), AppThemeColor.SUNFLOWER.argb)
        assertEquals(0xFFA63C4A.toInt(), AppThemeColor.BERRY.argb)
        assertEquals(0xFF974700.toInt(), AppThemeColor.TANGERINE.argb)
        assertEquals(0xFF386A20.toInt(), AppThemeColor.CLOVER.argb)
        assertEquals(0xFF00658B.toInt(), AppThemeColor.LAGOON.argb)
        assertEquals(0xFF6555C7.toInt(), AppThemeColor.VIOLET.argb)
    }

    @Test
    fun anniversaryUsesCustomColorBeforeLegacyPreset() {
        val anniversary = Anniversary(
            id = "id",
            name = "name",
            dateEpochDay = 0,
            createdAtEpochMillis = 0,
            color = AnniversaryColor.ROSE,
            customColorArgb = 0xFF123456.toInt(),
        )

        assertEquals(0xFF123456.toInt(), anniversary.effectiveColorArgb)
        assertEquals(AnniversaryColor.ROSE.argb, anniversary.copy(customColorArgb = null).effectiveColorArgb)
    }
}
