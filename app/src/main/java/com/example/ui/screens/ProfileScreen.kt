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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AttributeEntity
import com.example.data.local.FolderEntity
import com.example.data.local.TagEntity
import com.example.data.local.parseStringCsv
import com.example.data.model.AchievementDefinition
import com.example.data.model.AppThemeId
import com.example.data.model.GAMEFY_ACHIEVEMENTS
import com.example.data.model.NotificationSettings
import com.example.data.model.StatsCoinRange
import com.example.data.model.StatsCompletionPeriod
import com.example.data.model.buildCompletionChartSeries
import com.example.data.model.calculateLevel
import com.example.data.model.computeCoinEconomySummary
import com.example.data.model.computePlayerAnalytics
import com.example.data.model.toNotificationSettings
import com.example.notifications.NotificationHelper
import com.example.ui.GamefyUiState
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.PixelProgressBar
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily
import com.example.ui.theme.paletteForTheme
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

private enum class AchievementFilter(val label: String) {
  ALL("Wszystkie"),
  UNLOCKED("Zdobyte"),
  LOCKED("Do zdobycia"),
  SECRET("Tajne"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  uiState: GamefyUiState,
  scrollToAchievementsTrigger: Int = 0,
  onAddAttribute: (String, String) -> Unit,
  onUpdateAttribute: (AttributeEntity) -> Unit,
  onDeleteAttribute: (Long) -> Unit,
  onAddFolder: (String, String) -> Unit,
  onUpdateFolder: (FolderEntity) -> Unit,
  onDeleteFolder: (Long) -> Unit,
  onCreateOrFindTag: suspend (String) -> TagEntity,
  onUpdateTag: (TagEntity) -> Unit,
  onDeleteTag: (Long) -> Unit,
  onSelectTheme: (AppThemeId) -> Unit,
  onToggleNotifications: (Boolean) -> Unit,
  onUpdateNotificationSettings: (NotificationSettings) -> Unit,
  onExportBackup: suspend () -> String,
  onImportBackup: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  var selectedTab by rememberSaveable { mutableStateOf(ProfileSubTab.CHARACTER) }
  var achievementFilter by rememberSaveable { mutableStateOf(AchievementFilter.ALL) }
  var statsCompletionPeriod by rememberSaveable { mutableStateOf(StatsCompletionPeriod.DAILY) }
  var statsCoinRange by rememberSaveable { mutableStateOf(StatsCoinRange.DAYS_30) }
  var editingAttribute by remember { mutableStateOf<AttributeEntity?>(null) }
  var showAddAttributeDialog by remember { mutableStateOf(false) }
  var showFolderManagerDialog by remember { mutableStateOf(false) }
  var showTagEditorDialog by remember { mutableStateOf(false) }
  var showReadyTasksDialog by remember { mutableStateOf(false) }
  var showRandomSideQuestDialog by remember { mutableStateOf(false) }

  LaunchedEffect(scrollToAchievementsTrigger) {
    if (scrollToAchievementsTrigger > 0) {
      selectedTab = ProfileSubTab.ACHIEVEMENTS
      achievementFilter = AchievementFilter.ALL
      listState.animateScrollToItem(0)
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    onToggleNotifications(granted)
  }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val json = onExportBackup()
        runCatching {
          context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
          }
        }
      }
    }
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
  val overallLevel = remember(uiState.profile.exp) { calculateLevel(uiState.profile.exp) }

  val filteredAchievements = remember(unlockedAch, achievementFilter) {
    when (achievementFilter) {
      AchievementFilter.ALL -> GAMEFY_ACHIEVEMENTS
      AchievementFilter.UNLOCKED -> GAMEFY_ACHIEVEMENTS.filter { it.id in unlockedAch }
      AchievementFilter.LOCKED -> GAMEFY_ACHIEVEMENTS.filter { it.id !in unlockedAch }
      AchievementFilter.SECRET -> GAMEFY_ACHIEVEMENTS.filter { it.isSecret }
    }
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
                .height(42.dp)
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
                  text = tab.label.uppercase(),
                  fontFamily = PixelFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.5.sp,
                  color = if (selected) Color.White else palette.textMuted,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
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
                unlockedCount = unlockedAch.size,
                totalAchievements = GAMEFY_ACHIEVEMENTS.size,
                onOpenAchievements = { selectedTab = ProfileSubTab.ACHIEVEMENTS },
              )
            }

            item(key = "attributes_header") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "ATRYBUTY (AWANS = +1 💎)",
                  style = MaterialTheme.typography.titleSmall,
                  color = palette.textMuted,
                )
                PixelButton(
                  text = "+ Dodaj",
                  compact = true,
                  modifier = Modifier.testTag("add_attribute_button"),
                  onClick = { showAddAttributeDialog = true },
                )
              }
            }

            if (uiState.attributes.isEmpty()) {
              item(key = "attributes_empty") {
                PixelCard {
                  Text(
                    text = "Brak atrybutów. Dodaj np. Siła, Wiedza lub Zdrowie, aby rozwijać postać za wykonane zadania.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textMuted,
                  )
                }
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
            item(key = "achievements_summary") {
              val achFraction = if (GAMEFY_ACHIEVEMENTS.isEmpty()) {
                0f
              } else {
                unlockedAch.size.toFloat() / GAMEFY_ACHIEVEMENTS.size.toFloat()
              }
              PixelCard {
                Text(
                  text = "OSIĄGNIĘCIA (${unlockedAch.size}/${GAMEFY_ACHIEVEMENTS.size})",
                  style = MaterialTheme.typography.titleSmall,
                  color = palette.textMuted,
                  modifier = Modifier.testTag("achievements_section_header"),
                )
                Spacer(modifier = Modifier.height(6.dp))
                PixelProgressBar(
                  progress = achFraction,
                  leadingIcon = "trophy",
                  barHeight = 16.dp,
                  fillStartColor = PixelColors.Gold,
                  fillEndColor = PixelColors.Orange,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = palette.surfaceSubtle,
                  border = BorderStroke(1.dp, palette.divider),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                    Text(
                      text = "🔥 Aktualna seria: ${uiState.profile.currentStreak} dni   ·   🏆 Rekord: ${uiState.profile.bestStreak} dni",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = palette.textPrimary,
                    )
                    Text(
                      text = "Każde odblokowane trofeum przyznaje bonusowe EXP, monety lub kryształy.",
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textMuted,
                    )
                  }
                }
              }
            }

            item(key = "achievements_filters") {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                AchievementFilter.entries.forEach { filter ->
                  val selected = achievementFilter == filter
                  Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) palette.accent else palette.surface,
                    border = BorderStroke(1.5.dp, if (selected) palette.outline else palette.divider),
                    modifier = Modifier.clickable { achievementFilter = filter },
                  ) {
                    Text(
                      text = filter.label,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = if (selected) Color.White else palette.textPrimary,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                  }
                }
              }
            }

            if (filteredAchievements.isEmpty()) {
              item(key = "achievements_empty") {
                PixelCard {
                  Text(
                    text = "Brak osiągnięć w wybranym filtrze.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted,
                  )
                }
              }
            } else {
              items(filteredAchievements, key = { "ach_${it.id}" }) { ach ->
                AchievementCard(
                  ach = ach,
                  isUnlocked = ach.id in unlockedAch,
                )
              }
            }
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
            item(key = "settings_org_header") {
              Text(
                text = "ORGANIZACJA ZADAŃ",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textMuted,
                modifier = Modifier.padding(start = 4.dp),
              )
            }
            item(key = "settings_org_card") {
              PixelCard {
                // Folders row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Foldery (${uiState.folders.size})",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = palette.textPrimary,
                    )
                    Text(
                      text = uiState.folders.take(4).joinToString(" · ") { "${it.icon} ${it.name}" }
                        .ifBlank { "Brak własnych folderów" },
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textMuted,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  PixelButton(
                    text = "Zarządzaj",
                    compact = true,
                    modifier = Modifier.testTag("open_folder_editor_button"),
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    onClick = { showFolderManagerDialog = true },
                  )
                }

                HorizontalDivider(
                  modifier = Modifier.padding(vertical = 12.dp),
                  color = palette.divider,
                )

                // Tags row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Tagi (${uiState.tags.size})",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = palette.textPrimary,
                    )
                    Text(
                      text = uiState.tags.take(5).joinToString(" ") { "#${it.name}" }
                        .ifBlank { "Wyszukuj, edytuj i usuwaj tagi" },
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textMuted,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  PixelButton(
                    text = "Edycja tagów",
                    compact = true,
                    modifier = Modifier.testTag("open_tag_editor_button"),
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    onClick = { showTagEditorDialog = true },
                  )
                }

                HorizontalDivider(
                  modifier = Modifier.padding(vertical = 12.dp),
                  color = palette.divider,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  PixelButton(
                    text = "Baza Gotowych Zadań",
                    compact = true,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("open_ready_tasks_db_button"),
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.LibraryBooks,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    onClick = { showReadyTasksDialog = true },
                  )

                  PixelButton(
                    text = "Random Side Quest",
                    compact = true,
                    containerColor = PixelColors.Orange,
                    contentColor = Color.White,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("open_random_side_quest_button"),
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    onClick = { showRandomSideQuestDialog = true },
                  )
                }
              }
            }

            item(key = "settings_appearance_header") {
              Text(
                text = "WYGLĄD I POWIADOMIENIA",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textMuted,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
              )
            }
            item(key = "settings_appearance_card") {
              PixelCard {
                Text(
                  text = "Motyw kolorystyczny",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = palette.textPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  AppThemeId.entries.forEach { theme ->
                    val selected = uiState.themeId == theme
                    val themePalette = remember(theme) { paletteForTheme(theme) }
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = if (selected) palette.accent else palette.surfaceSubtle,
                      border = BorderStroke(2.dp, if (selected) palette.outline else palette.divider),
                      modifier = Modifier
                        .clickable { onSelectTheme(theme) }
                        .testTag("theme_${theme.code}"),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Box(
                          modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(themePalette.accent)
                            .border(1.5.dp, Color.White, CircleShape),
                        )
                        Text(
                          text = theme.label.uppercase(),
                          fontFamily = PixelFontFamily,
                          fontWeight = FontWeight.Bold,
                          fontSize = 11.sp,
                          color = if (selected) Color.White else palette.textPrimary,
                        )
                        if (selected) {
                          Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                          )
                        }
                      }
                    }
                  }
                }

                HorizontalDivider(
                  modifier = Modifier.padding(vertical = 12.dp),
                  color = palette.divider,
                )

                val notifSettings = remember(uiState.profile) {
                  uiState.profile.toNotificationSettings()
                }
                NotificationPreferencesBlock(
                  notificationsEnabled = uiState.profile.notificationsEnabled,
                  settings = notifSettings,
                  tasks = uiState.tasks,
                  onToggleNotifications = { enable ->
                    if (enable) {
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !NotificationHelper.hasNotificationPermission(context)
                      ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                      } else {
                        onToggleNotifications(true)
                      }
                    } else {
                      onToggleNotifications(false)
                    }
                  },
                  onUpdateSettings = onUpdateNotificationSettings,
                )
              }
            }

            item(key = "settings_backup_header") {
              Text(
                text = "KOPIA ZAPASOWA (URZĄDZENIE / DYSK GOOGLE)",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textMuted,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
              )
            }
            item(key = "settings_backup_card") {
              PixelCard {
                Text(
                  text = "Aplikacja działa w 100% lokalnie i nie ma dostępu do internetu. Kopię możesz zapisać na urządzeniu lub wskazać folder Dysku Google w oknie systemowym.",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textMuted,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  PixelButton(
                    text = "Eksportuj",
                    compact = true,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    onClick = {
                      val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                      exportLauncher.launch("gamefy-kopia-$date.json")
                    },
                  )
                  PixelButton(
                    text = "Importuj",
                    compact = true,
                    containerColor = palette.surfaceSubtle,
                    contentColor = palette.textPrimary,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    onClick = {
                      importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                  )
                }
              }
            }
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

  if (showReadyTasksDialog) {
    AlertDialog(
      onDismissRequest = { showReadyTasksDialog = false },
      title = { Text("Baza Gotowych Zadań") },
      text = { Text("Sekcja gotowa do skonfigurowania.") },
      confirmButton = {
        TextButton(onClick = { showReadyTasksDialog = false }) {
          Text("Zamknij")
        }
      },
    )
  }

  if (showRandomSideQuestDialog) {
    AlertDialog(
      onDismissRequest = { showRandomSideQuestDialog = false },
      title = { Text("Random Side Quest") },
      text = { Text("Sekcja gotowa do skonfigurowania.") },
      confirmButton = {
        TextButton(onClick = { showRandomSideQuestDialog = false }) {
          Text("Zamknij")
        }
      },
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
          text = "POZIOM $level",
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
        emoji = "🔥",
        value = "$currentStreak dni",
        label = "Seria (rekord: $bestStreak)",
        modifier = Modifier.weight(1f),
      )
      HeroStatTile(
        emoji = "✅",
        value = "$tasksDone",
        label = "Ukończone zadania",
        modifier = Modifier.weight(1f),
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      HeroStatTile(
        emoji = "🍅",
        value = "$pomosDone",
        label = "Sesje skupienia",
        modifier = Modifier.weight(1f),
      )
      HeroStatTile(
        emoji = "🏆",
        value = "$unlockedCount/$totalAchievements",
        label = "Osiągnięcia ›",
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onOpenAchievements),
      )
    }
  }
}

@Composable
private fun HeroStatTile(
  emoji: String,
  value: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.dp, palette.divider),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(text = emoji, fontSize = 15.sp)
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
          ) {
            Text(
              text = "Poz. ${level.level} · ${level.currentExp}/${level.neededExp}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = palette.accent,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
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

@Composable
private fun AchievementCard(
  ach: AchievementDefinition,
  isUnlocked: Boolean,
) {
  val palette = LocalGamefyPalette.current
  val isHiddenSecret = ach.isSecret && !isUnlocked
  val cardBg = if (isUnlocked) {
    PixelColors.Gold.copy(alpha = 0.12f)
  } else {
    palette.surface
  }
  val cardBorder = if (isUnlocked) PixelColors.Gold else palette.divider

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = cardBg,
    border = BorderStroke(1.5.dp, cardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .alpha(if (isUnlocked) 1f else 0.72f)
      .testTag("achievement_item_${ach.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (isHiddenSecret) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surfaceSubtle)
            .border(1.5.dp, palette.divider, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "?",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = palette.textMuted,
          )
        }
      } else {
        PixelIcon(icon = ach.icon, size = 38.dp)
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = if (isHiddenSecret) "Tajne osiągnięcie" else ach.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          if (ach.isSecret) {
            val unlockedGoldTint = if (palette.isDark) PixelColors.Gold else PixelColors.Warning
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isUnlocked) PixelColors.Gold.copy(alpha = 0.25f) else palette.surfaceSubtle,
            ) {
              Text(
                text = if (isUnlocked) "TAJNE ✓" else "TAJNE",
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = if (isUnlocked) unlockedGoldTint else palette.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }

        Text(
          text = if (isHiddenSecret) {
            "Sekretne wyzwanie — odkryj podczas korzystania z aplikacji."
          } else {
            ach.description
          },
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )

        val rewardText = if (isHiddenSecret) "Nagroda: ???" else ach.rewardSummary()
        val rewardColor = if (isUnlocked) {
          if (palette.isDark) PixelColors.Gold else PixelColors.Warning
        } else {
          palette.accent
        }
        if (rewardText.isNotBlank()) {
          Text(
            text = rewardText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = rewardColor,
          )
        }
      }

      if (isUnlocked) {
        PixelIcon(icon = "trophy", size = 24.dp)
      }
    }
  }
}
