package com.example.anniversarycountdown.widget

import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetColorPaletteTest {
    @Test
    fun widgetUsesAnniversaryCustomColorBeforeFallbackTheme() {
        val anniversary = anniversary(customColorArgb = 0xFF123456.toInt())

        assertEquals(0xFF123456.toInt(), widgetSeedArgb(anniversary, FALLBACK_COLOR))
        assertEquals(FALLBACK_COLOR, widgetSeedArgb(null, FALLBACK_COLOR))
    }

    @Test
    fun widgetUsesLegacyPresetWhenCustomColorIsMissing() {
        val anniversary = anniversary(customColorArgb = null)

        assertEquals(AnniversaryColor.SUNSHINE.argb, widgetSeedArgb(anniversary, FALLBACK_COLOR))
    }

    @Test
    fun eventColorsCreateDifferentWidgetPalettes() {
        val rose = widgetColorPalette(AnniversaryColor.ROSE.argb, darkMode = false)
        val ocean = widgetColorPalette(AnniversaryColor.OCEAN.argb, darkMode = false)

        assertNotEquals(rose.background, ocean.background)
        assertNotEquals(rose.accent, ocean.accent)
    }

    @Test
    fun widgetTextMeetsMinimumContrastInLightAndDarkModes() {
        val colors = AnniversaryColor.entries.map { it.argb } + listOf(
            0xFFFFFF00.toInt(),
            0xFF00FFFF.toInt(),
            0xFF111111.toInt(),
            0xFFF5F5F5.toInt(),
        )

        colors.forEach { color ->
            listOf(false, true).forEach { darkMode ->
                val palette = widgetColorPalette(color, darkMode)
                assertTrue(contrastRatio(palette.primaryText, palette.background) >= 4.5)
                assertTrue(contrastRatio(palette.secondaryText, palette.background) >= 4.5)
                assertTrue(contrastRatio(palette.accent, palette.background) >= 4.5)
            }
        }
    }

    private fun anniversary(customColorArgb: Int?) = Anniversary(
        id = "id",
        name = "name",
        dateEpochDay = 0,
        createdAtEpochMillis = 0,
        color = AnniversaryColor.SUNSHINE,
        customColorArgb = customColorArgb,
    )

    private companion object {
        val FALLBACK_COLOR = 0xFFA63C4A.toInt()
    }
}
