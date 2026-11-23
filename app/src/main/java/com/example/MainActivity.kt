package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AchievementDefinition
import com.example.notifications.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.components.AchievementPillBanner
import com.example.ui.components.GuidedTutorialOverlay
import com.example.ui.components.GuidedTutorialStep
import com.example.ui.components.LocalTutorialTargetRegistry
import com.example.ui.components.PixelIcon
import com.example.ui.components.TopHudHeader
import com.example.ui.components.TutorialTargetRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.ui.screens.ChestScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RandomSideQuestScreen
import com.example.ui.screens.ReadyTasksDatabaseScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.TaskEditorScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.GamefyTheme
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelColors
import com.example.ui.theme.PixelFontFamily

private const val ROUTE_START = "start"
private const val ROUTE_TASKS = "tasks"
private const val ROUTE_SHOP = "shop"
private const val ROUTE_CHEST = "chest"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_TASK_EDIT = "task_edit/{taskId}"
private const val ROUTE_READY_TASKS = "ready_tasks"
private const val ROUTE_ADVENTURE_BOARD = "adventure_board"

private data class NavTab(
  val route: String,
  val pixelIcon: String,
  val label: String,
)

private val NAV_TABS = listOf(
  NavTab(ROUTE_START, "timer", "Start"),
  NavTab(ROUTE_TASKS, "check", "Zadania"),
  NavTab(ROUTE_SHOP, "bag", "Sklep"),
  NavTab(ROUTE_CHEST, "chest", "Skrzynia"),
  NavTab(ROUTE_PROFILE, "hero", "Profil"),
)

class MainActivity : ComponentActivity() {
  private var requestedRouteState by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestedRouteState = intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_ROUTE)
    enableEdgeToEdge()
    setContent {
      GamefyRootScreen(
        requestedRoute = requestedRouteState,
        onConsumeRequestedRoute = { requestedRouteState = null },
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    requestedRouteState = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_ROUTE)
  }
}

@Composable
fun GamefyRootScreen(
  viewModel: MainViewModel = viewModel(),
  requestedRoute: String? = null,
  onConsumeRequestedRoute: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val mainTimer by viewModel.mainTimer.collectAsStateWithLifecycle()
  val taskTimer by viewModel.taskTimer.collectAsStateWithLifecycle()
  val pendingRandomQuestIds by viewModel.pendingRandomQuestIds.collectAsStateWithLifecycle()
  val isAcceptingRandomQuests by viewModel.isAcceptingRandomQuests.collectAsStateWithLifecycle()
  val showWeeklyBackupReminder by viewModel.showWeeklyBackupReminder.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val achievementQueue = remember { mutableStateListOf<AchievementDefinition>() }
  val currentAchievementBanner = achievementQueue.firstOrNull()
  var scrollToAchievementsNonce by remember { mutableIntStateOf(0) }
  var scrollToNotificationsNonce by remember { mutableIntStateOf(0) }
  var scrollToBackupNonce by remember { mutableIntStateOf(0) }
  var openLibraryNonce by remember { mutableIntStateOf(0) }
  var pendingOpenTasksAfterOnboarding by remember { mutableStateOf(false) }
  var showExitTimerDialog by remember { mutableStateOf(false) }
  var activeTutorialStep by remember { mutableStateOf<GuidedTutorialStep?>(null) }

  val context = LocalContext.current
  val activity = context as? Activity
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val anyTimerRunning = mainTimer.isRunning || taskTimer.isRunning

  val quickBackupExportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val json = viewModel.exportBackupJson()
        val ok = runCatching {
          context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
          } != null
        }.getOrDefault(false)
        viewModel.notifyBackupExportResult(ok)
      }
    }
  }

  var askedTimerNotifPermission by rememberSaveable { mutableStateOf(false) }
  val timerNotificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      viewModel.refreshOngoingTimerNotification()
    }
  }

  LaunchedEffect(uiState.isLoaded, uiState.profile.onboardingCompleted) {
    if (uiState.isLoaded && uiState.profile.onboardingCompleted) {
      viewModel.checkWeeklyBackupReminder()
    }
  }

  DisposableEffect(lifecycleOwner, uiState.isLoaded, uiState.profile.onboardingCompleted) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.syncTimerFromWidgetStore()
        if (uiState.isLoaded && uiState.profile.onboardingCompleted) {
          viewModel.checkWeeklyBackupReminder()
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  DisposableEffect(anyTimerRunning, activity) {
    if (anyTimerRunning) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.snackbarEvents.collectLatest { event ->
      snackbarHostState.currentSnackbarData?.dismiss()
      val customDurationMs = event.durationMillis
      if (customDurationMs != null) {
        val autoDismissJob = launch {
          delay(customDurationMs)
          snackbarHostState.currentSnackbarData?.dismiss()
        }
        try {
          val result = snackbarHostState.showSnackbar(
            message = event.message,
            actionLabel = event.actionLabel,
            duration = SnackbarDuration.Indefinite,
          )
          if (result == SnackbarResult.ActionPerformed && event.undoSnapshot != null) {
            if (event.unlockedAchievementIdsToRevert.isNotEmpty()) {
              achievementQueue.removeAll { it.id in event.unlockedAchievementIdsToRevert }
            }
            viewModel.undoTaskAction(event.undoSnapshot)
          }
        } finally {
          autoDismissJob.cancel()
        }
      } else {
        snackbarHostState.showSnackbar(
          message = event.message,
          actionLabel = event.actionLabel,
          duration = SnackbarDuration.Short,
        )
      }
    }
  }

  LaunchedEffect(Unit) {
    viewModel.achievementEvents.collect { ach ->
      achievementQueue.add(ach)
    }
  }

  LaunchedEffect(currentAchievementBanner?.id) {
    if (currentAchievementBanner != null) {
      delay(4_500L)
      if (achievementQueue.isNotEmpty()) {
        achievementQueue.removeAt(0)
      }
    }
  }

  val isSystemDark = isSystemInDarkTheme()
  val effectiveThemeId = uiState.resolvedThemeId(isSystemDark)

  GamefyTheme(
    themeId = effectiveThemeId,
    fontSize = uiState.fontSize,
    fontFamilyOption = uiState.fontFamilyOption,
    wordSpacing = uiState.wordSpacing,
  ) {
    val palette = LocalGamefyPalette.current
    val view = LocalView.current
    DisposableEffect(palette.isDark, view, activity) {
      val window = activity?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !palette.isDark
        controller.isAppearanceLightNavigationBars = !palette.isDark
      }
      onDispose {}
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_START
    val isTopTab = NAV_TABS.any { it.route == currentRoute }

    LaunchedEffect(requestedRoute, uiState.isLoaded, uiState.profile.onboardingCompleted) {
      val target = requestedRoute ?: return@LaunchedEffect
      if (!uiState.isLoaded) return@LaunchedEffect
      if (!uiState.profile.onboardingCompleted) return@LaunchedEffect
      when {
        target == ROUTE_TASKS || target == ROUTE_START || target == ROUTE_SHOP ||
          target == ROUTE_CHEST || target == ROUTE_PROFILE -> {
          if (currentRoute != target) {
            navController.navigate(target) {
              popUpTo(ROUTE_START) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          }
          onConsumeRequestedRoute()
        }
        target.startsWith("task_edit/") -> {
          navController.navigate(target) {
            launchSingleTop = true
          }
          onConsumeRequestedRoute()
        }
        else -> onConsumeRequestedRoute()
      }
    }

    if (!uiState.isLoaded) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(palette.background),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(color = palette.accent)
      }
      return@GamefyTheme
    }

    if (!uiState.profile.onboardingCompleted) {
      val hasExistingData = uiState.tasks.isNotEmpty() ||
        uiState.shopItems.isNotEmpty() ||
        uiState.profile.exp > 0 ||
        uiState.profile.coins > 0
      OnboardingScreen(
        hasExistingData = hasExistingData,
        onStartFromScratch = viewModel::completeOnboardingStartFromScratch,
        onCompleteSurvey = { areas, timePref, detailPref, rewardPrefs, selectedTaskIds, selectedRewardIds ->
          viewModel.completeOnboardingFromSurvey(
            areas = areas,
            timePref = timePref,
            detailPref = detailPref,
            rewardPrefs = rewardPrefs,
            selectedTemplateIds = selectedTaskIds,
            selectedRewardTemplateIds = selectedRewardIds,
            onDone = { addedTasks ->
              if (addedTasks > 0) {
                openLibraryNonce++
                pendingOpenTasksAfterOnboarding = true
              }
            },
          )
        },
        onRestoreFromJson = viewModel::importBackupDuringOnboarding,
      )
      return@GamefyTheme
    }

    LaunchedEffect(pendingOpenTasksAfterOnboarding, uiState.profile.onboardingCompleted) {
      if (pendingOpenTasksAfterOnboarding && uiState.profile.onboardingCompleted) {
        pendingOpenTasksAfterOnboarding = false
        navController.navigate(ROUTE_TASKS) {
          popUpTo(ROUTE_START) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
      }
    }

    // Guided post-onboarding tutorial lifecycle (active only after "Zacznij od zera" or Survey)
    val starterInactiveTask = remember(uiState.tasks) {
      uiState.tasks.find { !it.isActive && it.name == com.example.data.repository.GamefyRepository.STARTER_TUTORIAL_TASK_NAME }
        ?: uiState.tasks.firstOrNull { !it.isActive }
    }

    fun navigateTopTabForTutorial(route: String) {
      navController.navigate(route) {
        popUpTo(ROUTE_START) { saveState = false }
        launchSingleTop = true
        restoreState = false
      }
    }

    fun goToTutorialStep(step: GuidedTutorialStep) {
      activeTutorialStep = step
      when (step) {
        GuidedTutorialStep.TASKS_AND_INACTIVE -> {
          openLibraryNonce++
          navigateTopTabForTutorial(ROUTE_TASKS)
        }
        GuidedTutorialStep.TASK_EDITOR -> {
          val targetId = starterInactiveTask?.id ?: uiState.tasks.firstOrNull()?.id ?: 0L
          if (currentRoute != ROUTE_TASK_EDIT) {
            navController.navigate("task_edit/$targetId") { launchSingleTop = true }
          }
        }
        GuidedTutorialStep.ACTIVATE_AND_ADD -> {
          openLibraryNonce++
          navigateTopTabForTutorial(ROUTE_TASKS)
        }
        GuidedTutorialStep.SIDE_QUESTS -> {
          navController.navigate(ROUTE_ADVENTURE_BOARD) { launchSingleTop = true }
        }
        GuidedTutorialStep.SHOP -> {
          navigateTopTabForTutorial(ROUTE_SHOP)
        }
        GuidedTutorialStep.CHEST -> {
          navigateTopTabForTutorial(ROUTE_CHEST)
        }
        GuidedTutorialStep.READY_TASKS -> {
          navController.navigate(ROUTE_READY_TASKS) { launchSingleTop = true }
        }
        GuidedTutorialStep.SETTINGS_AND_BACKUP -> {
          scrollToBackupNonce++
          navigateTopTabForTutorial(ROUTE_PROFILE)
        }
      }
    }

    LaunchedEffect(uiState.profile.tutorialPending, uiState.profile.onboardingCompleted) {
      if (uiState.profile.onboardingCompleted && uiState.profile.tutorialPending && activeTutorialStep == null) {
        goToTutorialStep(GuidedTutorialStep.TASKS_AND_INACTIVE)
      } else if (!uiState.profile.tutorialPending && activeTutorialStep != null) {
        activeTutorialStep = null
      }
    }

    val levelProgress = remember(uiState.profile.exp) {
      com.example.data.model.calculateLevel(uiState.profile.exp)
    }
    val tutorialTargetRegistry = remember { TutorialTargetRegistry() }

    CompositionLocalProvider(LocalTutorialTargetRegistry provides tutorialTargetRegistry) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .background(palette.background),
    ) {
      val useNavigationRail = maxWidth >= 600.dp

      Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
          SnackbarHost(hostState = snackbarHostState) { data ->
            val snackbarBg = if (palette.isDark) palette.surfaceSubtle else palette.outline
            val snackbarFg = if (palette.isDark) palette.textPrimary else Color.White
            val snackbarActionColor = if (palette.isHighContrast && palette.isDark) {
              palette.accent
            } else {
              PixelColors.Gold
            }
            Snackbar(
              snackbarData = data,
              containerColor = snackbarBg,
              contentColor = snackbarFg,
              actionColor = snackbarActionColor,
            )
          }
        },
        topBar = {
          if (isTopTab) {
            TopHudHeader(
              levelProgress = levelProgress,
              coins = uiState.profile.coins,
              gems = uiState.profile.gems,
              modifier = Modifier
                .background(palette.background)
                .statusBarsPadding(),
            )
          }
        },
        bottomBar = {
          if (isTopTab && !useNavigationRail) {
            NavigationBar(
              containerColor = palette.surface,
              modifier = Modifier.navigationBarsPadding(),
            ) {
              NAV_TABS.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    if (currentRoute != tab.route) {
                      navController.navigate(tab.route) {
                        popUpTo(ROUTE_START) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                  icon = {
                    PixelIcon(
                      icon = tab.pixelIcon,
                      size = 26.dp,
                      monochromeTint = if (selected) null else palette.textMuted,
                    )
                  },
                  label = {
                    Text(
                      text = tab.label,
                      fontFamily = PixelFontFamily,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 12.sp,
                      maxLines = 1,
                    )
                  },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.accent,
                    selectedTextColor = palette.accent,
                    indicatorColor = palette.accent.copy(alpha = 0.18f),
                    unselectedIconColor = palette.textMuted,
                    unselectedTextColor = palette.textMuted,
                  ),
                  modifier = Modifier.testTag("nav_${tab.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          if (isTopTab && useNavigationRail) {
            NavigationRail(
              containerColor = palette.surface,
              modifier = Modifier
                .fillMaxHeight()
                .navigationBarsPadding(),
            ) {
              NAV_TABS.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    if (currentRoute != tab.route) {
                      navController.navigate(tab.route) {
                        popUpTo(ROUTE_START) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                  icon = {
                    PixelIcon(
                      icon = tab.pixelIcon,
                      size = 26.dp,
                      monochromeTint = if (selected) null else palette.textMuted,
                    )
                  },
                  label = {
                    Text(
                      text = tab.label,
                      fontFamily = PixelFontFamily,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 12.sp,
                      maxLines = 1,
                    )
                  },
                  colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = palette.accent,
                    selectedTextColor = palette.accent,
                    indicatorColor = palette.accent.copy(alpha = 0.18f),
                    unselectedIconColor = palette.textMuted,
                    unselectedTextColor = palette.textMuted,
                  ),
                  modifier = Modifier.testTag("nav_${tab.route}"),
                )
              }
            }
          }

          NavHost(
            navController = navController,
            startDestination = ROUTE_START,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
          ) {
            composable(ROUTE_START) {
              HomeScreen(
                uiState = uiState,
                timerState = mainTimer,
                inlineTimer = taskTimer,
                onSelectTimerMode = viewModel::setMainTimerMode,
                onSelectTimerPreset = viewModel::setTimerPreset,
                onUpdateCustomDuration = viewModel::updateCustomDuration,
                onToggleMainTimer = {
                  val startingNow = !mainTimer.isRunning
                  viewModel.toggleMainTimer()
                  if (
                    startingNow &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationHelper.hasNotificationPermission(context) &&
                    !askedTimerNotifPermission
                  ) {
                    askedTimerNotifPermission = true
                    timerNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                },
                onSkipMainTimer = viewModel::skipMainTimerPhase,
                onResetMainTimer = viewModel::resetMainTimer,
                onCompleteTask = viewModel::completeTask,
                onToggleSubtask = viewModel::toggleSubtask,
                onSkipTask = viewModel::skipTask,
                onGiveUpTask = viewModel::giveUpTask,
                onEditTask = { taskId -> navController.navigate("task_edit/$taskId") },
                onToggleTaskTimer = viewModel::toggleTaskTimer,
                onResetTaskTimer = viewModel::resetTaskTimer,
                onCreateNewTask = { navController.navigate("task_edit/0") },
                onOpenReadyTasksDb = {
                  navController.navigate(ROUTE_READY_TASKS) { launchSingleTop = true }
                },
                onOpenRandomQuest = {
                  navController.navigate(ROUTE_ADVENTURE_BOARD) { launchSingleTop = true }
                },
              )
            }

            composable(ROUTE_TASKS) {
              TasksScreen(
                uiState = uiState,
                inlineTimer = taskTimer,
                onCreateNewTask = { navController.navigate("task_edit/0") },
                onEditTask = { taskId ->
                  if (activeTutorialStep == GuidedTutorialStep.TASKS_AND_INACTIVE) {
                    activeTutorialStep = GuidedTutorialStep.TASK_EDITOR
                  }
                  navController.navigate("task_edit/$taskId")
                },
                onCompleteTask = viewModel::completeTask,
                onToggleSubtask = viewModel::toggleSubtask,
                onSkipTask = viewModel::skipTask,
                onGiveUpTask = viewModel::giveUpTask,
                onApplyBulkUpdate = viewModel::applyBulkUpdate,
                onToggleTaskTimer = viewModel::toggleTaskTimer,
                onResetTaskTimer = viewModel::resetTaskTimer,
                onDismissFocusView = viewModel::dismissTaskFocusView,
                onActivateTask = { taskId ->
                  viewModel.activateLibraryTask(taskId)
                  if (activeTutorialStep == GuidedTutorialStep.ACTIVATE_AND_ADD) {
                    goToTutorialStep(GuidedTutorialStep.SIDE_QUESTS)
                  }
                },
                onOpenRandomQuest = {
                  navController.navigate(ROUTE_ADVENTURE_BOARD) { launchSingleTop = true }
                },
                onOpenReadyTasksDb = {
                  navController.navigate(ROUTE_READY_TASKS) { launchSingleTop = true }
                },
                openLibraryTrigger = openLibraryNonce,
              )
            }

            composable(ROUTE_SHOP) {
              ShopScreen(
                uiState = uiState,
                countPurchasesInPeriod = viewModel::countPurchasesInPeriod,
                onBuyItem = viewModel::buyShopItem,
                onSaveItem = viewModel::saveShopItem,
                onDeleteItem = viewModel::deleteShopItem,
                onCompressImageUri = viewModel::compressImageUri,
                onAddRewardFromCatalog = viewModel::addRewardFromCatalog,
              )
            }

            composable(ROUTE_CHEST) {
              ChestScreen(
                uiState = uiState,
                onUseItem = viewModel::useInventoryItem,
                onOpenShop = {
                  navController.navigate(ROUTE_SHOP) {
                    popUpTo(ROUTE_START) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
              )
            }

            composable(ROUTE_PROFILE) {
              ProfileScreen(
                uiState = uiState,
                scrollToAchievementsTrigger = scrollToAchievementsNonce,
                scrollToNotificationsTrigger = scrollToNotificationsNonce,
                scrollToBackupTrigger = scrollToBackupNonce,
                onAddAttribute = viewModel::addAttribute,
                onUpdateAttribute = viewModel::updateAttribute,
                onDeleteAttribute = viewModel::deleteAttribute,
                onAddFolder = viewModel::addFolder,
                onUpdateFolder = viewModel::updateFolder,
                onDeleteFolder = viewModel::deleteFolder,
                onCreateOrFindTag = viewModel::getOrCreateTag,
                onUpdateTag = viewModel::updateTag,
                onDeleteTag = viewModel::deleteTag,
                onSelectTheme = viewModel::setTheme,
                onSelectSystemTheme = viewModel::setSystemTheme,
                onSelectFontSize = viewModel::setFontSize,
                onSelectFontFamily = viewModel::setFontFamily,
                onSelectWordSpacing = viewModel::setWordSpacing,
                onToggleNotifications = viewModel::setNotificationsEnabled,
                onUpdateNotificationSettings = viewModel::updateNotificationSettings,
                onExportBackup = viewModel::exportBackupJson,
                onBackupExportResult = viewModel::notifyBackupExportResult,
                onImportBackup = viewModel::importBackupJson,
                onOpenReadyTasksDb = { navController.navigate(ROUTE_READY_TASKS) },
                onOpenRandomQuest = {
                  navController.navigate(ROUTE_ADVENTURE_BOARD) { launchSingleTop = true }
                },
              )
            }

            composable(ROUTE_ADVENTURE_BOARD) {
              RandomSideQuestScreen(
                profile = uiState.profile,
                pendingSelectedIds = pendingRandomQuestIds,
                isAccepting = isAcceptingRandomQuests,
                onEnsureDailyBoard = viewModel::ensureDailyRandomSideQuestBoard,
                onToggleSelect = viewModel::togglePendingRandomQuest,
                onAcceptSelected = {
                  viewModel.acceptSelectedRandomQuests { addedCount ->
                    if (addedCount > 0) {
                      navController.popBackStack(ROUTE_ADVENTURE_BOARD, inclusive = true)
                    }
                  }
                },
                onNavigateBack = { navController.popBackStack() },
              )
            }

            composable(ROUTE_READY_TASKS) {
              val existingTemplateIds = remember(uiState.tasks) {
                uiState.tasks.mapNotNull { it.sourceTemplateId }.toSet()
              }
              ReadyTasksDatabaseScreen(
                existingSourceTemplateIds = existingTemplateIds,
                onAddTemplate = { tpl ->
                  viewModel.addTaskFromTemplate(tpl, asInactive = true)
                  openLibraryNonce++
                },
                onNavigateBack = { navController.popBackStack() },
              )
            }

            composable(
              route = ROUTE_TASK_EDIT,
              arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) { entry ->
              val taskId = entry.arguments?.getLong("taskId") ?: 0L
              val existingTask = uiState.tasks.find { it.id == taskId }
              TaskEditorScreen(
                existingTask = existingTask,
                uiState = uiState,
                onCreateOrFindTag = viewModel::getOrCreateTag,
                onSaveTask = { editedTask ->
                  viewModel.saveTask(editedTask)
                  if (activeTutorialStep == GuidedTutorialStep.TASK_EDITOR) {
                    activeTutorialStep = GuidedTutorialStep.ACTIVATE_AND_ADD
                    openLibraryNonce++
                  }
                },
                onDeleteTask = viewModel::deleteTask,
                onOpenNotificationSettings = {
                  scrollToNotificationsNonce++
                  navController.navigate(ROUTE_PROFILE) {
                    launchSingleTop = true
                  }
                },
                onBack = {
                  if (
                    activeTutorialStep == GuidedTutorialStep.ACTIVATE_AND_ADD &&
                    navController.previousBackStackEntry?.destination?.route != ROUTE_TASKS
                  ) {
                    navigateTopTabForTutorial(ROUTE_TASKS)
                  } else {
                    navController.popBackStack()
                  }
                },
              )
            }
          }
        }
      }

      // Placed after NavHost so it intercepts root back-presses before Activity exits
      BackHandler(enabled = anyTimerRunning && currentRoute == ROUTE_START) {
        showExitTimerDialog = true
      }

      if (showExitTimerDialog) {
        val activeDescription = when {
          mainTimer.isRunning && taskTimer.isRunning -> "Główny timer oraz timer zadania wciąż działają."
          mainTimer.isRunning -> "Timer (${mainTimer.mode.label}) wciąż odlicza czas."
          else -> "Timer zadania wciąż odlicza czas."
        }
        AlertDialog(
          onDismissRequest = { showExitTimerDialog = false },
          title = { Text("Timer nadal działa") },
          text = {
            Text(
              "$activeDescription Czy chcesz przerwać odliczanie (zatrzymać i zresetować timer), aby wyjść z aplikacji?"
            )
          },
          confirmButton = {
            TextButton(
              modifier = Modifier.testTag("confirm_stop_and_exit_button"),
              onClick = {
                viewModel.stopAndResetAllTimers()
                showExitTimerDialog = false
                activity?.finish()
              },
            ) {
              Text("Przerwij i wyjdź", color = com.example.ui.theme.PixelColors.Red)
            }
          },
          dismissButton = {
            TextButton(
              modifier = Modifier.testTag("cancel_exit_button"),
              onClick = { showExitTimerDialog = false },
            ) {
              Text("Zostań")
            }
          },
        )
      }

      if (showWeeklyBackupReminder) {
        AlertDialog(
          onDismissRequest = { viewModel.dismissWeeklyBackupReminder() },
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              PixelIcon(icon = "save", size = 22.dp)
              Text("Pamiętaj o kopii zapasowej")
            }
          },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                "Ta aplikacja nie ma automatycznej kopii zapasowej. Pamiętaj, żeby regularnie robić kopię zapasową (Profil → Kopia zapasowa), aby nie stracić swoich postępów."
              )
              OutlinedButton(
                onClick = { viewModel.snoozeWeeklyBackupReminder30Days() },
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("weekly_backup_snooze_30_days_button"),
              ) {
                Text("Nie przypominaj przez 30 dni")
              }
            }
          },
          confirmButton = {
            TextButton(
              modifier = Modifier.testTag("weekly_backup_export_now_button"),
              onClick = {
                viewModel.dismissWeeklyBackupReminder()
                quickBackupExportLauncher.launch("gamefy-backup.json")
              },
            ) {
              Text("Zrób kopię teraz", fontWeight = FontWeight.Bold)
            }
          },
          dismissButton = {
            TextButton(
              modifier = Modifier.testTag("weekly_backup_dismiss_button"),
              onClick = { viewModel.dismissWeeklyBackupReminder() },
            ) {
              Text("Później")
            }
          },
        )
      }

      val tutorialStepNow = activeTutorialStep
      if (tutorialStepNow != null) {
        val steps = GuidedTutorialStep.entries
        val currentIdx = steps.indexOf(tutorialStepNow)
        GuidedTutorialOverlay(
          step = tutorialStepNow,
          hasStarterTaskToEdit = starterInactiveTask != null || uiState.tasks.isNotEmpty(),
          showBottomNavOffset = isTopTab && !useNavigationRail,
          onPrimaryAction = {
            if (currentIdx + 1 < steps.size) {
              goToTutorialStep(steps[currentIdx + 1])
            } else {
              activeTutorialStep = null
              viewModel.markTutorialCompleted()
              navigateTopTabForTutorial(ROUTE_TASKS)
            }
          },
          onPrevious = if (currentIdx > 0) {
            { goToTutorialStep(steps[currentIdx - 1]) }
          } else {
            null
          },
          onSkip = {
            activeTutorialStep = null
            viewModel.markTutorialCompleted()
          },
          modifier = Modifier.fillMaxSize(),
        )
      }

      AnimatedVisibility(
        visible = currentAchievementBanner != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(horizontal = 14.dp, vertical = 10.dp),
      ) {
        currentAchievementBanner?.let { ach ->
          AchievementPillBanner(
            achievement = ach,
            onClick = {
              if (achievementQueue.isNotEmpty()) {
                achievementQueue.removeAt(0)
              }
              scrollToAchievementsNonce++
              if (currentRoute != ROUTE_PROFILE) {
                navController.navigate(ROUTE_PROFILE) {
                  popUpTo(ROUTE_START) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              }
            },
          )
        }
      }
    }
    }
  }
}
