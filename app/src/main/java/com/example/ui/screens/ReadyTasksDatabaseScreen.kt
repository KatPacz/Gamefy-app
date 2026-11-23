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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.OnboardingArea
import com.example.data.model.OnboardingTimePref
import com.example.data.model.READY_TASK_TEMPLATES
import com.example.data.model.ReadyTaskTemplate
import com.example.data.model.formatFrequencyLabel
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelEmptyStateCard
import com.example.ui.components.PixelCard
import com.example.ui.components.TutorialTargetIds
import com.example.ui.components.tutorialTarget
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily

private val BUILTIN_ATTRIBUTES = listOf("Siła", "Wiedza", "Zdrowie", "Kreatywność", "Dyscyplina")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadyTasksDatabaseScreen(
  existingSourceTemplateIds: Set<String>,
  onAddTemplate: (ReadyTaskTemplate) -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current

  BackHandler(enabled = true) {
    onNavigateBack()
  }

  var searchQuery by remember { mutableStateOf("") }
  var selectedArea by remember { mutableStateOf<OnboardingArea?>(null) }
  var selectedTimePref by remember { mutableStateOf<OnboardingTimePref?>(null) }
  var selectedDifficulty by remember { mutableStateOf<Int?>(null) }
  var selectedAttribute by remember { mutableStateOf<String?>(null) }
  var showExtraFilters by remember { mutableStateOf(false) }
  var detailTemplate by remember { mutableStateOf<ReadyTaskTemplate?>(null) }

  val filteredTemplates = remember(
    searchQuery,
    selectedArea,
    selectedTimePref,
    selectedDifficulty,
    selectedAttribute,
  ) {
    val q = searchQuery.trim()
    READY_TASK_TEMPLATES.filter { tpl ->
      val queryMatches = q.isEmpty() ||
        tpl.name.contains(q, ignoreCase = true) ||
        tpl.description.contains(q, ignoreCase = true) ||
        tpl.tags.any { it.contains(q, ignoreCase = true) }
      val areaMatches = selectedArea == null || tpl.area == selectedArea
      val timeMatches = selectedTimePref == null || selectedTimePref!!.matchesMinutes(tpl.estimatedMinutes)
      val diffMatches = selectedDifficulty == null || tpl.difficultyLevel == selectedDifficulty
      val attrMatches = selectedAttribute == null || tpl.attributeWeights.keys.any {
        it.equals(selectedAttribute, ignoreCase = true)
      }
      queryMatches && areaMatches && timeMatches && diffMatches && attrMatches
    }
  }

  val activeExtraFilterCount = listOfNotNull(selectedTimePref, selectedDifficulty, selectedAttribute).size

  Box(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
    ) {
      // Top header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier
            .size(48.dp)
            .testTag("ready_tasks_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Wróć do ustawień",
            tint = palette.textPrimary,
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Baza gotowych zadań",
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = palette.textPrimary,
          )
          Text(
            text = "Przeglądaj ${READY_TASK_TEMPLATES.size} szablonów i dodawaj je do swojej biblioteki",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
          )
        }
      }

      // Search & Filter toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Szukaj po nazwie lub tagu...") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = palette.textMuted,
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Wyczyść wyszukiwanie",
                )
              }
            }
          },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("ready_tasks_search_input"),
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (showExtraFilters || activeExtraFilterCount > 0) {
            palette.accent.copy(alpha = 0.16f)
          } else {
            palette.surface
          },
          border = BorderStroke(
            width = 1.5.dp,
            color = if (activeExtraFilterCount > 0) palette.accent else palette.divider,
          ),
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showExtraFilters = !showExtraFilters }
            .testTag("ready_tasks_filter_toggle"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = "Filtry czasu, trudności i atrybutów",
              tint = if (activeExtraFilterCount > 0) palette.accent else palette.textPrimary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Category filter row
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        item {
          FilterChip(
            selected = selectedArea == null,
            onClick = { selectedArea = null },
            label = { Text("Wszystkie") },
          )
        }
        items(OnboardingArea.entries, key = { it.id }) { area ->
          FilterChip(
            selected = selectedArea == area,
            onClick = { selectedArea = if (selectedArea == area) null else area },
            label = { Text("${area.icon} ${area.folderName}") },
            modifier = Modifier.testTag("ready_tasks_area_${area.id}"),
          )
        }
      }

      if (showExtraFilters) {
        PixelCard(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          contentPadding = PaddingValues(12.dp),
        ) {
          Text(
            text = "Czas wykonania",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textMuted,
          )
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            FilterChip(
              selected = selectedTimePref == null,
              onClick = { selectedTimePref = null },
              label = { Text("Dowolny") },
            )
            listOf(
              OnboardingTimePref.VERY_SHORT to "≤ 5 min",
              OnboardingTimePref.SHORT to "5–15 min",
              OnboardingTimePref.MEDIUM to "15–30 min",
            ).forEach { (pref, label) ->
              FilterChip(
                selected = selectedTimePref == pref,
                onClick = { selectedTimePref = if (selectedTimePref == pref) null else pref },
                label = { Text(label) },
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Trudność (1–5)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textMuted,
          )
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            FilterChip(
              selected = selectedDifficulty == null,
              onClick = { selectedDifficulty = null },
              label = { Text("Wszystkie") },
            )
            (1..5).forEach { level ->
              FilterChip(
                selected = selectedDifficulty == level,
                onClick = { selectedDifficulty = if (selectedDifficulty == level) null else level },
                label = { Text("Poziom $level") },
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Atrybut EXP",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textMuted,
          )
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            FilterChip(
              selected = selectedAttribute == null,
              onClick = { selectedAttribute = null },
              label = { Text("Wszystkie") },
            )
            BUILTIN_ATTRIBUTES.forEach { attr ->
              FilterChip(
                selected = selectedAttribute == attr,
                onClick = { selectedAttribute = if (selectedAttribute == attr) null else attr },
                label = { Text(attr) },
              )
            }
          }

          if (activeExtraFilterCount > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
              onClick = {
                selectedTimePref = null
                selectedDifficulty = null
                selectedAttribute = null
              },
            ) {
              Text("Wyczyść filtry", color = palette.accent)
            }
          }
        }
      }

      Text(
        text = "Znaleziono: ${filteredTemplates.size} z ${READY_TASK_TEMPLATES.size}",
        style = MaterialTheme.typography.labelMedium,
        color = palette.textMuted,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
      )

      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (filteredTemplates.isEmpty()) {
          item {
            PixelEmptyStateCard(
              illustrationRes = R.drawable.img_empty_tasks,
              illustrationContentDescription = "Ilustracja pustych wyników bazy zadań",
              title = "Brak pasujących szablonów",
              subtitle = "Spróbuj zmienić wyszukiwanie lub wyczyścić aktywne filtry.",
              primaryActionLabel = "Wyczyść filtry",
              primaryActionTestTag = "ready_tasks_empty_clear_filters_button",
              onPrimaryAction = {
                searchQuery = ""
                selectedTimePref = null
                selectedDifficulty = null
                selectedAttribute = null
              },
            )
          }
        } else {
          val firstTplId = filteredTemplates.firstOrNull()?.id
          items(filteredTemplates, key = { it.id }) { tpl ->
            val alreadyAdded = tpl.id in existingSourceTemplateIds
            Box(
              modifier = if (tpl.id == firstTplId) {
                Modifier.tutorialTarget(TutorialTargetIds.READY_TASKS_FIRST)
              } else {
                Modifier
              },
            ) {
              ReadyTaskTemplateCard(
                template = tpl,
                alreadyAdded = alreadyAdded,
                onOpenDetails = { detailTemplate = tpl },
                onAdd = { onAddTemplate(tpl) },
              )
            }
          }
        }
        item { Spacer(modifier = Modifier.height(14.dp)) }
      }
    }
  }

  detailTemplate?.let { tpl ->
    val alreadyAdded = tpl.id in existingSourceTemplateIds
    ReadyTaskDetailDialog(
      template = tpl,
      alreadyAdded = alreadyAdded,
      onAdd = {
        onAddTemplate(tpl)
        detailTemplate = null
      },
      onDismiss = { detailTemplate = null },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadyTaskTemplateCard(
  template: ReadyTaskTemplate,
  alreadyAdded: Boolean,
  onOpenDetails: () -> Unit,
  onAdd: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  PixelCard(
    contentPadding = PaddingValues(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onOpenDetails)
      .testTag("ready_task_card_${template.id}"),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${template.area.icon} ${template.area.label}",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = palette.accent,
        )
        Text(
          text = formatFrequencyLabel(template.suggestedCategory, template.suggestedInterval),
          style = MaterialTheme.typography.labelSmall,
          color = palette.textMuted,
        )
      }

      Text(
        text = template.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
      )

      Text(
        text = template.description,
        style = MaterialTheme.typography.bodySmall,
        color = palette.textMuted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        TemplateMetaBadge("${template.estimatedMinutes} min")
        TemplateMetaBadge("Trudność ${template.difficultyLevel}/5")
        TemplateMetaBadge("+${template.coins} monet · +${template.exp} EXP")
        if (template.subtasks.isNotEmpty()) {
          TemplateMetaBadge(formatPolishSubtasksCount(template.subtasks.size))
        }
      }

      Text(
        text = template.attributeWeights.entries.joinToString(" · ") { (k, v) -> "$k $v%" },
        style = MaterialTheme.typography.labelSmall,
        color = palette.accent,
      )

      Spacer(modifier = Modifier.height(2.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onOpenDetails,
        ) {
          Text("Szczegóły", color = palette.textMuted)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (alreadyAdded) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = PixelColors.Green.copy(alpha = 0.14f),
            border = BorderStroke(1.5.dp, PixelColors.Green),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = PixelColors.GreenDark,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "W bibliotece",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PixelColors.GreenDark,
              )
            }
          }
        } else {
          PixelButton(
            text = "Dodaj do moich zadań",
            compact = true,
            modifier = Modifier.testTag("add_ready_task_${template.id}"),
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
            },
            onClick = onAdd,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadyTaskDetailDialog(
  template: ReadyTaskTemplate,
  alreadyAdded: Boolean,
  onAdd: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(
          text = template.area.label,
          style = MaterialTheme.typography.labelSmall,
          color = palette.accent,
        )
        Text(
          text = template.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = template.description,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textPrimary,
        )

        HorizontalDivider(color = palette.divider)

        Text(
          text = "Szacowany czas: ${template.estimatedMinutes} min",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "Trudność: ${template.difficultyLevel}/5 (${template.taskDifficulty.label})",
          style = MaterialTheme.typography.bodySmall,
        )
        Text(
          text = "Zdobycz po wykonaniu: +${template.exp} EXP · +${template.coins} monet",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "Podział EXP na atrybuty: " +
            template.attributeWeights.entries.joinToString(", ") { (k, v) -> "$k $v%" },
          style = MaterialTheme.typography.bodySmall,
          color = palette.accent,
        )
        Text(
          text = "Sugerowana częstotliwość: ${formatFrequencyLabel(template.suggestedCategory, template.suggestedInterval)}",
          style = MaterialTheme.typography.bodySmall,
        )

        if (template.subtasks.isNotEmpty()) {
          HorizontalDivider(color = palette.divider)
          Text(
            text = "Podzadania (${template.subtasks.size}):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          template.subtasks.forEachIndexed { idx, step ->
            Text(
              text = "${idx + 1}. $step",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textPrimary,
            )
          }
          Text(
            text = "Podzadania pomagają rozbić zadanie — zdobycz przyznawana jest raz za ukończenie całości.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted,
          )
        }

        if (template.tags.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            template.tags.forEach { tag ->
              TemplateMetaBadge("#$tag")
            }
          }
        }
      }
    },
    confirmButton = {
      if (!alreadyAdded) {
        TextButton(onClick = onAdd) {
          Text("Dodaj do moich zadań")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Zamknij")
      }
    },
  )
}

@Composable
private fun TemplateMetaBadge(text: String) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = palette.surfaceSubtle,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = palette.textPrimary,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
  }
}

private fun formatPolishSubtasksCount(count: Int): String = when {
  count == 1 -> "1 podzadanie"
  count % 10 in 2..4 && count % 100 !in 12..14 -> "$count podzadania"
  else -> "$count podzadań"
}
