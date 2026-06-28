package com.replog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.replog.ui.exercise.ExerciseLibraryScreen
import com.replog.ui.history.HistoryScreen
import com.replog.ui.home.HomeScreen
import com.replog.ui.onboarding.OnboardingFlow
import com.replog.ui.onboarding.OnboardingViewModel
import com.replog.ui.progress.ProgressScreen
import com.replog.ui.settings.SettingsScreen
import com.replog.ui.trainingdna.TrainingDnaInsightScreen
import com.replog.ui.workout.ActiveWorkoutScreen

sealed class RepLogRoute(val route: String, val label: String, val icon: ImageVector) {
    // Primary tabs (bottom navigation)
    data object Home : RepLogRoute("home", "Home", Icons.Default.Home)
    data object Workout : RepLogRoute("workout", "Training", Icons.Default.FitnessCenter)
    // Recommendation deep link: opens the Training screen and consumes the staged
    // Coach recommendation once. Distinct from the Workout tab so it can never be
    // resurfaced by tab state restoration (the "Home loops to Training" bug).
    data object WorkoutRecommendation : RepLogRoute("workout_recommendation", "Training", Icons.Default.FitnessCenter)
    data object Progress : RepLogRoute("progress", "Progress", Icons.Default.ShowChart)
    data object History : RepLogRoute("history", "History", Icons.Default.History)
    data object Exercises : RepLogRoute("exercises", "Exercises", Icons.Default.List)
    data object Settings : RepLogRoute("settings", "Settings", Icons.Default.Settings)
    // Secondary screens (reached from cards/buttons; show a back arrow, no bottom bar)
    data object TrainingDna : RepLogRoute("training_dna", "Training DNA", Icons.Default.ShowChart)
    data object CoachHistory : RepLogRoute("coach_history", "Coach History", Icons.Default.History)
    data object Goals : RepLogRoute("goals", "Goals", Icons.Default.Flag)
    data object QuickWorkouts : RepLogRoute("quick_workouts", "Quick Workouts", Icons.Default.FitnessCenter)
    data object RecoveryCentre : RepLogRoute("recovery_centre", "Recovery Centre", Icons.Default.ShowChart)
    data object MuscleBalance : RepLogRoute("muscle_balance", "Muscle Balance", Icons.Default.ShowChart)
    data object DnaEvolution : RepLogRoute("dna_evolution", "DNA Evolution", Icons.Default.ShowChart)
    // Sprint 12: Legal Centre (secondary screens reached from Settings)
    data object LegalDisclaimer : RepLogRoute("legal_disclaimer", "Health & Safety Disclaimer", Icons.Default.Settings)
    data object LegalTerms : RepLogRoute("legal_terms", "Terms of Use", Icons.Default.Settings)
    data object LegalPrivacy : RepLogRoute("legal_privacy", "Privacy Policy", Icons.Default.Settings)
    data object LegalHistory : RepLogRoute("legal_history", "Acceptance History", Icons.Default.History)
    data object OpenSourceLicences : RepLogRoute("open_source_licences", "Open Source Licences", Icons.Default.List)
}

/** Bottom-navigation tabs, in order. */
private val bottomTabs = listOf(
    RepLogRoute.Home,
    RepLogRoute.Workout,
    RepLogRoute.Progress,
    RepLogRoute.History,
    RepLogRoute.Exercises,
    RepLogRoute.Settings
)

/** Base route (ignoring query args) of every primary tab. */
private val tabRoutes = bottomTabs.map { it.route }.toSet()

/** Secondary screens that get a back-arrow TopAppBar instead of the bottom bar. */
private val secondaryTitles = mapOf(
    RepLogRoute.TrainingDna.route to "Training DNA",
    RepLogRoute.CoachHistory.route to "Coach History",
    RepLogRoute.Goals.route to "Goals",
    RepLogRoute.QuickWorkouts.route to "Quick Workouts",
    RepLogRoute.RecoveryCentre.route to "Recovery Centre",
    RepLogRoute.MuscleBalance.route to "Muscle Balance",
    RepLogRoute.DnaEvolution.route to "DNA Evolution",
    RepLogRoute.LegalDisclaimer.route to "Health & Safety Disclaimer",
    RepLogRoute.LegalTerms.route to "Terms of Use",
    RepLogRoute.LegalPrivacy.route to "Privacy Policy",
    RepLogRoute.LegalHistory.route to "Acceptance History",
    RepLogRoute.OpenSourceLicences.route to "Open Source Licences"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepLogNavGraph(
    navController: NavHostController = rememberNavController(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    rootGateViewModel: com.replog.ui.navigation.RootGateViewModel = hiltViewModel()
) {
    val gate by rootGateViewModel.state.collectAsState()

    // Single app-entry gate. Home (the NavHost) is NOT composed until the user
    // has a profile AND has accepted the current legal documents. A legal version
    // bump flips the gate to NEEDS_REACCEPTANCE while all stored data is preserved.
    when (gate.gate) {
        null -> {
            // Still loading persisted state: render nothing to avoid flashing the
            // onboarding flow to an already-onboarded user.
            return
        }
        com.replog.util.legal.GateState.NEEDS_ONBOARDING -> {
            OnboardingFlow(viewModel = onboardingViewModel)
            return
        }
        com.replog.util.legal.GateState.NEEDS_REACCEPTANCE -> {
            // Existing user must re-accept updated documents; profile untouched.
            androidx.compose.runtime.LaunchedEffect(gate.displayName) {
                onboardingViewModel.startReacceptance(gate.displayName)
            }
            OnboardingFlow(viewModel = onboardingViewModel)
            return
        }
        com.replog.util.legal.GateState.READY -> Unit // fall through to the app
    }

    val entry by navController.currentBackStackEntryAsState()
    val currentBase = entry?.destination?.route?.substringBefore("?")
    // The recommendation deep link is part of the Training experience, so it
    // shows the bottom bar (Training tab highlighted) rather than being a
    // trapped, bar-less screen.
    val isPrimaryTab = currentBase in tabRoutes || currentBase == RepLogRoute.WorkoutRecommendation.route
    val secondaryTitle = secondaryTitles[currentBase]

    // Switching to a primary tab must always behave like a bottom-bar tab switch:
    // collapse back to the single Home-rooted stack so tabs never pile up and the
    // user can never get trapped in a navigation loop.
    fun switchTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Opening a secondary detail screen just pushes one entry (back arrow returns).
    fun openDetail(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    Scaffold(
        topBar = {
            // Secondary screens get a back arrow so users are never trapped.
            if (secondaryTitle != null) {
                TopAppBar(
                    title = { Text(secondaryTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isPrimaryTab) RepLogBottomBar(navController)
        }
    ) { padding ->
        NavHost(navController, startDestination = RepLogRoute.Home.route) {
            composable(RepLogRoute.Home.route) {
                HomeScreen(
                    contentPadding = padding,
                    // Primary-tab targets use tab-switch semantics (no stacking / loops).
                    onStartWorkout = { switchTab(RepLogRoute.Workout.route) },
                    onViewHistory = { switchTab(RepLogRoute.History.route) },
                    onStartRecommendedWorkout = {
                        // Recommendation is a one-shot deep link into the Training screen.
                        // It does NOT save/restore tab state, so it can never be resurfaced
                        // later when the user taps the Home tab (the cause of the reported
                        // "Home loops back to Training" issue). Back returns to Home.
                        navController.navigate(RepLogRoute.WorkoutRecommendation.route) {
                            launchSingleTop = true
                        }
                    },
                    // Secondary detail targets just push one entry (back arrow returns).
                    onViewRecoveryGuidance = { openDetail(RepLogRoute.RecoveryCentre.route) },
                    onOpenCoachHistory = { openDetail(RepLogRoute.CoachHistory.route) },
                    onOpenGoals = { openDetail(RepLogRoute.Goals.route) },
                    onOpenTrainingDna = { openDetail(RepLogRoute.TrainingDna.route) },
                    onOpenQuickWorkouts = { openDetail(RepLogRoute.QuickWorkouts.route) },
                    onOpenRecoveryCentre = { openDetail(RepLogRoute.RecoveryCentre.route) },
                    onOpenMuscleBalance = { openDetail(RepLogRoute.MuscleBalance.route) },
                    onOpenDnaEvolution = { openDetail(RepLogRoute.DnaEvolution.route) }
                )
            }
            composable(RepLogRoute.QuickWorkouts.route) {
                com.replog.ui.library.QuickWorkoutsScreen(
                    contentPadding = padding,
                    // Starting a Quick Workout marks a session active; jump to the
                    // Training tab, which resumes it for logging.
                    onWorkoutStarted = { switchTab(RepLogRoute.Workout.route) }
                )
            }
            composable(RepLogRoute.RecoveryCentre.route) {
                com.replog.ui.intelligence.RecoveryCentreScreen(contentPadding = padding)
            }
            composable(RepLogRoute.MuscleBalance.route) {
                com.replog.ui.intelligence.MuscleBalanceScreen(
                    contentPadding = padding,
                    onWorkoutStarted = { switchTab(RepLogRoute.Workout.route) }
                )
            }
            composable(RepLogRoute.DnaEvolution.route) {
                com.replog.ui.intelligence.DnaEvolutionScreen(contentPadding = padding)
            }
            // Training tab: the plain Workout destination (bottom-bar / Home button).
            composable(RepLogRoute.Workout.route) {
                ActiveWorkoutScreen(padding, startFromRecommendation = false)
            }
            // Recommendation deep link: a distinct route that opens the Training
            // screen and consumes the staged Coach recommendation exactly once.
            composable(RepLogRoute.WorkoutRecommendation.route) {
                ActiveWorkoutScreen(padding, startFromRecommendation = true)
            }
            composable(RepLogRoute.Progress.route) {
                ProgressScreen(
                    padding,
                    onOpenTrainingDna = { openDetail(RepLogRoute.TrainingDna.route) }
                )
            }
            composable(RepLogRoute.TrainingDna.route) { TrainingDnaInsightScreen(padding) }
            composable(RepLogRoute.CoachHistory.route) { com.replog.ui.coach.CoachHistoryScreen(padding) }
            composable(RepLogRoute.Goals.route) { com.replog.ui.goals.GoalsScreen(padding) }
            composable(RepLogRoute.History.route) { HistoryScreen(padding) }
            composable(RepLogRoute.Exercises.route) { ExerciseLibraryScreen(padding) }
            composable(RepLogRoute.Settings.route) {
                SettingsScreen(
                    contentPadding = padding,
                    onOpenDisclaimer = { openDetail(RepLogRoute.LegalDisclaimer.route) },
                    onOpenTerms = { openDetail(RepLogRoute.LegalTerms.route) },
                    onOpenPrivacy = { openDetail(RepLogRoute.LegalPrivacy.route) },
                    onOpenAcceptanceHistory = { openDetail(RepLogRoute.LegalHistory.route) },
                    onOpenLicences = { openDetail(RepLogRoute.OpenSourceLicences.route) }
                )
            }
            composable(RepLogRoute.LegalDisclaimer.route) {
                com.replog.ui.legal.LegalDocumentScreen(com.replog.util.legal.LegalDocId.DISCLAIMER, padding)
            }
            composable(RepLogRoute.LegalTerms.route) {
                com.replog.ui.legal.LegalDocumentScreen(com.replog.util.legal.LegalDocId.TERMS, padding)
            }
            composable(RepLogRoute.LegalPrivacy.route) {
                com.replog.ui.legal.LegalDocumentScreen(com.replog.util.legal.LegalDocId.PRIVACY, padding)
            }
            composable(RepLogRoute.LegalHistory.route) { com.replog.ui.legal.AcceptanceHistoryScreen(padding) }
            composable(RepLogRoute.OpenSourceLicences.route) { com.replog.ui.legal.OpenSourceLicencesScreen(padding) }
        }
    }
}

@Composable
private fun RepLogBottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination
    val currentRoute = current?.route?.substringBefore("?")
    NavigationBar {
        bottomTabs.forEach { item ->
            // The recommendation deep link highlights the Training tab.
            val isRecommendationOnWorkout =
                item.route == RepLogRoute.Workout.route &&
                    currentRoute == RepLogRoute.WorkoutRecommendation.route
            NavigationBarItem(
                selected = isRecommendationOnWorkout ||
                    current?.hierarchy?.any { it.route?.substringBefore("?") == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        // Always return to a single Home-rooted back stack; never trap the user.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
