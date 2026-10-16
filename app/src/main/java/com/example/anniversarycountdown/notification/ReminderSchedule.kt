package com.example.anniversarycountdown.notification

import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AppSettings
import com.example.anniversarycountdown.data.RepeatRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class ReminderType { ADVANCE, SAME_DAY }

data class ReminderSchedule(
    val type: ReminderType,
    val triggerAtMillis: Long,
    val occurrenceDate: LocalDate,
    val advanceDays: Int? = null,
)

internal fun nextReminderSchedules(
    anniversary: Anniversary,
    settings: AppSettings,
    nowEpochMillis: Long,
    deviceZoneId: ZoneId = ZoneId.systemDefault(),
): List<ReminderSchedule> {
    if (!anniversary.reminderEnabled || !settings.remindersEnabled) return emptyList()
    if (!settings.advanceReminderEnabled && !settings.sameDayReminderEnabled) return emptyList()

    val zone = anniversary.calculationZone(deviceZoneId)
    val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
    val firstOccurrenceDate = anniversary.occurrence(
        today.atStartOfDay(zone).toInstant().toEpochMilli(),
        deviceZoneId,
    ).dateTime.toLocalDate()
    val reminderTime = LocalTime.of(settings.reminderHour, settings.reminderMinute)

    return buildList {
        if (settings.advanceReminderEnabled) {
            settings.advanceReminderDays.sorted().forEach { days ->
                nextSchedule(
                    anniversary,
                    ReminderType.ADVANCE,
                    days,
                    firstOccurrenceDate,
                    reminderTime,
                    zone,
                    deviceZoneId,
                    nowEpochMillis,
                )?.let(::add)
            }
        }
        if (settings.sameDayReminderEnabled) {
            nextSchedule(
                anniversary,
                ReminderType.SAME_DAY,
                null,
                firstOccurrenceDate,
                reminderTime,
                zone,
                deviceZoneId,
                nowEpochMillis,
            )?.let(::add)
        }
    }.sortedBy { it.triggerAtMillis }
}

private fun nextSchedule(
    anniversary: Anniversary,
    type: ReminderType,
    advanceDays: Int?,
    firstOccurrenceDate: LocalDate,
    reminderTime: LocalTime,
    zone: ZoneId,
    deviceZoneId: ZoneId,
    nowEpochMillis: Long,
): ReminderSchedule? {
    var occurrenceDate = firstOccurrenceDate
    repeat(ReminderScheduleLimits.MAX_OCCURRENCE_SEARCH) {
        val reminderDate = when (type) {
            ReminderType.ADVANCE -> occurrenceDate.minusDays(advanceDays!!.toLong())
            ReminderType.SAME_DAY -> occurrenceDate
        }
        val triggerAtMillis = reminderDate.atTime(reminderTime)
            .atZone(zone).toInstant().toEpochMilli()
        if (triggerAtMillis > nowEpochMillis) {
            return ReminderSchedule(type, triggerAtMillis, occurrenceDate, advanceDays)
        }
        if (anniversary.repeatRule == RepeatRule.NONE) return null
        occurrenceDate = anniversary.occurrence(
            occurrenceDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            deviceZoneId,
        ).dateTime.toLocalDate()
    }
    return null
}

private object ReminderScheduleLimits {
    const val MAX_OCCURRENCE_SEARCH = 400
}
