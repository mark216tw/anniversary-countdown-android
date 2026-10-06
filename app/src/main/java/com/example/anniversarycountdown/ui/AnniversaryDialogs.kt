package com.example.anniversarycountdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.anniversarycountdown.BuildConfig
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryCategory
import com.example.anniversarycountdown.data.AnniversaryColor
import com.example.anniversarycountdown.data.AppSettings
import com.example.anniversarycountdown.data.AppThemeColor
import com.example.anniversarycountdown.data.DisplayMode
import com.example.anniversarycountdown.data.LeapDayRule
import com.example.anniversarycountdown.data.RepeatRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class AnniversaryDraft(
    val name: String,
    val date: LocalDate,
    val time: LocalTime?,
    val repeatRule: RepeatRule,
    val leapDayRule: LeapDayRule,
    val category: AnniversaryCategory,
    val colorArgb: Int,
    val fixedZoneId: String?,
    val reminderEnabled: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryEditorScreen(
    anniversary: Anniversary?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AnniversaryDraft) -> Unit,
    onRequestDelete: (() -> Unit)?,
    onRequestNotificationPermission: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember(anniversary?.id) { mutableStateOf(anniversary?.name.orEmpty()) }
    var date by remember(anniversary?.id) { mutableStateOf(anniversary?.date ?: LocalDate.now().plusDays(1)) }
    var includesTime by remember(anniversary?.id) { mutableStateOf(anniversary?.hasTime == true) }
    var time by remember(anniversary?.id) {
        mutableStateOf(
            anniversary?.takeIf { it.hasTime }?.localDateTime()?.toLocalTime()
                ?: LocalTime.now().withSecond(0).withNano(0),
        )
    }
    var repeatRule by remember(anniversary?.id) { mutableStateOf(anniversary?.repeatRule ?: RepeatRule.NONE) }
    var leapDayRule by remember(anniversary?.id) {
        mutableStateOf(anniversary?.leapDayRule ?: LeapDayRule.FEBRUARY_28)
    }
    var category by remember(anniversary?.id) {
        mutableStateOf(anniversary?.category ?: AnniversaryCategory.OTHER)
    }
    var colorArgb by remember(anniversary?.id) {
        mutableIntStateOf(anniversary?.effectiveColorArgb ?: AnniversaryColor.ROSE.argb)
    }
    var fixedTimeZone by remember(anniversary?.id) { mutableStateOf(anniversary?.fixedZoneId != null) }
    var reminderEnabled by remember(anniversary?.id) {
        mutableStateOf(anniversary?.reminderEnabled ?: false)
    }
    val storedZoneId = remember(anniversary?.id) {
        anniversary?.fixedZoneId ?: ZoneId.systemDefault().id
    }

    fun save() {
        onSave(
            AnniversaryDraft(
                name = name.trim(),
                date = date,
                time = if (includesTime) time else null,
                repeatRule = repeatRule,
                leapDayRule = leapDayRule,
                category = category,
                colorArgb = colorArgb,
                fixedZoneId = if (fixedTimeZone) storedZoneId else null,
                reminderEnabled = reminderEnabled,
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (anniversary == null) "新增紀念日" else "編輯紀念日") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = ::save, enabled = name.isNotBlank()) { Text("儲存") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("紀念日名稱") },
                    placeholder = { Text("例如：小安的生日") },
                    singleLine = true,
                )
            }
            item {
                PickerRow("日期", date.format(dateFormatter)) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth,
                    ).show()
                }
            }
            item {
                OptionTitle("重複")
                Spacer(Modifier.height(6.dp))
                ChoiceRow(RepeatRule.entries, repeatRule, RepeatRule::label) { repeatRule = it }
            }
            if (repeatRule == RepeatRule.YEARLY && date.monthValue == 2 && date.dayOfMonth == 29) {
                item {
                    OptionTitle("非閏年的 2 月 29 日")
                    Spacer(Modifier.height(6.dp))
                    ChoiceRow(
                        LeapDayRule.entries,
                        leapDayRule,
                        { if (it == LeapDayRule.FEBRUARY_28) "2 月 28 日" else "3 月 1 日" },
                    ) { leapDayRule = it }
                }
            }
            item {
                ToggleRow(
                    title = "指定時間",
                    subtitle = "未指定時，當天會顯示「就是今天」",
                    checked = includesTime,
                    onCheckedChange = { includesTime = it },
                )
            }
            item {
                val rules = buildList {
                    if (settings.advanceReminderEnabled) add("提前 ${settings.advanceReminderDays} 天")
                    if (settings.sameDayReminderEnabled) add("當天")
                }.joinToString("與")
                ToggleRow(
                    title = "啟用通知",
                    subtitle = if (rules.isEmpty()) {
                        "請先到設定頁啟用提醒規則"
                    } else {
                        "${rules}約 ${LocalTime.of(settings.reminderHour, settings.reminderMinute).format(timeFormatter)} 提醒"
                    },
                    checked = reminderEnabled,
                    onCheckedChange = {
                        reminderEnabled = it
                        if (it) onRequestNotificationPermission()
                    },
                )
            }
            if (includesTime) {
                item {
                    PickerRow("時間", time.format(timeFormatter)) {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> time = LocalTime.of(hour, minute) },
                            time.hour,
                            time.minute,
                            true,
                        ).show()
                    }
                }
            }
            item {
                ToggleRow(
                    title = "固定時區",
                    subtitle = if (fixedTimeZone) storedZoneId else "跟隨手機目前時區",
                    checked = fixedTimeZone,
                    onCheckedChange = { fixedTimeZone = it },
                )
            }
            item {
                OptionTitle("分類")
                Spacer(Modifier.height(6.dp))
                ChoiceGrid(
                    options = AnniversaryCategory.entries,
                    selected = category,
                    label = AnniversaryCategory::label,
                    iconRes = AnniversaryCategory::iconRes,
                    onSelected = { category = it },
                )
            }
            item {
                OptionTitle("紀念日識別色")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    anniversaryPresetColors.forEach { preset ->
                        ColorChoice(
                            color = Color(preset.argb),
                            selected = colorArgb == preset.argb,
                            size = 36,
                            onClick = { colorArgb = preset.argb },
                            accessibilityLabel = preset.label,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                HueColorPicker(
                    colorArgb = colorArgb,
                    selected = true,
                    onColorChange = { colorArgb = it },
                    onColorChangeFinished = {},
                )
            }
            if (onRequestDelete != null) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    TextButton(onClick = onRequestDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("刪除此紀念日", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeColorChange: (AppThemeColor) -> Unit,
    onCustomThemeColorChange: (Int) -> Unit,
    onDisplayModeChange: (DisplayMode) -> Unit,
    notificationsAvailable: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onAdvanceReminderEnabledChange: (Boolean) -> Unit,
    onAdvanceReminderDaysChange: (Int) -> Unit,
    onSameDayReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var customPreviewArgb by remember(settings.themeSeedArgb) { mutableIntStateOf(settings.themeSeedArgb) }
    val presetOrder = listOf(
        AppThemeColor.SUNFLOWER,
        AppThemeColor.BERRY,
        AppThemeColor.TANGERINE,
        AppThemeColor.CLOVER,
        AppThemeColor.LAGOON,
        AppThemeColor.VIOLET,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SectionTitle("通知與提醒")
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    title = "紀念日通知",
                    subtitle = if (!settings.remindersEnabled) {
                        "所有提醒暫停，已設定的紀念日會顯示靜音鈴鐺"
                    } else if (notificationsAvailable) {
                        "通知功能正常"
                    } else {
                        "系統通知權限未開啟"
                    },
                    checked = settings.remindersEnabled,
                    onCheckedChange = onRemindersEnabledChange,
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    title = "提前提醒",
                    subtitle = "在紀念日前先通知一次",
                    checked = settings.advanceReminderEnabled,
                    onCheckedChange = onAdvanceReminderEnabledChange,
                )
                if (settings.advanceReminderEnabled) {
                    Spacer(Modifier.height(6.dp))
                    AdvanceDaysSelector(
                        options = listOf(1, 3, 7, 14, 30),
                        selected = settings.advanceReminderDays,
                        onSelected = onAdvanceReminderDaysChange,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    title = "當天提醒",
                    subtitle = "在紀念日當天再通知一次",
                    checked = settings.sameDayReminderEnabled,
                    onCheckedChange = onSameDayReminderEnabledChange,
                )
                Spacer(Modifier.height(8.dp))
                PickerRow(
                    "通知時間（系統可能因省電延後數分鐘）",
                    LocalTime.of(settings.reminderHour, settings.reminderMinute).format(timeFormatter),
                ) {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onReminderTimeChange(hour, minute) },
                        settings.reminderHour,
                        settings.reminderMinute,
                        true,
                    ).show()
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (!notificationsAvailable) {
                        TextButton(onClick = onRequestNotificationPermission) { Text("允許通知") }
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                },
                            )
                        }) { Text("系統設定") }
                    } else {
                        TextButton(onClick = onSendTestNotification) { Text("發送測試通知") }
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                SectionTitle("顯示模式")
                Spacer(Modifier.height(10.dp))
                DisplayModeSelector(settings.displayMode, onDisplayModeChange)
            }
            item { HorizontalDivider() }
            item {
                SectionTitle("主題色彩")
                Spacer(Modifier.height(12.dp))
                presetOrder.chunked(3).forEachIndexed { index, row ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { option ->
                            ThemeColorOption(
                                option = option,
                                selected = settings.customThemeColorArgb == null && settings.themeColor == option,
                                onClick = { onThemeColorChange(option) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                HueColorPicker(
                    colorArgb = customPreviewArgb,
                    selected = settings.customThemeColorArgb != null,
                    onColorChange = { customPreviewArgb = it },
                    onColorChangeFinished = { onCustomThemeColorChange(customPreviewArgb) },
                )
            }
            item { HorizontalDivider() }
            item {
                SectionTitle("日期與時區規則")
                Spacer(Modifier.height(10.dp))
                RuleText("每月遇到不存在的日期（如 2 月 31 日），改用當月最後一天。")
                RuleText("每年 2 月 29 日可選擇非閏年落在 2 月 28 日或 3 月 1 日。")
                RuleText("跟隨手機時區會維持當地相同時間；固定時區則在旅行時維持原地實際到期時刻。")
                RuleText("日光節約時間造成不存在的時間時，系統會順延到下一個有效時間。")
            }
            item { HorizontalDivider() }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SectionTitle("關於")
                    Spacer(Modifier.height(10.dp))
                    Text("版本 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Build ${BuildConfig.BUILD_ID}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayModeSelector(selected: DisplayMode, onSelected: (DisplayMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DisplayMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Surface(
                onClick = { onSelected(mode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(
                    text = mode.label(),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ThemeColorOption(
    option: AppThemeColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ColorChoice(Color(option.argb), selected, 26, onClick)
        Spacer(Modifier.height(5.dp))
        Text(option.label(), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun HueColorPicker(
    colorArgb: Int,
    selected: Boolean,
    onColorChange: (Int) -> Unit,
    onColorChangeFinished: () -> Unit,
) {
    var hue by remember(colorArgb) { mutableFloatStateOf(colorHue(colorArgb)) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorChoice(Color(colorArgb), selected, 26, {})
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(HUE_COLORS)),
            )
            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    onColorChange(hueColor(it))
                },
                onValueChangeFinished = onColorChangeFinished,
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(colorArgb),
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier.semantics { contentDescription = "色相" },
            )
        }
    }
}

private val HUE_COLORS = listOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Blue,
    Color.Magenta,
    Color.Red,
)

internal fun hueColor(hue: Float): Int = AndroidColor.HSVToColor(
    floatArrayOf(hue.coerceIn(0f, 360f), 0.72f, 0.72f),
)

internal fun colorHue(argb: Int): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(argb, hsv)
    return hsv[0]
}

@Composable
private fun RuleText(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("•", color = MaterialTheme.colorScheme.primary)
        Text(text, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun OptionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(label(option), maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun <T> ChoiceGrid(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    iconRes: (T) -> Int,
    onSelected: (T) -> Unit,
) {
    options.chunked(3).forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(label(option), maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(iconRes(option)),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AdvanceDaysSelector(
    options: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    options.chunked(3).forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { days ->
                FilterChip(
                    selected = selected == days,
                    onClick = { onSelected(days) },
                    label = { Text("$days 天") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ColorChoice(
    color: Color,
    selected: Boolean,
    size: Int,
    onClick: () -> Unit,
    accessibilityLabel: String? = null,
) {
    val accessibilityModifier = if (accessibilityLabel == null) {
        Modifier
    } else {
        Modifier.semantics {
            contentDescription = if (selected) "$accessibilityLabel，已選取" else accessibilityLabel
        }
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(size.dp)
            .then(accessibilityModifier),
        shape = CircleShape,
        color = color,
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Text(
                    "✓",
                    color = if (color.luminance() > 0.45f) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(2.dp))
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Text("變更", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun DisplayMode.label(): String = when (this) {
    DisplayMode.SYSTEM -> "跟隨系統"
    DisplayMode.LIGHT -> "淺色"
    DisplayMode.DARK -> "深色"
}

private fun AppThemeColor.label(): String = when (this) {
    AppThemeColor.SUNFLOWER -> "暖陽黃"
    AppThemeColor.BERRY -> "珊瑚紅"
    AppThemeColor.TANGERINE -> "活力橘"
    AppThemeColor.CLOVER -> "青草綠"
    AppThemeColor.LAGOON -> "天空藍"
    AppThemeColor.VIOLET -> "葡萄紫"
}
