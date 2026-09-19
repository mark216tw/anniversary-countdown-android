package com.example.anniversarycountdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.anniversarycountdown.MainActivity
import com.example.anniversarycountdown.R
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryRepository
import com.example.anniversarycountdown.data.AppSettings
import com.example.anniversarycountdown.data.DisplayMode
import com.example.anniversarycountdown.data.RepeatRule
import com.example.anniversarycountdown.data.SettingsRepository
import com.example.anniversarycountdown.ui.anniversaryCountdownText
import com.example.anniversarycountdown.ui.iconRes
import com.example.anniversarycountdown.ui.label
import com.example.anniversarycountdown.ui.sortedAnniversaries
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
        if (intent.action in UPDATE_ACTIONS) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    AnniversaryWidgetUpdater.updateSingleAll(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AnniversaryWidgetUpdater.update(context, manager, intArrayOf(appWidgetId))
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

object AnniversaryWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val anniversaries = AnniversaryRepository(context).anniversaries.first()
        val settings = SettingsRepository(context).settings.first()
        val now = System.currentTimeMillis()
        val singleIds = manager.getAppWidgetIds(ComponentName(context, AnniversaryWidgetProvider::class.java))
        val twoIds = manager.getAppWidgetIds(ComponentName(context, TwoAnniversaryWidgetProvider::class.java))
        render(context, manager, singleIds, anniversaries, settings, now)
        TwoAnniversaryWidgetUpdater.render(context, manager, twoIds, anniversaries, settings, now)
    }

    suspend fun updateSingleAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, AnniversaryWidgetProvider::class.java))
        update(context, manager, ids)
    }

    suspend fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val anniversaries = AnniversaryRepository(context).anniversaries.first()
        val settings = SettingsRepository(context).settings.first()
        val now = System.currentTimeMillis()
        render(context, manager, ids, anniversaries, settings, now)
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
        val nearest = sortedAnniversaries(anniversaries, now)
            .firstOrNull { !it.isExpired(now) }
            ?: sortedAnniversaries(anniversaries, now).firstOrNull()
        val darkMode = isWidgetDarkMode(context, settings)
        val palette = widgetColorPalette(
            seedArgb = widgetSeedArgb(nearest, settings.themeSeedArgb),
            darkMode = darkMode,
        )

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.anniversary_widget)
            views.setImageViewBitmap(R.id.widget_background, createBackground(manager, id, palette))
            views.setTextColor(R.id.widget_title, palette.accent)
            views.setTextColor(R.id.widget_name, palette.primaryText)
            views.setTextColor(R.id.widget_date, palette.secondaryText)
            views.setTextColor(R.id.widget_countdown, palette.accent)
            views.setTextColor(R.id.widget_meta, palette.secondaryText)

            if (nearest == null) {
                views.setTextViewText(R.id.widget_name, "還沒有紀念日")
                views.setTextViewText(R.id.widget_date, "點一下新增重要日子")
                views.setTextViewText(R.id.widget_countdown, "開始記錄期待")
                views.setTextViewText(R.id.widget_meta, "")
                views.setViewVisibility(R.id.widget_meta_row, View.GONE)
            } else {
                val occurrence = nearest.occurrence(now)
                views.setViewVisibility(R.id.widget_meta_row, View.VISIBLE)
                views.setImageViewBitmap(
                    R.id.widget_category_icon,
                    createCategoryIcon(context, nearest.category.iconRes(), palette.accent),
                )
                views.setTextViewText(R.id.widget_name, nearest.name)
                views.setTextViewText(
                    R.id.widget_date,
                    occurrence.dateTime.format(DateTimeFormatter.ofPattern("yyyy/M/d  HH:mm", Locale.TAIWAN))
                        .let { if (nearest.hasTime) it else it.substringBefore("  ") },
                )
                views.setTextViewText(R.id.widget_countdown, anniversaryCountdownText(nearest, now))
                views.setTextViewText(
                    R.id.widget_meta,
                    listOf(nearest.category.label(), nearest.repeatRule.widgetLabel())
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

    internal fun createBackground(
        manager: AppWidgetManager,
        appWidgetId: Int,
        palette: WidgetColorPalette,
    ): Bitmap {
        val options = manager.getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            .coerceIn(1, MAX_BACKGROUND_SIZE)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            .coerceIn(1, MAX_BACKGROUND_SIZE)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(0.5f, 0.5f, width - 0.5f, height - 0.5f)

        canvas.drawRoundRect(bounds, CORNER_RADIUS, CORNER_RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.background
            style = Paint.Style.FILL
        })
        canvas.drawRoundRect(bounds, CORNER_RADIUS, CORNER_RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.outline
            style = Paint.Style.STROKE
            strokeWidth = 1f
        })
        return bitmap
    }

    internal fun createCategoryIcon(context: Context, iconRes: Int, tint: Int): Bitmap {
        val size = (CATEGORY_ICON_SIZE_DP * context.resources.displayMetrics.density).roundToInt()
            .coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.density = context.resources.displayMetrics.densityDpi
        requireNotNull(context.getDrawable(iconRes)).mutate().apply {
            setTint(tint)
            setBounds(0, 0, size, size)
            draw(Canvas(bitmap))
        }
        return bitmap
    }

    private const val CORNER_RADIUS = 24f
    private const val MAX_BACKGROUND_SIZE = 512
    private const val CATEGORY_ICON_SIZE_DP = 14f
}

internal fun isWidgetDarkMode(context: Context, settings: AppSettings): Boolean =
    when (settings.displayMode) {
        DisplayMode.SYSTEM -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        DisplayMode.LIGHT -> false
        DisplayMode.DARK -> true
    }

private fun RepeatRule.widgetLabel(): String = when (this) {
    RepeatRule.NONE -> ""
    RepeatRule.MONTHLY -> "每月"
    RepeatRule.YEARLY -> "每年"
}
