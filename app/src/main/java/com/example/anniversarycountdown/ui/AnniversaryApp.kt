package com.example.anniversarycountdown.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryCategory
import com.example.anniversarycountdown.data.AnniversaryColor
import com.example.anniversarycountdown.data.RepeatRule
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

internal val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日", Locale.TAIWAN)
internal val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.TAIWAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryApp(viewModel: AnniversaryViewModel) {
    val anniversaries by viewModel.anniversaries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var nowEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var editorOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var editingAnniversary by remember { mutableStateOf<Anniversary?>(null) }
    var deleteCandidate by remember { mutableStateOf<Anniversary?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowEpochMillis = current
            delay(60_000L - current % 60_000L)
        }
    }

    val sorted = remember(anniversaries, nowEpochMillis) {
        sortedAnniversaries(anniversaries, nowEpochMillis)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("紀念日倒數", fontWeight = FontWeight.Bold)
                        if (anniversaries.isNotEmpty()) {
                            Text(
                                "${anniversaries.size} 個重要日子",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { settingsOpen = true }) { Text("設定") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAnniversary = null
                    editorOpen = true
                },
                modifier = Modifier.semantics { contentDescription = "新增紀念日" },
                shape = CircleShape,
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        },
    ) { innerPadding ->
        if (sorted.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                onAdd = {
                    editingAnniversary = null
                    editorOpen = true
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "把重要日子放在心上，期待就有了形狀。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                items(sorted, key = { it.id }) { anniversary ->
                    AnniversaryCard(anniversary, nowEpochMillis) {
                        editingAnniversary = anniversary
                        editorOpen = true
                    }
                }
            }
        }
    }

    if (editorOpen) {
        AnniversaryEditorDialog(
            anniversary = editingAnniversary,
            onDismiss = { editorOpen = false },
            onSave = { draft ->
                viewModel.save(
                    editingAnniversary,
                    draft.name,
                    draft.date,
                    draft.time,
                    draft.repeatRule,
                    draft.leapDayRule,
                    draft.category,
                    draft.color,
                    draft.fixedZoneId,
                )
                editorOpen = false
            },
            onRequestDelete = editingAnniversary?.let { anniversary ->
                {
                    editorOpen = false
                    deleteCandidate = anniversary
                }
            },
        )
    }

    if (settingsOpen) {
        SettingsDialog(
            settings = settings,
            onThemeColorChange = viewModel::setThemeColor,
            onDarkModeChange = viewModel::setDarkMode,
            onDismiss = { settingsOpen = false },
        )
    }

    deleteCandidate?.let { anniversary ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("刪除紀念日？") },
            text = { Text("「${anniversary.name}」刪除後無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(anniversary)
                    deleteCandidate = null
                }) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("♡", color = MaterialTheme.colorScheme.primary, fontSize = 48.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text("記住每個值得期待的日子", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "新增生日、旅行、週年或任何重要時刻，\n這裡會替你算好每一分期待。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            onClick = onAdd,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                "新增第一個紀念日",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AnniversaryCard(
    anniversary: Anniversary,
    nowEpochMillis: Long,
    onClick: () -> Unit,
) {
    val occurrence = anniversary.occurrence(nowEpochMillis)
    val expired = anniversary.isExpired(nowEpochMillis)
    val eventColor = anniversary.color.composeColor()
    val lessThanAWeek = occurrence.epochMillis in nowEpochMillis..(nowEpochMillis + 7 * 86_400_000L)
    val containerColor = when {
        expired -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        lessThanAWeek -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(6.dp)
                    .height(116.dp)
                    .background(eventColor),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.width(58.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        occurrence.dateTime.monthValue.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium,
                        color = eventColor,
                    )
                    Text(
                        occurrence.dateTime.dayOfMonth.toString().padStart(2, '0'),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        occurrence.dateTime.year.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            anniversary.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (anniversary.repeatRule != RepeatRule.NONE) {
                            Spacer(Modifier.width(6.dp))
                            Tag(anniversary.repeatRule.label(), eventColor)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append(occurrence.dateTime.toLocalDate().format(dateFormatter))
                            if (anniversary.hasTime) append("　${occurrence.dateTime.toLocalTime().format(timeFormatter)}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Tag(anniversary.category.label(), eventColor)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            anniversaryCountdownText(anniversary, nowEpochMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (expired) MaterialTheme.colorScheme.onSurfaceVariant else eventColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

internal fun AnniversaryCategory.label(): String = when (this) {
    AnniversaryCategory.BIRTHDAY -> "生日"
    AnniversaryCategory.LOVE -> "愛情"
    AnniversaryCategory.FAMILY -> "家庭"
    AnniversaryCategory.TRAVEL -> "旅行"
    AnniversaryCategory.WORK -> "工作"
    AnniversaryCategory.OTHER -> "其他"
}

internal fun RepeatRule.label(): String = when (this) {
    RepeatRule.NONE -> "不重複"
    RepeatRule.MONTHLY -> "每月"
    RepeatRule.YEARLY -> "每年"
}

internal fun AnniversaryColor.composeColor(): Color = when (this) {
    AnniversaryColor.ROSE -> Color(0xFFE65378)
    AnniversaryColor.ORANGE -> Color(0xFFF07A3C)
    AnniversaryColor.SUNSHINE -> Color(0xFFD19A00)
    AnniversaryColor.MINT -> Color(0xFF36A56B)
    AnniversaryColor.OCEAN -> Color(0xFF258DA4)
    AnniversaryColor.GRAPE -> Color(0xFF8964C4)
}
