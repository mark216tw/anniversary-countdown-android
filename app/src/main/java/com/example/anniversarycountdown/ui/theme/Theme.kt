package com.example.anniversarycountdown.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.anniversarycountdown.data.AppThemeColor
import kotlin.math.pow

data class ThemePalette(
    val primary: Color,
    val lightContainer: Color,
    val darkContainer: Color,
    val lightBackground: Color,
    val darkBackground: Color,
)

fun themePalette(themeColor: AppThemeColor): ThemePalette = themePalette(themeColor.argb)

fun themePalette(seedArgb: Int): ThemePalette = ThemePalette(
    primary = Color(opaque(seedArgb)),
    lightContainer = Color(seedTone(seedArgb, saturation = 0.28f, value = 0.96f)),
    darkContainer = Color(seedTone(seedArgb, saturation = 0.66f, value = 0.42f)),
    lightBackground = Color(seedTone(seedArgb, saturation = 0.035f, value = 0.99f)),
    darkBackground = Color(seedTone(seedArgb, saturation = 0.12f, value = 0.10f)),
)

@Composable
fun AnniversaryCountdownTheme(
    seedArgb: Int,
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val seed = opaque(seedArgb)
    val primary = if (darkTheme) seedTone(seed, 0.45f, 0.90f) else seed
    val primaryContainer = if (darkTheme) {
        seedTone(seed, 0.66f, 0.42f)
    } else {
        seedTone(seed, 0.28f, 0.96f)
    }
    val secondary = seedTone(seed, if (darkTheme) 0.24f else 0.46f, if (darkTheme) 0.82f else 0.52f)
    val secondaryContainer = seedTone(seed, if (darkTheme) 0.30f else 0.18f, if (darkTheme) 0.34f else 0.92f)
    val tertiary = shiftedTone(seed, 55f, if (darkTheme) 0.38f else 0.58f, if (darkTheme) 0.84f else 0.52f)
    val tertiaryContainer = shiftedTone(seed, 55f, 0.28f, if (darkTheme) 0.34f else 0.92f)
    val background = seedTone(seed, if (darkTheme) 0.12f else 0.035f, if (darkTheme) 0.10f else 0.99f)
    val surfaceVariant = seedTone(seed, if (darkTheme) 0.12f else 0.08f, if (darkTheme) 0.25f else 0.91f)
    val outline = seedTone(seed, 0.08f, if (darkTheme) 0.65f else 0.48f)
    val onBackground = seedTone(seed, 0.06f, if (darkTheme) 0.93f else 0.14f)
    val onSurfaceVariant = seedTone(seed, 0.08f, if (darkTheme) 0.82f else 0.34f)

    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(primary),
            onPrimary = Color(contrastingForeground(primary)),
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(contrastingForeground(primaryContainer)),
            secondary = Color(secondary),
            onSecondary = Color(contrastingForeground(secondary)),
            secondaryContainer = Color(secondaryContainer),
            onSecondaryContainer = Color(contrastingForeground(secondaryContainer)),
            tertiary = Color(tertiary),
            onTertiary = Color(contrastingForeground(tertiary)),
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(contrastingForeground(tertiaryContainer)),
            background = Color(background),
            onBackground = Color(onBackground),
            surface = Color(background),
            onSurface = Color(onBackground),
            surfaceVariant = Color(surfaceVariant),
            onSurfaceVariant = Color(onSurfaceVariant),
            outline = Color(outline),
        )
    } else {
        lightColorScheme(
            primary = Color(primary),
            onPrimary = Color(contrastingForeground(primary)),
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(contrastingForeground(primaryContainer)),
            secondary = Color(secondary),
            onSecondary = Color(contrastingForeground(secondary)),
            secondaryContainer = Color(secondaryContainer),
            onSecondaryContainer = Color(contrastingForeground(secondaryContainer)),
            tertiary = Color(tertiary),
            onTertiary = Color(contrastingForeground(tertiary)),
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(contrastingForeground(tertiaryContainer)),
            background = Color(background),
            onBackground = Color(onBackground),
            surface = Color(background),
            onSurface = Color(onBackground),
            surfaceVariant = Color(surfaceVariant),
            onSurfaceVariant = Color(onSurfaceVariant),
            outline = Color(outline),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private fun opaque(argb: Int): Int = argb or 0xFF000000.toInt()

private fun seedTone(seedArgb: Int, saturation: Float, value: Float): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(opaque(seedArgb), hsv)
    hsv[1] = saturation.coerceIn(0f, 1f)
    hsv[2] = value.coerceIn(0f, 1f)
    return AndroidColor.HSVToColor(hsv)
}

private fun shiftedTone(seedArgb: Int, hueShift: Float, saturation: Float, value: Float): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(opaque(seedArgb), hsv)
    hsv[0] = (hsv[0] + hueShift) % 360f
    hsv[1] = saturation.coerceIn(0f, 1f)
    hsv[2] = value.coerceIn(0f, 1f)
    return AndroidColor.HSVToColor(hsv)
}

private fun contrastingForeground(background: Int): Int =
    if (luminance(background) > 0.179) AndroidColor.BLACK else AndroidColor.WHITE

private fun luminance(argb: Int): Double {
    fun channel(value: Int): Double {
        val component = value / 255.0
        return if (component <= 0.04045) component / 12.92 else ((component + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(AndroidColor.red(argb)) +
        0.7152 * channel(AndroidColor.green(argb)) +
        0.0722 * channel(AndroidColor.blue(argb))
}
