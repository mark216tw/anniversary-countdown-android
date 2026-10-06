package com.example.anniversarycountdown.notification

import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AppSettings
import com.example.anniversarycountdown.data.RepeatRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderScheduleTest {
    private val zone = ZoneId.of("Asia/Taipei")
    private val settings = AppSettings(
        advanceReminderDays = 7,
        reminderHour = 9,
        reminderMinute = 0,
    )

    @Test
    fun `schedules advance and same day reminders`() {
        val anniversary = event(LocalDate.of(2026, 10, 20))
        val schedules = nextReminderSchedules(
            anniversary,
            settings,
            at(2026, 10, 1, 10, 0),
            zone,
        )

        assertEquals(listOf(ReminderType.ADVANCE, ReminderType.SAME_DAY), schedules.map { it.type })
        assertEquals(at(2026, 10, 13, 9, 0), schedules[0].triggerAtMillis)
        assertEquals(at(2026, 10, 20, 9, 0), schedules[1].triggerAtMillis)
    }

    @Test
    fun `keeps same day reminder after advance reminder has passed`() {
        val anniversary = event(LocalDate.of(2026, 10, 6))
        val schedules = nextReminderSchedules(
            anniversary,
            settings,
            at(2026, 10, 6, 8, 0),
            zone,
        )

        assertEquals(listOf(ReminderType.SAME_DAY), schedules.map { it.type })
        assertEquals(at(2026, 10, 6, 9, 0), schedules.single().triggerAtMillis)
    }

    @Test
    fun `event time earlier today does not skip same day reminder`() {
        val anniversary = event(LocalDate.of(2000, 10, 6), RepeatRule.YEARLY).copy(hour = 7, minute = 0)
        val schedules = nextReminderSchedules(
            anniversary,
            settings,
            at(2026, 10, 6, 8, 0),
            zone,
        )

        val sameDay = schedules.single { it.type == ReminderType.SAME_DAY }
        assertEquals(LocalDate.of(2026, 10, 6), sameDay.occurrenceDate)
    }

    @Test
    fun `moves recurring reminders to next occurrence after today reminder`() {
        val anniversary = event(LocalDate.of(2000, 10, 6), RepeatRule.YEARLY)
        val schedules = nextReminderSchedules(
            anniversary,
            settings,
            at(2026, 10, 6, 10, 0),
            zone,
        )

        assertEquals(LocalDate.of(2027, 10, 6), schedules.first().occurrenceDate)
        assertEquals(at(2027, 9, 29, 9, 0), schedules.first().triggerAtMillis)
    }

    @Test
    fun `monthly advance reminder can overlap current same day reminder`() {
        val anniversary = event(LocalDate.of(2024, 1, 31), RepeatRule.MONTHLY)
        val longAdvance = settings.copy(advanceReminderDays = 30)
        val schedules = nextReminderSchedules(
            anniversary,
            longAdvance,
            at(2026, 1, 1, 10, 0),
            zone,
        )

        assertEquals(LocalDate.of(2026, 2, 28), schedules.first().occurrenceDate)
        assertEquals(at(2026, 1, 29, 9, 0), schedules.first().triggerAtMillis)
        assertEquals(LocalDate.of(2026, 1, 31), schedules.last().occurrenceDate)
    }

    @Test
    fun `returns no schedules when event reminder is disabled`() {
        val anniversary = event(LocalDate.of(2026, 10, 20)).copy(reminderEnabled = false)

        assertTrue(
            nextReminderSchedules(anniversary, settings, at(2026, 10, 1, 10, 0), zone).isEmpty(),
        )
    }

    private fun event(date: LocalDate, repeatRule: RepeatRule = RepeatRule.NONE) = Anniversary(
        id = "event",
        name = "紀念日",
        dateEpochDay = date.toEpochDay(),
        createdAtEpochMillis = 0,
        repeatRule = repeatRule,
        fixedZoneId = zone.id,
        reminderEnabled = true,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
}
