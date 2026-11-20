package com.example.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GamefyApp
import com.example.data.local.ActivityEventEntity
import com.example.data.local.AttributeEntity
import com.example.data.local.FolderEntity
import com.example.data.local.GamefyDatabase
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.ShopItemEntity
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.parseStringCsv
import com.example.data.model.AchievementDefinition
import com.example.data.model.AppFontFamilyOption
import com.example.data.model.AppFontSize
import com.example.data.model.AppThemeId
import com.example.data.model.AppWordSpacing
import com.example.data.model.NotificationSettings
import com.example.data.model.OnboardingArea
import com.example.data.model.OnboardingDetailPref
import com.example.data.model.OnboardingRewardPref
import com.example.data.model.OnboardingTimePref
import com.example.data.model.RANDOM_SIDE_QUEST_DAILY_LIMIT
import com.example.data.model.ReadyTaskTemplate
import com.example.data.model.ReminderMode
import com.example.data.model.RewardCatalogTemplate
import com.example.data.model.TaskCategory
import com.example.data.model.localCalendarDayKey
import com.example.data.repository.GamefyRepository
import com.example.data.repository.RewardOutcome
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainTimerMode(val label: String) {
  POMODORO("Tryb Pomodoro"),
  TIMER("Timer"),
}

enum class PomodoroPhase(val label: String) {
  FOCUS("Praca"),
  SHORT_BREAK("Krótka przerwa"),
  LONG_BREAK("Długa przerwa"),
}

data class MainTimerState(
  val mode: MainTimerMode = MainTimerMode.POMODORO,
  val pomodoroPhase: PomodoroPhase = PomodoroPhase.FOCUS,
  val completedFocusCount: Int = 0,
  val customHours: Int = 0,
  val customMinutes: Int = 25,
  val customSeconds: Int = 0,
  val totalSeconds: Int = 25 * 60,
  val remainingSeconds: Int = 25 * 60,
  val isRunning: Boolean = false,
) {
  val progressFraction: Float
    get() = if (totalSeconds <= 0) 0f else (1f - remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}

data class TaskInlineTimerState(
  val taskId: Long = 0L,
  val totalSeconds: Int = 0,
  val remainingSeconds: Int = 0,
  val isRunning: Boolean = false,
  val isFocusActive: Boolean = false,
) {
  val progressFraction: Float
    get() = if (totalSeconds <= 0) 0f else (1f - remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}

data class GamefyUiState(
  val profile: PlayerProfileEntity = PlayerProfileEntity(),
  val attributes: List<AttributeEntity> = emptyList(),
  val folders: List<FolderEntity> = emptyList(),
  val tags: List<TagEntity> = emptyList(),
  val tasks: List<TaskEntity> = emptyList(),
  val shopItems: List<ShopItemEntity> = emptyList(),
  val activityEvents: List<ActivityEventEntity> = emptyList(),
  val isLoaded: Boolean = false,
) {
  val themeId: AppThemeId
    get() = AppThemeId.fromCode(profile.themeCode)
  val fontSize: AppFontSize
    get() = AppFontSize.fromCode(profile.fontSizeCode)
  val fontFamilyOption: AppFontFamilyOption
    get() = AppFontFamilyOption.fromCode(profile.fontFamilyCode)
  val wordSpacing: AppWordSpacing
    get() = AppWordSpacing.fromCode(profile.wordSpacingCode)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as GamefyApp
  private val repository: GamefyRepository = app.repository

  private val _isLoaded = MutableStateFlow(false)
  val uiState: StateFlow<GamefyUiState> = combine(
    repository.profile,
    repository.attributes,
    repository.folders,
    repository.tags,
    repository.tasks,
    repository.shopItems,
    repository.activityEvents,
    _isLoaded,
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    GamefyUiState(
      profile = values[0] as PlayerProfileEntity,
      attributes = values[1] as List<AttributeEntity>,
      folders = values[2] as List<FolderEntity>,
      tags = values[3] as List<TagEntity>,
      tasks = values[4] as List<TaskEntity>,
      shopItems = values[5] as List<ShopItemEntity>,
      activityEvents = values[6] as List<ActivityEventEntity>,
      isLoaded = values[7] as Boolean,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = GamefyUiState(),
  )

  private val _mainTimer = MutableStateFlow(MainTimerState())
  val mainTimer: StateFlow<MainTimerState> = _mainTimer.asStateFlow()

  private val _taskTimer = MutableStateFlow(TaskInlineTimerState())
  val taskTimer: StateFlow<TaskInlineTimerState> = _taskTimer.asStateFlow()

  private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

  private val _achievementEvents = MutableSharedFlow<AchievementDefinition>(extraBufferCapacity = 16)
  val achievementEvents: SharedFlow<AchievementDefinition> = _achievementEvents.asSharedFlow()

  private val _pendingRandomQuestIds = MutableStateFlow<Set<String>>(emptySet())
  val pendingRandomQuestIds: StateFlow<Set<String>> = _pendingRandomQuestIds.asStateFlow()

  private val _isAcceptingRandomQuests = MutableStateFlow(false)
  val isAcceptingRandomQuests: StateFlow<Boolean> = _isAcceptingRandomQuests.asStateFlow()

  private val _showWeeklyBackupReminder = MutableStateFlow(false)
  val showWeeklyBackupReminder: StateFlow<Boolean> = _showWeeklyBackupReminder.asStateFlow()

  private var pendingRandomQuestDayKey: Long = -1L

  private fun emitOutcome(outcome: RewardOutcome?) {
    if (outcome == null) return
    if (outcome.toastMessage.isNotBlank()) {
      _toastEvents.tryEmit(outcome.toastMessage)
    }
    outcome.unlockedAchievements.forEach { ach ->
      _achievementEvents.tryEmit(ach)
    }
  }

  private var mainTimerTargetElapsedMs: Long = 0L
  private var taskTimerTargetElapsedMs: Long = 0L

  init {
    viewModelScope.launch {
      GamefyDatabase.ensureSeeded(app.database)
      _isLoaded.value = true
      runPeriodicChecks()
    }
    startTimerLoop()
    startPeriodicCheckLoop()
  }

  private fun startTimerLoop() {
    viewModelScope.launch {
      while (isActive) {
        delay(250L)
        val now = SystemClock.elapsedRealtime()

        val mt = _mainTimer.value
        if (mt.isRunning) {
          val rem = ((mainTimerTargetElapsedMs - now + 999L) / 1000L).toInt().coerceAtLeast(0)
          if (rem != mt.remainingSeconds) {
            _mainTimer.update { it.copy(remainingSeconds = rem) }
          }
          if (now >= mainTimerTargetElapsedMs) {
            onMainTimerFinished()
          }
        }

        val tt = _taskTimer.value
        if (tt.isRunning) {
          val rem = ((taskTimerTargetElapsedMs - now + 999L) / 1000L).toInt().coerceAtLeast(0)
          if (rem != tt.remainingSeconds) {
            _taskTimer.update { it.copy(remainingSeconds = rem) }
          }
          if (now >= taskTimerTargetElapsedMs) {
            onTaskTimerFinished()
          }
        }
      }
    }
  }

  private fun startPeriodicCheckLoop() {
    viewModelScope.launch {
      while (isActive) {
        delay(60_000L)
        runPeriodicChecks()
      }
    }
  }

  private suspend fun runPeriodicChecks() {
    val penalty = repository.evaluatePenalties()
    if (penalty > 0) {
      _toastEvents.tryEmit("💸 Kara za spóźnienie: −$penalty 🪙")
    }
    val alerts = repository.checkDueReminders()
    alerts.forEach { (taskId, message) ->
      NotificationHelper.showNotification(
        context = app,
        notificationId = (taskId % Int.MAX_VALUE).toInt(),
        channelId = NotificationHelper.CHANNEL_REMINDERS,
        title = "Gamefy – Przypomnienie",
        body = message,
      )
      _toastEvents.tryEmit("🔔 $message")
    }
  }

  // --- Main Timer Actions ---

  fun setMainTimerMode(mode: MainTimerMode) {
    val current = _mainTimer.value
    if (current.mode == mode) return
    if (mode == MainTimerMode.POMODORO) {
      val sec = 25 * 60
      _mainTimer.value = current.copy(
        mode = MainTimerMode.POMODORO,
        pomodoroPhase = PomodoroPhase.FOCUS,
        totalSeconds = sec,
        remainingSeconds = sec,
        isRunning = false,
      )
    } else {
      val sec = (current.customHours * 3600 + current.customMinutes * 60 + current.customSeconds)
        .coerceAtLeast(1)
      _mainTimer.value = current.copy(
        mode = MainTimerMode.TIMER,
        totalSeconds = sec,
        remainingSeconds = sec,
        isRunning = false,
      )
    }
  }

  fun setTimerPreset(minutes: Int) {
    val safeMin = minutes.coerceAtLeast(1)
    val sec = safeMin * 60
    _mainTimer.update {
      it.copy(
        mode = MainTimerMode.TIMER,
        customHours = safeMin / 60,
        customMinutes = safeMin % 60,
        customSeconds = 0,
        totalSeconds = sec,
        remainingSeconds = sec,
        isRunning = false,
      )
    }
  }

  fun updateCustomDuration(hours: Int, minutes: Int, seconds: Int) {
    val h = hours.coerceIn(0, 23)
    val m = minutes.coerceIn(0, 59)
    val s = seconds.coerceIn(0, 59)
    val total = (h * 3600 + m * 60 + s).coerceAtLeast(1)
    _mainTimer.update {
      it.copy(
        mode = MainTimerMode.TIMER,
        customHours = h,
        customMinutes = m,
        customSeconds = s,
        totalSeconds = total,
        remainingSeconds = total,
        isRunning = false,
      )
    }
  }

  fun toggleMainTimer() {
    val cur = _mainTimer.value
    if (cur.isRunning) {
      _mainTimer.update { it.copy(isRunning = false) }
    } else {
      val rem = if (cur.remainingSeconds <= 0) cur.totalSeconds.coerceAtLeast(1) else cur.remainingSeconds
      mainTimerTargetElapsedMs = SystemClock.elapsedRealtime() + rem * 1000L
      _mainTimer.update { it.copy(remainingSeconds = rem, isRunning = true) }
    }
  }

  fun resetMainTimer() {
    _mainTimer.update {
      it.copy(remainingSeconds = it.totalSeconds, isRunning = false)
    }
  }

  fun skipMainTimerPhase() {
    val cur = _mainTimer.value
    if (cur.mode == MainTimerMode.POMODORO) {
      val nextPhase = when (cur.pomodoroPhase) {
        PomodoroPhase.FOCUS ->
          if ((cur.completedFocusCount + 1) % 4 == 0) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
        PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS
      }
      val nextSec = when (nextPhase) {
        PomodoroPhase.FOCUS -> 25 * 60
        PomodoroPhase.SHORT_BREAK -> 5 * 60
        PomodoroPhase.LONG_BREAK -> 15 * 60
      }
      _mainTimer.update {
        it.copy(
          pomodoroPhase = nextPhase,
          totalSeconds = nextSec,
          remainingSeconds = nextSec,
          isRunning = false,
        )
      }
      _toastEvents.tryEmit("Przełączono na: ${nextPhase.label}")
    } else {
      resetMainTimer()
      _toastEvents.tryEmit("Zatrzymano i zresetowano timer")
    }
  }

  private suspend fun onMainTimerFinished() {
    val cur = _mainTimer.value
    NotificationHelper.vibratePattern(app)

    if (cur.mode == MainTimerMode.POMODORO) {
      if (cur.pomodoroPhase == PomodoroPhase.FOCUS) {
        val outcome = repository.completeFocusSession(cur.totalSeconds)
        val newFocusCount = cur.completedFocusCount + 1
        val nextPhase =
          if (newFocusCount % 4 == 0) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
        val nextSec = if (nextPhase == PomodoroPhase.LONG_BREAK) 15 * 60 else 5 * 60
        _mainTimer.update {
          it.copy(
            completedFocusCount = newFocusCount,
            pomodoroPhase = nextPhase,
            totalSeconds = nextSec,
            remainingSeconds = nextSec,
            isRunning = false,
          )
        }
        NotificationHelper.showNotification(
          context = app,
          notificationId = 8801,
          channelId = NotificationHelper.CHANNEL_TIMER,
          title = "Pomodoro ukończone!",
          body = "${outcome.toastMessage} — czas na: ${nextPhase.label}",
        )
        emitOutcome(outcome)
      } else {
        val nextSec = 25 * 60
        _mainTimer.update {
          it.copy(
            pomodoroPhase = PomodoroPhase.FOCUS,
            totalSeconds = nextSec,
            remainingSeconds = nextSec,
            isRunning = false,
          )
        }
        NotificationHelper.showNotification(
          context = app,
          notificationId = 8802,
          channelId = NotificationHelper.CHANNEL_TIMER,
          title = "Koniec przerwy!",
          body = "Czas wrócić do skupienia.",
        )
        _toastEvents.tryEmit("☕ Koniec przerwy — gotowy na kolejną sesję!")
      }
    } else {
      val outcome = repository.completeFocusSession(cur.totalSeconds)
      _mainTimer.update { it.copy(remainingSeconds = 0, isRunning = false) }
      NotificationHelper.showNotification(
        context = app,
        notificationId = 8803,
        channelId = NotificationHelper.CHANNEL_TIMER,
        title = "Timer zakończony!",
        body = outcome.toastMessage,
      )
      emitOutcome(outcome)
    }
  }

  // --- Inline Task Timer Actions ---

  fun toggleTaskTimer(taskId: Long, durationMinutes: Int) {
    val cur = _taskTimer.value
    val effectiveMinutes = if (durationMinutes > 0) durationMinutes else 25
    val totalSec = effectiveMinutes.coerceAtLeast(1) * 60
    if (cur.taskId == taskId && cur.isRunning) {
      _taskTimer.update { it.copy(isRunning = false) }
    } else {
      val rem = if (cur.taskId == taskId && cur.remainingSeconds > 0) cur.remainingSeconds else totalSec
      taskTimerTargetElapsedMs = SystemClock.elapsedRealtime() + rem * 1000L
      _taskTimer.value = TaskInlineTimerState(
        taskId = taskId,
        totalSeconds = totalSec,
        remainingSeconds = rem,
        isRunning = true,
        isFocusActive = true,
      )
    }
  }

  fun resetTaskTimer(taskId: Long, durationMinutes: Int) {
    val effectiveMinutes = if (durationMinutes > 0) durationMinutes else 25
    val totalSec = effectiveMinutes.coerceAtLeast(1) * 60
    _taskTimer.value = TaskInlineTimerState(
      taskId = 0L,
      totalSeconds = totalSec,
      remainingSeconds = totalSec,
      isRunning = false,
      isFocusActive = false,
    )
  }

  fun dismissTaskFocusView() {
    _taskTimer.update { it.copy(isFocusActive = false) }
  }

  fun stopAndResetAllTimers() {
    _mainTimer.update {
      it.copy(remainingSeconds = it.totalSeconds, isRunning = false)
    }
    val curTask = _taskTimer.value
    _taskTimer.value = TaskInlineTimerState(
      taskId = 0L,
      totalSeconds = curTask.totalSeconds,
      remainingSeconds = curTask.totalSeconds,
      isRunning = false,
      isFocusActive = false,
    )
    _toastEvents.tryEmit("Przerwano odliczanie i zresetowano timery")
  }

  private fun stopAndResetTaskTimerIfMatches(taskId: Long) {
    val cur = _taskTimer.value
    if (cur.taskId == taskId) {
      _taskTimer.value = TaskInlineTimerState(
        taskId = 0L,
        totalSeconds = cur.totalSeconds,
        remainingSeconds = cur.totalSeconds,
        isRunning = false,
        isFocusActive = false,
      )
    }
  }

  private fun onTaskTimerFinished() {
    val cur = _taskTimer.value
    val taskName = uiState.value.tasks.find { it.id == cur.taskId }?.name ?: "zadanie"
    _taskTimer.update { it.copy(remainingSeconds = 0, isRunning = false) }
    NotificationHelper.vibratePattern(app)
    NotificationHelper.showNotification(
      context = app,
      notificationId = 8900 + (cur.taskId % 100).toInt(),
      channelId = NotificationHelper.CHANNEL_TIMER,
      title = "Czas zadania minął!",
      body = "Zakończono odliczanie dla: $taskName",
    )
    _toastEvents.tryEmit("⏱️ Czas minął: $taskName")
  }

  // --- Task & Gamification Actions ---

  fun completeTask(task: TaskEntity) {
    stopAndResetTaskTimerIfMatches(task.id)
    viewModelScope.launch {
      emitOutcome(repository.completeTask(task))
    }
  }

  fun toggleSubtask(task: TaskEntity, subtaskId: Long) {
    viewModelScope.launch {
      val outcome = repository.toggleSubtask(task, subtaskId)
      if (outcome != null) {
        stopAndResetTaskTimerIfMatches(task.id)
      }
      emitOutcome(outcome)
    }
  }

  fun skipTask(task: TaskEntity) {
    stopAndResetTaskTimerIfMatches(task.id)
    viewModelScope.launch {
      val msg = repository.skipTask(task)
      if (msg.isNotBlank()) _toastEvents.tryEmit(msg)
    }
  }

  fun giveUpTask(task: TaskEntity) {
    stopAndResetTaskTimerIfMatches(task.id)
    viewModelScope.launch {
      val msg = repository.giveUpTask(task)
      if (msg.isNotBlank()) _toastEvents.tryEmit(msg)
    }
  }

  fun saveTask(task: TaskEntity) {
    viewModelScope.launch {
      emitOutcome(repository.saveTask(task))
    }
  }

  fun deleteTask(taskId: Long) {
    stopAndResetTaskTimerIfMatches(taskId)
    viewModelScope.launch {
      repository.deleteTask(taskId)
      _toastEvents.tryEmit("Usunięto zadanie")
    }
  }

  fun applyBulkUpdate(
    taskIds: Set<Long>,
    category: TaskCategory?,
    interval: Int,
    folderId: Long?,
    reminder: ReminderMode?,
  ) {
    viewModelScope.launch {
      val count = repository.applyBulkUpdate(taskIds, category, interval, folderId, reminder)
      _toastEvents.tryEmit("Zaktualizowano $count zadań")
    }
  }

  suspend fun getOrCreateTag(rawName: String): TagEntity = repository.getOrCreateTag(rawName)

  fun updateTag(tag: TagEntity) {
    viewModelScope.launch { repository.updateTag(tag) }
  }

  fun deleteTag(id: Long) {
    viewModelScope.launch { repository.deleteTag(id) }
  }

  // --- Shop & Chest ---

  fun buyShopItem(item: ShopItemEntity) {
    viewModelScope.launch {
      emitOutcome(repository.buyShopItem(item))
    }
  }

  fun useInventoryItem(item: ShopItemEntity) {
    viewModelScope.launch {
      val msg = repository.useInventoryItem(item)
      if (msg.isNotBlank()) _toastEvents.tryEmit(msg)
    }
  }

  fun countPurchasesInPeriod(item: ShopItemEntity): Int = repository.countPurchasesInPeriod(item)

  fun saveShopItem(item: ShopItemEntity) {
    viewModelScope.launch {
      repository.saveShopItem(item)
      _toastEvents.tryEmit("Zapisano nagrodę")
    }
  }

  fun deleteShopItem(id: Long) {
    viewModelScope.launch {
      repository.deleteShopItem(id)
      _toastEvents.tryEmit("Usunięto nagrodę")
    }
  }

  suspend fun compressImageUri(uri: Uri): String? = repository.compressImageUriToDataUrl(app, uri)

  // --- Profile, Attributes, Folders, Theme, Notifications, Backup ---

  fun addAttribute(icon: String, name: String) {
    if (name.isBlank()) return
    viewModelScope.launch { repository.addAttribute(icon, name) }
  }

  fun updateAttribute(attr: AttributeEntity) {
    viewModelScope.launch { repository.updateAttribute(attr) }
  }

  fun deleteAttribute(id: Long) {
    viewModelScope.launch { repository.deleteAttribute(id) }
  }

  fun addFolder(icon: String, name: String, colorHex: String = "") {
    if (name.isBlank()) return
    viewModelScope.launch { repository.addFolder(icon, name, colorHex) }
  }

  fun updateFolder(folder: FolderEntity) {
    viewModelScope.launch { repository.updateFolder(folder) }
  }

  fun deleteFolder(id: Long) {
    viewModelScope.launch { repository.deleteFolder(id) }
  }

  fun setTheme(theme: AppThemeId) {
    viewModelScope.launch { repository.setTheme(theme) }
  }

  fun setFontSize(fontSize: AppFontSize) {
    viewModelScope.launch { repository.setFontSize(fontSize) }
  }

  fun setFontFamily(fontFamily: AppFontFamilyOption) {
    viewModelScope.launch { repository.setFontFamily(fontFamily) }
  }

  fun setWordSpacing(wordSpacing: AppWordSpacing) {
    viewModelScope.launch { repository.setWordSpacing(wordSpacing) }
  }

  fun setNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setNotificationsEnabled(enabled)
      if (enabled) {
        ReminderScheduler.scheduleNextCheck(app)
        runPeriodicChecks()
        _toastEvents.tryEmit("Powiadomienia włączone")
      } else {
        ReminderScheduler.cancel(app)
        _toastEvents.tryEmit("Powiadomienia wyłączone")
      }
    }
  }

  fun updateNotificationSettings(settings: NotificationSettings) {
    viewModelScope.launch {
      repository.updateNotificationSettings(settings)
      if (uiState.value.profile.notificationsEnabled) {
        ReminderScheduler.scheduleNextCheck(app)
      }
    }
  }

  suspend fun exportBackupJson(): String {
    repository.markBackupReminderAcknowledged()
    _showWeeklyBackupReminder.value = false
    return repository.exportToJson()
  }

  fun checkWeeklyBackupReminder() {
    viewModelScope.launch {
      if (repository.checkAndPrepareWeeklyBackupReminder()) {
        _showWeeklyBackupReminder.value = true
      }
    }
  }

  fun dismissWeeklyBackupReminder() {
    _showWeeklyBackupReminder.value = false
    viewModelScope.launch {
      repository.markBackupReminderAcknowledged()
    }
  }

  fun notifyBackupExportResult(success: Boolean) {
    _toastEvents.tryEmit(
      if (success) "Kopia zapasowa została zapisana!" else "Nie udało się zapisać kopii zapasowej"
    )
  }

  fun importBackupJson(json: String) {
    viewModelScope.launch {
      val ok = repository.importFromJson(json)
      _toastEvents.tryEmit(
        if (ok) "Kopia zapasowa została wczytana!" else "Nieprawidłowy plik kopii zapasowej"
      )
    }
  }

  // --- Onboarding & Built-in Ready Tasks / Rewards Catalog ---

  fun completeOnboardingStartFromScratch() {
    viewModelScope.launch {
      repository.completeOnboardingStartFromScratch()
    }
  }

  fun completeOnboardingFromSurvey(
    areas: List<OnboardingArea>?,
    timePref: OnboardingTimePref?,
    detailPref: OnboardingDetailPref?,
    rewardPrefs: List<OnboardingRewardPref>?,
    selectedTemplateIds: List<String>,
    selectedRewardTemplateIds: List<String>,
    onDone: (Int) -> Unit = {},
  ) {
    viewModelScope.launch {
      val (addedTasks, addedRewards) = repository.completeOnboardingFromSurvey(
        areas = areas,
        timePref = timePref,
        detailPref = detailPref,
        rewardPrefs = rewardPrefs,
        selectedTemplateIds = selectedTemplateIds,
        selectedRewardTemplateIds = selectedRewardTemplateIds,
      )
      val tasksLabel = formatPolishTasksAccusative(addedTasks)
      val rewardsLabel = formatPolishRewardsAccusative(addedRewards)
      val msg = when {
        addedTasks > 0 && addedRewards > 0 ->
          "Dodano $tasksLabel do biblioteki (nieaktywne) i $rewardsLabel do sklepu."
        addedTasks > 0 ->
          "Dodano $tasksLabel do Twojej biblioteki jako nieaktywne."
        addedRewards > 0 ->
          "Dodano $rewardsLabel do sklepu."
        else -> "Gotowe! Możesz teraz samodzielnie dodawać zadania."
      }
      _toastEvents.tryEmit(msg)
      onDone(addedTasks)
    }
  }

  private fun formatPolishTasksAccusative(count: Int): String = when {
    count == 1 -> "1 zadanie"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "$count zadania"
    else -> "$count zadań"
  }

  private fun formatPolishRewardsAccusative(count: Int): String = when {
    count == 1 -> "1 nagrodę"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "$count nagrody"
    else -> "$count nagród"
  }

  suspend fun importBackupDuringOnboarding(json: String): Boolean {
    val ok = repository.importFromJson(json, markOnboardingCompleted = true)
    _toastEvents.tryEmit(
      if (ok) "Dane z kopii zapasowej zostały przywrócone!" else "Nie udało się wczytać pliku kopii zapasowej."
    )
    return ok
  }

  fun addTaskFromTemplate(template: ReadyTaskTemplate, asInactive: Boolean = true) {
    viewModelScope.launch {
      val added = repository.addTaskFromTemplate(template, asInactive = asInactive)
      _toastEvents.tryEmit(
        if (added) {
          "Dodano „${template.name}” do biblioteki zadań (nieaktywne)."
        } else {
          "To zadanie znajduje się już w Twoich zadaniach."
        }
      )
    }
  }

  fun activateLibraryTask(taskId: Long) {
    viewModelScope.launch {
      if (repository.activateLibraryTask(taskId)) {
        _toastEvents.tryEmit("Zadanie zostało aktywowane!")
      }
    }
  }

  fun addRewardFromCatalog(
    template: RewardCatalogTemplate,
    customCoinPrice: Int = template.defaultCoinPrice,
  ) {
    viewModelScope.launch {
      val added = repository.addRewardFromCatalog(template, customCoinPrice)
      _toastEvents.tryEmit(
        if (added) {
          "Dodano nagrodę „${template.name}” do sklepu."
        } else {
          "Ta nagroda jest już w Twoim sklepie."
        }
      )
    }
  }

  fun setOnboardingDetailPref(detailPref: OnboardingDetailPref?) {
    viewModelScope.launch {
      repository.setOnboardingDetailPref(detailPref)
    }
  }

  // --- Random Side Quest (Tablica Przygód) ---

  fun ensureDailyRandomSideQuestBoard() {
    viewModelScope.launch {
      val board = repository.ensureDailyRandomSideQuestBoard()
      if (pendingRandomQuestDayKey != board.dayKey) {
        pendingRandomQuestDayKey = board.dayKey
        _pendingRandomQuestIds.value = emptySet()
      } else {
        val validIds = board.quests.map { it.id }.toSet()
        _pendingRandomQuestIds.update { current ->
          current
            .filter { it in validIds && it !in board.acceptedIdsToday }
            .take(board.remainingSlots)
            .toSet()
        }
      }
    }
  }

  fun togglePendingRandomQuest(questId: String) {
    val profile = uiState.value.profile
    val todayKey = localCalendarDayKey()
    val acceptedToday = if (profile.rsqBoardDayKey == todayKey) {
      parseStringCsv(profile.rsqAcceptedIdsCsv)
    } else {
      emptySet()
    }
    val acceptedCount = if (profile.rsqBoardDayKey == todayKey) {
      maxOf(profile.rsqAcceptedCountToday, acceptedToday.size)
    } else {
      0
    }
    if (questId in acceptedToday) return

    if (pendingRandomQuestDayKey != todayKey) {
      pendingRandomQuestDayKey = todayKey
      _pendingRandomQuestIds.value = emptySet()
    }

    _pendingRandomQuestIds.update { current ->
      if (questId in current) {
        current - questId
      } else {
        val remaining = (RANDOM_SIDE_QUEST_DAILY_LIMIT - acceptedCount).coerceAtLeast(0)
        if (current.size < remaining) current + questId else current
      }
    }
  }

  fun acceptSelectedRandomQuests(onAccepted: (Int) -> Unit = {}) {
    if (_isAcceptingRandomQuests.value) return
    val idsToAccept = _pendingRandomQuestIds.value.toList()
    if (idsToAccept.isEmpty()) return

    _isAcceptingRandomQuests.value = true
    viewModelScope.launch {
      try {
        val addedCount = repository.acceptRandomSideQuests(idsToAccept)
        _pendingRandomQuestIds.value = emptySet()
        if (addedCount > 0) {
          val missionLabel = when (addedCount) {
            1 -> "1 misję"
            2, 3, 4 -> "$addedCount misje"
            else -> "$addedCount misji"
          }
          _toastEvents.tryEmit("⚔️ Przyjęto $missionLabel! Znajdziesz je w aktywnych zadaniach.")
        } else {
          _toastEvents.tryEmit("Dzienny limit Random Side Questów (3/3) został już wykorzystany.")
        }
        onAccepted(addedCount)
      } finally {
        _isAcceptingRandomQuests.value = false
      }
    }
  }
}
