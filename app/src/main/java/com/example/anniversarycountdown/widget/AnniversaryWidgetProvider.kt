package com.example.anniversarycountdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.anniversarycountdown.MainActivity
import com.example.anniversarycountdown.R
import com.example.anniversarycountdown.data.AnniversaryCategory
import com.example.anniversarycountdown.data.AnniversaryRepository
import com.example.anniversarycountdown.data.AppThemeColor
import com.example.anniversarycountdown.data.RepeatRule
import com.example.anniversarycountdown.data.SettingsRepository
import com.example.anniversarycountdown.ui.anniversaryCountdownText
import com.example.anniversarycountdown.ui.sortedAnniversaries
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AnniversaryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AnniversaryWidgetUpdater.update(context, manager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in DATE_ACTIONS) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    AnniversaryWidgetUpdater.updateAll(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private companion object {
        val DATE_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}

object AnniversaryWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, AnniversaryWidgetProvider::class.java))
        update(context, manager, ids)
    }

    suspend fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val anniversaries = AnniversaryRepository(context).anniversaries.first()
        val settings = SettingsRepository(context).settings.first()
        val now = System.currentTimeMillis()
        val nearest = sortedAnniversaries(anniversaries, now)
            .firstOrNull { !it.isExpired(now) }
            ?: sortedAnniversaries(anniversaries, now).firstOrNull()
        val accent = settings.themeColor.widgetColor()

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.anniversary_widget)
            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                if (settings.darkMode) R.drawable.widget_background_dark else R.drawable.widget_background_light,
            )
            val primaryText = if (settings.darkMode) Color.WHITE else Color.rgb(43, 35, 38)
            val secondaryText = if (settings.darkMode) Color.rgb(210, 200, 204) else Color.rgb(105, 91, 96)
            views.setTextColor(R.id.widget_title, accent)
            views.setTextColor(R.id.widget_name, primaryText)
            views.setTextColor(R.id.widget_date, secondaryText)
            views.setTextColor(R.id.widget_countdown, accent)
            views.setTextColor(R.id.widget_meta, secondaryText)

            if (nearest == null) {
                views.setTextViewText(R.id.widget_name, "還沒有紀念日")
                views.setTextViewText(R.id.widget_date, "點一下新增重要日子")
                views.setTextViewText(R.id.widget_countdown, "開始記錄期待")
                views.setTextViewText(R.id.widget_meta, "")
            } else {
                val occurrence = nearest.occurrence(now)
                views.setTextViewText(R.id.widget_name, nearest.name)
                views.setTextViewText(
                    R.id.widget_date,
                    occurrence.dateTime.format(DateTimeFormatter.ofPattern("yyyy/M/d  HH:mm", Locale.TAIWAN))
                        .let { if (nearest.hasTime) it else it.substringBefore("  ") },
                )
                views.setTextViewText(R.id.widget_countdown, anniversaryCountdownText(nearest, now))
                views.setTextViewText(
                    R.id.widget_meta,
                    listOf(nearest.category.widgetLabel(), nearest.repeatRule.widgetLabel())
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                )
            }

            val openApp = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            manager.updateAppWidget(id, views)
        }
    }
}

private fun AppThemeColor.widgetColor(): Int = when (this) {
    AppThemeColor.BERRY -> Color.rgb(183, 62, 98)
    AppThemeColor.TANGERINE -> Color.rgb(224, 100, 53)
    AppThemeColor.SUNFLOWER -> Color.rgb(168, 127, 0)
    AppThemeColor.CLOVER -> Color.rgb(50, 132, 87)
    AppThemeColor.LAGOON -> Color.rgb(22, 124, 145)
    AppThemeColor.VIOLET -> Color.rgb(121, 82, 179)
}

private fun AnniversaryCategory.widgetLabel(): String = when (this) {
    AnniversaryCategory.BIRTHDAY -> "生日"
    AnniversaryCategory.LOVE -> "愛情"
    AnniversaryCategory.FAMILY -> "家庭"
    AnniversaryCategory.TRAVEL -> "旅行"
    AnniversaryCategory.WORK -> "工作"
    AnniversaryCategory.OTHER -> "其他"
}

private fun RepeatRule.widgetLabel(): String = when (this) {
    RepeatRule.NONE -> ""
    RepeatRule.MONTHLY -> "每月"
    RepeatRule.YEARLY -> "每年"
}
