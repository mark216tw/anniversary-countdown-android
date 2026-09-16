package com.example.anniversarycountdown.widget

import com.example.anniversarycountdown.data.Anniversary
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal data class WidgetColorPalette(
    val background: Int,
    val outline: Int,
    val primaryText: Int,
    val secondaryText: Int,
    val accent: Int,
)

internal fun widgetSeedArgb(anniversary: Anniversary?, fallbackArgb: Int): Int =
    anniversary?.effectiveColorArgb ?: fallbackArgb

internal fun widgetColorPalette(seedArgb: Int, darkMode: Boolean): WidgetColorPalette {
    val seed = opaque(seedArgb)
    val background = blend(seed, if (darkMode) BLACK else WHITE, if (darkMode) 0.82 else 0.90)
    val contrastTarget = if (darkMode) WHITE else BLACK

    return WidgetColorPalette(
        background = background,
        outline = blend(background, contrastTarget, if (darkMode) 0.22 else 0.12),
        primaryText = ensureContrast(
            blend(seed, contrastTarget, if (darkMode) 0.88 else 0.84),
            background,
            contrastTarget,
            MIN_TEXT_CONTRAST,
        ),
        secondaryText = ensureContrast(
            blend(seed, contrastTarget, if (darkMode) 0.68 else 0.62),
            background,
            contrastTarget,
            MIN_TEXT_CONTRAST,
        ),
        accent = ensureContrast(
            blend(seed, contrastTarget, if (darkMode) 0.18 else 0.20),
            background,
            contrastTarget,
            MIN_TEXT_CONTRAST,
        ),
    )
}

internal fun contrastRatio(first: Int, second: Int): Double {
    val lighter = max(relativeLuminance(first), relativeLuminance(second))
    val darker = min(relativeLuminance(first), relativeLuminance(second))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun ensureContrast(color: Int, background: Int, target: Int, minimumRatio: Double): Int {
    if (contrastRatio(color, background) >= minimumRatio) return color

    for (step in 1..20) {
        val candidate = blend(color, target, step / 20.0)
        if (contrastRatio(candidate, background) >= minimumRatio) return candidate
    }
    return target
}

private fun blend(from: Int, to: Int, toFraction: Double): Int {
    val fraction = toFraction.coerceIn(0.0, 1.0)
    fun channel(shift: Int): Int {
        val start = from ushr shift and 0xFF
        val end = to ushr shift and 0xFF
        return (start + (end - start) * fraction).toInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

private fun relativeLuminance(argb: Int): Double {
    fun channel(shift: Int): Double {
        val component = (argb ushr shift and 0xFF) / 255.0
        return if (component <= 0.04045) component / 12.92
        else ((component + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

private fun opaque(argb: Int): Int = argb or 0xFF000000.toInt()

private const val MIN_TEXT_CONTRAST = 4.5
private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
