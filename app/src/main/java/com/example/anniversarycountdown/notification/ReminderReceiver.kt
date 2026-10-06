package com.example.anniversarycountdown.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.anniversarycountdown.data.AnniversaryRepository
import com.example.anniversarycountdown.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val anniversaryId = intent.getStringExtra(EXTRA_ANNIVERSARY_ID) ?: return
        val type = intent.getStringExtra(EXTRA_REMINDER_TYPE)
            ?.let { runCatching { ReminderType.valueOf(it) }.getOrNull() }
            ?: return
        val occurrenceDate = intent.takeIf { it.hasExtra(EXTRA_OCCURRENCE_EPOCH_DAY) }
            ?.getLongExtra(EXTRA_OCCURRENCE_EPOCH_DAY, 0L)
            ?.let(LocalDate::ofEpochDay)
            ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val anniversary = AnniversaryRepository(context).anniversaries.first()
                    .firstOrNull { it.id == anniversaryId }
                val settings = SettingsRepository(context).settings.first()
                val typeEnabled = when (type) {
                    ReminderType.ADVANCE -> settings.advanceReminderEnabled
                    ReminderType.SAME_DAY -> settings.sameDayReminderEnabled
                }
                if (anniversary != null && anniversary.reminderEnabled && settings.remindersEnabled && typeEnabled) {
                    ReminderNotificationManager.showReminder(context, anniversary, type, settings, occurrenceDate)
                    ReminderScheduler(context).schedule(anniversary)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ANNIVERSARY_ID = "anniversary_id"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val EXTRA_OCCURRENCE_EPOCH_DAY = "occurrence_epoch_day"
    }
}
