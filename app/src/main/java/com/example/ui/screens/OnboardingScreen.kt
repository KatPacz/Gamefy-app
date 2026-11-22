package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OnboardingArea
import com.example.data.model.OnboardingDetailPref
import com.example.data.model.OnboardingRewardPref
import com.example.data.model.OnboardingTimePref
import com.example.data.model.ReadyTaskTemplate
import com.example.data.model.RewardCatalogTemplate
import com.example.data.model.recommendOnboardingRewards
import com.example.data.model.recommendOnboardingTasks
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.components.PixelProgressBar
import com.example.ui.components.PixelSectionHeader
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily
import kotlinx.coroutines.launch

private enum class OnboardingStep {
  WELCOME,
  Q1_AREAS,
  Q2_TIME,
  Q3_DETAIL,
  Q4_REWARDS,
  SUMMARY,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
  hasExistingData: Boolean,
  onStartFromScratch: () -> Unit,
  onCompleteSurvey: (
    areas: List<OnboardingArea>?,
    timePref: OnboardingTimePref?,
    detailPref: OnboardingDetailPref?,
    rewardPrefs: List<OnboardingRewardPref>?,
    selectedTemplateIds: List<String>,
    selectedRewardTemplateIds: List<String>,
  ) -> Unit,
  onRestoreFromJson: suspend (String) -> Boolean,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var step by remember { mutableStateOf(OnboardingStep.WELCOME) }

  // Survey answers: null means skipped (never auto-filled with defaults)
  var selectedAreas by remember { mutableStateOf<List<OnboardingArea>?>(null) }
  var selectedTimePref by remember { mutableStateOf<OnboardingTimePref?>(null) }
  var selectedDetailPref by remember { mutableStateOf<OnboardingDetailPref?>(null) }
  var selectedRewardPrefs by remember { mutableStateOf<List<OnboardingRewardPref>?>(null) }

  // Final result screen selections — ALL unchecked by default
  var checkedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var checkedRewardIds by remember { mutableStateOf<Set<String>>(emptySet()) }

  // Backup restore state
  var restoreErrorMessage by remember { mutableStateOf<String?>(null) }
  var pendingOverwriteJson by remember { mutableStateOf<String?>(null) }

  fun readJsonFromUri(uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
  }.getOrNull()

  val backupPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri == null) {
      // User canceled -> remain on welcome screen without completing onboarding
      return@rememberLauncherForActivityResult
    }
    val raw = readJsonFromUri(uri)
    if (raw.isNullOrBlank()) {
      restoreErrorMessage = "Nie udało się odczytać wybranego pliku."
      return@rememberLauncherForActivityResult
    }
    if (hasExistingData) {
      pendingOverwriteJson = raw
    } else {
      scope.launch {
        val ok = onRestoreFromJson(raw)
        if (!ok) {
          restoreErrorMessage = "Wybrany plik nie jest poprawną kopią zapasową Gamefy."
        }
      }
    }
  }

  BackHandler(enabled = step != OnboardingStep.WELCOME) {
    step = when (step) {
      OnboardingStep.WELCOME -> OnboardingStep.WELCOME
      OnboardingStep.Q1_AREAS -> OnboardingStep.WELCOME
      OnboardingStep.Q2_TIME -> OnboardingStep.Q1_AREAS
      OnboardingStep.Q3_DETAIL -> OnboardingStep.Q2_TIME
      OnboardingStep.Q4_REWARDS -> OnboardingStep.Q3_DETAIL
      OnboardingStep.SUMMARY -> OnboardingStep.Q4_REWARDS
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(palette.background)
      .statusBarsPadding()
      .navigationBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (step != OnboardingStep.WELCOME) {
      OnboardingStepProgressHeader(
        step = step,
        onSkipEntireSurvey = onStartFromScratch,
      )
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (step) {
        OnboardingStep.WELCOME -> {
          WelcomeStepContent(
            errorMessage = restoreErrorMessage,
            onStartSurvey = {
              restoreErrorMessage = null
              step = OnboardingStep.Q1_AREAS
            },
            onStartFromScratch = onStartFromScratch,
            onRestoreBackup = {
              restoreErrorMessage = null
              backupPickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
          )
        }

        OnboardingStep.Q1_AREAS -> {
          val currentAreas = selectedAreas.orEmpty()
          SurveyQuestionScaffold(
            questionTitle = "W czym chcesz, żeby aplikacja Ci pomogła?",
            questionSubtitle = "Wybierz maksymalnie 3 obszary, na których chcesz się skupić. (${currentAreas.size}/3)",
            onBack = { step = OnboardingStep.WELCOME },
            onSkipQuestion = {
              selectedAreas = null
              step = OnboardingStep.Q2_TIME
            },
            onNext = { step = OnboardingStep.Q2_TIME },
          ) {
          OnboardingArea.entries.forEach { area ->
            val isSelected = area in currentAreas
            val canSelectMore = currentAreas.size < 3
            SurveyOptionCard(
              iconEmoji = area.icon,
              title = area.label,
              subtitle = null,
              selected = isSelected,
              enabled = isSelected || canSelectMore,
              testTag = "onboarding_q1_${area.id}",
              onClick = {
                val next = if (isSelected) {
                  currentAreas - area
                } else if (canSelectMore) {
                  currentAreas + area
                } else {
                  currentAreas
                }
                selectedAreas = next.takeIf { it.isNotEmpty() }
              },
            )
          }
        }
      }

        OnboardingStep.Q2_TIME -> {
          SurveyQuestionScaffold(
            questionTitle = "Ile czasu chcesz poświęcać na pojedyncze zadanie?",
            questionSubtitle = "Pomoże to lepiej dopasować pierwsze propozycje — bez zmiany nagród ani trudności.",
            onBack = { step = OnboardingStep.Q1_AREAS },
            onSkipQuestion = {
              selectedTimePref = null
              step = OnboardingStep.Q3_DETAIL
            },
            onNext = { step = OnboardingStep.Q3_DETAIL },
          ) {
          OnboardingTimePref.entries.forEach { pref ->
            val iconKey = when (pref) {
              OnboardingTimePref.VERY_SHORT -> "bolt"
              OnboardingTimePref.SHORT -> "timer"
              OnboardingTimePref.MEDIUM -> "target"
              OnboardingTimePref.ANY -> "sparkle"
            }
            SurveyOptionCard(
              iconEmoji = iconKey,
              title = pref.label,
              subtitle = pref.subtitle,
              selected = selectedTimePref == pref,
              testTag = "onboarding_q2_${pref.id}",
              onClick = {
                selectedTimePref = if (selectedTimePref == pref) null else pref
              },
            )
          }
        }
      }

        OnboardingStep.Q3_DETAIL -> {
          SurveyQuestionScaffold(
            questionTitle = "Jak szczegółowe mają być Twoje zadania?",
            questionSubtitle = "Decyduje tylko o tym, jak wyświetlane są podzadania na karcie zadania.",
            onBack = { step = OnboardingStep.Q2_TIME },
            onSkipQuestion = {
              selectedDetailPref = null
              step = OnboardingStep.Q4_REWARDS
            },
            onNext = { step = OnboardingStep.Q4_REWARDS },
          ) {
          OnboardingDetailPref.entries.forEach { pref ->
            val iconKey = when (pref) {
              OnboardingDetailPref.STEP_BY_STEP -> "checklist"
              OnboardingDetailPref.SIMPLE -> "sparkle"
              OnboardingDetailPref.FLEXIBLE -> "target"
            }
            SurveyOptionCard(
              iconEmoji = iconKey,
              title = pref.label,
              subtitle = pref.description,
              selected = selectedDetailPref == pref,
              testTag = "onboarding_q3_${pref.id}",
              onClick = {
                selectedDetailPref = if (selectedDetailPref == pref) null else pref
              },
            )
          }
        }
      }

        OnboardingStep.Q4_REWARDS -> {
          val currentRewards = selectedRewardPrefs.orEmpty()
          SurveyQuestionScaffold(
            questionTitle = "Co sprawiłoby Ci przyjemność jako nagroda?",
            questionSubtitle = "Możesz wybrać dowolną liczbę kategorii. Wszystkie nagrody możesz później edytować.",
            onBack = { step = OnboardingStep.Q3_DETAIL },
            onSkipQuestion = {
              selectedRewardPrefs = null
              checkedTaskIds = emptySet()
              checkedRewardIds = emptySet()
              step = OnboardingStep.SUMMARY
            },
            onNext = {
              checkedTaskIds = emptySet()
              checkedRewardIds = emptySet()
              step = OnboardingStep.SUMMARY
            },
          ) {
          OnboardingRewardPref.entries.forEach { pref ->
            val isSelected = pref in currentRewards
            SurveyOptionCard(
              iconEmoji = pref.icon,
              title = pref.label,
              subtitle = null,
              selected = isSelected,
              testTag = "onboarding_q4_${pref.id}",
              onClick = {
                val next = if (isSelected) currentRewards - pref else currentRewards + pref
                selectedRewardPrefs = next.takeIf { it.isNotEmpty() }
              },
            )
          }
        }
      }

      OnboardingStep.SUMMARY -> {
        val recommendedTasks = remember(selectedAreas, selectedTimePref) {
          recommendOnboardingTasks(
            selectedAreas = selectedAreas.orEmpty(),
            timePref = selectedTimePref,
            maxResults = 8,
          )
        }
        val recommendedRewards = remember(selectedRewardPrefs) {
          recommendOnboardingRewards(selectedRewardPrefs.orEmpty())
        }

        SummaryStepContent(
          q1WasSkipped = selectedAreas.isNullOrEmpty(),
          recommendedTasks = recommendedTasks,
          recommendedRewards = recommendedRewards,
          checkedTaskIds = checkedTaskIds,
          checkedRewardIds = checkedRewardIds,
          onToggleTask = { id ->
            checkedTaskIds = if (id in checkedTaskIds) checkedTaskIds - id else checkedTaskIds + id
          },
          onToggleReward = { id ->
            checkedRewardIds = if (id in checkedRewardIds) checkedRewardIds - id else checkedRewardIds + id
          },
          onToggleAllRewards = {
            checkedRewardIds = if (checkedRewardIds.size == recommendedRewards.size) {
              emptySet()
            } else {
              recommendedRewards.map { it.id }.toSet()
            }
          },
          onBack = { step = OnboardingStep.Q4_REWARDS },
          onAddSelected = {
            onCompleteSurvey(
              selectedAreas,
              selectedTimePref,
              selectedDetailPref,
              selectedRewardPrefs,
              checkedTaskIds.toList(),
              checkedRewardIds.toList(),
            )
          },
          onSkipAndStartEmpty = {
            onCompleteSurvey(
              selectedAreas,
              selectedTimePref,
              selectedDetailPref,
              selectedRewardPrefs,
              emptyList(),
              emptyList(),
            )
          },
        )
      }
    }
    }
  }

  if (pendingOverwriteJson != null) {
    AlertDialog(
      onDismissRequest = { pendingOverwriteJson = null },
      title = { Text("Nadpisać istniejące dane?") },
      text = {
        Text("Wczytanie kopii zapasowej zastąpi obecne dane w aplikacji. Czy na pewno chcesz kontynuować?")
      },
      confirmButton = {
        TextButton(
          onClick = {
            val raw = pendingOverwriteJson
            pendingOverwriteJson = null
            if (raw != null) {
              scope.launch {
                val ok = onRestoreFromJson(raw)
                if (!ok) {
                  restoreErrorMessage = "Wybrany plik nie jest poprawną kopią zapasową Gamefy."
                }
              }
            }
          },
        ) {
          Text("Tak, przywróć")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingOverwriteJson = null }) {
          Text("Anuluj")
        }
      },
    )
  }
}

@Composable
private fun WelcomeStepContent(
  errorMessage: String?,
  onStartSurvey: () -> Unit,
  onStartFromScratch: () -> Unit,
  onRestoreBackup: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(Brush.linearGradient(listOf(palette.accent, palette.accentSecondary)))
          .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.White.copy(alpha = 0.2f),
        ) {
          Box(
            modifier = Modifier.padding(14.dp),
            contentAlignment = Alignment.Center,
          ) {
            PixelIcon(icon = "trophy", size = 44.dp)
          }
        }
        Text(
          text = "Witaj w aplikacji!",
          fontFamily = PixelFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          color = Color.White,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Wybierz, jak chcesz rozpocząć swoją przygodę. Wszystko możesz później dostosować.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.94f),
          textAlign = TextAlign.Center,
        )
      }
    }

    if (!errorMessage.isNullOrBlank()) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PixelColors.Red.copy(alpha = 0.14f),
          border = BorderStroke(1.5.dp, PixelColors.Red),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = PixelColors.Red,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    }

    item {
      WelcomeOptionCard(
        icon = Icons.Default.AutoAwesome,
        title = "Dopasuj aplikację do siebie",
        description = "Odpowiedz na kilka krótkich pytań, a zaproponujemy Ci zadania i nagrody.",
        badgeText = "Około 2 minuty · 100% lokalnie",
        isHighlighted = true,
        testTag = "onboarding_option_survey",
        onClick = onStartSurvey,
      )
    }

    item {
      WelcomeOptionCard(
        icon = Icons.Default.Explore,
        title = "Zacznij od zera",
        description = "Przejdź do aplikacji i samodzielnie dodawaj swoje zadania.",
        badgeText = null,
        isHighlighted = false,
        testTag = "onboarding_option_scratch",
        onClick = onStartFromScratch,
      )
    }

    item {
      WelcomeOptionCard(
        icon = Icons.Default.Restore,
        title = "Przywróć dane z kopii zapasowej",
        description = "Wczytaj wcześniej zapisane zadania, postępy i ustawienia.",
        badgeText = null,
        isHighlighted = false,
        testTag = "onboarding_option_restore",
        onClick = onRestoreBackup,
      )
    }
  }
}

@Composable
private fun WelcomeOptionCard(
  icon: ImageVector,
  title: String,
  description: String,
  badgeText: String?,
  isHighlighted: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isHighlighted) palette.accent.copy(alpha = 0.1f) else palette.surface,
    border = BorderStroke(
      width = if (isHighlighted) 2.5.dp else 1.5.dp,
      color = if (isHighlighted) palette.accent else palette.outline.copy(alpha = 0.45f),
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlighted) palette.accent else palette.surfaceSubtle,
      ) {
        Box(
          modifier = Modifier.size(46.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlighted) Color.White else palette.accent,
            modifier = Modifier.size(24.dp),
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )
        if (badgeText != null) {
          Spacer(modifier = Modifier.height(2.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.accent.copy(alpha = 0.16f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
              )
            }
          }
        }
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = if (isHighlighted) palette.accent else palette.textMuted,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun OnboardingStepProgressHeader(
  step: OnboardingStep,
  onSkipEntireSurvey: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val stepIndex = when (step) {
    OnboardingStep.WELCOME -> 0
    OnboardingStep.Q1_AREAS -> 1
    OnboardingStep.Q2_TIME -> 2
    OnboardingStep.Q3_DETAIL -> 3
    OnboardingStep.Q4_REWARDS -> 4
    OnboardingStep.SUMMARY -> 4
  }
  val isSummary = step == OnboardingStep.SUMMARY
  val stepLabel = if (isSummary) "Podsumowanie · 4/4" else "Pytanie $stepIndex/4"
  val targetProgress = (stepIndex / 4f).coerceIn(0f, 1f)
  val animatedProgress by animateFloatAsState(
    targetValue = targetProgress,
    animationSpec = tween(durationMillis = 260),
    label = "onboardingProgress",
  )

  Surface(
    color = palette.surface,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("onboarding_progress_header"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 620.dp)
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.accent.copy(alpha = 0.14f),
            border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.45f)),
          ) {
            Text(
              text = stepLabel,
              fontFamily = PixelFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = palette.accent,
              modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("onboarding_step_label"),
            )
          }

          if (!isSummary) {
            TextButton(
              onClick = onSkipEntireSurvey,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("onboarding_skip_all"),
            ) {
              Text(
                text = "Pomiń ankietę",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
              )
            }
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Ankieta ukończona",
                tint = PixelColors.Green,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Gotowe",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PixelColors.Green,
              )
            }
          }
        }

        PixelProgressBar(
          progress = animatedProgress,
          barHeight = 12.dp,
          modifier = Modifier.testTag("onboarding_progress_bar"),
        )
      }
      HorizontalDivider(color = palette.divider)
    }
  }
}

@Composable
private fun SurveyQuestionScaffold(
  questionTitle: String,
  questionSubtitle: String,
  onBack: () -> Unit,
  onSkipQuestion: () -> Unit,
  onNext: () -> Unit,
  optionsContent: @Composable () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 600.dp)
      .padding(horizontal = 16.dp, vertical = 14.dp),
  ) {
    Text(
      text = questionTitle,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = palette.textPrimary,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = questionSubtitle,
      style = MaterialTheme.typography.bodySmall,
      color = palette.textMuted,
    )

    Spacer(modifier = Modifier.height(14.dp))

    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 12.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          optionsContent()
        }
      }
    }

    HorizontalDivider(color = palette.divider)
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PixelButton(
        text = "Wstecz",
        compact = true,
        containerColor = palette.surface,
        contentColor = palette.textPrimary,
        modifier = Modifier.testTag("onboarding_back"),
        leadingIcon = {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = palette.textPrimary,
            modifier = Modifier.size(16.dp),
          )
        },
        onClick = onBack,
      )
      TextButton(
        onClick = onSkipQuestion,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("onboarding_skip_question"),
      ) {
        Text(
          text = "Pomiń",
          fontWeight = FontWeight.SemiBold,
          color = palette.textMuted,
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      PixelButton(
        text = "Dalej",
        compact = true,
        modifier = Modifier.testTag("onboarding_next"),
        leadingIcon = {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
          )
        },
        onClick = onNext,
      )
    }
  }
}

@Composable
private fun SurveyOptionCard(
  iconEmoji: String,
  title: String,
  subtitle: String?,
  selected: Boolean,
  enabled: Boolean = true,
  testTag: String,
  onClick: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val bgColor = when {
    selected -> palette.accent.copy(alpha = 0.14f)
    !enabled -> palette.surfaceSubtle.copy(alpha = 0.6f)
    else -> palette.surface
  }
  val borderColor = if (selected) palette.accent else palette.divider

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    border = BorderStroke(if (selected) 2.dp else 1.5.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      PixelIcon(icon = iconEmoji, size = 24.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = if (enabled) palette.textPrimary else palette.textMuted,
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
          )
        }
      }
      if (selected) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Wybrano",
          tint = palette.accent,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryStepContent(
  q1WasSkipped: Boolean,
  recommendedTasks: List<ReadyTaskTemplate>,
  recommendedRewards: List<RewardCatalogTemplate>,
  checkedTaskIds: Set<String>,
  checkedRewardIds: Set<String>,
  onToggleTask: (String) -> Unit,
  onToggleReward: (String) -> Unit,
  onToggleAllRewards: () -> Unit,
  onBack: () -> Unit,
  onAddSelected: () -> Unit,
  onSkipAndStartEmpty: () -> Unit,
) {
  val palette = LocalGamefyPalette.current
  val totalSelected = checkedTaskIds.size + checkedRewardIds.size

  Column(
    modifier = Modifier
      .fillMaxSize()
      .widthIn(max = 620.dp)
      .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        border = BorderStroke(1.5.dp, palette.divider),
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onBack),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Wstecz",
            tint = palette.textPrimary,
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Oto zadania dla Ciebie!",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Text(
          text = "Zaznacz tylko te propozycje, które chcesz dodać do swojej biblioteki (jako nieaktywne).",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(bottom = 12.dp),
    ) {
      if (q1WasSkipped) {
        item {
          PixelCard {
            Text(
              text = "Pominięto wybór obszarów zainteresowań",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Nie proponujemy zadań bez Twojego wskazania. Pełną Bazę gotowych zadań znajdziesz w dowolnej chwili w Profilu → Ustawienia → Baza gotowych zadań.",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textMuted,
            )
          }
        }
      } else if (recommendedTasks.isEmpty()) {
        item {
          PixelCard {
            Text(
              text = "Brak dopasowań dla wybranego czasu",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Możesz wrócić i zmienić preferowany czas albo przejrzeć wszystkie gotowe zadania później w Profilu → Ustawienia → Baza gotowych zadań.",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textMuted,
            )
          }
        }
      } else {
        item {
          PixelSectionHeader(
            title = "Proponowane zadania",
            badgeText = "${recommendedTasks.size}",
          )
        }
        items(recommendedTasks, key = { it.id }) { tpl ->
          val checked = tpl.id in checkedTaskIds
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (checked) palette.accent.copy(alpha = 0.1f) else palette.surface,
            border = BorderStroke(if (checked) 2.dp else 1.5.dp, if (checked) palette.accent else palette.divider),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable { onToggleTask(tpl.id) }
              .testTag("onboarding_task_row_${tpl.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = { onToggleTask(tpl.id) },
                modifier = Modifier.testTag("onboarding_task_check_${tpl.id}"),
              )
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = tpl.name,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Bold,
                  color = palette.textPrimary,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  SummaryTagChip(tpl.area.label)
                  SummaryTagChip("${tpl.estimatedMinutes} min")
                  SummaryTagChip("Trudność ${tpl.difficultyLevel}/5")
                  SummaryTagChip("+${tpl.coins} monet · +${tpl.exp} EXP")
                }
                val attrsText = tpl.attributeWeights.entries.joinToString(" · ") { (k, v) -> "$k $v%" }
                Text(
                  text = "Atrybuty: $attrsText",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.accent,
                )
              }
            }
          }
        }
      }

      if (recommendedRewards.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            PixelSectionHeader(
              title = "Proponowane nagrody",
              subtitle = "Do Twojego sklepu",
              badgeText = "${recommendedRewards.size}",
              modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleAllRewards) {
              Text(
                text = if (checkedRewardIds.size == recommendedRewards.size) "Odznacz" else "Zaznacz wszystkie",
                style = MaterialTheme.typography.labelSmall,
                color = palette.accent,
              )
            }
          }
        }
        items(recommendedRewards, key = { it.id }) { reward ->
          val checked = reward.id in checkedRewardIds
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (checked) palette.accent.copy(alpha = 0.1f) else palette.surface,
            border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) palette.accent else palette.divider),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onToggleReward(reward.id) }
              .testTag("onboarding_reward_row_${reward.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = { onToggleReward(reward.id) },
                modifier = Modifier.testTag("onboarding_reward_check_${reward.id}"),
              )
              PixelIcon(icon = reward.icon, size = 22.dp)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = reward.name,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = palette.textPrimary,
                )
                Text(
                  text = "Cena w sklepie: ${reward.defaultCoinPrice} monet (edytowalna)",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.textMuted,
                )
              }
            }
          }
        }
      }
    }

    HorizontalDivider(color = palette.divider)
    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val tasksLabel = when {
        checkedTaskIds.size == 1 -> "1 zadanie"
        checkedTaskIds.size % 10 in 2..4 && checkedTaskIds.size % 100 !in 12..14 -> "${checkedTaskIds.size} zadania"
        else -> "${checkedTaskIds.size} zadań"
      }
      val rewardsLabel = when {
        checkedRewardIds.size == 1 -> "1 nagroda"
        checkedRewardIds.size % 10 in 2..4 && checkedRewardIds.size % 100 !in 12..14 -> "${checkedRewardIds.size} nagrody"
        else -> "${checkedRewardIds.size} nagród"
      }
      val confirmButtonLabel = when {
        checkedTaskIds.isNotEmpty() && checkedRewardIds.isNotEmpty() ->
          "Dodaj wybrane ($tasksLabel · $rewardsLabel)"
        checkedTaskIds.isNotEmpty() ->
          "Dodaj wybrane do biblioteki (${checkedTaskIds.size})"
        checkedRewardIds.isNotEmpty() ->
          "Dodaj wybrane nagrody do sklepu (${checkedRewardIds.size})"
        else -> "Dodaj wybrane do biblioteki"
      }
      PixelButton(
        text = confirmButtonLabel,
        enabled = totalSelected > 0,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("onboarding_add_selected"),
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
          )
        },
        onClick = onAddSelected,
      )
      TextButton(
        onClick = onSkipAndStartEmpty,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("onboarding_skip_results"),
      ) {
        Text(
          text = "Pomiń — samodzielnie dodam zadania",
          fontWeight = FontWeight.SemiBold,
          color = palette.textMuted,
        )
      }
    }
  }
}

@Composable
private fun SummaryTagChip(text: String) {
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
