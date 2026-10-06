package com.example.anniversarycountdown.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.anniversarycountdown.MainActivity
import com.example.anniversarycountdown.R
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AppSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReminderNotificationManager {
    const val CHANNEL_ID = "anniversary_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notificationsAvailable(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val channelEnabled = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)
            ?.importance != NotificationManager.IMPORTANCE_NONE
        return permissionGranted && channelEnabled && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showReminder(
        context: Context,
        anniversary: Anniversary,
        type: ReminderType,
        settings: AppSettings,
        occurrenceDate: LocalDate,
    ) {
        val title = if (type == ReminderType.ADVANCE) {
            context.getString(R.string.advance_reminder_title, settings.advanceReminderDays, anniversary.name)
        } else {
            context.getString(R.string.same_day_reminder_title, anniversary.name)
        }
        val text = context.getString(
            R.string.reminder_notification_text,
            occurrenceDate.format(DATE_FORMATTER),
        )
        notify(context, anniversary.id.hashCode() * 31 + type.ordinal, title, text)
    }

    fun showTest(context: Context) {
        notify(
            context,
            TEST_NOTIFICATION_ID,
            context.getString(R.string.test_notification_title),
            context.getString(R.string.test_notification_text),
        )
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        createChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission may have changed between the UI check and delivery.
        }
    }

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日", Locale.TAIWAN)
    private const val TEST_NOTIFICATION_ID = 912_001
}
