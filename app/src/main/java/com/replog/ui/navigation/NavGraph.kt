package com.replog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.navigation.navArgument
import com.replog.ui.exercise.ExerciseLibraryScreen
import com.replog.ui.history.HistoryScreen
import com.replog.ui.home.HomeScreen
import com.replog.ui.onboarding.OnboardingScreen
import com.replog.ui.onboarding.OnboardingViewModel
import com.replog.ui.progress.ProgressScreen
import com.replog.ui.settings.SettingsScreen
import com.replog.ui.trainingdna.TrainingDnaInsightScreen
import com.replog.ui.workout.ActiveWorkoutScreen

sealed class RepLogRoute(val route: String, val label: String, val icon: ImageVector) {
    data object Home : RepLogRoute("home", "Home", Icons.Default.Home)
    data object Workout : RepLogRoute("workout", "Workout", Icons.Default.FitnessCenter)
    data object Progress : RepLogRoute("progress", "Progress", Icons.Default.ShowChart)
    data object History : RepLogRoute("history", "History", Icons.Default.History)
    data object Exercises : RepLogRoute("exercises", "Exercises", Icons.Default.List)
    data object Settings : RepLogRoute("settings", "Settings", Icons.Default.Settings)
    data object TrainingDna : RepLogRoute("training_dna", "Training DNA", Icons.Default.ShowChart)
    data object CoachHistory : RepLogRoute("coach_history", "Coach History", Icons.Default.History)
    data object Goals : RepLogRoute("goals", "Goals", Icons.Default.Flag)
}

private val items = listOf(
    RepLogRoute.Home,
    RepLogRoute.Workout,
    RepLogRoute.Progress,
    RepLogRoute.History,
    RepLogRoute.Exercises,
    RepLogRoute.Settings
)

@Composable
fun RepLogNavGraph(
    navController: NavHostController = rememberNavController(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    if (!onboardingState.onboardingComplete) {
        OnboardingScreen(
            onComplete = onboardingViewModel::finish,
            onPersonalize = onboardingViewModel::finishWithPersonalization
        )
        return
    }

    Scaffold(bottomBar = { RepLogBottomBar(navController) }) { padding ->
        NavHost(navController, startDestination = RepLogRoute.Home.route) {
            composable(RepLogRoute.Home.route) {
                HomeScreen(
                    contentPadding = padding,
                    onStartWorkout = {
                        navController.navigate(RepLogRoute.Workout.route) { launchSingleTop = true }
                    },
                    onViewHistory = {
                        navController.navigate(RepLogRoute.History.route) { launchSingleTop = true }
                    },
                    onStartRecommendedWorkout = {
                        navController.navigate(RepLogRoute.Workout.route + "?fromRecommendation=true") { launchSingleTop = true }
                    },
                    onViewRecoveryGuidance = {
                        navController.navigate(RepLogRoute.TrainingDna.route) { launchSingleTop = true }
                    },
                    onOpenCoachHistory = {
                        navController.navigate(RepLogRoute.CoachHistory.route) { launchSingleTop = true }
                    },
                    onOpenGoals = {
                        navController.navigate(RepLogRoute.Goals.route) { launchSingleTop = true }
                    }
                )
            }
            composable(
                route = RepLogRoute.Workout.route + "?fromRecommendation={fromRecommendation}",
                arguments = listOf(navArgument("fromRecommendation") { defaultValue = "false" })
            ) { entry ->
                val fromRec = entry.arguments?.getString("fromRecommendation") == "true"
                ActiveWorkoutScreen(padding, startFromRecommendation = fromRec)
            }
            composable(RepLogRoute.Progress.route) { ProgressScreen(padding, onOpenTrainingDna = { navController.navigate(RepLogRoute.TrainingDna.route) { launchSingleTop = true } }) }
            composable(RepLogRoute.TrainingDna.route) { TrainingDnaInsightScreen(padding) }
            composable(RepLogRoute.CoachHistory.route) { com.replog.ui.coach.CoachHistoryScreen(padding) }
            composable(RepLogRoute.Goals.route) { com.replog.ui.goals.GoalsScreen(padding) }
            composable(RepLogRoute.History.route) { HistoryScreen(padding) }
            composable(RepLogRoute.Exercises.route) { ExerciseLibraryScreen(padding) }
            composable(RepLogRoute.Settings.route) { SettingsScreen(padding) }
        }
    }
}

@Composable
private fun RepLogBottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = current?.hierarchy?.any { it.route?.substringBefore("?") == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
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
