/*
 * ============================================================================
 * PROPRIETARY AND CONFIDENTIAL PROPERTY OF CLEMENT KOFI ACHEAMPONG
 * (BIBINII TECH) - ALL RIGHTS RESERVED.
 *
 * Application: Internal Auditing Support & Statutory Reporting Platform (IAA App)
 * Author: Clement Kofi Acheampong (Internal Auditor & Software Engineer)
 * Copyright (c) 2024-2026 Clement Kofi Acheampong. All Rights Reserved.
 *
 * NOTICE: All information, source code, algorithms, and data designs contained
 * herein are protected under the Ghana Copyright Act, 2005 (Act 690) and
 * international intellectual property treaties. Unauthorized copying,
 * decompilation, reverse engineering, redistribution, or modification of this
 * file via any medium is strictly prohibited without prior written consent.
 * ============================================================================
 */

package com.bibiniitech.iaaapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.bibiniitech.iaaapp.*
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import java.io.File
import java.io.FileOutputStream

@Composable
fun TrainingsScreen() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val syncTokenState = if (isPreview) {
        remember { mutableLongStateOf(0L) }
    } else {
        FirebaseHelper.syncToken.collectAsState()
    }
    val syncToken by syncTokenState

    var seminarsList by remember { mutableStateOf<List<Seminar>>(emptyList()) }
    var trainingDocs by remember { mutableStateOf<List<DbDocument>>(emptyList()) }
    
    LaunchedEffect(syncToken) {
        if (isPreview) return@LaunchedEffect
        seminarsList = FirebaseHelper.getSeminars()
        trainingDocs = FirebaseHelper.getDocuments("training_resources")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Training Portal",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        
        Text(
            text = "Keep up with professional training modules and seminars available for skills development.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        TrainingCategoryItem(
            title = "Video Tutorials",
            subtitle = "Step-by-step guides on relevant audit activities.",
            icon = Icons.Default.PlayCircleOutline,
            isExpandable = true
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                VideoListItem("IAA Audit Methodology 101", "https://www.youtube.com/results?search_query=internal+audit+methodology") { uriHandler.openUri(it) }
                VideoListItem("Risk Assessment Techniques", "https://www.youtube.com/results?search_query=audit+risk+assessment") { uriHandler.openUri(it) }
                VideoListItem("Reporting Best Practices", "https://www.youtube.com/results?search_query=internal+audit+reporting") { uriHandler.openUri(it) }
            }
        }
        
        TrainingCategoryItem(
            title = "Upcoming Seminars",
            subtitle = "Register for upcoming professional seminars.",
            icon = Icons.Default.Event,
            isExpandable = true
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                
                val today = java.time.LocalDate.now()
                val upcomingSeminars = seminarsList.filter { sem ->
                    val sDate = try { java.time.LocalDate.parse(sem.date.trim()) } catch (_: Exception) { null }
                    sDate == null || !sDate.isBefore(today)
                }

                if (upcomingSeminars.isEmpty()) {
                    Text(
                        text = "No upcoming seminars scheduled at the moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    upcomingSeminars.forEachIndexed { index, sem ->
                        SeminarDetailItem(
                            topic = sem.topic,
                            date = sem.date,
                            time = sem.time,
                            location = sem.location,
                            fee = sem.fee,
                            registrationUrl = sem.registrationUrl
                        ) { uriHandler.openUri(it) }
                        
                        if (index < (seminarsList.size - 1)) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
        
        val pastSeminars = seminarsList.filter { sem ->
            val sDate = try { java.time.LocalDate.parse(sem.date.trim()) } catch (_: Exception) { null }
            sDate != null && sDate.isBefore(java.time.LocalDate.now())
        }

        if (pastSeminars.isNotEmpty()) {
            TrainingCategoryItem(
                title = "Archived Seminars",
                subtitle = "Concluded professional seminars and past workshops.",
                icon = Icons.Default.History,
                isExpandable = true
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    pastSeminars.forEachIndexed { index, sem ->
                        SeminarDetailItem(
                            topic = "${sem.topic} [Concluded]",
                            date = sem.date,
                            time = sem.time,
                            location = sem.location,
                            fee = sem.fee,
                            registrationUrl = "",
                            onRegisterClick = {}
                        )
                        if (index < (pastSeminars.size - 1)) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

        TrainingCategoryItem(
            title = "Training Resources",
            subtitle = "Presentation files and resources.",
            icon = Icons.Default.CastForEducation,
            isExpandable = true
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                if (trainingDocs.isEmpty()) {
                    Text(
                        text = "No training resources available",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    trainingDocs.forEach { doc ->
                        TrainingResourceFileItem(
                            title = doc.name
                        ) { openRemoteFile(context, doc.downloadUrl) }
                    }
                }
            }
        }

        // Extra spacing at the bottom so content isn't covered by the floating menu
        Spacer(modifier = Modifier.height(150.dp))
    }
}

private fun openRemoteFile(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun TrainingCategoryItem(
    title: String, 
    subtitle: String, 
    icon: ImageVector,
    isExpandable: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(value = false) }

    Card(
        onClick = { if (isExpandable) expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = if (isExpandable) {
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.ArrowForwardIos
                    },
                    contentDescription = null,
                    modifier = Modifier.size(if (isExpandable) 24.dp else 16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            
            if (isExpandable && expanded && content != null) {
                content()
            }
        }
    }
}

@Composable
fun VideoListItem(title: String, url: String, onVideoClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVideoClick(url) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TrainingResourceFileItem(title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.FileOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SeminarDetailItem(
    topic: String, 
    date: String, 
    time: String, 
    location: String, 
    fee: String,
    registrationUrl: String,
    onRegisterClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = topic,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fee: $fee",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { onRegisterClick(registrationUrl) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Register Now")
        }
    }
}

@ThemePreviews
@Composable
fun TrainingsScreenPreview() {
    IAAAppTheme {
        TrainingsScreen()
    }
}
