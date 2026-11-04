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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews

@Composable
fun SubmissionsScreen(
    scrollToDeadlines: Boolean = false,
    onNavigateToQuarterly: () -> Unit = {},
    onNavigateToAnnual: () -> Unit = {},
    onNavigateToCCC: () -> Unit = {},
    onNavigateToStatusOfImplementation: () -> Unit = {},
    onNavigateToOther: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    androidx.compose.runtime.LaunchedEffect(scrollToDeadlines) {
        if (scrollToDeadlines) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Submissions Portal",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
        
        Text(
            text = "Facilitating easy and reliable submission of reports and documentations to the IAA," +
                    " with copies automatically sent to the relevant stakeholders.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "What would you like to submit?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubmissionActionButton(
                    text = "Quarterly IA Reports",
                    icon = Icons.Default.UploadFile,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToQuarterly
                )
                SubmissionActionButton(
                    text = "Annual Reports",
                    icon = Icons.Default.Description,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToAnnual
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubmissionActionButton(
                    text = "CCC Review Reports",
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToCCC
                )
                SubmissionActionButton(
                    text = "Status of Implementation",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onNavigateToStatusOfImplementation
                )
            }

            SubmissionActionButton(
                text = "Other Reports",
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToOther
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        SubmissionDeadlinesReminder()
        
        // Increased spacing at the bottom so content isn't covered by the floating menu
        Spacer(modifier = Modifier.height(150.dp))
    }
}

@Composable
fun SubmissionActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 100.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(12.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
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
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 14.sp
            )
        }
    }
}

@ThemePreviews
@Composable
fun SubmissionsScreenPreview() {
    IAAAppTheme {
        SubmissionsScreen()
    }
}
