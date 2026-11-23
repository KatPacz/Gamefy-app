package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AttributeEntity
import com.example.data.local.FolderEntity
import com.example.data.local.TagEntity
import com.example.ui.components.PixelEmptyStateCard
import com.example.data.local.parseStringCsv
import com.example.data.model.AchievementCategory
import com.example.data.model.AppFontFamilyOption
import com.example.data.model.AppFontSize
import com.example.data.model.AppThemeId
import com.example.data.model.AppWordSpacing
import com.example.data.model.GAMEFY_ACHIEVEMENTS
import com.example.data.model.GAMEFY_ACHIEVEMENT_GROUPS
import com.example.data.model.NotificationSettings
import com.example.data.model.StatsCoinRange
import com.example.data.model.StatsCompletionPeriod
import com.example.data.model.buildCompletionChartSeries
import com.example.data.model.buildPlayerStatsSnapshot
import com.example.data.model.calculateLevel
import com.example.data.model.computeCoinEconomySummary
import com.example.data.model.computeGroupProgress
import com.example.data.model.computePlayerAnalytics
import com.example.data.model.isWeeklyStreakFreezeAvailable
import com.example.notifications.NotificationHelper
import com.example.ui.GamefyUiState
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.PixelProgressBar
import com.example.ui.components.PixelSectionHeader
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class ProfileSubTab(val label: String, val tag: String) {
  CHARACTER("Postać", "profile_tab_character"),
  ACHIEVEMENTS("Osiągnięcia", "profile_tab_achievements"),
  STATS("Statystyki", "profile_tab_stats"),
  SETTINGS("Ustawienia", "profile_tab_settings"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  uiState: GamefyUiState,
  scrollToAchievementsTrigger: Int = 0,
  scrollToNotificationsTrigger: Int = 0,
  scrollToBackupTrigger: Int = 0,
  onAddAttribute: (String, String) -> Unit,
  onUpdateAttribute: (AttributeEntity) -> Unit,
  onDeleteAttribute: (Long) -> Unit,
  onAddFolder: (String, String, String) -> Unit,
  onUpdateFolder: (FolderEntity) -> Unit,
  onDeleteFolder: (Long) -> Unit,
  onCreateOrFindTag: suspend (String) -> TagEntity,
  onUpdateTag: (TagEntity) -> Unit,
  onDeleteTag: (Long) -> Unit,
  onSelectTheme: (AppThemeId) -> Unit,
  onSelectSystemTheme: () -> Unit = {},
  onSelectFontSize: (AppFontSize) -> Unit = {},
  onSelectFontFamily: (AppFontFamilyOption) -> Unit = {},
  onSelectWordSpacing: (AppWordSpacing) -> Unit = {},
  onToggleNotifications: (Boolean) -> Unit,
  onUpdateNotificationSettings: (NotificationSettings) -> Unit,
  onExportBackup: suspend () -> String,
  onBackupExportResult: (Boolean) -> Unit = {},
  onImportBackup: (String) -> Unit,
  onOpenReadyTasksDb: () -> Unit = {},
  onOpenRandomQuest: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  var selectedTab by rememberSaveable { mutableStateOf(ProfileSubTab.CHARACTER) }
  var selectedAchievementCategory by rememberSaveable { mutableStateOf<AchievementCategory?>(null) }
  var achievementSortMode by rememberSaveable { mutableStateOf(AchievementSortMode.CLOSEST) }
  var statsCompletionPeriod by rememberSaveable { mutableStateOf(StatsCompletionPeriod.DAILY) }
  var statsCoinRange by rememberSaveable { mutableStateOf(StatsCoinRange.DAYS_30) }
  var editingAttribute by remember { mutableStateOf<AttributeEntity?>(null) }
  var showAddAttributeDialog by remember { mutableStateOf(false) }
  var showFolderManagerDialog by remember { mutableStateOf(false) }
  var showTagEditorDialog by remember { mutableStateOf(false) }
  var notificationPermissionDenied by remember { mutableStateOf(false) }

  val hasBackupWarningBanner = remember(uiState.profile.lastBackupExportTimestamp) {
    com.example.data.model.isBackupOverdueWarning(uiState.profile.lastBackupExportTimestamp)
  }

  LaunchedEffect(scrollToAchievementsTrigger) {
    if (scrollToAchievementsTrigger > 0) {
      selectedTab = ProfileSubTab.ACHIEVEMENTS
      selectedAchievementCategory = null
      achievementSortMode = AchievementSortMode.CLOSEST
      listState.animateScrollToItem(0)
    }
  }

  LaunchedEffect(scrollToNotificationsTrigger) {
    if (scrollToNotificationsTrigger > 0) {
      selectedTab = ProfileSubTab.SETTINGS
      withFrameNanos { }
      val targetIdx = settingsSectionIndex(
        section = ProfileSettingsSection.NOTIFICATIONS,
        hasBackupWarningBanner = hasBackupWarningBanner,
      )
      listState.animateScrollToItem(targetIdx)
    }
  }

  LaunchedEffect(scrollToBackupTrigger) {
    if (scrollToBackupTrigger > 0) {
      selectedTab = ProfileSubTab.SETTINGS
      withFrameNanos { }
      val targetIdx = settingsSectionIndex(
        section = ProfileSettingsSection.BACKUP,
        hasBackupWarningBanner = hasBackupWarningBanner,
      )
      listState.animateScrollToItem(targetIdx)
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    notificationPermissionDenied = !granted
    onToggleNotifications(granted)
  }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val json = onExportBackup()
        val ok = runCatching {
          context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
          } != null
        }.getOrDefault(false)
        onBackupExportResult(ok)
      }
    }
  }

  val launchBackupExport = {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    exportLauncher.launch("gamefy-kopia-$date.json")
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val content = runCatching {
          context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (!content.isNullOrBlank()) {
          onImportBackup(content)
        }
      }
    }
  }

  val unlockedAch = remember(uiState.profile.unlockedAchievementsCsv) {
    parseStringCsv(uiState.profile.unlockedAchievementsCsv)
  }
  val validUnlockedAchCount = remember(unlockedAch) {
    GAMEFY_ACHIEVEMENTS.count { it.id in unlockedAch }
  }
  val overallLevel = remember(uiState.profile.exp) { calculateLevel(uiState.profile.exp) }
  val streakFreezeAvailable = remember(uiState.profile.streakFreezeUsedWeekKey) {
    isWeeklyStreakFreezeAvailable(uiState.profile.streakFreezeUsedWeekKey)
  }

  val playerStatsSnapshot = remember(
    uiState.profile,
    uiState.tasks,
    uiState.attributes,
    uiState.activityEvents,
  ) {
    buildPlayerStatsSnapshot(
      profile = uiState.profile,
      tasks = uiState.tasks,
      attributes = uiState.attributes,
      events = uiState.activityEvents,
    )
  }
  val visibleAchievementGroups = remember(
    playerStatsSnapshot,
    unlockedAch,
    selectedAchievementCategory,
    achievementSortMode,
  ) {
    val allProgress = GAMEFY_ACHIEVEMENT_GROUPS.map { group ->
      computeGroupProgress(
        group = group,
        stats = playerStatsSnapshot,
        unlockedIds = unlockedAch,
      )
    }
    filterAndSortAchievementGroups(
      allProgress = allProgress,
      selectedCategory = selectedAchievementCategory,
      sortMode = achievementSortMode,
    )
  }

  val playerAnalytics = remember(uiState.profile, uiState.tasks, uiState.activityEvents) {
    computePlayerAnalytics(
      profile = uiState.profile,
      tasks = uiState.tasks,
      events = uiState.activityEvents,
    )
  }
  val completionChartSeries = remember(uiState.activityEvents, statsCompletionPeriod) {
    buildCompletionChartSeries(
      events = uiState.activityEvents,
      period = statsCompletionPeriod,
    )
  }
  val coinEconomySummary = remember(uiState.profile, uiState.shopItems, uiState.activityEvents, statsCoinRange) {
    computeCoinEconomySummary(
      profile = uiState.profile,
      shopItems = uiState.shopItems,
      events = uiState.activityEvents,
      range = statsCoinRange,
    )
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
    ) {
      // Clean segmented sub-tab selector at the top of Profile
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceSubtle,
        border = BorderStroke(1.5.dp, palette.divider),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        Row(
          modifier = Modifier.padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          ProfileSubTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (selected) palette.accent else Color.Transparent,
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Tab) {
                  selectedTab = tab
                  scope.launch { listState.scrollToItem(0) }
                }
                .testTag(tab.tag),
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 2.dp),
              ) {
                Text(
                  text = tab.label,
                  fontFamily = PixelFontFamily,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                  fontSize = 13.sp,
                  color = if (selected) palette.onAccent else palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      if (selectedTab == ProfileSubTab.SETTINGS) {
        ProfileSettingsShortcutsBar(
          onJumpToSection = { section ->
            val idx = settingsSectionIndex(
              section = section,
              hasBackupWarningBanner = hasBackupWarningBanner,
            )
            scope.launch { listState.animateScrollToItem(idx) }
          },
        )
      }

      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .testTag("profile_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        when (selectedTab) {
          ProfileSubTab.CHARACTER -> {
            item(key = "hero_card") {
              PlayerHeroCard(
                level = overallLevel.level,
                currentExp = overallLevel.currentExp,
                neededExp = overallLevel.neededExp,
                fraction = overallLevel.fraction,
                currentStreak = uiState.profile.currentStreak,
                bestStreak = uiState.profile.bestStreak,
                tasksDone = uiState.profile.tasksDone,
                pomosDone = uiState.profile.pomosDone,
                unlockedCount = validUnlockedAchCount,
                totalAchievements = GAMEFY_ACHIEVEMENTS.size,
                onOpenAchievements = { selectedTab = ProfileSubTab.ACHIEVEMENTS },
              )
            }

            item(key = "profile_backup_status_card") {
              ProfileBackupStatusCard(
                lastBackupExportTimestamp = uiState.profile.lastBackupExportTimestamp,
                onCreateBackup = launchBackupExport,
              )
            }

            item(key = "attributes_header") {
              PixelSectionHeader(
                title = "Atrybuty",
                subtitle = "Awans atrybutu = +1 brylant",
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                trailing = {
                  PixelButton(
                    text = "+ Nowy",
                    compact = true,
                    modifier = Modifier.testTag("add_attribute_button"),
                    onClick = { showAddAttributeDialog = true },
                  )
                },
              )
            }

            if (uiState.attributes.isEmpty()) {
              item(key = "attributes_empty") {
                PixelEmptyStateCard(
                  illustrationRes = R.drawable.img_empty_tasks,
                  illustrationContentDescription = "Ilustracja pustej listy atrybutów",
                  title = "Brak atrybutów",
                  subtitle = "Dodaj np. Siła, Wiedza lub Zdrowie, aby rozwijać postać za wykonane zadania.",
                  primaryActionLabel = "Dodaj atrybut",
                  onPrimaryAction = { showAddAttributeDialog = true },
                  primaryActionTestTag = "empty_attributes_add_button",
                )
              }
            } else {
              items(uiState.attributes, key = { "attr_${it.id}" }) { attr ->
                AttributeStatCard(
                  attr = attr,
                  onEdit = { editingAttribute = attr },
                )
              }
            }
          }

          ProfileSubTab.ACHIEVEMENTS -> {
            profileAchievementsItems(
              unlockedCount = validUnlockedAchCount,
              totalCount = GAMEFY_ACHIEVEMENTS.size,
              currentStreak = uiState.profile.currentStreak,
              bestStreak = uiState.profile.bestStreak,
              streakFreezeAvailable = streakFreezeAvailable,
              selectedCategory = selectedAchievementCategory,
              onSelectCategory = { selectedAchievementCategory = it },
              sortMode = achievementSortMode,
              onSelectSortMode = { achievementSortMode = it },
              visibleGroups = visibleAchievementGroups,
            )
          }

          ProfileSubTab.STATS -> {
            profileStatsItems(
              analytics = playerAnalytics,
              currentStreak = uiState.profile.currentStreak,
              bestStreak = uiState.profile.bestStreak,
              completionPeriod = statsCompletionPeriod,
              onSelectCompletionPeriod = { statsCompletionPeriod = it },
              chartSeries = completionChartSeries,
              coinRange = statsCoinRange,
              onSelectCoinRange = { statsCoinRange = it },
              economy = coinEconomySummary,
            )
          }

          ProfileSubTab.SETTINGS -> {
            profileSettingsItems(
              uiState = uiState,
              notificationPermissionDenied = notificationPermissionDenied,
              onToggleNotifications = { enable ->
                if (enable) {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationHelper.hasNotificationPermission(context)
                  ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  } else {
                    notificationPermissionDenied = false
                    onToggleNotifications(true)
                  }
                } else {
                  onToggleNotifications(false)
                }
              },
              onUpdateNotificationSettings = onUpdateNotificationSettings,
              onOpenFolderManager = { showFolderManagerDialog = true },
              onOpenTagManager = { showTagEditorDialog = true },
              onOpenReadyTasksDb = onOpenReadyTasksDb,
              onSaveBackup = launchBackupExport,
              onLoadBackup = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
              },
              onSelectTheme = onSelectTheme,
              onSelectSystemTheme = onSelectSystemTheme,
              onSelectFontSize = onSelectFontSize,
              onSelectFontFamily = onSelectFontFamily,
              onSelectWordSpacing = onSelectWordSpacing,
            )
          }
        }
      }
    }
  }

  if (showAddAttributeDialog || editingAttribute != null) {
    AttributeEditorDialog(
      existingAttr = editingAttribute,
      onSaveNew = onAddAttribute,
      onUpdate = onUpdateAttribute,
      onDelete = onDeleteAttribute,
      onDismiss = {
        showAddAttributeDialog = false
        editingAttribute = null
      },
    )
  }

  if (showFolderManagerDialog) {
    FolderManagerDialog(
      folders = uiState.folders,
      onAddFolder = onAddFolder,
      onUpdateFolder = onUpdateFolder,
      onDeleteFolder = onDeleteFolder,
      onDismiss = { showFolderManagerDialog = false },
    )
  }

  if (showTagEditorDialog) {
    TagManagerDialog(
      tags = uiState.tags,
      onCreateTag = onCreateOrFindTag,
      onUpdateTag = onUpdateTag,
      onDeleteTag = onDeleteTag,
      onDismiss = { showTagEditorDialog = false },
    )
  }
}

@Composable
private fun PlayerHeroCard(
  level: Int,
  currentExp: Int,
  neededExp: Int,
  fraction: Float,
  currentStreak: Int,
  bestStreak: Int,
  tasksDone: Int,
  pomosDone: Int,
  unlockedCount: Int,
  totalAchievements: Int,
  onOpenAchievements: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  PixelCard {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Brush.linearGradient(listOf(palette.accent, palette.accentSecondary)))
          .border(2.5.dp, palette.outline, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "$level",
          fontFamily = PixelFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          color = Color.White,
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Poziom $level",
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
        Text(
          text = "$currentExp / $neededExp EXP do kolejnego poziomu",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )
        Spacer(modifier = Modifier.height(6.dp))
        PixelProgressBar(progress = fraction, barHeight = 14.dp)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      HeroStatTile(
        icon = "flame",
        value = "$currentStreak dni",
        label = "Seria (rekord: $bestStreak)",
        modifier = Modifier.weight(1f),
      )
      HeroStatTile(
        icon = "check",
        value = "$tasksDone",
        label = "Ukończone zadania",
        modifier = Modifier.weight(1f),
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      HeroStatTile(
        icon = "timer",
        value = "$pomosDone",
        label = "Sesje skupienia",
        modifier = Modifier.weight(1f),
      )
      HeroStatTile(
        icon = "trophy",
        value = "$unlockedCount/$totalAchievements",
        label = "Osiągnięcia",
        showChevron = true,
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .clickable(
            role = Role.Button,
            onClickLabel = "Otwórz zakładkę osiągnięć",
            onClick = onOpenAchievements,
          )
          .testTag("hero_achievements_tile"),
      )
    }
  }
}

@Composable
private fun HeroStatTile(
  icon: String,
  value: String,
  label: String,
  modifier: Modifier = Modifier,
  showChevron: Boolean = false,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.dp, palette.divider),
    modifier = modifier.heightIn(min = 48.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PixelIcon(icon = icon, size = 18.dp)
          Text(
            text = value,
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = palette.textPrimary,
            maxLines = 1,
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = palette.textMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (showChevron) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = palette.accent,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
private fun AttributeStatCard(
  attr: AttributeEntity,
  onEdit: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val level = remember(attr.exp) { calculateLevel(attr.exp) }

  PixelCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceSubtle,
        border = BorderStroke(1.5.dp, palette.divider),
      ) {
        Box(
          modifier = Modifier.size(44.dp),
          contentAlignment = Alignment.Center,
        ) {
          PixelIcon(icon = attr.icon, size = 28.dp)
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = attr.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.accent.copy(alpha = 0.14f),
            modifier = Modifier.semantics(mergeDescendants = true) {
              contentDescription = "Poziom ${level.level}, ${level.currentExp} z ${level.neededExp} EXP"
            },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              PixelIcon(icon = "star", size = 12.dp)
              Text(
                text = "${level.level} · ${level.currentExp}/${level.neededExp}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(7.dp))
        PixelProgressBar(
          progress = level.fraction,
          barHeight = 14.dp,
        )
      }

      IconButton(
        onClick = onEdit,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Edytuj atrybut ${attr.name}",
          tint = palette.textMuted,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}
