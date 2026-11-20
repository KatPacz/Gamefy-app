package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AchievementDefinition
import com.example.notifications.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.components.AchievementPillBanner
import com.example.ui.components.PixelIcon
import com.example.ui.components.TopHudHeader
import kotlinx.coroutines.delay
import com.example.ui.screens.ChestScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReadyTasksDatabaseScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.TaskEditorScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.GamefyTheme
import com.example.ui.theme.LocalGamefyPalette
import com.example.ui.theme.PixelFontFamily

private const val ROUTE_START = "start"
private const val ROUTE_TASKS = "tasks"
private const val ROUTE_SHOP = "shop"
private const val ROUTE_CHEST = "chest"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_TASK_EDIT = "task_edit/{taskId}"
private const val ROUTE_READY_TASKS = "ready_tasks"

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
  NavTab(ROUTE_PROFILE, "trophy", "Profil"),
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
  val snackbarHostState = remember { SnackbarHostState() }
  val achievementQueue = remember { mutableStateListOf<AchievementDefinition>() }
  val currentAchievementBanner = achievementQueue.firstOrNull()
  var scrollToAchievementsNonce by remember { mutableIntStateOf(0) }
  var openLibraryNonce by remember { mutableIntStateOf(0) }
  var pendingOpenTasksAfterOnboarding by remember { mutableStateOf(false) }
  var showExitTimerDialog by remember { mutableStateOf(false) }
  var showRandomSideQuestDialog by remember { mutableStateOf(false) }

  val activity = LocalContext.current as? Activity
  val anyTimerRunning = mainTimer.isRunning || taskTimer.isRunning

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
    viewModel.toastEvents.collect { msg ->
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(msg)
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

  GamefyTheme(themeId = uiState.themeId) {
    val palette = LocalGamefyPalette.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_START
    val isTopTab = NAV_TABS.any { it.route == currentRoute }

    LaunchedEffect(requestedRoute, uiState.isLoaded) {
      if (uiState.isLoaded && requestedRoute == ROUTE_TASKS) {
        if (currentRoute != ROUTE_TASKS) {
          navController.navigate(ROUTE_TASKS) {
            popUpTo(ROUTE_START) { saveState = true }
            launchSingleTop = true
            restoreState = true
          }
        }
        onConsumeRequestedRoute()
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

    val levelProgress = remember(uiState.profile.exp) {
      com.example.data.model.calculateLevel(uiState.profile.exp)
    }

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
            Snackbar(
              snackbarData = data,
              containerColor = palette.outline,
              contentColor = Color.White,
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
                  icon = { PixelIcon(icon = tab.pixelIcon, size = 28.dp) },
                  label = {
                    Text(
                      text = tab.label,
                      fontFamily = PixelFontFamily,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 11.sp,
                    )
                  },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.accent,
                    selectedTextColor = palette.accent,
                    indicatorColor = palette.accent.copy(alpha = 0.16f),
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
                  icon = { PixelIcon(icon = tab.pixelIcon, size = 28.dp) },
                  label = {
                    Text(
                      text = tab.label,
                      fontFamily = PixelFontFamily,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 11.sp,
                    )
                  },
                  colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = palette.accent,
                    selectedTextColor = palette.accent,
                    indicatorColor = palette.accent.copy(alpha = 0.16f),
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
                onToggleMainTimer = viewModel::toggleMainTimer,
                onSkipMainTimer = viewModel::skipMainTimerPhase,
                onResetMainTimer = viewModel::resetMainTimer,
                onCompleteTask = viewModel::completeTask,
                onToggleSubtask = viewModel::toggleSubtask,
                onSkipTask = viewModel::skipTask,
                onGiveUpTask = viewModel::giveUpTask,
                onEditTask = { taskId -> navController.navigate("task_edit/$taskId") },
                onToggleTaskTimer = viewModel::toggleTaskTimer,
                onResetTaskTimer = viewModel::resetTaskTimer,
              )
            }

            composable(ROUTE_TASKS) {
              TasksScreen(
                uiState = uiState,
                inlineTimer = taskTimer,
                onCreateNewTask = { navController.navigate("task_edit/0") },
                onEditTask = { taskId -> navController.navigate("task_edit/$taskId") },
                onCompleteTask = viewModel::completeTask,
                onToggleSubtask = viewModel::toggleSubtask,
                onSkipTask = viewModel::skipTask,
                onGiveUpTask = viewModel::giveUpTask,
                onApplyBulkUpdate = viewModel::applyBulkUpdate,
                onToggleTaskTimer = viewModel::toggleTaskTimer,
                onResetTaskTimer = viewModel::resetTaskTimer,
                onDismissFocusView = viewModel::dismissTaskFocusView,
                onActivateTask = viewModel::activateLibraryTask,
                onOpenRandomQuest = { showRandomSideQuestDialog = true },
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
              )
            }

            composable(ROUTE_PROFILE) {
              ProfileScreen(
                uiState = uiState,
                scrollToAchievementsTrigger = scrollToAchievementsNonce,
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
                onToggleNotifications = viewModel::setNotificationsEnabled,
                onUpdateNotificationSettings = viewModel::updateNotificationSettings,
                onExportBackup = viewModel::exportBackupJson,
                onImportBackup = viewModel::importBackupJson,
                onOpenReadyTasksDb = { navController.navigate(ROUTE_READY_TASKS) },
                onOpenRandomQuest = { showRandomSideQuestDialog = true },
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
                onSaveTask = viewModel::saveTask,
                onDeleteTask = viewModel::deleteTask,
                onBack = { navController.popBackStack() },
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
          mainTimer.isRunning && taskTimer.isRunning -> "Główny timer oraz czasomierz zadania wciąż działają."
          mainTimer.isRunning -> "Timer (${mainTimer.mode.label}) wciąż odlicza czas."
          else -> "Czasomierz zadania wciąż odlicza czas."
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

      if (showRandomSideQuestDialog) {
        AlertDialog(
          onDismissRequest = { showRandomSideQuestDialog = false },
          title = { Text("Random Side Quest") },
          text = { Text("Sekcja gotowa do skonfigurowania.") },
          confirmButton = {
            TextButton(
              modifier = Modifier.testTag("close_random_side_quest_dialog"),
              onClick = { showRandomSideQuestDialog = false },
            ) {
              Text("Zamknij")
            }
          },
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
