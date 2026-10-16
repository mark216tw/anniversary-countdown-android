package com.example.anniversarycountdown.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.anniversarycountdown.data.ADVANCE_REMINDER_DAY_OPTIONS
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryRepository
import com.example.anniversarycountdown.data.SettingsRepository
import kotlinx.coroutines.flow.first

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun schedule(anniversary: Anniversary) {
        cancel(anniversary.id)
        val settings = SettingsRepository(context).settings.first()
        nextReminderSchedules(anniversary, settings, System.currentTimeMillis()).forEach { schedule ->
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                schedule.triggerAtMillis,
                pendingIntent(
                    anniversary.id,
                    schedule.type,
                    schedule.advanceDays,
                    schedule.occurrenceDate.toEpochDay(),
                ),
            )
        }
    }

    suspend fun rescheduleAll() {
        AnniversaryRepository(context).anniversaries.first().forEach { anniversary ->
            schedule(anniversary)
        }
    }

    fun cancel(anniversaryId: String) {
        alarmManager.cancel(pendingIntent(anniversaryId, ReminderType.ADVANCE))
        ADVANCE_REMINDER_DAY_OPTIONS.forEach { days ->
            alarmManager.cancel(pendingIntent(anniversaryId, ReminderType.ADVANCE, days))
        }
        alarmManager.cancel(pendingIntent(anniversaryId, ReminderType.SAME_DAY))
    }

    private fun pendingIntent(
        anniversaryId: String,
        type: ReminderType,
        advanceDays: Int? = null,
        occurrenceEpochDay: Long? = null,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            data = Uri.Builder()
                .scheme("anniversary")
                .authority("reminder")
                .appendPath(anniversaryId)
                .appendPath(type.name)
                .apply { advanceDays?.let { days -> appendPath(days.toString()) } }
                .build()
            putExtra(ReminderReceiver.EXTRA_ANNIVERSARY_ID, anniversaryId)
            putExtra(ReminderReceiver.EXTRA_REMINDER_TYPE, type.name)
            advanceDays?.let { putExtra(ReminderReceiver.EXTRA_ADVANCE_DAYS, it) }
            occurrenceEpochDay?.let { putExtra(ReminderReceiver.EXTRA_OCCURRENCE_EPOCH_DAY, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
