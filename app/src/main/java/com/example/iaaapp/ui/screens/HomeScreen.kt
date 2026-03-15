package com.example.iaaapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iaaapp.R
import com.example.iaaapp.ui.theme.IAAAppTheme
import java.time.LocalDate

@Composable
fun HomeScreen(
    onNavigateToTrainings: () -> Unit = {},
    onNavigateToSubmissions: () -> Unit = {},
    onNavigateToQuarterly: () -> Unit = {},
    onNavigateToCCC: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToLegislation: () -> Unit = {}
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
            WelcomeSection()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            QuickStatsRow(onNavigateToTrainings, onNavigateToSubmissions)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            QuickAccessSection(
                onNavigateToQuarterly = onNavigateToQuarterly,
                onNavigateToCCC = onNavigateToCCC,
                onNavigateToLegislation = onNavigateToLegislation,
                onNavigateToTemplates = onNavigateToTemplates
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SubmissionDeadlinesReminder()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            NoticesCard()
        }
        
        // Extra spacing at the bottom so content isn't covered by the floating menu
        Spacer(modifier = Modifier.height(130.dp))
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
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
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
fun WelcomeSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = R.string.welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(id = R.string.welcome_description),
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
            contentColor = MaterialTheme.colorScheme.primary
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
                modifier = Modifier.size(32.dp)
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

@Composable
fun QuickStatsRow(onTrainingsClick: () -> Unit, onSubmissionsClick: () -> Unit) {
    val today = LocalDate.now()
    val thirtyDaysFromNow = today.plusDays(30)

    // Seminar dates calculation
    val seminars = listOf(
        LocalDate.of(2026, 3, 26),
        LocalDate.of(2026, 11, 12)
    )
    val upcomingSeminarsCount = seminars.count { !it.isBefore(today) && !it.isAfter(thirtyDaysFromNow) }

    // Submissions count calculation using SubmissionDeadline companion object
    val upcomingSubmissionsCount = SubmissionDeadline.allDeadlines.count { 
        !it.date.isBefore(today) && !it.date.isAfter(thirtyDaysFromNow) 
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
            icon = Icons.Default.School,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        StatItem(
            modifier = Modifier
                .weight(1f)
                .clickable { onSubmissionsClick() },
            label = stringResource(id = R.string.pending_submissions),
            value = upcomingSubmissionsCount.toString().padStart(2, '0'),
            icon = Icons.Default.Notifications,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

@Composable
fun StatItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun NoticesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    text = stringResource(id = R.string.notices_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(id = R.string.notices_office_relocation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    IAAAppTheme {
        HomeScreen()
    }
}
