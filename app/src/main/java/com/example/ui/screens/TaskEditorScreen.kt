package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val palette = LocalGamefyPalette.current
  val scope = rememberCoroutineScope()

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
  var nameError by remember { mutableStateOf(false) }

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
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = palette.surface,
          titleContentColor = palette.textPrimary,
          navigationIconContentColor = palette.textPrimary,
        ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          PixelCard {
            OutlinedTextField(
              value = name,
              onValueChange = { name = it; nameError = false },
              label = { Text("Nazwa zadania *") },
              placeholder = { Text("np. Podlać kwiaty") },
              isError = nameError,
              supportingText = if (nameError) {
                { Text("Podaj nazwę zadania") }
              } else null,
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("task_name_input"),
            )

            Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              ExposedDropdownMenuBox(
                expanded = folderMenuExpanded,
                onExpandedChange = { folderMenuExpanded = !folderMenuExpanded },
                modifier = Modifier.weight(1f),
              ) {
                val chosenFolder = uiState.folders.find { it.id == folderId }
                OutlinedTextField(
                  value = chosenFolder?.name ?: "Brak folderu",
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("Folder") },
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
                    DropdownMenuItem(
                      text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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

              OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it.filter(Char::isDigit) },
                label = { Text("Czas (min)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.85f),
              )
            }
          }
        }

        // Subtasks (parts of the main task)
        item {
          PixelCard {
            Text(
              text = "CZĘŚCI ZADANIA / PODZADANIA (${subtasks.size})",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textMuted,
            )
            Spacer(modifier = Modifier.height(6.dp))

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
                  label = { Text("Część ${idx + 1}") },
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
                label = { Text("Dodaj mniejszą część zadania") },
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

        // Tag Autocomplete Section directly inside task form!
        item {
          PixelCard {
            Text(
              text = "TAGI (WYSZUKIWANIE I DODAWANIE)",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textMuted,
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Utwórz #$cleanQuery",
                      style = MaterialTheme.typography.labelMedium,
                      color = Color.White,
                    )
                  }
                }
              }

              suggestions.forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = palette.surfaceSubtle,
                  border = BorderStroke(1.dp, palette.divider),
                  modifier = Modifier.clickable {
                    selectedTagIds = selectedTagIds + tag.id
                    tagQuery = ""
                  },
                ) {
                  Text(
                    text = "+ #${tag.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  )
                }
              }
            }
          }
        }

        // Schedule, Frequency & Repeat Limit
        item {
          PixelCard {
            Text(
              text = "CYKL I TERMIN",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textMuted,
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
                  label = { Text("Kategoria") },
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
                label = { Text("Limit powt. (0 = bez)") },
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
                  .clickable { showDatePicker = true },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
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
                      modifier = Modifier.size(28.dp),
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

        // Rewards: Auto Survey + EXP, Coins, Attributes, Item
        item {
          TaskRewardsSectionCard(
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

        // Reminders & Penalties (Manual / Auto penalty calculation)
        item {
          TaskReminderAndPenaltyCard(
            reminderMode = reminderMode,
            onSelectReminderMode = { reminderMode = it },
            preferredReminderHour = preferredReminderHour,
            preferredReminderMinute = preferredReminderMinute,
            onSelectPreferredTime = { h, m ->
              preferredReminderHour = h
              preferredReminderMinute = m
            },
            globalForcedModeActive = !uiState.profile.notifUseTaskSettings,
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

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PixelButton(
              text = "Zapisz zadanie",
              modifier = Modifier
                .weight(1f)
                .testTag("save_task_button"),
              onClick = {
                val cleanName = name.trim()
                if (cleanName.isEmpty()) {
                  nameError = true
                  return@PixelButton
                }
                val now = System.currentTimeMillis()
                val interval = (intervalText.toIntOrNull() ?: 1).coerceAtLeast(1)
                val limit = (repeatLimitText.toIntOrNull() ?: 0).coerceAtLeast(0)
                val minP = (penaltyMinText.toIntOrNull() ?: 0).coerceAtLeast(0)
                val maxP = (penaltyMaxText.toIntOrNull() ?: minP).coerceAtLeast(minP)
                val resolvedDue = dueTimestamp ?: if (category == TaskCategory.SIDEQUEST) {
                  null
                } else {
                  existingTask?.dueTimestamp ?: nextCycleTimestamp(now, category, interval)
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
                  attributeIdsCsv = toIdCsv(selectedAttrIds),
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
              },
            )

            if (existingTask != null) {
              PixelButton(
                text = "Usuń",
                containerColor = PixelColors.Red,
                contentColor = Color.White,
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                  )
                },
                onClick = {
                  onDeleteTask(existingTask.id)
                  onBack()
                },
              )
            }
          }
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
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
