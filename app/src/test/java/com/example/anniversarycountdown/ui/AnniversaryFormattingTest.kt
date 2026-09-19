package com.example.anniversarycountdown.ui

import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.LeapDayRule
import com.example.anniversarycountdown.data.RepeatRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class AnniversaryFormattingTest {
    private val zone = ZoneId.of("Asia/Taipei")
    private val now = LocalDateTime.of(2026, 8, 23, 10, 0)
        .atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `formats countdown without seconds`() {
        val target = now + 2 * 86_400_000L + 3 * 3_600_000L + 8 * 60_000L + 42_000L

        assertEquals("剩下 2 天 3 小時 8 分鐘", countdownText(target, now))
    }

    @Test
    fun `puts nearest upcoming event first and expired events last`() {
        val expired = event("expired", now - 60_000)
        val later = event("later", now + 10 * 60_000)
        val sooner = event("sooner", now + 5 * 60_000)

        val result = sortedAnniversaries(listOf(expired, later, sooner), now, zone)

        assertEquals(listOf("sooner", "later", "expired"), result.map { it.id })
    }

    @Test
    fun `selects only two nearest upcoming events for widget`() {
        val expired = event("expired", now - 60_000)
        val third = event("third", now + 15 * 60_000)
        val second = event("second", now + 10 * 60_000)
        val first = event("first", now + 5 * 60_000)

        val result = upcomingAnniversaries(
            listOf(expired, third, second, first),
            now,
            limit = 2,
            zoneId = zone,
        )

        assertEquals(listOf("first", "second"), result.map { it.id })
    }

    @Test
    fun `formats elapsed time`() {
        assertEquals("已過 1 天 2 小時", countdownText(now - 26 * 3_600_000L, now))
    }

    @Test
    fun `yearly birthday uses next occurrence instead of birth year`() {
        val birthday = Anniversary(
            id = "birthday",
            name = "生日",
            dateEpochDay = LocalDate.of(1990, 12, 10).toEpochDay(),
            createdAtEpochMillis = 0,
            repeatRule = RepeatRule.YEARLY,
            fixedZoneId = zone.id,
        )

        assertEquals(
            LocalDate.of(2026, 12, 10),
            birthday.occurrence(now, zone).dateTime.toLocalDate(),
        )
    }

    @Test
    fun `monthly day 31 uses last available day`() {
        val endOfMonth = Anniversary(
            id = "monthly",
            name = "月底",
            dateEpochDay = LocalDate.of(2024, 1, 31).toEpochDay(),
            createdAtEpochMillis = 0,
            repeatRule = RepeatRule.MONTHLY,
            fixedZoneId = zone.id,
        )
        val februaryNow = LocalDateTime.of(2026, 2, 1, 10, 0)
            .atZone(zone).toInstant().toEpochMilli()

        assertEquals(
            LocalDate.of(2026, 2, 28),
            endOfMonth.occurrence(februaryNow, zone).dateTime.toLocalDate(),
        )
    }

    @Test
    fun `leap day can move to March first in non leap year`() {
        val leapDay = Anniversary(
            id = "leap",
            name = "閏日",
            dateEpochDay = LocalDate.of(2024, 2, 29).toEpochDay(),
            createdAtEpochMillis = 0,
            repeatRule = RepeatRule.YEARLY,
            leapDayRule = LeapDayRule.MARCH_1,
            fixedZoneId = zone.id,
        )
        val februaryNow = LocalDateTime.of(2026, 2, 1, 10, 0)
            .atZone(zone).toInstant().toEpochMilli()

        assertEquals(
            LocalDate.of(2026, 3, 1),
            leapDay.occurrence(februaryNow, zone).dateTime.toLocalDate(),
        )
    }

    @Test
    fun `date only event remains today instead of rolling over`() {
        val today = Anniversary(
            id = "today",
            name = "今天",
            dateEpochDay = LocalDate.of(2000, 8, 23).toEpochDay(),
            createdAtEpochMillis = 0,
            repeatRule = RepeatRule.YEARLY,
            fixedZoneId = zone.id,
        )

        assertEquals("就是今天", anniversaryCountdownText(today, now, zone))
        assertEquals(LocalDate.of(2026, 8, 23), today.occurrence(now, zone).dateTime.toLocalDate())
    }

    @Test
    fun `fixed timezone keeps the same instant while travelling`() {
        val fixed = Anniversary(
            id = "fixed",
            name = "台北時間",
            dateEpochDay = LocalDate.of(2026, 8, 24).toEpochDay(),
            hour = 9,
            minute = 0,
            createdAtEpochMillis = 0,
            fixedZoneId = "Asia/Taipei",
        )

        val expected = LocalDateTime.of(2026, 8, 24, 9, 0)
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, fixed.occurrence(now, ZoneId.of("America/New_York")).epochMillis)
    }

    private fun event(id: String, target: Long): Anniversary {
        val local = java.time.Instant.ofEpochMilli(target).atZone(zone).toLocalDateTime()
        return Anniversary(
            id = id,
            name = id,
            dateEpochDay = local.toLocalDate().toEpochDay(),
            hour = local.hour,
            minute = local.minute,
            createdAtEpochMillis = 0,
        )
    }
}
