package com.example.anniversarycountdown.ui

import com.example.anniversarycountdown.data.AnniversaryColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnniversaryPresetColorsTest {
    @Test
    fun `provides six unique opaque festive colors`() {
        assertEquals(6, anniversaryPresetColors.size)
        assertEquals(6, anniversaryPresetColors.map { it.argb }.distinct().size)
        assertTrue(anniversaryPresetColors.all { it.argb ushr 24 == 0xFF })
    }

    @Test
    fun `includes the default anniversary color`() {
        assertTrue(anniversaryPresetColors.any { it.argb == AnniversaryColor.ROSE.argb })
    }
}
