package com.bibiniitech.iaaapp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bibiniitech.iaaapp.ui.screens.*
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Resources : Screen("resources?expand={expand}", "Resources", Icons.AutoMirrored.Filled.LibraryBooks) {
        fun createRoute(expand: String? = null) = if (expand != null) "resources?expand=$expand" else "resources"
    }
    data object FindingsCriteria : Screen("findings_criteria", "Findings & Criteria", Icons.Default.AutoAwesome)
    data object Calculators : Screen("tax_calculators", "Tax Calculator", Icons.Default.Calculate)
    data object Submissions : Screen("submissions", "Submissions", Icons.AutoMirrored.Filled.Send)
    data object Defaulters : Screen("defaulters", "Defaulters' List", Icons.Default.Warning)
    data object QuarterlySubmission : Screen("quarterly_submission", "Quarterly Submission", Icons.AutoMirrored.Filled.Send)
    data object AnnualSubmission : Screen("annual_submission", "Annual Submission", Icons.AutoMirrored.Filled.Send)
    data object CCCReviewSubmission : Screen("ccc_review_submission", "CCC Review Submission", Icons.AutoMirrored.Filled.Send)
    data object StatusOfImplementationSubmission : Screen("status_of_implementation_submission", "Status of Implementation Submission", Icons.AutoMirrored.Filled.Send)
    data object OtherReportsSubmission : Screen("other_reports_submission", "Other Reports Submission", Icons.AutoMirrored.Filled.Send)
    data object Trainings : Screen("trainings", "Trainings", Icons.Default.School)
    data object PFMLeague : Screen("pfm_league", "PFM League", Icons.Default.Leaderboard)
    data object Voting : Screen("voting", "Voting", Icons.Default.HowToVote)
    data object Contact : Screen("contact", "Contact", Icons.Default.ContactPage)
    data object Disclaimer : Screen("disclaimer", "Disclaimer", Icons.Default.Gavel)
    data object Login : Screen("login", "Sign In", Icons.AutoMirrored.Filled.Login)
    data object SignUp : Screen("signup", "Sign Up", Icons.Default.PersonAdd)
    data object Admin : Screen("admin", "Admin Dashboard", Icons.Default.AdminPanelSettings)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
    data object ProfileCompletion : Screen("profile_completion?email={email}&name={name}", "Complete Profile", Icons.Default.Person) {
        fun createRoute(email: String, name: String) = "profile_completion?email=${android.net.Uri.encode(email)}&name=${android.net.Uri.encode(name)}"
    }
    data object PdfViewer : Screen("pdf_viewer?url={url}&title={title}", "View Document", Icons.Default.Gavel) {
        fun createRoute(url: String, title: String) = "pdf_viewer?url=${android.net.Uri.encode(url)}&title=${android.net.Uri.encode(title)}"
    }

    companion object {
        val items by lazy { listOf(Home, Resources, FindingsCriteria, Calculators, Submissions, Defaulters, Trainings, PFMLeague, Voting, Contact, Disclaimer, Login, Profile, Admin) }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController, 
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
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
                    navController.navigateToScreenSafe(Screen.QuarterlySubmission, popUpToStart = false)
                },
                onNavigateToCCC = {
                    navController.navigateToScreenSafe(Screen.CCCReviewSubmission, popUpToStart = false)
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
            arguments = listOf(
                navArgument("expand") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val expand = backStackEntry.arguments?.getString("expand")
            ResourcesScreen(navController = navController, initialExpandedCategory = expand)
        }
        composable(Screen.FindingsCriteria.route) {
            FindingsCriteriaScreen(navController = navController)
        }
        composable(Screen.Calculators.route) {
            TaxCalculatorsScreen(navController = navController)
        }
        composable(Screen.Submissions.route) { 
            SubmissionsScreen(
                onNavigateToQuarterly = {
                    navController.navigateToScreenSafe(Screen.QuarterlySubmission, popUpToStart = false)
                },
                onNavigateToAnnual = {
                    navController.navigateToScreenSafe(Screen.AnnualSubmission, popUpToStart = false)
                },
                onNavigateToCCC = {
                    navController.navigateToScreenSafe(Screen.CCCReviewSubmission, popUpToStart = false)
                },
                onNavigateToStatusOfImplementation = {
                    navController.navigateToScreenSafe(Screen.StatusOfImplementationSubmission, popUpToStart = false)
                },
                onNavigateToOther = {
                    navController.navigateToScreenSafe(Screen.OtherReportsSubmission, popUpToStart = false)
                }
            ) 
        }
        composable(Screen.Defaulters.route) {
            DefaultersScreen(
                onBack = { navController.popBackStack() }
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
        composable(Screen.PFMLeague.route) { PFMLeagueScreen() }
        composable(Screen.Voting.route) { 
            VotingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Contact.route) { ContactScreen() }
        composable(Screen.Disclaimer.route) { DisclaimerScreen() }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigateToScreenSafe(Screen.SignUp)
                },
                onLoginSuccess = { isComplete, email, name ->
                    if (isComplete) {
                        navController.navigateToScreenSafe(Screen.Home)
                    } else {
                        navController.navigate(Screen.ProfileCompletion.createRoute(email ?: "", name ?: "")) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.navigateToScreenSafe(Screen.Login)
                },
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.ProfileCompletion.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            ProfileCompletionScreen(
                prefilledEmail = email,
                prefilledName = name,
                onProfileCompleteSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ProfileCompletion.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Admin.route) {
            AdminScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            PdfViewerScreen(url = url, title = title, onBack = { navController.popBackStack() })
        }
    }
}

fun NavController.navigateToScreenSafe(
    screen: Screen, 
    customRoute: String? = null,
    popUpToStart: Boolean = true
) {
    val targetRoute = customRoute ?: screen.route
    try {
        navigate(targetRoute) {
            if (popUpToStart) {
                popUpTo(graph.findStartDestination().id) {
                    saveState = true
                }
                restoreState = true
            }
            launchSingleTop = true
        }
    } catch (_: Exception) {
        navigate(targetRoute)
    }
}

@ThemePreviews
@Composable
fun NavGraphPreview() {
    val navController = rememberNavController()
    IAAAppTheme {
        NavGraph(navController = navController)
    }
}
