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
    return ReminderType.entries.mapNotNull { type ->
        val enabled = when (type) {
            ReminderType.ADVANCE -> settings.advanceReminderEnabled
            ReminderType.SAME_DAY -> settings.sameDayReminderEnabled
        }
        if (!enabled) return@mapNotNull null

        var occurrenceDate = firstOccurrenceDate
        repeat(MAX_OCCURRENCE_SEARCH) {
            val reminderDate = when (type) {
                ReminderType.ADVANCE -> occurrenceDate.minusDays(settings.advanceReminderDays.toLong())
                ReminderType.SAME_DAY -> occurrenceDate
            }
            val triggerAtMillis = reminderDate.atTime(reminderTime)
                .atZone(zone).toInstant().toEpochMilli()
            if (triggerAtMillis > nowEpochMillis) {
                return@mapNotNull ReminderSchedule(type, triggerAtMillis, occurrenceDate)
            }
            if (anniversary.repeatRule == RepeatRule.NONE) return@mapNotNull null
            occurrenceDate = anniversary.occurrence(
                occurrenceDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                deviceZoneId,
            ).dateTime.toLocalDate()
        }
        null
    }
}

private const val MAX_OCCURRENCE_SEARCH = 400
