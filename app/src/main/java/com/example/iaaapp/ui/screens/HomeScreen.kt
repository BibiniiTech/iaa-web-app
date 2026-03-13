package com.example.iaaapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    onNavigateToSubmissions: () -> Unit = {}
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SubmissionDeadlinesReminder()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            RecentUpdatesCard()
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
            text = "Welcome!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "This app aims to ease the submission of the various reports expected from Internal Audit Functions to the IAA" +
                    " and to make resources readily available and accessible to Internal Auditors and Stakeholders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 20.sp
        )
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
            label = "Upcoming Seminars",
            value = upcomingSeminarsCount.toString().padStart(2, '0'),
            icon = Icons.Default.School,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        StatItem(
            modifier = Modifier
                .weight(1f)
                .clickable { onSubmissionsClick() },
            label = "Pending Submissions",
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RecentUpdatesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Resources",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Check out the latest updated Guidelines & Manuals in the Resources portal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = { /* Navigate to resources */ },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("View Resources")
            }
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
