package com.example.anniversarycountdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.anniversarycountdown.MainActivity
import com.example.anniversarycountdown.R
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryRepository
import com.example.anniversarycountdown.data.AppSettings
import com.example.anniversarycountdown.data.SettingsRepository
import com.example.anniversarycountdown.ui.anniversaryCountdownText
import com.example.anniversarycountdown.ui.iconRes
import com.example.anniversarycountdown.ui.upcomingAnniversaries
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TwoAnniversaryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync {
            TwoAnniversaryWidgetUpdater.update(context, manager, appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in UPDATE_ACTIONS) {
            runAsync { TwoAnniversaryWidgetUpdater.updateAll(context) }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        runAsync { TwoAnniversaryWidgetUpdater.update(context, manager, intArrayOf(appWidgetId)) }
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val UPDATE_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED,
        )
    }
}

object TwoAnniversaryWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TwoAnniversaryWidgetProvider::class.java))
        update(context, manager, ids)
    }

    suspend fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val anniversaries = AnniversaryRepository(context).anniversaries.first()
        val settings = SettingsRepository(context).settings.first()
        render(context, manager, ids, anniversaries, settings, System.currentTimeMillis())
    }

    internal fun render(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        anniversaries: List<Anniversary>,
        settings: AppSettings,
        now: Long,
    ) {
        if (ids.isEmpty()) return
        val upcoming = upcomingAnniversaries(anniversaries, now, limit = 2)
        val palette = widgetColorPalette(
            seedArgb = widgetSeedArgb(upcoming.firstOrNull(), settings.themeSeedArgb),
            darkMode = isWidgetDarkMode(context, settings),
        )

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.two_anniversary_widget)
            views.setImageViewBitmap(
                R.id.two_widget_background,
                AnniversaryWidgetUpdater.createBackground(manager, id, palette),
            )
            views.setTextColor(R.id.two_widget_title, palette.accent)
            views.setTextColor(R.id.two_widget_empty, palette.secondaryText)

            if (upcoming.isEmpty()) {
                views.setViewVisibility(R.id.two_widget_empty, View.VISIBLE)
                views.setViewVisibility(R.id.two_widget_event_1, View.GONE)
                views.setViewVisibility(R.id.two_widget_event_2, View.GONE)
            } else {
                views.setViewVisibility(R.id.two_widget_empty, View.GONE)
                views.setViewVisibility(R.id.two_widget_event_1, View.VISIBLE)
                bindEvent(views, context, upcoming[0], now, palette, first = true)

                if (upcoming.size > 1) {
                    views.setViewVisibility(R.id.two_widget_event_2, View.VISIBLE)
                    bindEvent(views, context, upcoming[1], now, palette, first = false)
                } else {
                    views.setViewVisibility(R.id.two_widget_event_2, View.INVISIBLE)
                }
            }

            val openApp = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.two_widget_root, openApp)
            manager.updateAppWidget(id, views)
        }
    }

    private fun bindEvent(
        views: RemoteViews,
        context: Context,
        anniversary: Anniversary,
        now: Long,
        palette: WidgetColorPalette,
        first: Boolean,
    ) {
        val occurrence = anniversary.occurrence(now)
        val iconId = if (first) R.id.two_widget_icon_1 else R.id.two_widget_icon_2
        val nameId = if (first) R.id.two_widget_name_1 else R.id.two_widget_name_2
        val dateId = if (first) R.id.two_widget_date_1 else R.id.two_widget_date_2
        val countdownId = if (first) R.id.two_widget_countdown_1 else R.id.two_widget_countdown_2

        views.setImageViewBitmap(
            iconId,
            AnniversaryWidgetUpdater.createCategoryIcon(context, anniversary.category.iconRes(), palette.accent),
        )
        views.setTextColor(nameId, palette.primaryText)
        views.setTextColor(dateId, palette.secondaryText)
        views.setTextColor(countdownId, palette.accent)
        views.setTextViewText(nameId, anniversary.name)
        views.setTextViewText(
            dateId,
            buildString {
                append(occurrence.dateTime.format(DATE_FORMATTER))
                if (anniversary.hasTime) {
                    append(" ")
                    append(occurrence.dateTime.format(TIME_FORMATTER))
                }
            },
        )
        views.setTextViewText(countdownId, anniversaryCountdownText(anniversary, now))
    }

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d", Locale.TAIWAN)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.TAIWAN)
}
