package com.example.anniversarycountdown.ui

import com.example.anniversarycountdown.data.Anniversary
import java.time.ZoneId
import kotlin.math.abs

fun sortedAnniversaries(
    anniversaries: List<Anniversary>,
    nowEpochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<Anniversary> = anniversaries.sortedWith(
    compareBy<Anniversary> { it.isExpired(nowEpochMillis, zoneId) }
        .thenBy {
            val target = it.occurrence(nowEpochMillis, zoneId).epochMillis
            if (!it.isExpired(nowEpochMillis, zoneId)) target else -target
        }
        .thenBy { it.createdAtEpochMillis },
)

fun upcomingAnniversaries(
    anniversaries: List<Anniversary>,
    nowEpochMillis: Long,
    limit: Int,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<Anniversary> = sortedAnniversaries(anniversaries, nowEpochMillis, zoneId)
    .asSequence()
    .filterNot { it.isExpired(nowEpochMillis, zoneId) }
    .take(limit.coerceAtLeast(0))
    .toList()

fun anniversaryCountdownText(
    anniversary: Anniversary,
    nowEpochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val occurrence = anniversary.occurrence(nowEpochMillis, zoneId)
    val today = java.time.Instant.ofEpochMilli(nowEpochMillis)
        .atZone(occurrence.zoneId)
        .toLocalDate()
    if (!anniversary.hasTime && occurrence.dateTime.toLocalDate() == today) return "就是今天"
    return countdownText(occurrence.epochMillis, nowEpochMillis)
}

fun countdownText(targetEpochMillis: Long, nowEpochMillis: Long): String {
    val differenceMillis = targetEpochMillis - nowEpochMillis
    val totalMinutes = abs(differenceMillis) / 60_000
    if (totalMinutes == 0L) {
        return if (differenceMillis >= 0) "剩下不到 1 分鐘" else "剛剛到達"
    }

    val days = totalMinutes / (24 * 60)
    val hours = totalMinutes / 60 % 24
    val minutes = totalMinutes % 60
    val parts = buildList {
        if (days > 0) add("$days 天")
        if (hours > 0) add("$hours 小時")
        if (minutes > 0) add("$minutes 分鐘")
    }
    val prefix = if (differenceMillis >= 0) "剩下 " else "已過 "
    return prefix + parts.joinToString(" ")
}
