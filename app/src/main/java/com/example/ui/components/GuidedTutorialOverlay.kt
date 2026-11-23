package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object TutorialTargetIds {
  const val INACTIVE_EDIT = "tutorial_inactive_edit"
  const val INACTIVE_ACTIVATE = "tutorial_inactive_activate"
  const val INACTIVE_HEADER = "tutorial_inactive_header"
  const val NEW_TASK = "tutorial_new_task"
  const val TASK_SAVE = "tutorial_task_save"
  const val SIDE_QUEST_LIMIT = "tutorial_side_quest_limit"
  const val SHOP_ACTIONS = "tutorial_shop_actions"
  const val CHEST_CONTENT = "tutorial_chest_content"
  const val READY_TASKS_FIRST = "tutorial_ready_tasks_first"
  const val BACKUP_CARD = "tutorial_backup_card"
}

enum class TutorialBubbleDock {
  TOP,
  BOTTOM,
  ABOVE_BOTTOM_BAR,
}

@Stable
class TutorialTargetRegistry {
  private val _bounds = mutableStateMapOf<String, Rect>()

  fun reportBounds(id: String, rect: Rect) {
    if (rect.width > 0f && rect.height > 0f) {
      val prev = _bounds[id]
      if (prev == null ||
        kotlin.math.abs(prev.left - rect.left) > 1f ||
        kotlin.math.abs(prev.top - rect.top) > 1f ||
        kotlin.math.abs(prev.right - rect.right) > 1f ||
        kotlin.math.abs(prev.bottom - rect.bottom) > 1f
      ) {
        _bounds[id] = rect
      }
    }
  }

  fun clearBounds(id: String) {
    _bounds.remove(id)
  }

  fun getBounds(id: String?): Rect? = if (id == null) null else _bounds[id]
}

val LocalTutorialTargetRegistry = compositionLocalOf<TutorialTargetRegistry?> { null }

/**
 * Registers a Composable's live root-space bounding box with the active tutorial overlay
 * so the spotlight ring and pointer arrow can highlight it without obscuring it.
 */
fun Modifier.tutorialTarget(id: String): Modifier = composed {
  val registry = LocalTutorialTargetRegistry.current
  if (registry == null) {
    this
  } else {
    DisposableEffect(registry, id) {
      onDispose {
        registry.clearBounds(id)
      }
    }
    this.onGloballyPositioned { coords ->
      if (coords.isAttached) {
        registry.reportBounds(id, coords.boundsInRoot())
      }
    }
  }
}

/**
 * Pure placement resolver that keeps the tutorial bubble opposite the highlighted target
 * so it never covers what the user should inspect or tap.
 */
fun resolveTutorialBubbleDock(
  targetRect: Rect?,
  containerHeightPx: Float,
  preferredDock: TutorialBubbleDock,
): TutorialBubbleDock {
  if (targetRect == null || containerHeightPx <= 0f) return preferredDock
  if (preferredDock == TutorialBubbleDock.ABOVE_BOTTOM_BAR) {
    // Only flip to TOP if the screen is very short and placing above bottom bar would crowd the top form
    return if (containerHeightPx < 900f && targetRect.top < containerHeightPx * 0.65f) {
      TutorialBubbleDock.TOP
    } else {
      TutorialBubbleDock.ABOVE_BOTTOM_BAR
    }
  }
  val centerFraction = targetRect.center.y / containerHeightPx
  return when {
    centerFraction < 0.45f -> TutorialBubbleDock.BOTTOM
    centerFraction > 0.55f -> TutorialBubbleDock.TOP
    else -> {
      val spaceAbove = targetRect.top
      val spaceBelow = (containerHeightPx - targetRect.bottom).coerceAtLeast(0f)
      if (spaceAbove > spaceBelow) TutorialBubbleDock.TOP else TutorialBubbleDock.BOTTOM
    }
  }
}

enum class GuidedTutorialStep(
  val stepNumber: Int,
  val icon: String,
  val title: String,
  val body: String,
  val primaryLabel: String,
  val primaryTargetId: String,
  val fallbackTargetId: String? = null,
  val secondaryTargetId: String? = null,
  val preferredDock: TutorialBubbleDock = TutorialBubbleDock.BOTTOM,
  val targetHintLabel: String = "",
) {
  TASKS_AND_INACTIVE(
    stepNumber = 1,
    icon = "checklist",
    title = "Zakładka Zadania",
    body = "Tu zarządzasz zadaniami. W sekcji «Nieaktywne» czeka zadanie startowe — kliknij «Edytuj» albo przycisk poniżej, aby je otworzyć.",
    primaryLabel = "Edytuj zadanie",
    primaryTargetId = TutorialTargetIds.INACTIVE_EDIT,
    fallbackTargetId = TutorialTargetIds.INACTIVE_HEADER,
    preferredDock = TutorialBubbleDock.TOP,
    targetHintLabel = "Kliknij «Edytuj»",
  ),
  TASK_EDITOR(
    stepNumber = 2,
    icon = "sparkle",
    title = "Edytor zadania",
    body = "U góry zmienisz nazwę, termin, podzadania, nagrody (EXP i monety), folder oraz przypomnienia. Gdy będziesz gotowy, kliknij «Zapisz» na dole.",
    primaryLabel = "Dalej",
    primaryTargetId = TutorialTargetIds.TASK_SAVE,
    preferredDock = TutorialBubbleDock.ABOVE_BOTTOM_BAR,
    targetHintLabel = "Kliknij «Zapisz»",
  ),
  ACTIVATE_AND_ADD(
    stepNumber = 3,
    icon = "check",
    title = "Aktywacja i nowe zadania",
    body = "Kliknij «Aktywuj», aby przenieść zadanie na listę aktywnych. Nowe własne zadania dodasz zawsze przyciskiem «+ Nowe» na górnym pasku.",
    primaryLabel = "Dalej",
    primaryTargetId = TutorialTargetIds.INACTIVE_ACTIVATE,
    fallbackTargetId = TutorialTargetIds.NEW_TASK,
    secondaryTargetId = TutorialTargetIds.NEW_TASK,
    preferredDock = TutorialBubbleDock.TOP,
    targetHintLabel = "Kliknij «Aktywuj»",
  ),
  SIDE_QUESTS(
    stepNumber = 4,
    icon = "sword",
    title = "Tablica Przygód (Side Questy)",
    body = "Codziennie losuje lekkie misje poboczne (maks. 3 dziennie), bez kar i presji terminu — idealne na szybkie EXP i monety.",
    primaryLabel = "Dalej",
    primaryTargetId = TutorialTargetIds.SIDE_QUEST_LIMIT,
    preferredDock = TutorialBubbleDock.ABOVE_BOTTOM_BAR,
    targetHintLabel = "Limit 3/dzień",
  ),
  SHOP(
    stepNumber = 5,
    icon = "bag",
    title = "Sklep z nagrodami",
    body = "Monety i brylanty wymienisz tu na własne nagrody. U góry kliknij «Edytuj nagrody», aby dodać własny przedmiot, lub otwórz «Katalog nagród».",
    primaryLabel = "Dalej",
    primaryTargetId = TutorialTargetIds.SHOP_ACTIONS,
    preferredDock = TutorialBubbleDock.BOTTOM,
    targetHintLabel = "Dodaj lub Katalog",
  ),
  CHEST(
    stepNumber = 6,
    icon = "chest",
    title = "Skrzynia",
    body = "Kupione w Sklepie i zdobyte za zadania przedmioty trafiają tutaj. Kiedy zechcesz z nich skorzystać, po prostu kliknij «Użyj».",
    primaryLabel = "Dalej",
    primaryTargetId = TutorialTargetIds.CHEST_CONTENT,
    preferredDock = TutorialBubbleDock.BOTTOM,
    targetHintLabel = "Twoje nagrody",
  ),
  READY_TASKS(
    stepNumber = 7,
    icon = "book",
    title = "Baza gotowych zadań",
    body = "Gdy zabraknie Ci pomysłów, zajrzyj tutaj (dostępne też w Profil → Ustawienia). Jednym dotknięciem dodasz wybrane szablony do biblioteki.",
    primaryLabel = "Dalej",
    primaryTargetId = TutorialTargetIds.READY_TASKS_FIRST,
    preferredDock = TutorialBubbleDock.BOTTOM,
    targetHintLabel = "Gotowe szablony",
  ),
  SETTINGS_AND_BACKUP(
    stepNumber = 8,
    icon = "save",
    title = "Ustawienia i Kopia zapasowa",
    body = "W Profil → Ustawienia zmienisz motyw, czcionkę i powiadomienia. Aplikacja działa offline — tutaj w «Kopii zapasowej» zapisuj regularnie plik JSON.",
    primaryLabel = "Zakończ",
    primaryTargetId = TutorialTargetIds.BACKUP_CARD,
    preferredDock = TutorialBubbleDock.BOTTOM,
    targetHintLabel = "Zapisz kopię",
  ),
}

@Composable
fun GuidedTutorialOverlay(
  step: GuidedTutorialStep,
  hasStarterTaskToEdit: Boolean,
  showBottomNavOffset: Boolean,
  onPrimaryAction: () -> Unit,
  onPrevious: (() -> Unit)?,
  onSkip: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val density = LocalDensity.current
  val registry = LocalTutorialTargetRegistry.current

  var overlayBounds by remember { mutableStateOf<Rect?>(null) }
  var bubbleBounds by remember { mutableStateOf<Rect?>(null) }
  var manualDockOverride by remember(step) { mutableStateOf<TutorialBubbleDock?>(null) }

  val primaryRect = registry?.getBounds(step.primaryTargetId)
    ?: registry?.getBounds(step.fallbackTargetId)
  val secondaryRect = registry?.getBounds(step.secondaryTargetId)

  val containerHeightPx = overlayBounds?.height ?: 0f
  val autoDock = resolveTutorialBubbleDock(
    targetRect = primaryRect,
    containerHeightPx = containerHeightPx,
    preferredDock = step.preferredDock,
  )
  val effectiveDock = manualDockOverride ?: autoDock

  val infiniteTransition = rememberInfiniteTransition(label = "tutorial_spotlight_anim")
  val pulseProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "tutorial_pulse",
  )
  val arrowBounce by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "tutorial_arrow_bounce",
  )

  val highlightColor = if (palette.isHighContrast) palette.accent else PixelColors.Gold
  val outlineColor = if (palette.isDark) Color.Black else palette.outline
  val secondaryRingColor = palette.accent

  Box(
    modifier = modifier
      .fillMaxSize()
      .onGloballyPositioned { coords ->
        if (coords.isAttached) {
          overlayBounds = coords.boundsInRoot()
        }
      },
  ) {
    // Non-blocking visual coachmark layer: pulsing highlight ring(s) + animated pointer arrow
    val rootOffset = overlayBounds?.topLeft ?: Offset.Zero
    val localPrimary = primaryRect?.translate(-rootOffset.x, -rootOffset.y)
    val localSecondary = secondaryRect?.translate(-rootOffset.x, -rootOffset.y)
    val localBubble = bubbleBounds?.translate(-rootOffset.x, -rootOffset.y)

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .testTag("tutorial_spotlight_canvas"),
    ) {
      val padPx = with(density) { 6.dp.toPx() }
      val ringStrokePx = with(density) { 3.5.dp.toPx() }
      val haloExpandPx = with(density) { (4f + pulseProgress * 7f).dp.toPx() }
      val cornerPx = with(density) { 14.dp.toPx() }

      // Optional secondary subtle highlight (e.g., "+ Nowe" during Step 3)
      if (localSecondary != null && localSecondary != localPrimary) {
        val secRect = Rect(
          left = (localSecondary.left - padPx * 0.7f).coerceAtLeast(2f),
          top = (localSecondary.top - padPx * 0.7f).coerceAtLeast(2f),
          right = (localSecondary.right + padPx * 0.7f).coerceAtMost(size.width - 2f),
          bottom = (localSecondary.bottom + padPx * 0.7f).coerceAtMost(size.height - 2f),
        )
        drawRoundRect(
          color = secondaryRingColor.copy(alpha = 0.20f + 0.12f * pulseProgress),
          topLeft = secRect.topLeft,
          size = Size(secRect.width, secRect.height),
          cornerRadius = CornerRadius(cornerPx, cornerPx),
          style = Fill,
        )
        drawRoundRect(
          color = secondaryRingColor,
          topLeft = secRect.topLeft,
          size = Size(secRect.width, secRect.height),
          cornerRadius = CornerRadius(cornerPx, cornerPx),
          style = Stroke(
            width = with(density) { 2.5.dp.toPx() },
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
          ),
        )
      }

      // Primary pulsing spotlight ring around the exact element to inspect/tap
      if (localPrimary != null) {
        val targetFrame = Rect(
          left = (localPrimary.left - padPx).coerceAtLeast(3f),
          top = (localPrimary.top - padPx).coerceAtLeast(3f),
          right = (localPrimary.right + padPx).coerceAtMost(size.width - 3f),
          bottom = (localPrimary.bottom + padPx).coerceAtMost(size.height - 3f),
        )
        val useCircle = targetFrame.width < with(density) { 88.dp.toPx() } &&
          targetFrame.height < with(density) { 88.dp.toPx() } &&
          kotlin.math.abs(targetFrame.width - targetFrame.height) < with(density) { 24.dp.toPx() }

        if (useCircle) {
          val radius = maxOf(targetFrame.width, targetFrame.height) / 2f
          // Outer breathing halo
          drawCircle(
            color = highlightColor.copy(alpha = 0.28f * (1f - pulseProgress * 0.55f)),
            radius = radius + haloExpandPx,
            center = targetFrame.center,
            style = Stroke(width = with(density) { 5.dp.toPx() }),
          )
          // Dark crisp contrast border
          drawCircle(
            color = outlineColor.copy(alpha = 0.75f),
            radius = radius,
            center = targetFrame.center,
            style = Stroke(width = ringStrokePx + with(density) { 2.5.dp.toPx() }),
          )
          // Bright pulsing main ring
          drawCircle(
            color = highlightColor,
            radius = radius,
            center = targetFrame.center,
            style = Stroke(width = ringStrokePx),
          )
        } else {
          // Outer breathing halo
          drawRoundRect(
            color = highlightColor.copy(alpha = 0.30f * (1f - pulseProgress * 0.55f)),
            topLeft = Offset(
              targetFrame.left - haloExpandPx * 0.65f,
              targetFrame.top - haloExpandPx * 0.65f,
            ),
            size = Size(
              targetFrame.width + haloExpandPx * 1.3f,
              targetFrame.height + haloExpandPx * 1.3f,
            ),
            cornerRadius = CornerRadius(cornerPx + 4f, cornerPx + 4f),
            style = Stroke(width = with(density) { 5.dp.toPx() }),
          )
          // Subtle inner glow tint (stays fully readable & interactive)
          drawRoundRect(
            color = highlightColor.copy(alpha = 0.10f + 0.08f * pulseProgress),
            topLeft = targetFrame.topLeft,
            size = Size(targetFrame.width, targetFrame.height),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Fill,
          )
          // Crisp dark contrast outline under highlight ring
          drawRoundRect(
            color = outlineColor.copy(alpha = 0.75f),
            topLeft = targetFrame.topLeft,
            size = Size(targetFrame.width, targetFrame.height),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = ringStrokePx + with(density) { 2.5.dp.toPx() }),
          )
          // Bright main highlight ring
          drawRoundRect(
            color = highlightColor,
            topLeft = targetFrame.topLeft,
            size = Size(targetFrame.width, targetFrame.height),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = ringStrokePx),
          )
        }

        // Animated directional pointer arrow from the tutorial bubble edge straight toward the target
        if (localBubble != null) {
          val bubbleBelowTarget = localBubble.top >= targetFrame.bottom - with(density) { 8.dp.toPx() }
          val bubbleAboveTarget = localBubble.bottom <= targetFrame.top + with(density) { 8.dp.toPx() }

          if (bubbleBelowTarget || bubbleAboveTarget) {
            val marginX = with(density) { 28.dp.toPx() }
            val startX = targetFrame.center.x.coerceIn(
              localBubble.left + marginX,
              (localBubble.right - marginX).coerceAtLeast(localBubble.left + marginX),
            )
            val bouncePx = with(density) { (arrowBounce * 8f).dp.toPx() }
            val tipGapPx = with(density) { 10.dp.toPx() }

            val startPt: Offset
            val endPt: Offset
            if (bubbleBelowTarget) {
              startPt = Offset(startX, localBubble.top - with(density) { 4.dp.toPx() })
              val rawEndY = targetFrame.bottom + tipGapPx + bouncePx
              endPt = Offset(
                x = targetFrame.center.x,
                y = rawEndY.coerceAtMost(startPt.y - with(density) { 18.dp.toPx() }),
              )
            } else {
              startPt = Offset(startX, localBubble.bottom + with(density) { 4.dp.toPx() })
              val rawEndY = targetFrame.top - tipGapPx - bouncePx
              endPt = Offset(
                x = targetFrame.center.x,
                y = rawEndY.coerceAtLeast(startPt.y + with(density) { 18.dp.toPx() }),
              )
            }

            val distance = hypot((endPt.x - startPt.x).toDouble(), (endPt.y - startPt.y).toDouble()).toFloat()
            val minShaft = with(density) { 16.dp.toPx() }
            val maxShaft = with(density) { 110.dp.toPx() }
            if (distance >= minShaft) {
              // Keep the arrow crisp and neat even when bubble and target are far apart
              val effectiveStart = if (distance > maxShaft) {
                val ratio = maxShaft / distance
                Offset(
                  x = endPt.x - (endPt.x - startPt.x) * ratio,
                  y = endPt.y - (endPt.y - startPt.y) * ratio,
                )
              } else {
                startPt
              }
              val controlPt = Offset(
                x = effectiveStart.x,
                y = (effectiveStart.y + endPt.y) / 2f,
              )

              val shaftPath = Path().apply {
                moveTo(effectiveStart.x, effectiveStart.y)
                quadraticTo(controlPt.x, controlPt.y, endPt.x, endPt.y)
              }

              val shaftWidth = with(density) { 4.dp.toPx() }
              // Dark border for guaranteed contrast over any screen content
              drawPath(
                path = shaftPath,
                color = outlineColor.copy(alpha = 0.8f),
                style = Stroke(
                  width = shaftWidth + with(density) { 3.dp.toPx() },
                  cap = StrokeCap.Round,
                  join = StrokeJoin.Round,
                ),
              )
              drawPath(
                path = shaftPath,
                color = highlightColor,
                style = Stroke(
                  width = shaftWidth,
                  cap = StrokeCap.Round,
                  join = StrokeJoin.Round,
                ),
              )

              // Arrowhead aligned with tangent at endPt (controlPt -> endPt)
              val angle = atan2((endPt.y - controlPt.y).toDouble(), (endPt.x - controlPt.x).toDouble())
              val headLen = with(density) { 15.dp.toPx() }
              val spread = Math.toRadians(30.0)
              val leftWing = Offset(
                x = (endPt.x - headLen * cos(angle - spread)).toFloat(),
                y = (endPt.y - headLen * sin(angle - spread)).toFloat(),
              )
              val rightWing = Offset(
                x = (endPt.x - headLen * cos(angle + spread)).toFloat(),
                y = (endPt.y - headLen * sin(angle + spread)).toFloat(),
              )
              val headPath = Path().apply {
                moveTo(endPt.x, endPt.y)
                lineTo(leftWing.x, leftWing.y)
                lineTo(rightWing.x, rightWing.y)
                close()
              }
              drawPath(
                path = headPath,
                color = outlineColor.copy(alpha = 0.85f),
                style = Stroke(
                  width = with(density) { 3.5.dp.toPx() },
                  join = StrokeJoin.Round,
                ),
              )
              drawPath(
                path = headPath,
                color = highlightColor,
                style = Fill,
              )

              // Small glowing anchor dot at the start of the pointer
              drawCircle(
                color = outlineColor.copy(alpha = 0.8f),
                radius = with(density) { 5.dp.toPx() },
                center = effectiveStart,
              )
              drawCircle(
                color = highlightColor,
                radius = with(density) { 3.5.dp.toPx() },
                center = effectiveStart,
              )
            }
          }
        }
      }
    }

    val bubbleAlignment = when (effectiveDock) {
      TutorialBubbleDock.TOP -> Alignment.TopCenter
      TutorialBubbleDock.BOTTOM, TutorialBubbleDock.ABOVE_BOTTOM_BAR -> Alignment.BottomCenter
    }
    val bubblePaddingModifier = when (effectiveDock) {
      TutorialBubbleDock.TOP -> Modifier
        .statusBarsPadding()
        .padding(start = 12.dp, end = 12.dp, top = 8.dp)
      TutorialBubbleDock.ABOVE_BOTTOM_BAR -> Modifier
        .navigationBarsPadding()
        .padding(start = 12.dp, end = 12.dp, bottom = 92.dp)
      TutorialBubbleDock.BOTTOM -> Modifier
        .navigationBarsPadding()
        .padding(
          start = 12.dp,
          end = 12.dp,
          bottom = if (showBottomNavOffset) 86.dp else 14.dp,
        )
    }

    GuidedTutorialCard(
      step = step,
      hasStarterTaskToEdit = hasStarterTaskToEdit,
      dock = effectiveDock,
      onToggleDock = {
        manualDockOverride = if (effectiveDock == TutorialBubbleDock.TOP) {
          if (step.preferredDock == TutorialBubbleDock.ABOVE_BOTTOM_BAR) {
            TutorialBubbleDock.ABOVE_BOTTOM_BAR
          } else {
            TutorialBubbleDock.BOTTOM
          }
        } else {
          TutorialBubbleDock.TOP
        }
      },
      onPrimaryAction = onPrimaryAction,
      onPrevious = onPrevious,
      onSkip = onSkip,
      modifier = Modifier
        .align(bubbleAlignment)
        .then(bubblePaddingModifier)
        .onGloballyPositioned { coords ->
          if (coords.isAttached) {
            bubbleBounds = coords.boundsInRoot()
          }
        },
    )
  }
}

@Composable
fun GuidedTutorialCard(
  step: GuidedTutorialStep,
  hasStarterTaskToEdit: Boolean,
  onPrimaryAction: () -> Unit,
  onPrevious: (() -> Unit)?,
  onSkip: () -> Unit,
  dock: TutorialBubbleDock = TutorialBubbleDock.BOTTOM,
  onToggleDock: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  val totalSteps = GuidedTutorialStep.entries.size
  val progress = step.stepNumber.toFloat() / totalSteps.toFloat()
  val resolvedPrimaryLabel = if (step == GuidedTutorialStep.TASKS_AND_INACTIVE && !hasStarterTaskToEdit) {
    "Dalej"
  } else {
    step.primaryLabel
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = palette.surface,
    shadowElevation = 10.dp,
    border = BorderStroke(2.dp, palette.accent),
    modifier = modifier
      .fillMaxWidth()
      .widthIn(max = 560.dp)
      .testTag("guided_tutorial_card"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          PixelIcon(icon = step.icon, size = 20.dp)
          Column {
            Text(
              text = "Samouczek · Krok ${step.stepNumber}/$totalSteps",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = palette.accent,
              fontSize = 11.5.sp,
            )
            Text(
              text = step.title,
              fontFamily = PixelFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = palette.textPrimary,
            )
          }
        }
        if (onToggleDock != null) {
          val isTop = dock == TutorialBubbleDock.TOP
          IconButton(
            onClick = onToggleDock,
            modifier = Modifier
              .size(48.dp)
              .testTag("tutorial_flip_position_button"),
          ) {
            Icon(
              imageVector = if (isTop) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
              contentDescription = if (isTop) "Przenieś dymek na dół" else "Przenieś dymek na górę",
              tint = palette.textMuted,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        TextButton(
          onClick = onSkip,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("tutorial_skip_button"),
        ) {
          Text(
            text = "Pomiń",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.textMuted,
          )
        }
      }

      LinearProgressIndicator(
        progress = { progress },
        color = palette.accent,
        trackColor = palette.surfaceSubtle,
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(4.dp)),
      )

      Text(
        text = step.body,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = palette.textPrimary,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (onPrevious != null) {
          OutlinedButton(
            onClick = onPrevious,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, palette.divider),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("tutorial_back_button"),
          ) {
            Text(
              text = "Wstecz",
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
          }
        } else {
          Spacer(modifier = Modifier.weight(0.01f))
        }
        PixelButton(
          text = resolvedPrimaryLabel,
          compact = true,
          modifier = Modifier
            .weight(1f)
            .testTag("tutorial_next_button"),
          onClick = onPrimaryAction,
        )
      }
    }
  }
}
