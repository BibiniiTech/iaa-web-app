package com.example.iaaapp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.iaaapp.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Resources : Screen("resources", "Resources", Icons.AutoMirrored.Filled.LibraryBooks)
    data object Submissions : Screen("submissions", "Submissions", Icons.AutoMirrored.Filled.Send)
    data object QuarterlySubmission : Screen("quarterly_submission", "Quarterly Submission", Icons.AutoMirrored.Filled.Send)
    data object AnnualSubmission : Screen("annual_submission", "Annual Submission", Icons.AutoMirrored.Filled.Send)
    data object CCCReviewSubmission : Screen("ccc_review_submission", "CCC Review Submission", Icons.AutoMirrored.Filled.Send)
    data object StatusOfImplementationSubmission : Screen("status_of_implementation_submission", "Status of Implementation Submission", Icons.AutoMirrored.Filled.Send)
    data object Trainings : Screen("trainings", "Trainings", Icons.Default.School)
    data object Contact : Screen("contact", "Contact", Icons.Default.ContactPage)

    companion object {
        val items by lazy { listOf(Home, Resources, Submissions, Trainings, Contact) }
    }
}

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) }
    ) {
        composable(Screen.Home.route) { 
            HomeScreen(
                onNavigateToTrainings = {
                    navigateToScreenSafe(navController, Screen.Trainings)
                },
                onNavigateToSubmissions = {
                    navigateToScreenSafe(navController, Screen.Submissions)
                }
            ) 
        }
        composable(Screen.Resources.route) { ResourcesScreen() }
        composable(Screen.Submissions.route) { 
            SubmissionsScreen(
                onNavigateToQuarterly = {
                    navController.navigate(Screen.QuarterlySubmission.route)
                },
                onNavigateToAnnual = {
                    navController.navigate(Screen.AnnualSubmission.route)
                },
                onNavigateToCCC = {
                    navController.navigate(Screen.CCCReviewSubmission.route)
                },
                onNavigateToStatusOfImplementation = {
                    navController.navigate(Screen.StatusOfImplementationSubmission.route)
                }
            ) 
        }
        composable(Screen.QuarterlySubmission.route) {
            QuarterlyReportSubmissionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AnnualSubmission.route) {
            AnnualReportSubmissionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CCCReviewSubmission.route) {
            CCCReviewReportSubmissionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.StatusOfImplementationSubmission.route) {
            StatusOfImplementationSubmissionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Trainings.route) { TrainingsScreen() }
        composable(Screen.Contact.route) { ContactScreen() }
    }
}

private fun navigateToScreenSafe(navController: NavController, screen: Screen) {
    try {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } catch (_: Exception) {
        navController.navigate(screen.route)
    }
}
