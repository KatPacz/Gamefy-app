package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FolderEntity
import com.example.data.local.TaskEntity
import com.example.data.local.parseIdCsv
import com.example.data.local.parseSubtasksJson
import com.example.data.model.OnboardingDetailPref
import com.example.data.model.ReminderMode
import com.example.data.model.TaskCategory
import com.example.data.model.TaskSortDirection
import com.example.data.model.TaskViewMode
import com.example.data.model.formatFrequencyLabel
import com.example.data.model.addLocalDaysMillis
import com.example.data.model.addLocalMonthsMillis
import com.example.data.model.defaultSortDirectionFor
import com.example.data.model.groupTasksByCategory
import com.example.data.model.groupTasksByDifficulty
import com.example.data.model.sortTasksByReward
import com.example.data.model.sortTasksByTimeLeft
import com.example.data.model.startOfLocalDayMillis
import com.example.data.model.startOfLocalMonthMillis
import com.example.data.model.tasksDueOnDay
import com.example.ui.GamefyUiState
import com.example.ui.TaskInlineTimerState
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.TaskCardItem
import com.example.ui.components.resolveFolderColor
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
  uiState: GamefyUiState,
  inlineTimer: TaskInlineTimerState,
  onCreateNewTask: () -> Unit,
  onEditTask: (Long) -> Unit,
  onCompleteTask: (TaskEntity) -> Unit,
  onToggleSubtask: (TaskEntity, Long) -> Unit,
  onSkipTask: (TaskEntity) -> Unit,
  onGiveUpTask: (TaskEntity) -> Unit = {},
  onApplyBulkUpdate: (Set<Long>, TaskCategory?, Int, Long?, ReminderMode?) -> Unit,
  onToggleTaskTimer: (Long, Int) -> Unit,
  onResetTaskTimer: (Long, Int) -> Unit,
  onDismissFocusView: () -> Unit = {},
  onActivateTask: (Long) -> Unit = {},
  onOpenRandomQuest: () -> Unit = {},
  openLibraryTrigger: Int = 0,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val now = System.currentTimeMillis()

  val focusedTask = remember(inlineTimer.isFocusActive, inlineTimer.taskId, uiState.tasks) {
    if (inlineTimer.isFocusActive) {
      uiState.tasks.find { it.id == inlineTimer.taskId }
    } else {
      null
    }
  }

  if (focusedTask != null) {
    TaskFocusModeContent(
      task = focusedTask,
      uiState = uiState,
      inlineTimer = inlineTimer,
      onToggleTaskTimer = { onToggleTaskTimer(focusedTask.id, focusedTask.durationMinutes) },
      onResetTaskTimer = { onResetTaskTimer(focusedTask.id, focusedTask.durationMinutes) },
      onDismissFocusView = onDismissFocusView,
      onCompleteTask = { onCompleteTask(focusedTask) },
      onToggleSubtask = { subId -> onToggleSubtask(focusedTask, subId) },
      onSkipTask = { onSkipTask(focusedTask) },
      onGiveUpTask = { onGiveUpTask(focusedTask) },
      onEditTask = { onEditTask(focusedTask.id) },
      modifier = modifier,
    )
    return
  }

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  var selectedView by remember { mutableStateOf(TaskViewMode.TIME_LEFT) }
  var sortDirection by remember { mutableStateOf(defaultSortDirectionFor(TaskViewMode.TIME_LEFT)) }
  var calendarMonthStart by remember { mutableLongStateOf(startOfLocalMonthMillis()) }
  var selectedCalendarDay by remember { mutableLongStateOf(startOfLocalDayMillis()) }
  var horizonStart by remember { mutableLongStateOf(startOfLocalDayMillis()) }

  var showFilters by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var tagFilterQuery by remember { mutableStateOf("") }
  var selectedTagIdFilter by remember { mutableStateOf<Long?>(null) }
  var selectedCategoryFilter by remember { mutableStateOf<TaskCategory?>(null) }

  var isBulkMode by remember { mutableStateOf(false) }
  var selectedTaskIds by remember { mutableStateOf(setOf<Long>()) }
  var expandedTaskId by remember { mutableStateOf<Long?>(null) }

  // Safe tab clamping if folders change
  val tabCount = 1 + uiState.folders.size
  val safeTabIndex = selectedTabIndex.coerceIn(0, (tabCount - 1).coerceAtLeast(0))
  val selectedFolderId: Long? = if (safeTabIndex == 0) null else uiState.folders[safeTabIndex - 1].id

  val activeFilterCount = listOfNotNull(
    searchQuery.takeIf { it.isNotBlank() },
    selectedTagIdFilter,
    selectedCategoryFilter,
  ).size

  val matchedByFilters = remember(
    uiState.tasks,
    selectedFolderId,
    searchQuery,
    selectedTagIdFilter,
    selectedCategoryFilter,
  ) {
    uiState.tasks.filter { task ->
      val folderMatches = selectedFolderId == null || task.folderId == selectedFolderId
      val queryMatches = searchQuery.isBlank() ||
        task.name.contains(searchQuery, ignoreCase = true) ||
        task.description.contains(searchQuery, ignoreCase = true)
      val tagMatches = selectedTagIdFilter == null ||
        selectedTagIdFilter in parseIdCsv(task.tagIdsCsv)
      val catMatches = selectedCategoryFilter == null ||
        task.categoryCode == selectedCategoryFilter?.code
      folderMatches && queryMatches && tagMatches && catMatches
    }
  }

  val filteredTasks = remember(matchedByFilters) {
    matchedByFilters.filter { it.isActive }
  }
  val libraryTasks = remember(matchedByFilters) {
    matchedByFilters.filter { !it.isActive }
  }
  val allLibraryTasksCount = remember(uiState.tasks) {
    uiState.tasks.count { !it.isActive }
  }
  val emptyActiveMessage = if (libraryTasks.isNotEmpty() && activeFilterCount == 0 && selectedFolderId == null) {
    "Brak aktywnych zadań. Aktywuj wybrane zadania z biblioteki powyżej lub dodaj nowe przyciskiem + Nowe."
  } else {
    "Brak aktywnych zadań spełniających wybrane kryteria."
  }

  val activeTasks = remember(filteredTasks, now) {
    filteredTasks.filter { it.startTimestamp <= now }
  }
  val scheduledTasks = remember(filteredTasks, now) {
    filteredTasks.filter { it.startTimestamp > now }
  }
  var showLibrarySection by remember(openLibraryTrigger) { mutableStateOf(true) }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top Action Bar: Multi-Select, Search/Filter, yellow Random Quest, + Nowe
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 640.dp)
        .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isBulkMode) palette.accent else palette.surface,
        border = BorderStroke(2.dp, palette.outline),
        modifier = Modifier
          .size(44.dp)
          .clickable {
            isBulkMode = !isBulkMode
            selectedTaskIds = emptySet()
          }
          .testTag("bulk_edit_toggle"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isBulkMode) Icons.Default.Close else Icons.Default.Checklist,
            contentDescription = if (isBulkMode) "Anuluj zaznaczanie" else "Edycja zbiorcza",
            tint = if (isBulkMode) Color.White else palette.textPrimary,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (showFilters || activeFilterCount > 0) palette.accentSecondary.copy(alpha = 0.2f) else palette.surface,
        border = BorderStroke(2.dp, if (activeFilterCount > 0) palette.accent else palette.outline),
        modifier = Modifier
          .height(44.dp)
          .clickable { showFilters = !showFilters }
          .testTag("filter_toggle"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = if (activeFilterCount > 0) 8.dp else 11.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = "Szukaj i filtruj",
            tint = if (activeFilterCount > 0) palette.accent else palette.textPrimary,
            modifier = Modifier.size(20.dp),
          )
          if (activeFilterCount > 0) {
            Text(
              text = "$activeFilterCount",
              fontFamily = PixelFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = palette.accent,
            )
          }
        }
      }

      PixelButton(
        text = "Random Quest",
        compact = true,
        horizontalPadding = 8.dp,
        fontSize = 11.sp,
        containerColor = PixelColors.Gold,
        contentColor = palette.outline,
        modifier = Modifier
          .weight(1f)
          .testTag("tasks_random_quest_button"),
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Casino,
            contentDescription = null,
            tint = palette.outline,
            modifier = Modifier.size(16.dp),
          )
        },
        onClick = onOpenRandomQuest,
      )

      PixelButton(
        text = "Nowe",
        compact = true,
        horizontalPadding = 10.dp,
        modifier = Modifier
          .weight(1f)
          .testTag("new_task_button"),
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
          )
        },
        onClick = onCreateNewTask,
      )
    }

    // Browser-style Folder Tabs at the top (Main tab = Wszystkie)
    val selectedFolder = if (safeTabIndex == 0) null else uiState.folders[safeTabIndex - 1]
    val activeTabIndicatorColor = selectedFolder?.let { resolveFolderColor(it) } ?: palette.accent
    ScrollableTabRow(
      selectedTabIndex = safeTabIndex,
      containerColor = palette.surface,
      contentColor = activeTabIndicatorColor,
      edgePadding = 12.dp,
      indicator = { tabPositions ->
        if (safeTabIndex < tabPositions.size) {
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[safeTabIndex]),
            color = activeTabIndicatorColor,
            height = 3.dp,
          )
        }
      },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Tab(
        selected = safeTabIndex == 0,
        onClick = { selectedTabIndex = 0 },
        text = {
          Text(
            text = "Wszystkie",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
          )
        },
      )
      uiState.folders.forEachIndexed { idx, folder ->
        val tabIdx = idx + 1
        val folderColor = resolveFolderColor(folder)
        Tab(
          selected = safeTabIndex == tabIdx,
          onClick = { selectedTabIndex = tabIdx },
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(folderColor),
              )
              PixelIcon(icon = folder.icon, size = 16.dp)
              Text(
                text = folder.name,
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
            }
          },
        )
      }
    }

    // Collapsible Search & Tag Filter Drawer
    AnimatedVisibility(visible = showFilters) {
      PixelCard(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .padding(horizontal = 16.dp, vertical = 8.dp),
        contentPadding = PaddingValues(12.dp),
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = { Text("Szukaj zadań po nazwie lub opisie") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Wyczyść")
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "FILTRUJ PO TAGU (${uiState.tags.size})",
          style = MaterialTheme.typography.labelMedium,
          color = palette.textMuted,
        )
        OutlinedTextField(
          value = tagFilterQuery,
          onValueChange = { tagFilterQuery = it },
          placeholder = { Text("Wpisz fragment tagu...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        )
        val matchingTags = remember(uiState.tags, tagFilterQuery) {
          val q = tagFilterQuery.trim().removePrefix("#")
          if (q.isEmpty()) uiState.tags.take(10)
          else uiState.tags.filter { it.name.contains(q, ignoreCase = true) }.take(12)
        }
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FilterChip(
            selected = selectedTagIdFilter == null,
            onClick = { selectedTagIdFilter = null },
            label = { Text("Wszystkie tagi") },
          )
          matchingTags.forEach { tag ->
            FilterChip(
              selected = selectedTagIdFilter == tag.id,
              onClick = {
                selectedTagIdFilter = if (selectedTagIdFilter == tag.id) null else tag.id
              },
              label = { Text("#${tag.name}") },
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "TYP POWTARZANIA",
          style = MaterialTheme.typography.labelMedium,
          color = palette.textMuted,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FilterChip(
            selected = selectedCategoryFilter == null,
            onClick = { selectedCategoryFilter = null },
            label = { Text("Wszystkie") },
          )
          TaskCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategoryFilter == cat,
              onClick = {
                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
              },
              label = { Text(cat.label.substringBefore(" ")) },
            )
          }
        }
      }
    }

    // Bulk Edit Panel
    AnimatedVisibility(visible = isBulkMode) {
      BulkEditPanel(
        uiState = uiState,
        selectedCount = selectedTaskIds.size,
        onSelectAllActive = {
          selectedTaskIds = activeTasks.map { it.id }.toSet()
        },
        onApply = { cat, interval, folderId, reminder ->
          onApplyBulkUpdate(selectedTaskIds, cat, interval, folderId, reminder)
          isBulkMode = false
          selectedTaskIds = emptySet()
        },
      )
    }

    // View Selector ("Widok") + Dynamic View-Dependent Sort Toggle
    TaskViewAndSortBar(
      selectedView = selectedView,
      sortDirection = sortDirection,
      onSelectView = { newMode ->
        selectedView = newMode
        sortDirection = defaultSortDirectionFor(newMode)
      },
      onToggleSortDirection = {
        sortDirection = if (sortDirection == TaskSortDirection.ASCENDING) {
          TaskSortDirection.DESCENDING
        } else {
          TaskSortDirection.ASCENDING
        }
      },
    )

    val toggleExpandFn: (Long) -> Unit = { taskId ->
      expandedTaskId = if (expandedTaskId == taskId) null else taskId
    }
    val toggleSelectFn: (Long) -> Unit = { taskId ->
      selectedTaskIds = if (taskId in selectedTaskIds) {
        selectedTaskIds - taskId
      } else {
        selectedTaskIds + taskId
      }
    }
    val todayStart = remember { startOfLocalDayMillis() }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (libraryTasks.isNotEmpty()) {
        item(key = "personal_library_section") {
          PersonalLibraryCard(
            libraryTasks = libraryTasks,
            folders = uiState.folders,
            expanded = showLibrarySection,
            onToggleExpand = { showLibrarySection = !showLibrarySection },
            onActivateTask = onActivateTask,
            onEditTask = onEditTask,
          )
        }
      } else if (allLibraryTasksCount > 0) {
        item(key = "personal_library_hidden_notice") {
          PixelCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Masz nieaktywne zadania w bibliotece ($allLibraryTasksCount), ukryte przez aktywny filtr.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
                modifier = Modifier.weight(1f),
              )
              TextButton(
                onClick = {
                  selectedTabIndex = 0
                  searchQuery = ""
                  selectedTagIdFilter = null
                  selectedCategoryFilter = null
                  showLibrarySection = true
                },
              ) {
                Text("Pokaż", color = palette.accent, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      when (selectedView) {
        TaskViewMode.TIME_LEFT -> {
          val sortedActive = sortTasksByTimeLeft(activeTasks, sortDirection)
          if (sortedActive.isEmpty()) {
            item {
              PixelCard {
                Text(
                  text = emptyActiveMessage,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textMuted,
                  modifier = Modifier.padding(vertical = 8.dp),
                )
              }
            }
          } else {
            taskGroupSectionItems(
              header = "AKTYWNE · CZAS DO KOŃCA",
              tasks = sortedActive,
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }
          if (scheduledTasks.isNotEmpty()) {
            taskGroupSectionItems(
              header = "ZAPLANOWANE NA KOLEJNY CYKL",
              tasks = sortTasksByTimeLeft(scheduledTasks, sortDirection),
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }
        }

        TaskViewMode.CATEGORY -> {
          val grouped = groupTasksByCategory(activeTasks, sortDirection)
          if (grouped.isEmpty()) {
            item {
              PixelCard {
                Text(
                  text = emptyActiveMessage,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textMuted,
                  modifier = Modifier.padding(vertical = 8.dp),
                )
              }
            }
          } else {
            grouped.forEach { (cat, bucket) ->
              taskGroupSectionItems(
                header = cat.label.uppercase(),
                headerAccent = palette.accent,
                tasks = bucket,
                now = now,
                uiState = uiState,
                expandedTaskId = expandedTaskId,
                isBulkMode = isBulkMode,
                selectedTaskIds = selectedTaskIds,
                inlineTimer = inlineTimer,
                onToggleExpand = toggleExpandFn,
                onCompleteTask = onCompleteTask,
                onToggleSubtask = onToggleSubtask,
                onSkipTask = onSkipTask,
                onGiveUpTask = onGiveUpTask,
                onEditTask = onEditTask,
                onToggleSelect = toggleSelectFn,
                onToggleTaskTimer = onToggleTaskTimer,
                onResetTaskTimer = onResetTaskTimer,
              )
            }
          }
          if (scheduledTasks.isNotEmpty()) {
            taskGroupSectionItems(
              header = "ZAPLANOWANE NA KOLEJNY CYKL",
              tasks = sortTasksByTimeLeft(scheduledTasks, sortDirection),
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }
        }

        TaskViewMode.DIFFICULTY -> {
          val grouped = groupTasksByDifficulty(activeTasks, sortDirection)
          if (grouped.isEmpty()) {
            item {
              PixelCard {
                Text(
                  text = emptyActiveMessage,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textMuted,
                  modifier = Modifier.padding(vertical = 8.dp),
                )
              }
            }
          } else {
            grouped.forEach { (diff, bucket) ->
              taskGroupSectionItems(
                header = diff.label.uppercase(),
                headerAccent = difficultyColor(diff, palette.accent),
                tasks = bucket,
                now = now,
                uiState = uiState,
                expandedTaskId = expandedTaskId,
                isBulkMode = isBulkMode,
                selectedTaskIds = selectedTaskIds,
                inlineTimer = inlineTimer,
                onToggleExpand = toggleExpandFn,
                onCompleteTask = onCompleteTask,
                onToggleSubtask = onToggleSubtask,
                onSkipTask = onSkipTask,
                onGiveUpTask = onGiveUpTask,
                onEditTask = onEditTask,
                onToggleSelect = toggleSelectFn,
                onToggleTaskTimer = onToggleTaskTimer,
                onResetTaskTimer = onResetTaskTimer,
              )
            }
          }
          if (scheduledTasks.isNotEmpty()) {
            taskGroupSectionItems(
              header = "ZAPLANOWANE NA KOLEJNY CYKL",
              tasks = scheduledTasks,
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }
        }

        TaskViewMode.REWARD -> {
          item { RewardSortLegendBanner() }
          val sortedByReward = sortTasksByReward(activeTasks, sortDirection)
          if (sortedByReward.isEmpty()) {
            item {
              PixelCard {
                Text(
                  text = emptyActiveMessage,
                  style = MaterialTheme.typography.bodyMedium,
                  color = palette.textMuted,
                  modifier = Modifier.padding(vertical = 8.dp),
                )
              }
            }
          } else {
            taskGroupSectionItems(
              header = "ZADANIA WG NAGRODY",
              headerAccent = PixelColors.Warning,
              tasks = sortedByReward,
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              showRewardWorthBadge = true,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }
          if (scheduledTasks.isNotEmpty()) {
            taskGroupSectionItems(
              header = "ZAPLANOWANE NA KOLEJNY CYKL",
              tasks = sortTasksByReward(scheduledTasks, sortDirection),
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              showRewardWorthBadge = true,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }
        }

        TaskViewMode.CALENDAR -> {
          item {
            CalendarMonthCard(
              monthStartMillis = calendarMonthStart,
              selectedDayStartMillis = selectedCalendarDay,
              tasks = filteredTasks,
              onPrevMonth = { calendarMonthStart = addLocalMonthsMillis(calendarMonthStart, -1) },
              onNextMonth = { calendarMonthStart = addLocalMonthsMillis(calendarMonthStart, 1) },
              onSelectToday = {
                calendarMonthStart = startOfLocalMonthMillis()
                selectedCalendarDay = startOfLocalDayMillis()
              },
              onSelectDay = { dayStart ->
                selectedCalendarDay = dayStart
                calendarMonthStart = startOfLocalMonthMillis(dayStart)
              },
            )
          }

          val dayTasks = tasksDueOnDay(filteredTasks, selectedCalendarDay, sortDirection)
          val selectedDateLabel = SimpleDateFormat("EEEE, dd.MM.yyyy", Locale("pl", "PL"))
            .format(Date(selectedCalendarDay))
            .uppercase()
          if (dayTasks.isEmpty()) {
            item { EmptyDayCard(selectedDateLabel) }
          } else {
            taskGroupSectionItems(
              header = selectedDateLabel,
              headerAccent = palette.accent,
              tasks = dayTasks,
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }

          val overdue = sortTasksByTimeLeft(
            filteredTasks.filter { it.dueTimestamp != null && it.dueTimestamp < todayStart },
            sortDirection,
          )
          if (overdue.isNotEmpty()) {
            taskGroupSectionItems(
              header = "ZALEGŁE",
              headerAccent = PixelColors.Red,
              tasks = overdue,
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }

          val noDue = filteredTasks.filter { it.dueTimestamp == null }
          if (noDue.isNotEmpty()) {
            taskGroupSectionItems(
              header = "BEZ TERMINU",
              tasks = noDue,
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }
        }

        TaskViewMode.THREE_DAYS,
        TaskViewMode.WEEK,
        TaskViewMode.TWO_WEEKS -> {
          val dayCount = when (selectedView) {
            TaskViewMode.THREE_DAYS -> 3
            TaskViewMode.WEEK -> 7
            else -> 14
          }
          item {
            HorizonNavigatorCard(
              windowStartMillis = horizonStart,
              dayCount = dayCount,
              onPrevWindow = { horizonStart = addLocalDaysMillis(horizonStart, -dayCount) },
              onNextWindow = { horizonStart = addLocalDaysMillis(horizonStart, dayCount) },
              onResetToday = { horizonStart = startOfLocalDayMillis() },
            )
          }

          val overdue = sortTasksByTimeLeft(
            filteredTasks.filter { it.dueTimestamp != null && it.dueTimestamp < horizonStart },
            sortDirection,
          )
          if (overdue.isNotEmpty()) {
            taskGroupSectionItems(
              header = "ZALEGŁE PRZED OKRESEM",
              headerAccent = PixelColors.Red,
              tasks = overdue,
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }

          val dayBuckets = buildHorizonDayBuckets(
            tasks = filteredTasks,
            windowStartMillis = horizonStart,
            dayCount = dayCount,
            sortDirection = sortDirection,
          )
          dayBuckets.forEachIndexed { idx, (dayMillis, dayTasks) ->
            if (dayCount == 14 && (idx == 0 || idx == 7)) {
              item {
                val weekTitle = if (sortDirection == TaskSortDirection.ASCENDING) {
                  if (idx == 0) "TYDZIEŃ 1" else "TYDZIEŃ 2"
                } else {
                  if (idx == 0) "TYDZIEŃ 2" else "TYDZIEŃ 1"
                }
                Text(
                  text = weekTitle,
                  fontFamily = PixelFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = palette.accent,
                  modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
              }
            }
            val dayLabel = formatScheduleDayTitle(dayMillis, todayStart)
            if (dayTasks.isEmpty()) {
              item { EmptyDayCard(dayLabel) }
            } else {
              taskGroupSectionItems(
                header = dayLabel.uppercase(),
                headerAccent = if (dayMillis == todayStart) palette.accent else null,
                tasks = dayTasks,
                now = now,
                uiState = uiState,
                expandedTaskId = expandedTaskId,
                isBulkMode = isBulkMode,
                selectedTaskIds = selectedTaskIds,
                inlineTimer = inlineTimer,
                onToggleExpand = toggleExpandFn,
                onCompleteTask = onCompleteTask,
                onToggleSubtask = onToggleSubtask,
                onSkipTask = onSkipTask,
                onGiveUpTask = onGiveUpTask,
                onEditTask = onEditTask,
                onToggleSelect = toggleSelectFn,
                onToggleTaskTimer = onToggleTaskTimer,
                onResetTaskTimer = onResetTaskTimer,
              )
            }
          }

          val noDue = filteredTasks.filter { it.dueTimestamp == null }
          if (noDue.isNotEmpty()) {
            taskGroupSectionItems(
              header = "BEZ TERMINU",
              tasks = noDue,
              now = now,
              uiState = uiState,
              expandedTaskId = expandedTaskId,
              isBulkMode = isBulkMode,
              selectedTaskIds = selectedTaskIds,
              inlineTimer = inlineTimer,
              onToggleExpand = toggleExpandFn,
              onCompleteTask = onCompleteTask,
              onToggleSubtask = onToggleSubtask,
              onSkipTask = onSkipTask,
              onGiveUpTask = onGiveUpTask,
              onEditTask = onEditTask,
              onToggleSelect = toggleSelectFn,
              onToggleTaskTimer = onToggleTaskTimer,
              onResetTaskTimer = onResetTaskTimer,
            )
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkEditPanel(
  uiState: GamefyUiState,
  selectedCount: Int,
  onSelectAllActive: () -> Unit,
  onApply: (TaskCategory?, Int, Long?, ReminderMode?) -> Unit,
) {
  var chosenCategory by remember { mutableStateOf<TaskCategory?>(null) }
  var intervalText by remember { mutableStateOf("1") }
  var chosenFolderId by remember { mutableStateOf<Long?>(null) }
  var chosenReminder by remember { mutableStateOf<ReminderMode?>(null) }

  var catExpanded by remember { mutableStateOf(false) }
  var folderExpanded by remember { mutableStateOf(false) }
  var remExpanded by remember { mutableStateOf(false) }

  PixelCard(
    modifier = Modifier
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp, vertical = 6.dp),
    contentPadding = PaddingValues(12.dp),
  ) {
    Text(
      text = "EDYCJA ZBIORCZA (ZAZNACZONO: $selectedCount)",
      style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ExposedDropdownMenuBox(
        expanded = catExpanded,
        onExpandedChange = { catExpanded = !catExpanded },
        modifier = Modifier.weight(1f),
      ) {
        OutlinedTextField(
          value = chosenCategory?.label ?: "Kategoria: bez zmian",
          onValueChange = {},
          readOnly = true,
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
          modifier = Modifier
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
          DropdownMenuItem(
            text = { Text("Bez zmian") },
            onClick = { chosenCategory = null; catExpanded = false },
          )
          TaskCategory.entries.forEach { cat ->
            DropdownMenuItem(
              text = { Text(cat.label) },
              onClick = { chosenCategory = cat; catExpanded = false },
            )
          }
        }
      }

      OutlinedTextField(
        value = intervalText,
        onValueChange = { intervalText = it.filter(Char::isDigit) },
        label = { Text("Co ile") },
        singleLine = true,
        modifier = Modifier.width(86.dp),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ExposedDropdownMenuBox(
        expanded = folderExpanded,
        onExpandedChange = { folderExpanded = !folderExpanded },
        modifier = Modifier.weight(1f),
      ) {
        val folderName = when (chosenFolderId) {
          null -> "Folder: bez zmian"
          0L -> "Folder: Brak"
          else -> uiState.folders.find { it.id == chosenFolderId }?.name ?: "Folder"
        }
        OutlinedTextField(
          value = folderName,
          onValueChange = {},
          readOnly = true,
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderExpanded) },
          modifier = Modifier
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = folderExpanded, onDismissRequest = { folderExpanded = false }) {
          DropdownMenuItem(
            text = { Text("Bez zmian") },
            onClick = { chosenFolderId = null; folderExpanded = false },
          )
          DropdownMenuItem(
            text = { Text("Brak folderu") },
            onClick = { chosenFolderId = 0L; folderExpanded = false },
          )
          uiState.folders.forEach { f ->
            val folderColor = resolveFolderColor(f)
            DropdownMenuItem(
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(folderColor),
                  )
                  Text(f.name)
                }
              },
              onClick = { chosenFolderId = f.id; folderExpanded = false },
            )
          }
        }
      }

      ExposedDropdownMenuBox(
        expanded = remExpanded,
        onExpandedChange = { remExpanded = !remExpanded },
        modifier = Modifier.weight(1f),
      ) {
        OutlinedTextField(
          value = chosenReminder?.label ?: "Przypominajka: bez zmian",
          onValueChange = {},
          readOnly = true,
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = remExpanded) },
          modifier = Modifier
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = remExpanded, onDismissRequest = { remExpanded = false }) {
          DropdownMenuItem(
            text = { Text("Bez zmian") },
            onClick = { chosenReminder = null; remExpanded = false },
          )
          ReminderMode.entries.forEach { r ->
            DropdownMenuItem(
              text = { Text(r.label) },
              onClick = { chosenReminder = r; remExpanded = false },
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      PixelButton(
        text = "Zaznacz aktywne",
        compact = true,
        containerColor = LocalGamefyPalette.current.surfaceSubtle,
        contentColor = LocalGamefyPalette.current.textPrimary,
        onClick = onSelectAllActive,
      )
      PixelButton(
        text = "Zastosuj",
        compact = true,
        enabled = selectedCount > 0,
        modifier = Modifier.weight(1f),
        onClick = {
          onApply(
            chosenCategory,
            intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            chosenFolderId,
            chosenReminder,
          )
        },
      )
    }
  }
}

@Composable
private fun TaskFocusModeContent(
  task: TaskEntity,
  uiState: GamefyUiState,
  inlineTimer: TaskInlineTimerState,
  onToggleTaskTimer: () -> Unit,
  onResetTaskTimer: () -> Unit,
  onDismissFocusView: () -> Unit,
  onCompleteTask: () -> Unit,
  onToggleSubtask: (Long) -> Unit,
  onSkipTask: () -> Unit,
  onGiveUpTask: () -> Unit = {},
  onEditTask: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  var showDetails by remember(task.id) { mutableStateOf(true) }

  BackHandler(enabled = true) {
    onDismissFocusView()
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "TRYB SKUPIENIA NAD ZADANIEM",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textMuted,
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.surface,
            border = BorderStroke(1.5.dp, palette.divider),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onDismissFocusView)
              .testTag("dismiss_focus_view_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Wróć do listy zadań",
                tint = palette.textPrimary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "Lista zadań",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
              )
            }
          }
        }
      }

      // Big Focus Timer Card
      item {
        val shape = RoundedCornerShape(22.dp)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(palette.accent, palette.accentSecondary)))
            .border(2.5.dp, palette.outline, shape)
            .padding(18.dp)
            .testTag("task_focus_timer_card"),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color.White.copy(alpha = 0.22f),
            ) {
              Text(
                text = task.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              )
            }

            Box(
              modifier = Modifier.size(206.dp),
              contentAlignment = Alignment.Center,
            ) {
              val progress = inlineTimer.progressFraction
              Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                drawArc(
                  color = Color.White.copy(alpha = 0.24f),
                  startAngle = -90f,
                  sweepAngle = 360f,
                  useCenter = false,
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                  color = Color.White,
                  startAngle = -90f,
                  sweepAngle = (1f - progress) * 360f,
                  useCenter = false,
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
              }

              val hrs = inlineTimer.remainingSeconds / 3600
              val mins = (inlineTimer.remainingSeconds % 3600) / 60
              val secs = inlineTimer.remainingSeconds % 60
              val timeText = if (hrs > 0) {
                String.format("%02d:%02d:%02d", hrs, mins, secs)
              } else {
                String.format("%02d:%02d", mins, secs)
              }
              val statusLabel = when {
                inlineTimer.remainingSeconds <= 0 -> "CZAS MINĄŁ!"
                inlineTimer.isRunning -> "TRWA ODLICZANIE"
                else -> "PAUZA"
              }

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = timeText,
                  style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (timeText.length > 5) 34.sp else 46.sp,
                    fontFeatureSettings = "tnum",
                  ),
                  color = Color.White,
                )
                Text(
                  text = statusLabel,
                  fontFamily = BodyFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp,
                  letterSpacing = 1.sp,
                  color = Color.White.copy(alpha = 0.9f),
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PixelButton(
                text = if (inlineTimer.isRunning) "Pauza" else "Wznów",
                containerColor = Color.White,
                contentColor = palette.accent,
                modifier = Modifier
                  .weight(1f)
                  .testTag("task_focus_play_pause"),
                leadingIcon = {
                  Icon(
                    imageVector = if (inlineTimer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp),
                  )
                },
                onClick = onToggleTaskTimer,
              )

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.24f),
                border = BorderStroke(2.5.dp, palette.outline),
                modifier = Modifier
                  .size(48.dp)
                  .clickable(onClick = onResetTaskTimer)
                  .testTag("task_focus_stop_reset"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Zatrzymaj i zresetuj timer zadania",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                  )
                }
              }
            }
          }
        }
      }

      // Focused Task & Subtasks directly beneath the big timer
      item {
        Text(
          text = "ZADANIE I PODZADANIA",
          style = MaterialTheme.typography.titleSmall,
          color = palette.textMuted,
          modifier = Modifier.padding(start = 4.dp),
        )
      }

      item {
        TaskCardItem(
          task = task,
          folders = uiState.folders,
          attributes = uiState.attributes,
          tags = uiState.tags,
          isActive = true,
          isExpanded = showDetails,
          isBulkMode = false,
          isSelected = false,
          inlineTimer = inlineTimer,
          onToggleExpand = { showDetails = !showDetails },
          onComplete = onCompleteTask,
          onToggleSubtask = onToggleSubtask,
          onSkip = onSkipTask,
          onGiveUp = onGiveUpTask,
          onEdit = onEditTask,
          onToggleSelect = {},
          onToggleTaskTimer = onToggleTaskTimer,
          onResetTaskTimer = onResetTaskTimer,
          showInlineTimer = false,
          detailPref = OnboardingDetailPref.fromId(uiState.profile.onboardingDetailPref),
        )
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun PersonalLibraryCard(
  libraryTasks: List<TaskEntity>,
  folders: List<FolderEntity>,
  expanded: Boolean,
  onToggleExpand: () -> Unit,
  onActivateTask: (Long) -> Unit,
  onEditTask: (Long) -> Unit,
) {
  val palette = LocalGamefyPalette.current
  PixelCard(
    contentPadding = PaddingValues(14.dp),
    modifier = Modifier.testTag("personal_library_card"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onToggleExpand),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "BIBLIOTEKA ZADAŃ · NIEAKTYWNE (${libraryTasks.size})",
          fontFamily = PixelFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = palette.accent,
        )
        Text(
          text = "Aktywuj wybrane zadania lub edytuj ich częstotliwość, gdy będziesz gotowy.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )
      }
      Text(
        text = if (expanded) "Zwiń ▴" else "Rozwiń ▾",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = palette.accent,
      )
    }

    if (expanded) {
      Spacer(modifier = Modifier.height(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        libraryTasks.forEach { task ->
          val subtasksCount = remember(task.subtasksJson) { parseSubtasksJson(task.subtasksJson).size }
          val category = TaskCategory.fromCode(task.categoryCode)
          val folder = folders.find { it.id == task.folderId }
          val folderColor = folder?.let { resolveFolderColor(it) }
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.background,
            border = BorderStroke(
              width = if (folderColor != null) 2.dp else 1.5.dp,
              color = folderColor ?: palette.divider,
            ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                if (folder != null && folderColor != null) {
                  Box(
                    modifier = Modifier
                      .size(9.dp)
                      .clip(CircleShape)
                      .background(folderColor),
                  )
                }
                Text(
                  text = task.name,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Bold,
                  color = palette.textPrimary,
                )
              }
              val metaParts = buildList {
                if (folder != null) add("${folder.icon} ${folder.name}")
                if (task.durationMinutes > 0) add("⏱ ${task.durationMinutes} min")
                add(formatFrequencyLabel(category, task.repeatInterval))
                add("+${task.expReward} EXP · +${task.coinReward} 🪙")
                if (subtasksCount > 0) {
                  val subLabel = when {
                    subtasksCount == 1 -> "1 podzadanie"
                    subtasksCount % 10 in 2..4 && subtasksCount % 100 !in 12..14 -> "$subtasksCount podzadania"
                    else -> "$subtasksCount podzadań"
                  }
                  add("📋 $subLabel")
                }
              }
              Text(
                text = metaParts.joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                PixelButton(
                  text = "Aktywuj",
                  compact = true,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("activate_library_task_${task.id}"),
                  onClick = { onActivateTask(task.id) },
                )
                PixelButton(
                  text = "Edytuj",
                  compact = true,
                  containerColor = palette.surfaceSubtle,
                  contentColor = palette.textPrimary,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("edit_library_task_${task.id}"),
                  onClick = { onEditTask(task.id) },
                )
              }
            }
          }
        }
      }
    }
  }
}
