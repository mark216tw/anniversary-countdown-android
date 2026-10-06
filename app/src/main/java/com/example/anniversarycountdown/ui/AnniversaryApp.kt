package com.example.anniversarycountdown.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.example.anniversarycountdown.data.Anniversary
import com.example.anniversarycountdown.data.AnniversaryCategory
import com.example.anniversarycountdown.data.AppSettings
import com.example.anniversarycountdown.data.RepeatRule
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

internal val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日", Locale.TAIWAN)
internal val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.TAIWAN)

private sealed interface AppDestination {
    data object Main : AppDestination
    data object Settings : AppDestination
    data class Editor(val anniversary: Anniversary?) : AppDestination
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryApp(viewModel: AnniversaryViewModel, onMoveToBackground: () -> Unit) {
    val anniversaries by viewModel.anniversaries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val notificationsAvailable by viewModel.notificationsAvailable.collectAsStateWithLifecycle()
    val anniversariesLoaded by viewModel.anniversariesLoaded.collectAsStateWithLifecycle()
    val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var startupNotificationCheckCompleted by rememberSaveable { mutableStateOf(false) }
    var showNotificationGuidance by rememberSaveable { mutableStateOf(false) }
    var guidanceRequiresSystemSettings by rememberSaveable { mutableStateOf(false) }
    var permissionRequestFromGuidance by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshNotificationAvailability()
        if (!granted && permissionRequestFromGuidance) {
            guidanceRequiresSystemSettings = true
            showNotificationGuidance = true
        }
        permissionRequestFromGuidance = false
    }
    val requestNotificationPermission = {
        permissionRequestFromGuidance = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.refreshNotificationAvailability()
        }
    }
    var nowEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var destination by remember { mutableStateOf<AppDestination>(AppDestination.Main) }
    var deleteCandidate by remember { mutableStateOf<Anniversary?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshNotificationAvailability()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(
        anniversariesLoaded,
        settingsLoaded,
        anniversaries,
        settings,
        notificationsAvailable,
    ) {
        if (!startupNotificationCheckCompleted && anniversariesLoaded && settingsLoaded) {
            startupNotificationCheckCompleted = true
            showNotificationGuidance = shouldShowNotificationGuidance(
                anniversaries = anniversaries,
                settings = settings,
                dataLoaded = true,
                notificationsAvailable = notificationsAvailable,
            )
        }
    }

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

    BackHandler(enabled = destination != AppDestination.Main) {
        destination = AppDestination.Main
    }

    when (val currentDestination = destination) {
        AppDestination.Main -> MainScreen(
            anniversaries = anniversaries,
            sorted = sorted,
            nowEpochMillis = nowEpochMillis,
            remindersAvailable = settings.remindersEnabled && settings.hasEnabledReminderRule && notificationsAvailable,
            onOpenSettings = { destination = AppDestination.Settings },
            onMoveToBackground = onMoveToBackground,
            onAdd = { destination = AppDestination.Editor(null) },
            onEdit = { destination = AppDestination.Editor(it) },
        )
        AppDestination.Settings -> SettingsScreen(
            settings = settings,
            onThemeColorChange = viewModel::setThemeColor,
            onCustomThemeColorChange = viewModel::setCustomThemeColor,
            onDisplayModeChange = viewModel::setDisplayMode,
            notificationsAvailable = notificationsAvailable,
            onRemindersEnabledChange = {
                viewModel.setRemindersEnabled(it)
                if (it && !notificationsAvailable) requestNotificationPermission()
            },
            onAdvanceReminderEnabledChange = viewModel::setAdvanceReminderEnabled,
            onAdvanceReminderDaysChange = viewModel::setAdvanceReminderDays,
            onSameDayReminderEnabledChange = viewModel::setSameDayReminderEnabled,
            onReminderTimeChange = viewModel::setReminderTime,
            onRequestNotificationPermission = requestNotificationPermission,
            onSendTestNotification = viewModel::sendTestNotification,
            onDismiss = { destination = AppDestination.Main },
        )
        is AppDestination.Editor -> AnniversaryEditorScreen(
            anniversary = currentDestination.anniversary,
            settings = settings,
            onDismiss = { destination = AppDestination.Main },
            onSave = { draft ->
                viewModel.save(
                    currentDestination.anniversary,
                    draft.name,
                    draft.date,
                    draft.time,
                    draft.repeatRule,
                    draft.leapDayRule,
                    draft.category,
                    draft.colorArgb,
                    draft.fixedZoneId,
                    draft.reminderEnabled,
                )
                destination = AppDestination.Main
            },
            onRequestNotificationPermission = requestNotificationPermission,
            onRequestDelete = currentDestination.anniversary?.let { anniversary ->
                {
                    destination = AppDestination.Main
                    deleteCandidate = anniversary
                }
            },
        )
    }

    if (showNotificationGuidance) {
        AlertDialog(
            onDismissRequest = { showNotificationGuidance = false },
            title = {
                Text(if (guidanceRequiresSystemSettings) "開啟系統通知" else "開啟紀念日通知")
            },
            text = {
                Text(
                    if (guidanceRequiresSystemSettings) {
                        "通知權限目前仍未開啟。請前往系統設定允許通知，才能收到已設定的紀念日提醒。"
                    } else {
                        "你有已啟用提醒的紀念日。允許通知後，App 才能在紀念日前與當天通知你。"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationGuidance = false
                    val permissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (!guidanceRequiresSystemSettings && permissionMissing) {
                        permissionRequestFromGuidance = true
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings(context)
                    }
                }) {
                    Text(if (guidanceRequiresSystemSettings) "前往系統設定" else "啟用通知")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationGuidance = false }) { Text("稍後") }
            },
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

internal fun shouldShowNotificationGuidance(
    anniversaries: List<Anniversary>,
    settings: AppSettings,
    dataLoaded: Boolean,
    notificationsAvailable: Boolean,
): Boolean = dataLoaded &&
    settings.remindersEnabled &&
    settings.hasEnabledReminderRule &&
    anniversaries.any { it.reminderEnabled } &&
    !notificationsAvailable

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    anniversaries: List<Anniversary>,
    sorted: List<Anniversary>,
    nowEpochMillis: Long,
    remindersAvailable: Boolean,
    onOpenSettings: () -> Unit,
    onMoveToBackground: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Anniversary) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("紀念日提醒", fontWeight = FontWeight.Bold)
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "設定")
                    }
                    IconButton(onClick = onMoveToBackground) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = "將 APP 切換到背景")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
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
                onAdd = onAdd,
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
                    AnniversaryCard(anniversary, nowEpochMillis, remindersAvailable) { onEdit(anniversary) }
                }
            }
        }
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
    remindersAvailable: Boolean,
    onClick: () -> Unit,
) {
    val occurrence = anniversary.occurrence(nowEpochMillis)
    val expired = anniversary.isExpired(nowEpochMillis)
    val elapsedText = anniversaryElapsedText(anniversary, nowEpochMillis)
    val eventColor = Color(anniversary.effectiveColorArgb)
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
                    .height(if (elapsedText == null) 116.dp else 136.dp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                anniversary.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                buildString {
                                    append(occurrence.dateTime.toLocalDate().format(dateFormatter))
                                    if (anniversary.hasTime) {
                                        append("　${occurrence.dateTime.toLocalTime().format(timeFormatter)}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (anniversary.reminderEnabled) {
                            Icon(
                                imageVector = if (remindersAvailable) {
                                    Icons.Rounded.NotificationsActive
                                } else {
                                    Icons.Rounded.NotificationsOff
                                },
                                contentDescription = if (remindersAvailable) {
                                    "提醒已開啟"
                                } else {
                                    "提醒已設定，但通知目前關閉"
                                },
                                tint = if (remindersAvailable) eventColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 5.dp, top = 2.dp)
                                    .size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            if (anniversary.repeatRule != RepeatRule.NONE) {
                                RepeatTag(anniversary.repeatRule.label())
                                Spacer(Modifier.height(4.dp))
                            }
                            CategoryTag(anniversary.category, eventColor)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        anniversaryCountdownText(anniversary, nowEpochMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (expired) MaterialTheme.colorScheme.onSurfaceVariant else eventColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (elapsedText != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            elapsedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTag(category: AnniversaryCategory, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(category.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(category.label(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RepeatTag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

internal fun RepeatRule.label(): String = when (this) {
    RepeatRule.NONE -> "不重複"
    RepeatRule.MONTHLY -> "每月"
    RepeatRule.YEARLY -> "每年"
}
