package com.example.anniversarycountdown.ui

import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AppSettings
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationGuidanceTest {
    private val reminder = Anniversary(
        id = "reminder",
        name = "紀念日",
        dateEpochDay = LocalDate.of(2027, 1, 1).toEpochDay(),
        createdAtEpochMillis = 0,
        reminderEnabled = true,
    )

    @Test
    fun `shows guidance when an event reminder cannot notify`() {
        assertTrue(
            shouldShowNotificationGuidance(
                anniversaries = listOf(reminder),
                settings = AppSettings(),
                dataLoaded = true,
                notificationsAvailable = false,
            ),
        )
    }

    @Test
    fun `does not show guidance before data is loaded`() {
        assertFalse(
            shouldShowNotificationGuidance(
                anniversaries = listOf(reminder),
                settings = AppSettings(),
                dataLoaded = false,
                notificationsAvailable = false,
            ),
        )
    }

    @Test
    fun `does not show guidance without enabled event reminders`() {
        assertFalse(
            shouldShowNotificationGuidance(
                anniversaries = listOf(reminder.copy(reminderEnabled = false)),
                settings = AppSettings(),
                dataLoaded = true,
                notificationsAvailable = false,
            ),
        )
    }

    @Test
    fun `does not show guidance when global reminders are disabled`() {
        assertFalse(
            shouldShowNotificationGuidance(
                anniversaries = listOf(reminder),
                settings = AppSettings(remindersEnabled = false),
                dataLoaded = true,
                notificationsAvailable = false,
            ),
        )
    }

    @Test
    fun `does not show guidance when notifications are available`() {
        assertFalse(
            shouldShowNotificationGuidance(
                anniversaries = listOf(reminder),
                settings = AppSettings(),
                dataLoaded = true,
                notificationsAvailable = true,
            ),
        )
    }
}
