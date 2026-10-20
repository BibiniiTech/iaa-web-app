package com.bibiniitech.iaaapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.iaaapp.*
import com.bibiniitech.iaaapp.R
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTrainings: () -> Unit = {},
    onNavigateToSubmissions: () -> Unit = {},
    onNavigateToQuarterly: () -> Unit = {},
    onNavigateToCCC: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToLegislation: () -> Unit = {},
) {
    // Check if we are in Preview mode to avoid Firebase initialization issues
    val isPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(value = false) }

    val syncTokenState = if (isPreview) {
        remember { mutableLongStateOf(0L) }
    } else {
        FirebaseHelper.syncToken.collectAsState()
    }
    val syncToken by syncTokenState

    var welcomeConfig by remember { mutableStateOf(WelcomeConfig()) }
    var noticeConfig by remember { mutableStateOf(NoticeConfig()) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    val deadlinesList = remember { mutableStateListOf<SubmissionDeadline>() }
    val seminarsList = remember { mutableStateListOf<Seminar>() }

    LaunchedEffect(syncToken) {
        if (isPreview) {
            // Provide default mock data when rendering in Android Studio Preview
            welcomeConfig = WelcomeConfig()
            noticeConfig = NoticeConfig()
            deadlinesList.clear()
            deadlinesList.addAll(SubmissionDeadline.defaultDeadlines)
            return@LaunchedEffect
        }

        val baseUser = FirebaseHelper.getCurrentUser()
        baseUser?.let {
            userProfile = FirebaseHelper.fetchUserProfile(it.uid)
        }
        
        welcomeConfig = FirebaseHelper.getWelcomeConfig()
        noticeConfig = FirebaseHelper.getNoticeConfig()
        
        deadlinesList.clear()
        deadlinesList.addAll(FirebaseHelper.getDeadlines())
        
        seminarsList.clear()
        seminarsList.addAll(FirebaseHelper.getSeminars())
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                FirebaseHelper.refreshLocalCache()
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeHeader()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val personalizedWelcome = remember(welcomeConfig, userProfile) {
                    val profile = userProfile
                    if (profile != null && profile.firstName.isNotEmpty()) {
                        welcomeConfig.copy(title = "Welcome, IA ${profile.firstName}")
                    } else {
                        welcomeConfig
                    }
                }
                WelcomeSection(personalizedWelcome)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                QuickStatsRow(onNavigateToTrainings, onNavigateToSubmissions, deadlinesList, seminarsList)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                QuickAccessSection(
                    onNavigateToQuarterly = onNavigateToQuarterly,
                    onNavigateToCCC = onNavigateToCCC,
                    onNavigateToLegislation = onNavigateToLegislation,
                    onNavigateToTemplates = onNavigateToTemplates
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                NoticesCard(noticeConfig)
            }
            
            // Extra spacing at the bottom so content isn't covered by the floating menu
            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

@Composable
fun HomeHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Gradient background for the header
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.iaa_logo),
                contentDescription = "IAA Logo",
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun WelcomeSection(config: WelcomeConfig) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = config.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = config.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun QuickAccessSection(
    onNavigateToQuarterly: () -> Unit,
    onNavigateToCCC: () -> Unit,
    onNavigateToLegislation: () -> Unit,
    onNavigateToTemplates: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.quick_access_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    icon = Icons.Default.UploadFile,
                    label = stringResource(id = R.string.submit_quarterly_report),
                    onClick = onNavigateToQuarterly,
                    modifier = Modifier.weight(1f)
                )
                QuickAccessCard(
                    icon = Icons.Default.History,
                    label = stringResource(id = R.string.submit_ccc_review_report),
                    onClick = onNavigateToCCC,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    icon = Icons.Default.Gavel,
                    label = stringResource(id = R.string.legislation),
                    onClick = onNavigateToLegislation,
                    modifier = Modifier.weight(1f)
                )
                QuickAccessCard(
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    label = stringResource(id = R.string.reporting_templates),
                    onClick = onNavigateToTemplates,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuickAccessCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp
            )
        }
    }
}

private fun parseFlexibleDate(dateStr: String): LocalDate? {
    if (dateStr.isBlank()) return null
    val clean = dateStr.trim()
    try {
        return LocalDate.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {}

    val patterns = listOf(
        "MMM d, yyyy", "MMM dd, yyyy", "MMMM d, yyyy", "MMMM dd, yyyy",
        "d MMM yyyy", "dd MMM yyyy", "d MMMM yyyy", "dd MMMM yyyy",
        "yyyy/MM/dd", "dd/MM/yyyy"
    )
    for (pattern in patterns) {
        try {
            val formatter = DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
            return LocalDate.parse(clean, formatter)
        } catch (_: Exception) {}
    }
    return null
}

@Composable
fun QuickStatsRow(
    onTrainingsClick: () -> Unit,
    onSubmissionsClick: () -> Unit,
    deadlines: List<SubmissionDeadline>,
    seminars: List<Seminar>
) {
    val today = LocalDate.now()
    val thirtyDaysFromNow = today.plusDays(30)

    // Seminar dates calculation
    val upcomingSeminarsCount = seminars.count { sem ->
        val semDate = parseFlexibleDate(sem.date)
        if (semDate != null) {
            !semDate.isBefore(today) && !semDate.isAfter(thirtyDaysFromNow)
        } else {
            false
        }
    }

    // Submissions count calculation using SubmissionDeadline
    val upcomingSubmissionsCount = deadlines.count {
        val dDate = parseFlexibleDate(it.dateString) ?: it.date
        !dDate.isBefore(today) && !dDate.isAfter(thirtyDaysFromNow) 
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatItem(
            modifier = Modifier
                .weight(1f)
                .clickable { onTrainingsClick() },
            label = stringResource(id = R.string.upcoming_seminars),
            value = upcomingSeminarsCount.toString().padStart(2, '0'),
            icon = Icons.Default.School
        )
        StatItem(
            modifier = Modifier
                .weight(1f)
                .clickable { onSubmissionsClick() },
            label = stringResource(id = R.string.pending_submissions),
            value = upcomingSubmissionsCount.toString().padStart(2, '0'),
            icon = Icons.Default.Notifications
        )
    }
}

@Composable
fun StatItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoticesCard(config: NoticeConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = config.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@ThemePreviews
@Composable
fun HomeScreenPreview() {
    IAAAppTheme {
        HomeScreen()
    }
}
