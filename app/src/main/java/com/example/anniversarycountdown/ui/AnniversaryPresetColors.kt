package com.example.anniversarycountdown.ui

import com.example.anniversarycountdown.data.AnniversaryColor

internal data class AnniversaryPresetColor(
    val label: String,
    val argb: Int,
)

internal val anniversaryPresetColors = listOf(
    AnniversaryPresetColor("喜氣紅", 0xFFD7373F.toInt()),
    AnniversaryPresetColor("珊瑚紅", AnniversaryColor.ROSE.argb),
    AnniversaryPresetColor("活力橘", AnniversaryColor.ORANGE.argb),
    AnniversaryPresetColor("金黃", AnniversaryColor.SUNSHINE.argb),
    AnniversaryPresetColor("桃紅", 0xFFD9408B.toInt()),
    AnniversaryPresetColor("紫紅", 0xFFA63C72.toInt()),
)
