package com.example

import com.example.data.local.ActivityEventEntity
import com.example.data.local.FolderEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.TaskEntity
import com.example.data.local.defaultColorHexForFolder
import com.example.data.local.normalizeColorHex
import com.example.data.local.resolvedColorHex
import com.example.ui.components.colorToHexString
import com.example.ui.components.colorToHsv
import com.example.ui.components.hsvToColor
import com.example.ui.components.parseHexColor
import com.example.data.model.ActivityEventType
import com.example.data.model.MILLIS_PER_DAY
import com.example.data.model.NotificationSettings
import com.example.data.model.NotificationSummaryStyle
import com.example.data.model.NotificationTimeSlot
import com.example.data.model.OnboardingArea
import com.example.data.model.OnboardingRewardPref
import com.example.data.model.OnboardingTimePref
import com.example.data.model.PenaltyMode
import com.example.data.model.RANDOM_SIDE_QUEST_BOARD_SIZE
import com.example.data.model.RANDOM_SIDE_QUEST_DAILY_LIMIT
import com.example.data.model.RANDOM_SIDE_QUEST_TEMPLATES
import com.example.data.model.READY_TASK_TEMPLATES
import com.example.data.model.REWARD_CATALOG_TEMPLATES
import com.example.data.model.RandomSideQuestTemplate
import com.example.data.model.StatsCoinRange
import com.example.data.model.StatsCompletionPeriod
import com.example.data.model.TaskCategory
import com.example.data.model.TaskDifficulty
import com.example.data.model.TaskEffortBucket
import com.example.data.model.TaskSortDirection
import com.example.data.model.buildCompletionChartSeries
import com.example.data.model.buildMonthCalendarGrid
import com.example.data.model.buildSummaryNotificationText
import com.example.data.model.calculateAutoPenaltyRange
import com.example.data.model.calculateAutoRewardAndPenalty
import com.example.data.model.computeCoinEconomySummary
import com.example.data.model.computePlayerAnalytics
import com.example.data.model.distributeWeightedExp
import com.example.data.model.groupTasksByCategory
import com.example.data.model.groupTasksByDifficulty
import com.example.data.model.isWithinNotificationSchedule
import com.example.data.model.localCalendarDayKey
import com.example.data.model.pickRandomSideQuests
import com.example.data.model.recommendOnboardingRewards
import com.example.data.model.recommendOnboardingTasks
import com.example.data.model.resolveDailyBoardIds
import com.example.data.model.rewardForDifficultyLevel
import com.example.data.model.sortTasksByDifficulty
import com.example.data.model.sortTasksByReward
import com.example.data.model.sortTasksByTimeLeft
import com.example.data.model.startOfLocalDayMillis
import com.example.data.model.startOfLocalMonthMillis
import com.example.data.model.taskRewardSortValue
import com.example.ui.screens.buildHorizonDayBuckets
import java.util.Calendar
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun autoRewardScalesWithDifficultyEffortAttributesAndPenalty() {
    val easy = calculateAutoRewardAndPenalty(
      difficulty = TaskDifficulty.EASY,
      effort = TaskEffortBucket.SHORT,
      attributeCount = 1,
      hasPenalty = false,
      penaltyMode = PenaltyMode.NONE,
    )
    val hardPenalized = calculateAutoRewardAndPenalty(
      difficulty = TaskDifficulty.HARD,
      effort = TaskEffortBucket.LONG,
      attributeCount = 3,
      hasPenalty = true,
      penaltyMode = PenaltyMode.ON_DUE_ONCE,
    )

    assertTrue(hardPenalized.exp > easy.exp)
    assertTrue(hardPenalized.coins > easy.coins)
    assertEquals(0, easy.penaltyMinCoins)
    assertEquals(0, easy.penaltyMaxCoins)
    assertTrue(hardPenalized.penaltyMinCoins >= 3)
    assertTrue(hardPenalized.penaltyMaxCoins > hardPenalized.penaltyMinCoins)
  }

  @Test
  fun autoPenaltyScalesByModeAndRewardCoins() {
    val (everyMin, everyMax) = calculateAutoPenaltyRange(
      baseCoins = 40,
      difficulty = TaskDifficulty.MEDIUM,
      penaltyMode = PenaltyMode.EVERY_MISSED_PERIOD,
    )
    val (skipMin, skipMax) = calculateAutoPenaltyRange(
      baseCoins = 40,
      difficulty = TaskDifficulty.MEDIUM,
      penaltyMode = PenaltyMode.ON_SKIP_ONLY,
    )

    assertTrue(everyMin >= 3 && everyMax > everyMin)
    assertTrue(skipMin >= everyMin && skipMax > skipMin)
  }

  @Test
  fun timeLeftSortingOrdersAscendingAndDescendingWithNoDueLast() {
    val now = 1_000_000_000L
    val soon = TaskEntity(id = 1, name = "Soon", dueTimestamp = now + 1_000L)
    val later = TaskEntity(id = 2, name = "Later", dueTimestamp = now + 10_000L)
    val noDue = TaskEntity(id = 3, name = "NoDue", dueTimestamp = null)

    val asc = sortTasksByTimeLeft(listOf(noDue, later, soon), TaskSortDirection.ASCENDING)
    assertEquals(listOf("Soon", "Later", "NoDue"), asc.map { it.name })

    val desc = sortTasksByTimeLeft(listOf(noDue, soon, later), TaskSortDirection.DESCENDING)
    assertEquals(listOf("Later", "Soon", "NoDue"), desc.map { it.name })
  }

  @Test
  fun difficultySortingOrdersVeryEasyToExtremeAndReversed() {
    val ve = TaskEntity(id = 1, name = "VE", difficultyCode = TaskDifficulty.VERY_EASY.code)
    val med = TaskEntity(id = 2, name = "M", difficultyCode = TaskDifficulty.MEDIUM.code)
    val ext = TaskEntity(id = 3, name = "X", difficultyCode = TaskDifficulty.EXTREME.code)

    val asc = sortTasksByDifficulty(listOf(ext, ve, med), TaskSortDirection.ASCENDING)
    assertEquals(listOf("VE", "M", "X"), asc.map { it.name })

    val desc = sortTasksByDifficulty(listOf(ve, med, ext), TaskSortDirection.DESCENDING)
    assertEquals(listOf("X", "M", "VE"), desc.map { it.name })

    val groupedDesc = groupTasksByDifficulty(listOf(ve, med, ext), TaskSortDirection.DESCENDING)
    assertEquals(
      listOf(TaskDifficulty.EXTREME, TaskDifficulty.MEDIUM, TaskDifficulty.VERY_EASY),
      groupedDesc.map { it.first },
    )
  }

  @Test
  fun rewardSortingTreatsOneGemAs100Coins() {
    val coinsOnly = TaskEntity(id = 1, name = "90 coins", coinReward = 90, gemReward = 0)
    val oneGemTenCoins = TaskEntity(id = 2, name = "1 gem 10 coins", coinReward = 10, gemReward = 1)
    val twoGems = TaskEntity(id = 3, name = "2 gems", coinReward = 0, gemReward = 2)

    assertEquals(90, taskRewardSortValue(coinsOnly))
    assertEquals(110, taskRewardSortValue(oneGemTenCoins))
    assertEquals(200, taskRewardSortValue(twoGems))

    val desc = sortTasksByReward(listOf(coinsOnly, twoGems, oneGemTenCoins), TaskSortDirection.DESCENDING)
    assertEquals(listOf("2 gems", "1 gem 10 coins", "90 coins"), desc.map { it.name })

    val asc = sortTasksByReward(listOf(twoGems, coinsOnly, oneGemTenCoins), TaskSortDirection.ASCENDING)
    assertEquals(listOf("90 coins", "1 gem 10 coins", "2 gems"), asc.map { it.name })
  }

  @Test
  fun calendarGridAndCategoryGroupingWorkCleanly() {
    val grid = buildMonthCalendarGrid(startOfLocalMonthMillis())
    assertTrue(grid.isNotEmpty() && grid.size % 7 == 0)

    val today = startOfLocalDayMillis()
    val tomorrow = today + MILLIS_PER_DAY
    val t1 = TaskEntity(id = 1, name = "D", categoryCode = "d", dueTimestamp = today)
    val t2 = TaskEntity(id = 2, name = "O", categoryCode = "o", dueTimestamp = tomorrow)

    val grouped = groupTasksByCategory(listOf(t2, t1), TaskSortDirection.ASCENDING)
    assertEquals(2, grouped.size)
    assertEquals("d", grouped.first().first.code)

    val threeDays = buildHorizonDayBuckets(listOf(t1, t2), today, 3, TaskSortDirection.ASCENDING)
    assertEquals(3, threeDays.size)
    assertEquals(1, threeDays[0].second.size)
    assertEquals(1, threeDays[1].second.size)
    assertEquals(0, threeDays[2].second.size)
    assertEquals(14, buildHorizonDayBuckets(listOf(t1, t2), today, 14, TaskSortDirection.ASCENDING).size)
  }

  @Test
  fun notificationScheduleRespectsWeekdaysTimeSlotsAndOvernightCustomHours() {
    fun timeOn(dayOfWeek: Int, hour: Int, minute: Int): Long {
      return Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.WEEK_OF_YEAR, 10)
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis
    }

    val weekdaysMorning = NotificationSettings(
      daysOfWeek = setOf(1, 2, 3, 4, 5),
      allowAnytime = false,
      timeSlots = setOf(NotificationTimeSlot.MORNING),
    )
    assertTrue(isWithinNotificationSchedule(weekdaysMorning, timeOn(Calendar.MONDAY, 9, 30)))
    assertFalse(isWithinNotificationSchedule(weekdaysMorning, timeOn(Calendar.MONDAY, 14, 0)))
    assertFalse(isWithinNotificationSchedule(weekdaysMorning, timeOn(Calendar.SATURDAY, 9, 30)))

    val overnightCustom = NotificationSettings(
      daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
      allowAnytime = false,
      timeSlots = setOf(NotificationTimeSlot.CUSTOM),
      customStartHour = 22,
      customStartMinute = 0,
      customEndHour = 6,
      customEndMinute = 0,
    )
    assertTrue(isWithinNotificationSchedule(overnightCustom, timeOn(Calendar.WEDNESDAY, 23, 15)))
    assertTrue(isWithinNotificationSchedule(overnightCustom, timeOn(Calendar.THURSDAY, 3, 0)))
    assertFalse(isWithinNotificationSchedule(overnightCustom, timeOn(Calendar.THURSDAY, 12, 0)))
  }

  @Test
  fun summaryNotificationBuilderFormatsDigestAndTickTock() {
    val t1 = TaskEntity(id = 1, name = "A", durationMinutes = 30)
    val t2 = TaskEntity(id = 2, name = "B", durationMinutes = 45)

    val digest = buildSummaryNotificationText(listOf(t1, t2), NotificationSummaryStyle.DETAILED_DIGEST)
    assertTrue(digest.contains("2 zadania"))
    assertTrue(digest.contains("1 h 15 min"))

    val tickTock = buildSummaryNotificationText(listOf(t1, t2), NotificationSummaryStyle.URGENT_TICK_TOCK)
    assertTrue(tickTock.contains("Tik tak"))
    assertTrue(tickTock.contains("(2)"))
  }

  @Test
  fun playerAnalyticsComputesReliabilityCategoryPunctualityAndYoYCharts() {
    val now = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 20, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val lastYear = Calendar.getInstance().apply {
      set(2025, Calendar.JUNE, 10, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val events = listOf(
      ActivityEventEntity(timestamp = now, eventType = ActivityEventType.TASK_DONE, taskId = 1, taskName = "Woda", categoryCode = "d", onTime = true, coinsDelta = 20),
      ActivityEventEntity(timestamp = now - MILLIS_PER_DAY, eventType = ActivityEventType.TASK_DONE, taskId = 1, taskName = "Woda", categoryCode = "d", onTime = true, coinsDelta = 20),
      ActivityEventEntity(timestamp = now - 2 * MILLIS_PER_DAY, eventType = ActivityEventType.TASK_DONE, taskId = 2, taskName = "Pranie", categoryCode = "w", onTime = false, coinsDelta = 30),
      ActivityEventEntity(timestamp = now - 3 * MILLIS_PER_DAY, eventType = ActivityEventType.TASK_SKIP, taskId = 2, taskName = "Pranie", categoryCode = "w", onTime = false, coinsDelta = -10),
      ActivityEventEntity(timestamp = lastYear, eventType = ActivityEventType.TASK_DONE, taskId = 1, taskName = "Woda", categoryCode = "d", onTime = true, coinsDelta = 20),
      ActivityEventEntity(timestamp = now, eventType = ActivityEventType.SHOP_BUY, itemId = 1, itemName = "Kawa", itemIcon = "☕", coinsDelta = -30),
      ActivityEventEntity(timestamp = now - MILLIS_PER_DAY, eventType = ActivityEventType.SHOP_BUY, itemId = 1, itemName = "Kawa", itemIcon = "☕", coinsDelta = -30),
      ActivityEventEntity(timestamp = now - MILLIS_PER_DAY, eventType = ActivityEventType.SHOP_BUY, itemId = 2, itemName = "Serial", itemIcon = "🎬", coinsDelta = -80),
      ActivityEventEntity(timestamp = now, eventType = ActivityEventType.FOCUS_DONE, durationMinutes = 25, coinsDelta = 5),
    )

    val analytics = computePlayerAnalytics(PlayerProfileEntity(), emptyList(), events, now)
    assertEquals(75, analytics.overallOnTimePercent) // 3 on-time of 4 done
    assertEquals("Woda", analytics.reliableTasks.first().name)
    assertEquals(100, analytics.reliableTasks.first().onTimePercent)
    assertEquals("Pranie", analytics.delayedTasks.first().name)
    assertEquals(2, analytics.delayedTasks.first().lateOrSkipCount)
    assertEquals(TaskCategory.DAILY, analytics.categoryPunctuality.first().category)
    assertEquals(100, analytics.categoryPunctuality.first().onTimePercent)
    assertEquals(25, analytics.totalFocusMinutes)

    val yoy = buildCompletionChartSeries(events, StatsCompletionPeriod.YEAR_OVER_YEAR, now)
    val pt2025 = yoy.first { it.label == "2025" }
    val pt2026 = yoy.first { it.label == "2026" }
    assertEquals(1, pt2025.count)
    assertEquals(3, pt2026.count)
    assertEquals("+200% r/r", pt2026.secondaryNote)

    val econ7d = computeCoinEconomySummary(PlayerProfileEntity(), emptyList(), events, StatsCoinRange.DAYS_7, now)
    assertEquals(75, econ7d.coinsEarned) // 20 + 20 + 30 + 5
    assertEquals(140, econ7d.coinsSpentShop) // 30 + 30 + 80
    assertEquals(10, econ7d.coinsLostPenalty)
    assertEquals("Serial", econ7d.topPurchases.first().name)
    assertEquals("Kawa", econ7d.topPurchases[1].name)
    assertEquals(2, econ7d.topPurchases[1].purchaseCount)
  }

  @Test
  fun builtInReadyTasksAndRewardsCatalogsMeetAllSpecificationInvariants() {
    assertTrue(READY_TASK_TEMPLATES.size >= 41)
    assertEquals(READY_TASK_TEMPLATES.size, READY_TASK_TEMPLATES.map { it.id }.toSet().size)
    assertEquals(14, REWARD_CATALOG_TEMPLATES.size)
    assertEquals(14, REWARD_CATALOG_TEMPLATES.map { it.id }.toSet().size)

    // Every template has valid difficulty 1..5, exact reward table mapping, and attribute weights summing to 100
    READY_TASK_TEMPLATES.forEach { tpl ->
      assertTrue("Difficulty out of range for ${tpl.id}", tpl.difficultyLevel in 1..5)
      val (expectedCoins, expectedExp) = rewardForDifficultyLevel(tpl.difficultyLevel)
      assertEquals(expectedCoins, tpl.coins)
      assertEquals(expectedExp, tpl.exp)
      assertEquals("Weights must sum to 100 for ${tpl.id}", 100, tpl.attributeWeights.values.sum())
    }

    // Exact difficulty table check
    assertEquals(2 to 2, rewardForDifficultyLevel(1))
    assertEquals(5 to 5, rewardForDifficultyLevel(2))
    assertEquals(10 to 10, rewardForDifficultyLevel(3))
    assertEquals(20 to 20, rewardForDifficultyLevel(4))
    assertEquals(40 to 40, rewardForDifficultyLevel(5))

    // pet_05 must be excluded from auto recommendations
    assertTrue(READY_TASK_TEMPLATES.first { it.id == "pet_05" }.excludeFromAutoRecommendation)
    // Subtask templates verified
    assertTrue(READY_TASK_TEMPLATES.first { it.id == "dom_03" }.subtasks.size >= 3)
    assertTrue(READY_TASK_TEMPLATES.first { it.id == "nauka_05" }.subtasks.size >= 4)
    assertTrue(READY_TASK_TEMPLATES.first { it.id == "praca_01" }.subtasks.size >= 3)
    assertTrue(READY_TASK_TEMPLATES.first { it.id == "ruch_05" }.subtasks.size >= 3)
    assertTrue(READY_TASK_TEMPLATES.first { it.id == "pet_06" }.subtasks.size >= 3)
    assertTrue(READY_TASK_TEMPLATES.first { it.id == "quest_04" }.subtasks.size >= 3)
  }

  @Test
  fun onboardingRecommendationsEnforceDiversityCapTimeFilterPet05AndSkippedQ1() {
    // Skipped Q1 -> empty list (never assumes interests)
    assertTrue(recommendOnboardingTasks(emptyList(), OnboardingTimePref.SHORT).isEmpty())

    // Multi-area selection -> max 8, diverse across all 3 selected areas, no duplicates
    val diverse = recommendOnboardingTasks(
      selectedAreas = listOf(OnboardingArea.DOM, OnboardingArea.NAUKA, OnboardingArea.RUCH),
      timePref = OnboardingTimePref.ANY,
    )
    assertEquals(8, diverse.size)
    assertEquals(8, diverse.map { it.id }.toSet().size)
    assertEquals(setOf(OnboardingArea.DOM, OnboardingArea.NAUKA, OnboardingArea.RUCH), diverse.map { it.area }.toSet())

    // Strict Q2 time filter: very_short -> all <= 5 min, never pads with longer tasks
    val veryShort = recommendOnboardingTasks(
      selectedAreas = listOf(OnboardingArea.DOM, OnboardingArea.NAUKA),
      timePref = OnboardingTimePref.VERY_SHORT,
    )
    assertTrue(veryShort.isNotEmpty())
    assertTrue(veryShort.all { it.estimatedMinutes <= 5 })

    // pet_05 never appears in automatic recommendations
    val pets = recommendOnboardingTasks(
      selectedAreas = listOf(OnboardingArea.ZWIERZETA),
      timePref = OnboardingTimePref.ANY,
    )
    assertTrue(pets.none { it.id == "pet_05" })
    assertTrue(pets.size in 5..8)

    // Q4 rewards mapping returns 2 editable rewards per chosen category
    val rewards = recommendOnboardingRewards(listOf(OnboardingRewardPref.GAMING, OnboardingRewardPref.BOOKS))
    assertEquals(4, rewards.size)
  }

  @Test
  fun weightedExpDistributionPreservesTotalExpDeterministically() {
    assertEquals(
      mapOf("Wiedza" to 7, "Dyscyplina" to 3),
      distributeWeightedExp(10, mapOf("Wiedza" to 70, "Dyscyplina" to 30)),
    )
    assertEquals(
      mapOf("Siła" to 12, "Zdrowie" to 6, "Dyscyplina" to 2),
      distributeWeightedExp(20, mapOf("Siła" to 60, "Zdrowie" to 30, "Dyscyplina" to 10)),
    )
    val split5 = distributeWeightedExp(5, mapOf("Dyscyplina" to 70, "Zdrowie" to 30))
    assertEquals(5, split5.values.sum())
    assertEquals(4, split5["Dyscyplina"])
    assertEquals(1, split5["Zdrowie"])
  }

  @Test
  fun skippedTimePreferenceMatchesAnyDurationAndStrictRangesNeverLeak() {
    val withNullTime = recommendOnboardingTasks(
      selectedAreas = listOf(OnboardingArea.NAUKA),
      timePref = null,
    )
    val withAnyTime = recommendOnboardingTasks(
      selectedAreas = listOf(OnboardingArea.NAUKA),
      timePref = OnboardingTimePref.ANY,
    )
    assertTrue(withNullTime.size in 5..8)
    assertEquals(withAnyTime.map { it.id }, withNullTime.map { it.id })

    val shortOnly = recommendOnboardingTasks(
      selectedAreas = listOf(OnboardingArea.DOM, OnboardingArea.NAUKA, OnboardingArea.RUCH),
      timePref = OnboardingTimePref.SHORT,
    )
    assertTrue(shortOnly.isNotEmpty())
    assertTrue(shortOnly.all { it.estimatedMinutes in 5..15 })

    val mediumOnly = recommendOnboardingTasks(
      selectedAreas = listOf(OnboardingArea.DOM, OnboardingArea.NAUKA, OnboardingArea.RUCH),
      timePref = OnboardingTimePref.MEDIUM,
    )
    assertTrue(mediumOnly.isNotEmpty())
    assertTrue(mediumOnly.all { it.estimatedMinutes in 15..30 })
  }

  @Test
  fun folderColorDefaultsNormalizationAndHsvWheelConversionWorkCorrectly() {
    assertEquals("#F59E0B", defaultColorHexForFolder("Dom"))
    assertEquals("#3B82F6", defaultColorHexForFolder("Praca"))
    assertEquals("#8B5CF6", defaultColorHexForFolder("Nauka"))
    assertEquals("#10B981", defaultColorHexForFolder("Zdrowie"))
    assertEquals("#FF00AA", normalizeColorHex("ff00aa"))
    assertEquals("#12AB34", normalizeColorHex(" #12ab34 "))
    assertEquals("", normalizeColorHex("not-a-color"))

    val customFolder = FolderEntity(id = 7L, icon = "🎸", name = "Muzyka", colorHex = "#e11d48")
    assertEquals("#E11D48", customFolder.resolvedColorHex())

    val fallbackFolder = FolderEntity(id = 7L, icon = "🏠", name = "Dom", colorHex = "")
    assertEquals("#F59E0B", fallbackFolder.resolvedColorHex())

    val parsed = parseHexColor("#3B82F6")
    val (h, s, v) = colorToHsv(parsed)
    val roundTripped = colorToHexString(hsvToColor(h, s, v))
    assertEquals("#3B82F6", roundTripped)
  }

  @Test
  fun randomSideQuestCatalogAndPickingLogicMeetAllRequirements() {
    assertEquals(6, RANDOM_SIDE_QUEST_BOARD_SIZE)
    assertEquals(3, RANDOM_SIDE_QUEST_DAILY_LIMIT)
    assertEquals(6, RANDOM_SIDE_QUEST_TEMPLATES.size)
    assertEquals(
      listOf("rq_001", "rq_002", "rq_003", "rq_004", "rq_005", "rq_006"),
      RANDOM_SIDE_QUEST_TEMPLATES.map { it.id },
    )

    RANDOM_SIDE_QUEST_TEMPLATES.forEach { tpl ->
      assertTrue(tpl.emoji.isNotBlank())
      assertTrue(tpl.title.isNotBlank())
      assertTrue(tpl.story.isNotBlank())
      assertTrue(tpl.goal.isNotBlank())
      assertTrue(tpl.estimatedMinutes > 0)
      assertEquals(1, tpl.difficultyLevel)
      assertEquals(2, tpl.coins)
      assertEquals(2, tpl.exp)
      assertEquals("Weights must sum to 100 for ${tpl.id}", 100, tpl.attributeWeights.values.sum())
    }

    // Current 6-item catalog returns all 6 distinct quests in randomized order
    val pickedSix = pickRandomSideQuests(random = Random(42))
    assertEquals(6, pickedSix.size)
    assertEquals(6, pickedSix.map { it.id }.toSet().size)

    // Expanded synthetic 20-item catalog automatically yields 6 distinct random quests
    val expandedCatalog = (1..20).map { idx ->
      RandomSideQuestTemplate(
        id = "rq_${idx.toString().padStart(3, '0')}",
        emoji = "⚔️",
        title = "Misja $idx",
        story = "Historia $idx",
        goal = "Cel $idx",
        estimatedMinutes = 5,
        difficultyLevel = 1,
        attributeWeights = mapOf("Dyscyplina" to 100),
      )
    }
    val pickedFromExpanded = pickRandomSideQuests(catalog = expandedCatalog, count = 6, random = Random(99))
    assertEquals(6, pickedFromExpanded.size)
    assertEquals(6, pickedFromExpanded.map { it.id }.toSet().size)

    // resolveDailyBoardIds preserves exact saved order on same day
    val savedCsv = "rq_006,rq_002,rq_004,rq_001,rq_005,rq_003"
    assertEquals(
      listOf("rq_006", "rq_002", "rq_004", "rq_001", "rq_005", "rq_003"),
      resolveDailyBoardIds(savedIdsCsv = savedCsv),
    )

    // localCalendarDayKey advances across local midnight
    val beforeMidnight = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 20, 23, 59, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val afterMidnight = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 21, 0, 1, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    assertEquals(20261120L, localCalendarDayKey(beforeMidnight))
    assertEquals(20261121L, localCalendarDayKey(afterMidnight))
  }
}


