package com.example.iaaapp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.iaaapp.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Resources : Screen("resources?expand={expand}", "Resources", Icons.AutoMirrored.Filled.LibraryBooks) {
        fun createRoute(expand: String? = null) = if (expand != null) "resources?expand=$expand" else "resources"
    }
    data object Submissions : Screen("submissions", "Submissions", Icons.AutoMirrored.Filled.Send)
    data object QuarterlySubmission : Screen("quarterly_submission", "Quarterly Submission", Icons.AutoMirrored.Filled.Send)
    data object AnnualSubmission : Screen("annual_submission", "Annual Submission", Icons.AutoMirrored.Filled.Send)
    data object CCCReviewSubmission : Screen("ccc_review_submission", "CCC Review Submission", Icons.AutoMirrored.Filled.Send)
    data object StatusOfImplementationSubmission : Screen("status_of_implementation_submission", "Status of Implementation Submission", Icons.AutoMirrored.Filled.Send)
    data object OtherReportsSubmission : Screen("other_reports_submission", "Other Reports Submission", Icons.AutoMirrored.Filled.Send)
    data object Trainings : Screen("trainings", "Trainings", Icons.Default.School)
    data object Contact : Screen("contact", "Contact", Icons.Default.ContactPage)
    data object Disclaimer : Screen("disclaimer", "Disclaimer", Icons.Default.Gavel)

    companion object {
        val items by lazy { listOf(Home, Resources, Submissions, Trainings, Contact, Disclaimer) }
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
                    navController.navigateToScreenSafe(Screen.Trainings)
                },
                onNavigateToSubmissions = {
                    navController.navigateToScreenSafe(Screen.Submissions)
                },
                onNavigateToQuarterly = {
                    navController.navigateToScreenSafe(Screen.QuarterlySubmission)
                },
                onNavigateToCCC = {
                    navController.navigateToScreenSafe(Screen.CCCReviewSubmission)
                },
                onNavigateToTemplates = {
                    navController.navigateToScreenSafe(Screen.Resources, Screen.Resources.createRoute(expand = "templates"))
                },
                onNavigateToLegislation = {
                    navController.navigateToScreenSafe(Screen.Resources, Screen.Resources.createRoute(expand = "legislations"))
                }
            ) 
        }
        composable(
            route = Screen.Resources.route,
            arguments = listOf(navArgument("expand") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val expand = backStackEntry.arguments?.getString("expand")
            ResourcesScreen(initialExpandedCategory = expand)
        }
        composable(Screen.Submissions.route) { 
            SubmissionsScreen(
                onNavigateToQuarterly = {
                    navController.navigateToScreenSafe(Screen.QuarterlySubmission)
                },
                onNavigateToAnnual = {
                    navController.navigateToScreenSafe(Screen.AnnualSubmission)
                },
                onNavigateToCCC = {
                    navController.navigateToScreenSafe(Screen.CCCReviewSubmission)
                },
                onNavigateToStatusOfImplementation = {
                    navController.navigateToScreenSafe(Screen.StatusOfImplementationSubmission)
                },
                onNavigateToOther = {
                    navController.navigateToScreenSafe(Screen.OtherReportsSubmission)
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
        composable(Screen.OtherReportsSubmission.route) {
            OtherReportsSubmissionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Trainings.route) { TrainingsScreen() }
        composable(Screen.Contact.route) { ContactScreen() }
        composable(Screen.Disclaimer.route) { DisclaimerScreen() }
    }
}

fun NavController.navigateToScreenSafe(screen: Screen, customRoute: String? = null) {
    val targetRoute = customRoute ?: screen.route
    try {
        navigate(targetRoute) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } catch (_: Exception) {
        navigate(targetRoute)
    }
}
