package com.example.anniversarycountdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryCategory
import com.example.anniversarycountdown.data.AnniversaryColor
import com.example.anniversarycountdown.data.AppSettings
import com.example.anniversarycountdown.data.AppThemeColor
import com.example.anniversarycountdown.data.LeapDayRule
import com.example.anniversarycountdown.data.RepeatRule
import com.example.anniversarycountdown.ui.theme.themePalette
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
    val color: AnniversaryColor,
    val fixedZoneId: String?,
)

@Composable
fun AnniversaryEditorDialog(
    anniversary: Anniversary?,
    onDismiss: () -> Unit,
    onSave: (AnniversaryDraft) -> Unit,
    onRequestDelete: (() -> Unit)?,
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
    var color by remember(anniversary?.id) { mutableStateOf(anniversary?.color ?: AnniversaryColor.ROSE) }
    var fixedTimeZone by remember(anniversary?.id) { mutableStateOf(anniversary?.fixedZoneId != null) }
    val storedZoneId = remember(anniversary?.id) {
        anniversary?.fixedZoneId ?: ZoneId.systemDefault().id
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (anniversary == null) "新增紀念日" else "編輯紀念日") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("紀念日名稱") },
                    placeholder = { Text("例如：小安的生日") },
                    singleLine = true,
                )
                PickerRow("日期", date.format(dateFormatter)) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth,
                    ).show()
                }
                OptionTitle("重複")
                ChoiceRow(
                    options = RepeatRule.entries,
                    selected = repeatRule,
                    label = RepeatRule::label,
                    onSelected = { repeatRule = it },
                )
                if (repeatRule == RepeatRule.YEARLY && date.monthValue == 2 && date.dayOfMonth == 29) {
                    OptionTitle("非閏年的 2 月 29 日")
                    ChoiceRow(
                        options = LeapDayRule.entries,
                        selected = leapDayRule,
                        label = { if (it == LeapDayRule.FEBRUARY_28) "2 月 28 日" else "3 月 1 日" },
                        onSelected = { leapDayRule = it },
                    )
                }
                ToggleRow(
                    title = "指定時間",
                    subtitle = "未指定時，當天會顯示「就是今天」",
                    checked = includesTime,
                    onCheckedChange = { includesTime = it },
                )
                if (includesTime) {
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
                ToggleRow(
                    title = "固定時區",
                    subtitle = if (fixedTimeZone) storedZoneId else "跟隨手機目前時區",
                    checked = fixedTimeZone,
                    onCheckedChange = { fixedTimeZone = it },
                )
                OptionTitle("分類")
                ChoiceGrid(
                    options = AnniversaryCategory.entries,
                    selected = category,
                    label = AnniversaryCategory::label,
                    onSelected = { category = it },
                )
                OptionTitle("紀念日顏色")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AnniversaryColor.entries.forEach { option ->
                        ColorChoice(
                            color = option.composeColor(),
                            selected = color == option,
                            onClick = { color = option },
                        )
                    }
                }
                if (onRequestDelete != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    TextButton(onClick = onRequestDelete, modifier = Modifier.align(Alignment.End)) {
                        Text("刪除此紀念日", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        AnniversaryDraft(
                            name.trim(),
                            date,
                            if (includesTime) time else null,
                            repeatRule,
                            leapDayRule,
                            category,
                            color,
                            if (fixedTimeZone) storedZoneId else null,
                        ),
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun SettingsDialog(
    settings: AppSettings,
    onThemeColorChange: (AppThemeColor) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App 設定") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("主題色彩", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "點選後立即套用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppThemeColor.entries.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        row.forEach { option ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ColorChoice(
                                    color = themePalette(option).primary,
                                    selected = settings.themeColor == option,
                                    size = 44,
                                    onClick = { onThemeColorChange(option) },
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(option.label(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                HorizontalDivider()
                ToggleRow(
                    title = "深色模式",
                    subtitle = "同步調整狀態列與手機導覽列",
                    checked = settings.darkMode,
                    onCheckedChange = onDarkModeChange,
                )
                HorizontalDivider()
                Text("日期與時區規則", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                RuleText("每月遇到不存在的日期（如 2 月 31 日），改用當月最後一天。")
                RuleText("每年 2 月 29 日可選擇非閏年落在 2 月 28 日或 3 月 1 日。")
                RuleText("跟隨手機時區會維持當地相同時間；固定時區則在旅行時維持原地實際到期時刻。")
                RuleText("日光節約時間造成不存在的時間時，系統會順延到下一個有效時間。")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun RuleText(text: String) {
    Row {
        Text("•", color = MaterialTheme.colorScheme.primary)
        Text(text, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
    }
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
                label = {
                    Text(if (selected == option) "✓ ${label(option)}" else label(option))
                },
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
    onSelected: (T) -> Unit,
) {
    options.chunked(3).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = {
                        Text(if (selected == option) "✓ ${label(option)}" else label(option))
                    },
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
    size: Int = 36,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size.dp),
        shape = CircleShape,
        color = color,
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
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

private fun AppThemeColor.label(): String = when (this) {
    AppThemeColor.BERRY -> "莓果"
    AppThemeColor.TANGERINE -> "橘子"
    AppThemeColor.SUNFLOWER -> "向日葵"
    AppThemeColor.CLOVER -> "幸運草"
    AppThemeColor.LAGOON -> "湖水"
    AppThemeColor.VIOLET -> "葡萄"
}
