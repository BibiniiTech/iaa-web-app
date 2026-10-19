package com.bibiniitech.iaaapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bibiniitech.iaaapp.ui.navigation.NavGraph
import com.bibiniitech.iaaapp.ui.navigation.Screen
import com.bibiniitech.iaaapp.ui.navigation.navigateToScreenSafe
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase mode (Disable Mock mode for real app usage)
        FirebaseHelper.setMockMode(enabled = false)
        
        // Initialize Remote Config
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 10800 // Fetch every 3 hours
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Set default values (so app doesn't break if offline)
        val defaults = mapOf(
            "show_contact_us" to true,
            "show_disclaimer" to true,
            "main_menu_label" to "Menu",
            "pfm_league_heading" to "PFM Compliance League Table",
            "pfm_league_description" to "The Public Financial Management (PFM) Compliance League Table is a landmark transparency and accountability initiative aimed at strengthening fiscal discipline and improving the management of public resources. This fulfills the Government’s commitment in the 2025 Budget Statement to publish an objective, evidence-based assessment of how public institutions comply with the PFM Act, 2016 (Act 921), its Regulations and associated laws. The League Table serves as a performance benchmarking tool that measures the extent to which public institutions adhere to the rules and procedures governing the use of public funds.",
            "pfm_league_mdas" to "",
            "pfm_league_mmdas" to "",
            "submission_deadlines" to "", // Empty string means use local hardcoded defaults
            "brevo_api_key" to "",
        )
        remoteConfig.setDefaultsAsync(defaults)
        
        // Fetch values from the cloud
        remoteConfig.fetchAndActivate()

        // Perform Integrity and Environment Checks
        if (!performSecurityChecks()) {
            finish()
            return
        }

        setupAppContent()
    }

    /**
     * Checks for Root, Emulators, Debuggers, and App Integrity.
     * Prevents reverse engineering and unauthorized modifications.
     */
    private fun performSecurityChecks(): Boolean {
        val isDebugMode = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // 1. Check for Debugger (Prevent reverse engineering / dynamic analysis)
        if (SecurityUtils.isDebuggerAttached()) {
            Toast.makeText(this, "Security Violation: Debugger detected.", Toast.LENGTH_LONG).show()
            return false
        }

        // 2. Check for Root (Prevent system level tampering)
        if (SecurityUtils.isDeviceRooted()) {
            Toast.makeText(this, "Security Violation: Rooted device detected.", Toast.LENGTH_LONG).show()
            return false
        }

        // 3. Check for Emulator (Optional: Disable if only physical devices are allowed)
        if (SecurityUtils.isRunningOnEmulator() && !isDebugMode) {
            Toast.makeText(this, "Security Violation: Emulator not allowed.", Toast.LENGTH_LONG).show()
            return false
        }

        // 4. Check for Patching Tools (e.g. Lucky Patcher)
        if (SecurityUtils.isTamperToolDetected(this)) {
            Toast.makeText(this, "Security Violation: Tampering tools detected.", Toast.LENGTH_LONG).show()
            return false
        }

        // 5. App Signature Verification (Prevent patching/re-signing)
        // IMPORTANT: Update this hash with your actual release key SHA-256 (Base64)
        val expectedReleaseHash = "" 
        if (expectedReleaseHash.isNotEmpty() && !SecurityUtils.isAppSignatureValid(this, expectedReleaseHash)) {
            Toast.makeText(this, "Security Violation: App modification detected.", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun setupAppContent() {
        // Request Notification Permission for Android 13+ after biometric success
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Use ActivityCompat.requestPermissions with a 16-bit requestCode
                // to avoid the FragmentActivity limitation with ActivityResultLauncher.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101,
                )
            }
        }

        // Start Firebase real-time Sync Token listener
        FirebaseHelper.startSyncListener()

        enableEdgeToEdge()
        setContent {
            IAAAppTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        FirebaseHelper.stopSyncListener()
        super.onDestroy()
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Use LocalInspectionMode to check if we are in a Preview.
    val isPreview = LocalInspectionMode.current
    
    // Auth state check
    val currentUser by FirebaseHelper.authState.collectAsState()
    val isAuthInitialized by FirebaseHelper.isAuthInitialized.collectAsState()
    
    // Accessing Remote Config values
    val showContactUs = remember(isPreview) {
        if (isPreview) true else Firebase.remoteConfig.getBoolean("show_contact_us")
    }
    
    val syncToken by FirebaseHelper.syncToken.collectAsState()
    var showDisclaimer by remember { mutableStateOf(true) }
    var showPfmLeague by remember { mutableStateOf(true) }
    var showVoting by remember { mutableStateOf(false) }
    
    LaunchedEffect(syncToken) {
        if (!isPreview) {
            showDisclaimer = FirebaseHelper.getShowDisclaimer()
            showPfmLeague = FirebaseHelper.getShowPfmLeague()
            showVoting = FirebaseHelper.getVotingConfig().isVisible
        }
    }
    
    val mainMenuLabel = remember(isPreview) {
        if (isPreview) "Menu" else Firebase.remoteConfig.getString("main_menu_label")
    }

    var startDest by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser, isAuthInitialized) {
        if (!isAuthInitialized && !isPreview) return@LaunchedEffect
        val user = currentUser
        if (user == null) {
            startDest = Screen.Login.route
        } else {
            val complete = FirebaseHelper.isProfileComplete(user.uid)
            if (complete) {
                startDest = Screen.Home.route
            } else {
                val fullProfile = FirebaseHelper.fetchUserProfile(user.uid) ?: user
                val fullName = "${fullProfile.firstName} ${fullProfile.surname}".trim()
                startDest = Screen.ProfileCompletion.createRoute(user.email, if (fullName.isNotEmpty()) fullName else "Google User")
            }
        }
    }

    if (startDest == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            // Use RTL trick to open drawer from the right
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        // Reset to Ltr for drawer content
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            DrawerContent(
                                navController = navController,
                                drawerState = drawerState,
                                scope = scope,
                                showContactUs = showContactUs,
                                showDisclaimer = showDisclaimer,
                                showPfmLeague = showPfmLeague,
                                showVoting = showVoting,
                                mainMenuLabel = mainMenuLabel
                            )
                        }
                    },
                    gesturesEnabled = currentUser != null && startDest == Screen.Home.route
                ) {
                    // Reset to Ltr for main screen content
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Fixed Background Image
                            Image(
                                painter = painterResource(id = R.drawable.bg_footer),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                contentScale = ContentScale.FillWidth,
                                alpha = 0.2f
                            )

                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = Color.Transparent,
                                floatingActionButton = {
                                    // Only show Floating Menu Button if user is signed in and profile is complete
                                    if (currentUser != null && startDest == Screen.Home.route) {
                                        FloatingActionButton(
                                            onClick = { scope.launch { drawerState.open() } },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            shape = CircleShape,
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Open Navigation Menu"
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    NavGraph(
                                        navController = navController,
                                        startDestination = startDest!!,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    showContactUs: Boolean,
    showDisclaimer: Boolean,
    showPfmLeague: Boolean,
    showVoting: Boolean,
    mainMenuLabel: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            val user = FirebaseHelper.getCurrentUser()
            currentUserProfile = user?.let {
                FirebaseHelper.fetchUserProfile(it.uid) ?: user
            }
        }
    }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxWidth(0.55f) // Covers half of the screen (slightly more for better spacing)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = currentUserProfile?.firstName?.takeIf { it.isNotBlank() }?.let { "Hello, $it" } ?: mainMenuLabel,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Standard Navigation Items
            val standardItems = Screen.items.filter { 
                (it != Screen.Disclaimer) && (it != Screen.Profile) && (it != Screen.Admin) && (it != Screen.Login) && (it != Screen.SignUp)
            }
            
            standardItems.forEach { screen ->
                if ((screen == Screen.Contact) && !showContactUs) return@forEach
                if ((screen == Screen.PFMLeague) && !showPfmLeague) return@forEach
                if ((screen == Screen.Voting) && !showVoting) return@forEach
                
                DrawerItem(
                    screen = screen,
                    currentRoute = currentRoute,
                    currentUserProfile = currentUserProfile,
                    onNavigate = {
                        scope.launch { drawerState.close() }
                        if ((!it) || (screen == Screen.Home)) {
                            navController.navigateToScreenSafe(screen)
                        }
                    }
                )
            }

            // Disclaimer Item
            if (showDisclaimer) {
                DrawerItem(
                    screen = Screen.Disclaimer,
                    currentRoute = currentRoute,
                    currentUserProfile = currentUserProfile,
                    onNavigate = {
                        scope.launch { drawerState.close() }
                        if (!it) navController.navigateToScreenSafe(Screen.Disclaimer)
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Profile Item
            DrawerItem(
                screen = Screen.Profile,
                currentRoute = currentRoute,
                currentUserProfile = currentUserProfile,
                onNavigate = {
                    scope.launch { drawerState.close() }
                    if (!it) navController.navigateToScreenSafe(Screen.Profile)
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Admin Dashboard (Conditional)
            if (currentUserProfile?.role == "admin") {
                DrawerItem(
                    screen = Screen.Admin,
                    currentRoute = currentRoute,
                    currentUserProfile = currentUserProfile,
                    onNavigate = {
                        scope.launch { drawerState.close() }
                        if (!it) navController.navigateToScreenSafe(Screen.Admin)
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Sign Out (Login Item)
            DrawerItem(
                screen = Screen.Login,
                currentRoute = currentRoute,
                currentUserProfile = currentUserProfile,
                onSignOut = {
                    scope.launch { drawerState.close() }
                    FirebaseHelper.signOut()
                    currentUserProfile = null
                    Toast.makeText(context, "Signed out successfully.", Toast.LENGTH_SHORT).show()
                    navController.navigateToScreenSafe(Screen.Login)
                },
                onNavigate = {
                    scope.launch { drawerState.close() }
                    if (!it) navController.navigateToScreenSafe(Screen.Login)
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DrawerItem(
    screen: Screen,
    currentRoute: String?,
    currentUserProfile: UserProfile?,
    onNavigate: (isSelected: Boolean) -> Unit,
    onSignOut: (() -> Unit)? = null
) {
    val baseRoute = screen.route.substringBefore("?")
    val currentRouteBase = currentRoute?.substringBefore("?")
    val isSelected = currentRouteBase == baseRoute

    val isLoginItem = screen == Screen.Login
    val itemLabel = if (isLoginItem && (currentUserProfile != null)) "Sign Out" else screen.label
    val itemIcon = if (isLoginItem && (currentUserProfile != null)) Icons.AutoMirrored.Filled.Logout else screen.icon

    NavigationDrawerItem(
        label = { 
            Text(
                itemLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ) 
        },
        selected = isSelected,
        onClick = {
            val isSigningOut = isLoginItem && (currentUserProfile != null) && (onSignOut != null)
        if (isSigningOut) {
                onSignOut()
            } else {
                onNavigate(isSelected)
            }
        },
        icon = { 
            Icon(
                itemIcon, 
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            ) 
        },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@ThemePreviews
@Composable
fun MainScreenPreview() {
    IAAAppTheme {
        MainScreen()
    }
}
