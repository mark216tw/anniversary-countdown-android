package com.example.anniversarycountdown.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.Serializable

@Serializable
enum class RepeatRule { NONE, MONTHLY, YEARLY }

@Serializable
enum class LeapDayRule { FEBRUARY_28, MARCH_1 }

@Serializable
enum class AnniversaryCategory { BIRTHDAY, LOVE, FAMILY, TRAVEL, WORK, OTHER }

@Serializable
enum class AnniversaryColor(val argb: Int) {
    ROSE(0xFFE65378.toInt()),
    ORANGE(0xFFF07A3C.toInt()),
    SUNSHINE(0xFFD19A00.toInt()),
    MINT(0xFF36A56B.toInt()),
    OCEAN(0xFF258DA4.toInt()),
    GRAPE(0xFF8964C4.toInt()),
}

data class AnniversaryOccurrence(
    val dateTime: LocalDateTime,
    val zoneId: ZoneId,
    val epochMillis: Long,
)

@Serializable
data class Anniversary(
    val id: String,
    val name: String,
    val dateEpochDay: Long,
    val hour: Int? = null,
    val minute: Int? = null,
    val createdAtEpochMillis: Long,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val leapDayRule: LeapDayRule = LeapDayRule.FEBRUARY_28,
    val category: AnniversaryCategory = AnniversaryCategory.OTHER,
    val color: AnniversaryColor = AnniversaryColor.ROSE,
    val customColorArgb: Int? = null,
    val fixedZoneId: String? = null,
) {
    val effectiveColorArgb: Int
        get() = customColorArgb ?: color.argb

    val date: LocalDate
        get() = LocalDate.ofEpochDay(dateEpochDay)

    val hasTime: Boolean
        get() = hour != null && minute != null

    fun localDateTime(): LocalDateTime = date.atTime(
        if (hasTime) LocalTime.of(requireNotNull(hour), requireNotNull(minute)) else LocalTime.MIDNIGHT,
    )

    fun calculationZone(deviceZoneId: ZoneId = ZoneId.systemDefault()): ZoneId =
        fixedZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: deviceZoneId

    fun occurrence(
        nowEpochMillis: Long,
        deviceZoneId: ZoneId = ZoneId.systemDefault(),
    ): AnniversaryOccurrence {
        val zone = calculationZone(deviceZoneId)
        val now = java.time.Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
        val original = localDateTime().atZone(zone)
        val candidate = when (repeatRule) {
            RepeatRule.NONE -> original
            RepeatRule.MONTHLY -> nextMonthlyOccurrence(now, original, zone)
            RepeatRule.YEARLY -> nextYearlyOccurrence(now, original, zone)
        }
        val epochMillis = if (!hasTime && candidate.toLocalDate() == now.toLocalDate()) {
            nowEpochMillis
        } else {
            candidate.toInstant().toEpochMilli()
        }
        return AnniversaryOccurrence(candidate.toLocalDateTime(), zone, epochMillis)
    }

    fun isExpired(
        nowEpochMillis: Long,
        deviceZoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (repeatRule != RepeatRule.NONE) return false
        val zone = calculationZone(deviceZoneId)
        val now = java.time.Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
        val original = localDateTime().atZone(zone)
        return if (hasTime) {
            original.toInstant().toEpochMilli() < nowEpochMillis
        } else {
            original.toLocalDate() < now.toLocalDate()
        }
    }

    private fun nextMonthlyOccurrence(
        now: ZonedDateTime,
        original: ZonedDateTime,
        zone: ZoneId,
    ): ZonedDateTime {
        var month = maxOf(YearMonth.from(now), YearMonth.from(original))
        while (true) {
            val candidateDate = month.atDay(minOf(date.dayOfMonth, month.lengthOfMonth()))
            val candidate = candidateDate.atTime(localDateTime().toLocalTime()).atZone(zone)
            if (!isBeforeNow(candidate, now) && !candidate.isBefore(original)) return candidate
            month = month.plusMonths(1)
        }
    }

    private fun nextYearlyOccurrence(
        now: ZonedDateTime,
        original: ZonedDateTime,
        zone: ZoneId,
    ): ZonedDateTime {
        var year = maxOf(now.year, date.year)
        while (true) {
            val candidateDate = annualDate(year)
            val candidate = candidateDate.atTime(localDateTime().toLocalTime()).atZone(zone)
            if (!isBeforeNow(candidate, now) && !candidate.isBefore(original)) return candidate
            year++
        }
    }

    private fun annualDate(year: Int): LocalDate {
        if (date.monthValue == 2 && date.dayOfMonth == 29 && !java.time.Year.isLeap(year.toLong())) {
            return if (leapDayRule == LeapDayRule.FEBRUARY_28) {
                LocalDate.of(year, 2, 28)
            } else {
                LocalDate.of(year, 3, 1)
            }
        }
        return LocalDate.of(year, date.monthValue, date.dayOfMonth)
    }

    private fun isBeforeNow(candidate: ZonedDateTime, now: ZonedDateTime): Boolean =
        if (hasTime) candidate.toInstant().isBefore(now.toInstant())
        else candidate.toLocalDate().isBefore(now.toLocalDate())
}
