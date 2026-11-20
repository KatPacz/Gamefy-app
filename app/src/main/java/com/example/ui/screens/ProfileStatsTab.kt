package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryPunctualityStat
import com.example.data.model.CoinEconomySummary
import com.example.data.model.PlayerAnalyticsSummary
import com.example.data.model.ShopSpendRank
import com.example.data.model.StatsBarPoint
import com.example.data.model.StatsCoinRange
import com.example.data.model.StatsCompletionPeriod
import com.example.data.model.TaskReliabilitySummary
import com.example.data.model.formatEstimatedDurationMinutes
import com.example.ui.components.PixelCard
import com.example.ui.components.PixelIcon
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily
import java.util.Locale

fun LazyListScope.profileStatsItems(
  analytics: PlayerAnalyticsSummary,
  currentStreak: Int,
  bestStreak: Int,
  completionPeriod: StatsCompletionPeriod,
  onSelectCompletionPeriod: (StatsCompletionPeriod) -> Unit,
  chartSeries: List<StatsBarPoint>,
  coinRange: StatsCoinRange,
  onSelectCoinRange: (StatsCoinRange) -> Unit,
  economy: CoinEconomySummary,
) {
  item(key = "stats_kpi_grid") {
    StatsKpiGrid(
      analytics = analytics,
      currentStreak = currentStreak,
      bestStreak = bestStreak,
    )
  }

  item(key = "stats_completion_chart") {
    CompletionHistoryCard(
      period = completionPeriod,
      onSelectPeriod = onSelectCompletionPeriod,
      series = chartSeries,
    )
  }

  item(key = "stats_task_reliability") {
    TaskReliabilityCard(
      reliableTasks = analytics.reliableTasks,
      delayedTasks = analytics.delayedTasks,
    )
  }

  item(key = "stats_category_punctuality") {
    CategoryPunctualityCard(categories = analytics.categoryPunctuality)
  }

  item(key = "stats_coin_economy") {
    CoinEconomyCard(
      range = coinRange,
      onSelectRange = onSelectCoinRange,
      economy = economy,
    )
  }
}

@Composable
private fun StatsKpiGrid(
  analytics: PlayerAnalyticsSummary,
  currentStreak: Int,
  bestStreak: Int,
) {
  val avgFormatted = String.format(Locale("pl", "PL"), "%.1f", analytics.avgTasksPerDay)
  val punctualityText = analytics.overallOnTimePercent?.let { "$it%" } ?: "—"
  val focusDurationText = if (analytics.totalFocusMinutes > 0) {
    formatEstimatedDurationMinutes(analytics.totalFocusMinutes)
  } else {
    "0 min"
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("stats_kpi_section"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      KpiTile(
        title = "Średnio / dzień",
        value = "$avgFormatted zad.",
        subtitle = "Dziś: ${analytics.todayDoneCount} · 7 dni: ${analytics.weekDoneCount}",
        accent = LocalGamefyPalette.current.accent,
        modifier = Modifier.weight(1f),
      )
      KpiTile(
        title = "Terminowość",
        value = punctualityText,
        subtitle = "Łącznie: ${analytics.totalTasksDone} ukończ.",
        accent = PixelColors.Green,
        modifier = Modifier.weight(1f),
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      KpiTile(
        title = "Seria dni",
        value = "🔥 $currentStreak dni",
        subtitle = "Rekord: $bestStreak dni",
        accent = PixelColors.Orange,
        modifier = Modifier.weight(1f),
      )
      KpiTile(
        title = "Czas skupienia",
        value = focusDurationText,
        subtitle = "Sesje: ${analytics.totalFocusSessions}",
        accent = PixelColors.GemBlue,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun KpiTile(
  title: String,
  value: String,
  subtitle: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = palette.surface,
    border = BorderStroke(1.5.dp, palette.divider),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = palette.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = palette.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CompletionHistoryCard(
  period: StatsCompletionPeriod,
  onSelectPeriod: (StatsCompletionPeriod) -> Unit,
  series: List<StatsBarPoint>,
) {
  val palette = LocalGamefyPalette.current
  val maxCount = (series.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
  val totalInSeries = series.sumOf { it.count }

  PixelCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.BarChart,
          contentDescription = null,
          tint = palette.accent,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "UKOŃCZONE ZADANIA",
          style = MaterialTheme.typography.titleSmall,
          color = palette.textMuted,
        )
      }
      Text(
        text = "Razem: $totalInSeries",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = palette.accent,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      shape = RoundedCornerShape(10.dp),
      color = palette.surfaceSubtle,
      border = BorderStroke(1.dp, palette.divider),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        StatsCompletionPeriod.entries.forEach { option ->
          val selected = period == option
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (selected) palette.accent else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 36.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickable(role = Role.Tab) { onSelectPeriod(option) }
              .testTag("stats_period_${option.name.lowercase()}"),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
              Text(
                text = option.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (totalInSeries == 0) {
      StatsEmptyHint(
        text = "Ukończ pierwsze zadania, aby zobaczyć wykres postępów w czasie (dziennie, tygodniowo oraz rok do roku).",
      )
    } else {
      val visibleBars = when (period) {
        StatsCompletionPeriod.DAILY -> series.takeLast(10)
        StatsCompletionPeriod.WEEKLY -> series.takeLast(8)
        StatsCompletionPeriod.YEAR_OVER_YEAR -> series
      }
      val chartMax = (visibleBars.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(146.dp)
          .testTag("stats_bar_chart"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        visibleBars.forEachIndexed { idx, point ->
          val fraction = (point.count.toFloat() / chartMax.toFloat()).coerceIn(0f, 1f)
          val isLatest = idx == visibleBars.lastIndex
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
          ) {
            Text(
              text = if (point.count > 0) point.count.toString() else "",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Medium,
              color = if (isLatest) palette.accent else palette.textMuted,
              maxLines = 1,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth(0.74f)
                .height((84f * fraction).coerceAtLeast(if (point.count > 0) 8f else 3f).dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                  if (point.count == 0) {
                    Brush.verticalGradient(listOf(palette.divider, palette.divider))
                  } else if (isLatest) {
                    Brush.verticalGradient(listOf(palette.accent, palette.accent.copy(alpha = 0.78f)))
                  } else {
                    Brush.verticalGradient(
                      listOf(
                        palette.accent.copy(alpha = 0.72f),
                        palette.accent.copy(alpha = 0.42f),
                      )
                    )
                  }
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = point.label,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              color = if (isLatest) palette.textPrimary else palette.textMuted,
              maxLines = 1,
              overflow = TextOverflow.Clip,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      if (period == StatsCompletionPeriod.YEAR_OVER_YEAR) {
        val yoyNotes = series.filter { it.secondaryNote.isNotBlank() }
        if (yoyNotes.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            yoyNotes.takeLast(3).forEach { pt ->
              val positive = pt.secondaryNote.startsWith("+")
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = (if (positive) PixelColors.Green else PixelColors.Red).copy(alpha = 0.12f),
              ) {
                Text(
                  text = "${pt.label}: ${pt.secondaryNote}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (positive) PixelColors.GreenDark else PixelColors.Red,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TaskReliabilityCard(
  reliableTasks: List<TaskReliabilitySummary>,
  delayedTasks: List<TaskReliabilitySummary>,
) {
  val palette = LocalGamefyPalette.current
  PixelCard {
    Text(
      text = "NAWYKI: PEWNIAKI I POŚLIZGI",
      style = MaterialTheme.typography.titleSmall,
      color = palette.textMuted,
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (reliableTasks.isEmpty() && delayedTasks.isEmpty()) {
      StatsEmptyHint(
        text = "Po kilku ukończonych lub pominiętych zadaniach zobaczysz tu zadania wykonywane prawie w 100% oraz te najczęściej opóźniane.",
      )
      return@PixelCard
    }

    if (reliableTasks.isNotEmpty()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = PixelColors.Green,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "Najbardziej niezawodne (blisko 100%)",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      reliableTasks.forEach { item ->
        TaskReliabilityRow(
          item = item,
          badgeText = "${item.onTimePercent}% (${item.onTimeCount}/${item.doneCount})",
          badgeBg = PixelColors.Green.copy(alpha = 0.14f),
          badgeColor = PixelColors.GreenDark,
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
    }

    if (reliableTasks.isNotEmpty() && delayedTasks.isNotEmpty()) {
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = palette.divider)
    }

    if (delayedTasks.isNotEmpty()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = PixelColors.Red,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "Regularnie opóźniane lub pomijane",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      delayedTasks.forEach { item ->
        val badge = if (item.isCurrentlyOverdue) {
          "Po terminie"
        } else {
          "${item.lateOrSkipCount}× poślizg · ${item.onTimePercent}%"
        }
        TaskReliabilityRow(
          item = item,
          badgeText = badge,
          badgeBg = PixelColors.Red.copy(alpha = 0.12f),
          badgeColor = PixelColors.Red,
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
    }
  }
}

@Composable
private fun TaskReliabilityRow(
  item: TaskReliabilitySummary,
  badgeText: String,
  badgeBg: Color,
  badgeColor: Color,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.dp, palette.divider),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = item.name,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeBg,
      ) {
        Text(
          text = badgeText,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = badgeColor,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }
    }
  }
}

@Composable
private fun CategoryPunctualityCard(
  categories: List<CategoryPunctualityStat>,
) {
  val palette = LocalGamefyPalette.current
  PixelCard {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Schedule,
        contentDescription = null,
        tint = palette.accent,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "TERMINOWOŚĆ WG KATEGORII",
        style = MaterialTheme.typography.titleSmall,
        color = palette.textMuted,
      )
    }
    Spacer(modifier = Modifier.height(10.dp))

    if (categories.isEmpty()) {
      StatsEmptyHint(
        text = "Brak danych o kategoriach. Zobaczysz tu, w których cyklach (dzienne, tygodniowe, miesięczne) najłatwiej dotrzymujesz terminów.",
      )
      return@PixelCard
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      categories.forEach { stat ->
        val barColor = when {
          stat.onTimePercent >= 80 -> PixelColors.Green
          stat.onTimePercent >= 50 -> PixelColors.Orange
          else -> PixelColors.Red
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stat.category.label,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = palette.textPrimary,
            )
            Text(
              text = "${stat.onTimePercent}% (${stat.onTimeCount}/${stat.totalDoneCount})",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = barColor,
            )
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(palette.surfaceSubtle),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth((stat.onTimePercent / 100f).coerceIn(0.04f, 1f))
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(barColor),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoinEconomyCard(
  range: StatsCoinRange,
  onSelectRange: (StatsCoinRange) -> Unit,
  economy: CoinEconomySummary,
) {
  val palette = LocalGamefyPalette.current
  PixelCard {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Default.ShoppingBag,
        contentDescription = null,
        tint = PixelColors.Gold,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "MONETY I WYDATKI W SKLEPIE",
        style = MaterialTheme.typography.titleSmall,
        color = palette.textMuted,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      StatsCoinRange.entries.forEach { option ->
        val selected = range == option
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (selected) palette.accent else palette.surfaceSubtle,
          border = BorderStroke(1.dp, if (selected) palette.outline else palette.divider),
          modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelectRange(option) }
            .testTag("stats_coin_range_${option.name.lowercase()}"),
        ) {
          Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else palette.textPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      EconomyMiniStat(
        label = "Zdobyte",
        value = "+${economy.coinsEarned} 🪙",
        color = PixelColors.GreenDark,
        modifier = Modifier.weight(1f),
      )
      EconomyMiniStat(
        label = "Sklep",
        value = "−${economy.coinsSpentShop} 🪙",
        color = palette.accent,
        modifier = Modifier.weight(1f),
      )
      EconomyMiniStat(
        label = "Kary",
        value = "−${economy.coinsLostPenalty} 🪙",
        color = PixelColors.Red,
        modifier = Modifier.weight(1f),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    val netPrefix = if (economy.netCoins >= 0) "+" else ""
    Text(
      text = "Bilans okresu: $netPrefix${economy.netCoins} 🪙",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = if (economy.netCoins >= 0) PixelColors.GreenDark else PixelColors.Red,
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = palette.divider)

    Text(
      text = "Na co najczęściej wydajesz nagrody",
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color = palette.textPrimary,
    )
    Spacer(modifier = Modifier.height(6.dp))

    if (economy.topPurchases.isEmpty()) {
      StatsEmptyHint(text = "Brak zakupów w wybranym okresie.")
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        economy.topPurchases.forEachIndexed { idx, spend ->
          ShopSpendRow(rank = idx + 1, item = spend)
        }
      }
    }
  }
}

@Composable
private fun EconomyMiniStat(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.dp, palette.divider),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = palette.textMuted,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ShopSpendRow(
  rank: Int,
  item: ShopSpendRank,
) {
  val palette = LocalGamefyPalette.current
  val costSummary = buildString {
    if (item.totalCoins > 0) append("${item.totalCoins} 🪙")
    if (item.totalGems > 0) {
      if (isNotEmpty()) append(" · ")
      append("${item.totalGems} 💎")
    }
    if (isEmpty()) append("0 🪙")
  }
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.dp, palette.divider),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "#$rank",
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = palette.accent,
      )
      PixelIcon(icon = item.icon, size = 20.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Kupiono ${item.purchaseCount}×",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textMuted,
        )
      }
      Text(
        text = costSummary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
      )
    }
  }
}

@Composable
private fun StatsEmptyHint(text: String) {
  val palette = LocalGamefyPalette.current
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = palette.surfaceSubtle,
    border = BorderStroke(1.dp, palette.divider),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = palette.textMuted,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
  }
}
