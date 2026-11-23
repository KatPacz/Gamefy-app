package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.SubtaskItem
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.parseIdCsv
import com.example.data.local.parseSubtasksJson
import com.example.data.local.toIdCsv
import com.example.data.local.toSubtasksJson
import com.example.data.model.PenaltyMode
import com.example.data.model.ReminderMode
import com.example.data.model.TaskCategory
import com.example.data.model.TaskDifficulty
import com.example.data.model.TaskEffortBucket
import com.example.data.model.calculateAutoPenaltyRange
import com.example.data.model.calculateAutoRewardAndPenalty
import com.example.data.model.nextCycleTimestamp
import com.example.ui.GamefyUiState
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.PixelSectionHeader
import com.example.ui.components.TutorialTargetIds
import com.example.ui.components.resolveFolderColor
import com.example.ui.components.tutorialTarget
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
  existingTask: TaskEntity?,
  uiState: GamefyUiState,
  onCreateOrFindTag: suspend (String) -> TagEntity,
  onSaveTask: (TaskEntity) -> Unit,
  onDeleteTask: (Long) -> Unit,
  onOpenNotificationSettings: () -> Unit = {},
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val palette = LocalGamefyPalette.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  var name by remember(existingTask) { mutableStateOf(existingTask?.name ?: "") }
  var description by remember(existingTask) { mutableStateOf(existingTask?.description ?: "") }
  var link by remember(existingTask) { mutableStateOf(existingTask?.link ?: "") }
  var folderId by remember(existingTask) { mutableStateOf(existingTask?.folderId ?: 0L) }
  var selectedTagIds by remember(existingTask) {
    mutableStateOf(existingTask?.let { parseIdCsv(it.tagIdsCsv) } ?: emptyList())
  }
  var tagQuery by remember { mutableStateOf("") }
  var subtasks by remember(existingTask) {
    mutableStateOf(existingTask?.let { parseSubtasksJson(it.subtasksJson) } ?: emptyList())
  }
  var newSubtaskTitle by remember { mutableStateOf("") }

  var category by remember(existingTask) {
    mutableStateOf(TaskCategory.fromCode(existingTask?.categoryCode ?: "w"))
  }
  var intervalText by remember(existingTask) {
    mutableStateOf((existingTask?.repeatInterval ?: 1).toString())
  }
  var repeatLimitText by remember(existingTask) {
    mutableStateOf((existingTask?.repeatLimit ?: 0).toString())
  }
  var dueTimestamp by remember(existingTask) { mutableStateOf(existingTask?.dueTimestamp) }
  var showDatePicker by remember { mutableStateOf(false) }

  var durationText by remember(existingTask) {
    mutableStateOf((existingTask?.durationMinutes ?: 0).toString())
  }
  var expText by remember(existingTask) {
    mutableStateOf((existingTask?.expReward ?: 20).toString())
  }
  var coinsText by remember(existingTask) {
    mutableStateOf((existingTask?.coinReward ?: 10).toString())
  }
  var gemsText by remember(existingTask) {
    mutableStateOf((existingTask?.gemReward ?: 0).toString())
  }
  var selectedAttrIds by remember(existingTask) {
    mutableStateOf(existingTask?.let { parseIdCsv(it.attributeIdsCsv) } ?: emptyList())
  }
  var rewardItemId by remember(existingTask) { mutableStateOf(existingTask?.rewardItemId ?: 0L) }
  var reminderMode by remember(existingTask) {
    mutableStateOf(ReminderMode.fromCode(existingTask?.reminderModeCode ?: "d1"))
  }
  var preferredReminderHour by remember(existingTask) {
    mutableIntStateOf(existingTask?.preferredReminderHour ?: -1)
  }
  var preferredReminderMinute by remember(existingTask) {
    mutableIntStateOf(existingTask?.preferredReminderMinute ?: 0)
  }
  var penaltyMode by remember(existingTask) {
    mutableStateOf(PenaltyMode.fromCode(existingTask?.penaltyModeCode ?: "0"))
  }
  var penaltyMinText by remember(existingTask) {
    mutableStateOf((existingTask?.penaltyMinCoins ?: 5).toString())
  }
  var penaltyMaxText by remember(existingTask) {
    mutableStateOf((existingTask?.penaltyMaxCoins ?: 10).toString())
  }

  var folderMenuExpanded by remember { mutableStateOf(false) }
  var categoryMenuExpanded by remember { mutableStateOf(false) }
  var topOverflowExpanded by remember { mutableStateOf(false) }
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }
  var nameError by remember { mutableStateOf(false) }
  var nameHadFocus by remember { mutableStateOf(false) }

  // Collapsible optional sections — all collapsed by default so mandatory fields stay compact
  var expandedDetails by remember { mutableStateOf(false) }
  var expandedSubtasks by remember { mutableStateOf(false) }
  var expandedRewards by remember { mutableStateOf(false) }
  var expandedRemindersPenalties by remember { mutableStateOf(false) }
  var expandedOrganization by remember { mutableStateOf(false) }

  var autoRewardEnabled by remember(existingTask) { mutableStateOf(false) }
  var surveyDifficulty by remember(existingTask) {
    mutableStateOf(TaskDifficulty.fromCode(existingTask?.difficultyCode ?: "m"))
  }
  var surveyEffort by remember(existingTask) {
    mutableStateOf(TaskEffortBucket.fromMinutes(existingTask?.durationMinutes ?: 0))
  }
  var surveyAttrCount by remember(existingTask) {
    mutableIntStateOf(existingTask?.let { parseIdCsv(it.attributeIdsCsv).size } ?: 1)
  }
  var autoPenaltyEnabled by remember(existingTask) { mutableStateOf(existingTask == null) }

  LaunchedEffect(
    autoRewardEnabled,
    autoPenaltyEnabled,
    surveyDifficulty,
    surveyEffort,
    surveyAttrCount,
    penaltyMode,
    coinsText,
  ) {
    val hasPenalty = penaltyMode != PenaltyMode.NONE
    if (autoRewardEnabled) {
      val calculated = calculateAutoRewardAndPenalty(
        difficulty = surveyDifficulty,
        effort = surveyEffort,
        attributeCount = surveyAttrCount,
        hasPenalty = hasPenalty,
        penaltyMode = penaltyMode,
      )
      expText = calculated.exp.toString()
      coinsText = calculated.coins.toString()
      gemsText = calculated.gems.toString()
      if (autoPenaltyEnabled && hasPenalty) {
        penaltyMinText = calculated.penaltyMinCoins.toString()
        penaltyMaxText = calculated.penaltyMaxCoins.toString()
      }
    } else if (autoPenaltyEnabled && hasPenalty) {
      val (pMin, pMax) = calculateAutoPenaltyRange(
        baseCoins = coinsText.toIntOrNull() ?: 10,
        difficulty = surveyDifficulty,
        penaltyMode = penaltyMode,
      )
      penaltyMinText = pMin.toString()
      penaltyMaxText = pMax.toString()
    }
  }

  val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale("pl", "PL")) }

  fun handleSave() {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) {
      nameError = true
      scope.launch { listState.animateScrollToItem(0) }
      return
    }
    val now = System.currentTimeMillis()
    val interval = (intervalText.toIntOrNull() ?: 1).coerceAtLeast(1)
    val limit = (repeatLimitText.toIntOrNull() ?: 0).coerceAtLeast(0)
    val minP = (penaltyMinText.toIntOrNull() ?: 0).coerceAtLeast(0)
    val maxP = (penaltyMaxText.toIntOrNull() ?: minP).coerceAtLeast(minP)
    val isInactiveLibraryTask = existingTask?.isActive == false
    val resolvedDue = dueTimestamp ?: if (category == TaskCategory.SIDEQUEST || isInactiveLibraryTask) {
      null
    } else {
      existingTask?.dueTimestamp ?: nextCycleTimestamp(now, category, interval)
    }
    val newAttrCsv = toIdCsv(selectedAttrIds)
    val resolvedWeightsJson = if (existingTask != null && existingTask.attributeIdsCsv == newAttrCsv) {
      existingTask.attributeWeightsJson
    } else {
      ""
    }

    val taskToSave = (existingTask ?: TaskEntity(name = cleanName)).copy(
      name = cleanName,
      description = description.trim(),
      link = link.trim(),
      folderId = folderId,
      tagIdsCsv = toIdCsv(selectedTagIds),
      categoryCode = category.code,
      repeatInterval = interval,
      repeatLimit = limit,
      expReward = (expText.toIntOrNull() ?: 0).coerceAtLeast(0),
      coinReward = (coinsText.toIntOrNull() ?: 0).coerceAtLeast(0),
      gemReward = (gemsText.toIntOrNull() ?: 0).coerceAtLeast(0),
      difficultyCode = surveyDifficulty.code,
      attributeIdsCsv = newAttrCsv,
      attributeWeightsJson = resolvedWeightsJson,
      rewardItemId = rewardItemId,
      durationMinutes = (durationText.toIntOrNull() ?: 0).coerceAtLeast(0),
      reminderModeCode = reminderMode.code,
      preferredReminderHour = preferredReminderHour,
      preferredReminderMinute = preferredReminderMinute,
      penaltyModeCode = penaltyMode.code,
      penaltyMinCoins = minP,
      penaltyMaxCoins = maxP,
      startTimestamp = existingTask?.startTimestamp ?: now,
      dueTimestamp = resolvedDue,
      subtasksJson = toSubtasksJson(subtasks.filter { it.title.isNotBlank() }),
    )
    onSaveTask(taskToSave)
    onBack()
  }

  // Live 1-line summaries for collapsible cards
  val detailsSummary = remember(description, link, durationText) {
    val parts = mutableListOf<String>()
    val mins = durationText.toIntOrNull() ?: 0
    if (mins > 0) parts.add("Czas: $mins min")
    if (description.isNotBlank()) parts.add("Opis dodany")
    if (link.isNotBlank()) parts.add("Link")
    if (parts.isEmpty()) "Opis: brak · Czas: brak" else parts.joinToString(" · ")
  }

  val subtasksSummary = remember(subtasks) {
    when (subtasks.size) {
      0 -> "Podzadania: brak"
      1 -> "Podzadania: 1 (${subtasks.first().title.ifBlank { "bez nazwy" }})"
      else -> "Podzadania: ${subtasks.size}"
    }
  }

  val chosenFolder = uiState.folders.find { it.id == folderId }
  val organizationSummary = remember(chosenFolder, selectedTagIds.size) {
    val folderText = chosenFolder?.name ?: "Brak folderu"
    val tagsText = when (selectedTagIds.size) {
      0 -> "brak tagów"
      1 -> "1 tag"
      in 2..4 -> "${selectedTagIds.size} tagi"
      else -> "${selectedTagIds.size} tagów"
    }
    "$folderText · $tagsText"
  }

  Scaffold(
    containerColor = palette.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (existingTask == null) "Nowe zadanie" else "Edytuj zadanie",
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Wróć",
            )
          }
        },
        actions = {
          if (existingTask != null) {
            Box {
              IconButton(
                onClick = { topOverflowExpanded = true },
                modifier = Modifier.testTag("task_editor_more_menu"),
              ) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "Więcej opcji",
                  tint = palette.textPrimary,
                )
              }
              DropdownMenu(
                expanded = topOverflowExpanded,
                onDismissRequest = { topOverflowExpanded = false },
              ) {
                DropdownMenuItem(
                  text = {
                    Text(
                      text = "Usuń",
                      color = PixelColors.Red,
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = null,
                      tint = PixelColors.Red,
                    )
                  },
                  onClick = {
                    topOverflowExpanded = false
                    showDeleteConfirmDialog = true
                  },
                )
              }
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = palette.surface,
          titleContentColor = palette.textPrimary,
          navigationIconContentColor = palette.textPrimary,
        ),
      )
    },
    bottomBar = {
      Surface(
        color = palette.surface,
        shadowElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("task_editor_sticky_bottom_bar"),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          HorizontalDivider(color = palette.divider)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 640.dp)
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = onBack,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.5.dp, palette.divider),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = palette.textPrimary,
              ),
              modifier = Modifier
                .weight(0.85f)
                .heightIn(min = 48.dp)
                .testTag("cancel_task_button"),
            ) {
              Text(
                text = "Anuluj",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
              )
            }
            PixelButton(
              text = "Zapisz",
              modifier = Modifier
                .weight(1.35f)
                .tutorialTarget(TutorialTargetIds.TASK_SAVE)
                .testTag("save_task_button"),
              onClick = ::handleSave,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Mandatory core card: ONLY Name + Cycle & Deadline
        item {
          PixelCard {
            OutlinedTextField(
              value = name,
              onValueChange = {
                name = it
                nameError = it.trim().isEmpty()
              },
              label = { Text("Nazwa zadania *") },
              placeholder = { Text("np. Podlać kwiaty") },
              isError = nameError,
              trailingIcon = if (nameError) {
                {
                  Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Pole wymagane",
                    tint = PixelColors.Red,
                  )
                }
              } else {
                null
              },
              supportingText = {
                if (nameError) {
                  Text(
                    text = "Wpisz nazwę zadania — pole jest wymagane",
                    color = PixelColors.Red,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("task_name_error_text"),
                  )
                } else if (name.isBlank()) {
                  Text(
                    text = "Pole wymagane",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.divider,
                errorBorderColor = PixelColors.Red,
                errorLabelColor = PixelColors.Red,
                errorSupportingTextColor = PixelColors.Red,
                errorCursorColor = PixelColors.Red,
                errorTrailingIconColor = PixelColors.Red,
              ),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                  if (focusState.isFocused) {
                    nameHadFocus = true
                  } else if (nameHadFocus && name.trim().isEmpty()) {
                    nameError = true
                  }
                }
                .testTag("task_name_input"),
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = palette.divider)
            Spacer(modifier = Modifier.height(10.dp))

            PixelSectionHeader(
              title = "Powtarzanie i termin",
              titleColor = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                modifier = Modifier.weight(1.3f),
              ) {
                OutlinedTextField(
                  value = category.label,
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("Cykl") },
                  trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                  },
                  modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                  expanded = categoryMenuExpanded,
                  onDismissRequest = { categoryMenuExpanded = false },
                ) {
                  TaskCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                      text = { Text(cat.label) },
                      onClick = { category = cat; categoryMenuExpanded = false },
                    )
                  }
                }
              }

              OutlinedTextField(
                value = intervalText,
                onValueChange = { intervalText = it.filter(Char::isDigit) },
                label = { Text("Co ile") },
                enabled = category != TaskCategory.SIDEQUEST,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.7f),
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = repeatLimitText,
                onValueChange = { repeatLimitText = it.filter(Char::isDigit) },
                label = { Text("Limit powtórzeń") },
                enabled = category != TaskCategory.SIDEQUEST,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
              )

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceSubtle,
                border = BorderStroke(1.5.dp, palette.divider),
                modifier = Modifier
                  .weight(1.1f)
                  .heightIn(min = 56.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { showDatePicker = true },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Termin",
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.textMuted,
                    )
                    Text(
                      text = dueTimestamp?.let { dateFormatter.format(Date(it)) } ?: "Brak / auto",
                      style = MaterialTheme.typography.bodyMedium,
                      color = palette.textPrimary,
                    )
                  }
                  if (dueTimestamp != null) {
                    IconButton(
                      onClick = { dueTimestamp = null },
                      modifier = Modifier.size(32.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Usuń termin",
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  } else {
                    Icon(
                      imageVector = Icons.Default.CalendarMonth,
                      contentDescription = "Wybierz datę",
                      tint = palette.accent,
                    )
                  }
                }
              }
            }
          }
        }

        // Collapsible: Rewards & 4-row chip survey
        item {
          TaskRewardsSectionCard(
            expanded = expandedRewards,
            onToggleExpand = { expandedRewards = !expandedRewards },
            autoRewardEnabled = autoRewardEnabled,
            onToggleAutoReward = {
              val next = !autoRewardEnabled
              if (next) {
                val typedMinutes = durationText.toIntOrNull() ?: 0
                if (typedMinutes > 0) {
                  surveyEffort = TaskEffortBucket.fromMinutes(typedMinutes)
                }
                if (selectedAttrIds.isNotEmpty()) {
                  surveyAttrCount = selectedAttrIds.size
                }
              }
              autoRewardEnabled = next
            },
            difficulty = surveyDifficulty,
            onSelectDifficulty = { surveyDifficulty = it },
            effort = surveyEffort,
            onSelectEffort = { surveyEffort = it },
            surveyAttrCount = surveyAttrCount,
            onSelectSurveyAttrCount = { surveyAttrCount = it },
            isPenalized = penaltyMode != PenaltyMode.NONE,
            onSelectIsPenalized = { penalized ->
              if (penalized) {
                if (penaltyMode == PenaltyMode.NONE) {
                  penaltyMode = PenaltyMode.ON_DUE_ONCE
                }
                autoPenaltyEnabled = true
              } else {
                penaltyMode = PenaltyMode.NONE
              }
            },
            expText = expText,
            onExpTextChange = { expText = it },
            coinsText = coinsText,
            onCoinsTextChange = { coinsText = it },
            gemsText = gemsText,
            onGemsTextChange = { gemsText = it },
            attributes = uiState.attributes,
            selectedAttrIds = selectedAttrIds,
            onToggleAttribute = { attrId ->
              val updated = if (attrId in selectedAttrIds) {
                selectedAttrIds - attrId
              } else {
                selectedAttrIds + attrId
              }
              selectedAttrIds = updated
              surveyAttrCount = updated.size
            },
            shopItems = uiState.shopItems,
            rewardItemId = rewardItemId,
            onSelectRewardItem = { rewardItemId = it },
          )
        }

        // Collapsible: Reminders & Penalties
        item {
          TaskReminderAndPenaltyCard(
            expanded = expandedRemindersPenalties,
            onToggleExpand = { expandedRemindersPenalties = !expandedRemindersPenalties },
            reminderMode = reminderMode,
            onSelectReminderMode = { reminderMode = it },
            preferredReminderHour = preferredReminderHour,
            preferredReminderMinute = preferredReminderMinute,
            onSelectPreferredTime = { h, m ->
              preferredReminderHour = h
              preferredReminderMinute = m
            },
            globalForcedModeActive = !uiState.profile.notifUseTaskSettings,
            onOpenNotificationSettings = onOpenNotificationSettings,
            penaltyMode = penaltyMode,
            onSelectPenaltyMode = { penaltyMode = it },
            autoPenaltyEnabled = autoPenaltyEnabled,
            onToggleAutoPenalty = { autoPenaltyEnabled = it },
            penaltyMinText = penaltyMinText,
            onPenaltyMinChange = { penaltyMinText = it },
            penaltyMaxText = penaltyMaxText,
            onPenaltyMaxChange = { penaltyMaxText = it },
          )
        }

        // Collapsible: Description, Link & Duration
        item {
          CollapsibleEditorCard(
            title = "Cel",
            summary = detailsSummary,
            expanded = expandedDetails,
            onToggle = { expandedDetails = !expandedDetails },
            testTag = "editor_section_details",
          ) {
            OutlinedTextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Opis (linki będą klikalne)") },
              minLines = 2,
              maxLines = 4,
              modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = link,
              onValueChange = { link = it },
              label = { Text("Link (opcjonalnie)") },
              placeholder = { Text("https://...") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = durationText,
              onValueChange = { durationText = it.filter(Char::isDigit) },
              label = { Text("Szacowany czas (min)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        // Collapsible: Subtasks
        item {
          CollapsibleEditorCard(
            title = "Podzadania",
            summary = subtasksSummary,
            badgeText = if (subtasks.isNotEmpty()) "${subtasks.size}" else null,
            expanded = expandedSubtasks,
            onToggle = { expandedSubtasks = !expandedSubtasks },
            testTag = "editor_section_subtasks",
          ) {
            subtasks.forEachIndexed { idx, sub ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp),
              ) {
                OutlinedTextField(
                  value = sub.title,
                  onValueChange = { updatedTitle ->
                    subtasks = subtasks.toMutableList().apply {
                      set(idx, sub.copy(title = updatedTitle))
                    }
                  },
                  label = { Text("Podzadanie ${idx + 1}") },
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                IconButton(
                  onClick = {
                    subtasks = subtasks.filterIndexed { index, _ -> index != idx }
                  },
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Usuń podzadanie",
                    tint = PixelColors.Red,
                  )
                }
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = newSubtaskTitle,
                onValueChange = { newSubtaskTitle = it },
                label = { Text("Dodaj podzadanie") },
                placeholder = { Text("np. Umyć lustro") },
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("new_subtask_input"),
              )
              PixelButton(
                text = "Dodaj",
                compact = true,
                modifier = Modifier.testTag("add_subtask_button"),
                onClick = {
                  val clean = newSubtaskTitle.trim()
                  if (clean.isNotEmpty()) {
                    val nextId = (subtasks.maxOfOrNull { it.id } ?: 0L) + 1L
                    subtasks = subtasks + SubtaskItem(id = nextId, title = clean, isDone = false)
                    newSubtaskTitle = ""
                  }
                },
              )
            }
          }
        }

        // Collapsible: Organization (Folder + Tags)
        item {
          CollapsibleEditorCard(
            title = "Organizacja",
            subtitle = "Folder, tagi",
            summary = organizationSummary,
            expanded = expandedOrganization,
            onToggle = { expandedOrganization = !expandedOrganization },
            testTag = "editor_section_organization",
          ) {
            ExposedDropdownMenuBox(
              expanded = folderMenuExpanded,
              onExpandedChange = { folderMenuExpanded = !folderMenuExpanded },
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedTextField(
                value = chosenFolder?.name ?: "Brak folderu",
                onValueChange = {},
                readOnly = true,
                label = { Text("Folder") },
                leadingIcon = chosenFolder?.let { f ->
                  {
                    Box(
                      modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(resolveFolderColor(f)),
                    )
                  }
                },
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderMenuExpanded)
                },
                modifier = Modifier
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                  .fillMaxWidth(),
              )
              ExposedDropdownMenu(
                expanded = folderMenuExpanded,
                onDismissRequest = { folderMenuExpanded = false },
              ) {
                DropdownMenuItem(
                  text = { Text("Brak") },
                  onClick = { folderId = 0L; folderMenuExpanded = false },
                )
                uiState.folders.forEach { folder ->
                  val folderColor = resolveFolderColor(folder)
                  DropdownMenuItem(
                    text = {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                          modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(folderColor),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        PixelIcon(icon = folder.icon, size = 18.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(folder.name)
                      }
                    },
                    onClick = { folderId = folder.id; folderMenuExpanded = false },
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Tagi",
              style = MaterialTheme.typography.labelLarge,
              color = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (selectedTagIds.isNotEmpty()) {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp),
              ) {
                selectedTagIds.mapNotNull { id -> uiState.tags.find { it.id == id } }.forEach { tag ->
                  InputChip(
                    selected = true,
                    onClick = { selectedTagIds = selectedTagIds - tag.id },
                    label = { Text("#${tag.name}") },
                    trailingIcon = {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Usuń tag",
                        modifier = Modifier.size(16.dp),
                      )
                    },
                  )
                }
              }
            }

            OutlinedTextField(
              value = tagQuery,
              onValueChange = { tagQuery = it },
              label = { Text("Wpisz tag, aby wyszukać lub utworzyć") },
              placeholder = { Text("np. pilne, dom, zakupy...") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("task_tag_autocomplete_input"),
            )

            val cleanQuery = tagQuery.trim().removePrefix("#")
            val suggestions = remember(uiState.tags, selectedTagIds, cleanQuery) {
              uiState.tags
                .filter { it.id !in selectedTagIds }
                .filter { cleanQuery.isEmpty() || it.name.contains(cleanQuery, ignoreCase = true) }
                .take(8)
            }
            val exactExists = cleanQuery.isNotEmpty() &&
              uiState.tags.any { it.name.equals(cleanQuery, ignoreCase = true) }

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (cleanQuery.isNotEmpty() && !exactExists) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = palette.accent,
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      scope.launch {
                        val created = onCreateOrFindTag(cleanQuery)
                        if (created.id !in selectedTagIds) {
                          selectedTagIds = selectedTagIds + created.id
                        }
                        tagQuery = ""
                      }
                    }
                    .testTag("create_inline_tag_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = null,
                      tint = palette.onAccent,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Utwórz #$cleanQuery",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = palette.onAccent,
                    )
                  }
                }
              }

              suggestions.forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = palette.surfaceSubtle,
                  border = BorderStroke(1.dp, palette.divider),
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      selectedTagIds = selectedTagIds + tag.id
                      tagQuery = ""
                    },
                ) {
                  Text(
                    text = "+ #${tag.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                  )
                }
              }
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }

  if (showDeleteConfirmDialog && existingTask != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = { Text("Usunąć zadanie?") },
      text = {
        Text("Zadanie „${existingTask.name}” zostanie trwale usunięte z listy.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirmDialog = false
            onDeleteTask(existingTask.id)
            onBack()
          },
          modifier = Modifier.testTag("confirm_delete_task_button"),
        ) {
          Text("Usuń", color = PixelColors.Red)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmDialog = false }) {
          Text("Anuluj")
        }
      },
    )
  }

  if (showDatePicker) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueTimestamp ?: System.currentTimeMillis())
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val selected = pickerState.selectedDateMillis
            if (selected != null) {
              val cal = Calendar.getInstance().apply {
                timeInMillis = selected
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
              }
              dueTimestamp = cal.timeInMillis
            }
            showDatePicker = false
          },
        ) { Text("Ustaw") }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }
}
