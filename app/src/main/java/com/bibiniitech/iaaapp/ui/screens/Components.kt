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

import com.bibiniitech.iaaapp.FirebaseHelper
import com.bibiniitech.iaaapp.data.InstitutionData
import androidx.annotation.Keep
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import android.net.Uri
import android.content.Context
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Keep
data class SubmissionDeadline(
    val report: String = "",
    val dateString: String = "", // Used for JSON parsing
    val targetGroup: String = "ALL", // Human-readable summary or primary target group
    val targetGroups: List<String> = emptyList(), // Criteria keys: "ALL", "TYPE:MDA", "REGION_ALL:Ashanti", "REGION_MMDA:Ashanti", etc.
    val targetInstitutions: List<String> = emptyList() // Specific institution names
) {
    val date: LocalDate
        get() = try {
            LocalDate.parse(dateString)
        } catch (_: Exception) {
            LocalDate.now()
        }

    val displayDate: String
        get() = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))

    fun matchesInstitution(instName: String, instType: String = "", instRegion: String = ""): Boolean {
        val (normName, resolvedType, resolvedRegion) = resolveInstitution(instName, instType, instRegion)
        val normLower = normName.lowercase().trim()
        val typeUpper = resolvedType.uppercase().trim()
        val regionTrimmed = resolvedRegion.trim()

        // 1. Specific institution check (priority)
        if (targetInstitutions.isNotEmpty()) {
            if (targetInstitutions.any { it.trim().equals(normLower, ignoreCase = true) }) {
                return true
            }
        }

        // 2. Legacy fallback if both targetGroups & targetInstitutions are empty
        if (targetGroups.isEmpty() && targetInstitutions.isEmpty()) {
            val legacy = targetGroup.uppercase().trim()
            if (legacy.isBlank() || legacy == "ALL") return true
            return if (typeUpper.isNotBlank()) legacy == typeUpper else true
        }

        // 3. ALL check
        if (targetGroups.any { it.equals("ALL", ignoreCase = true) }) return true

        // 4. Institution category / type check
        if (typeUpper.isNotBlank()) {
            if (targetGroups.any { it.equals("TYPE:$typeUpper", ignoreCase = true) || it.equals(typeUpper, ignoreCase = true) }) {
                return true
            }
        }

        // 5. Regional filters check
        if (regionTrimmed.isNotBlank()) {
            if (targetGroups.any { it.equals("REGION_ALL:$regionTrimmed", ignoreCase = true) }) {
                return true
            }
            if (typeUpper.isNotBlank() && targetGroups.any { it.equals("REGION_$typeUpper:$regionTrimmed", ignoreCase = true) }) {
                return true
            }
        }

        return false
    }

    fun formatTargetSummary(): String {
        if (targetGroups.any { it.equals("ALL", ignoreCase = true) } || (targetGroups.isEmpty() && targetInstitutions.isEmpty())) {
            return when (targetGroup.uppercase().trim()) {
                "MDA" -> "MDAs Only"
                "SOE" -> "SOEs Only"
                "RCC" -> "RCCs Only"
                "MMDA" -> "MMDAs Only"
                else -> if (targetGroup.isNotBlank()) targetGroup else "All Institutions"
            }
        }
        val parts = mutableListOf<String>()
        for (g in targetGroups) {
            when {
                g.equals("TYPE:MDA", ignoreCase = true) -> parts.add("All MDAs")
                g.equals("TYPE:SOE", ignoreCase = true) -> parts.add("All SOEs")
                g.equals("TYPE:RCC", ignoreCase = true) -> parts.add("All RCCs")
                g.equals("TYPE:MMDA", ignoreCase = true) -> parts.add("All MMDAs")
                g.startsWith("REGION_ALL:", ignoreCase = true) -> parts.add("${g.removePrefix("REGION_ALL:")} (All)")
                g.startsWith("REGION_MDA:", ignoreCase = true) -> parts.add("${g.removePrefix("REGION_MDA:")} (MDAs)")
                g.startsWith("REGION_MMDA:", ignoreCase = true) -> parts.add("${g.removePrefix("REGION_MMDA:")} (MMDAs)")
                g.startsWith("REGION_RCC:", ignoreCase = true) -> parts.add("${g.removePrefix("REGION_RCC:")} (RCC)")
                else -> parts.add(g)
            }
        }
        if (targetInstitutions.size == 1) {
            parts.add(targetInstitutions.first())
        } else if (targetInstitutions.size > 1) {
            parts.add("${targetInstitutions.size} specific institutions")
        }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "All Institutions"
    }

    companion object {
        fun resolveInstitution(instName: String, fallbackType: String = "", fallbackRegion: String = ""): Triple<String, String, String> {
            val trimmed = instName.trim()
            val rccRegion = InstitutionData.REGIONS.firstOrNull { InstitutionData.RCC_DATA[it].equals(trimmed, ignoreCase = true) }
            if (rccRegion != null || trimmed.endsWith(" RCC", ignoreCase = true)) {
                val reg = rccRegion ?: trimmed.removeSuffix(" RCC").trim()
                return Triple(trimmed, "RCC", reg)
            }
            val mmdaEntry = InstitutionData.MMDA_DATA.entries.firstOrNull { entry ->
                entry.value.any { it.equals(trimmed, ignoreCase = true) }
            }
            if (mmdaEntry != null) {
                return Triple(trimmed, "MMDA", mmdaEntry.key)
            }
            if (InstitutionData.MDA_LIST.any { it.equals(trimmed, ignoreCase = true) }) {
                return Triple(trimmed, "MDA", fallbackRegion)
            }
            if (InstitutionData.SOE_LIST.any { it.equals(trimmed, ignoreCase = true) }) {
                return Triple(trimmed, "SOE", fallbackRegion)
            }
            return Triple(trimmed, fallbackType, fallbackRegion)
        }
        val defaultDeadlines = listOf(
            SubmissionDeadline("1st Quarter Internal Audit Report", "2026-04-30"),
            SubmissionDeadline("2nd Quarter Internal Audit Report", "2026-07-31"),
            SubmissionDeadline("3rd Quarter Internal Audit Report", "2026-10-31"),
            SubmissionDeadline("Status of Implementation on 4th Quarter IA Report", "2026-04-30"),
            SubmissionDeadline("Status of Implementation on 1st Quarter IA Report", "2026-07-31"),
            SubmissionDeadline("Status of Implementation on 2nd Quarter IA Report", "2026-10-31"),
            SubmissionDeadline("1st Quarter Commitment Control Compliance (CCC) Review Report", "2026-04-10"),
            SubmissionDeadline("2nd Quarter Commitment Control Compliance (CCC) Review Report", "2026-07-10"),
            SubmissionDeadline("3rd Quarter Commitment Control Compliance (CCC) Review Report", "2026-10-10"),
            SubmissionDeadline("2025 Annual Statement on Status of Audit Recommendations Implementations", "2026-06-30"),
        ).sortedBy { it.date }
    }
}

@Composable
fun SubmissionDeadlinesReminder() {
    val isPreview = LocalInspectionMode.current
    val syncTokenState = if (isPreview) {
        remember { mutableLongStateOf(0L) }
    } else {
        FirebaseHelper.syncToken.collectAsState()
    }
    val syncToken by syncTokenState

    var deadlines by remember { mutableStateOf<List<SubmissionDeadline>>(emptyList()) }
    LaunchedEffect(syncToken) {
        if (isPreview) {
            deadlines = SubmissionDeadline.defaultDeadlines
            return@LaunchedEffect
        }
        deadlines = FirebaseHelper.getDeadlines()
    }

    val today = LocalDate.now()

    // Filter out past deadlines and take only the next 5
    val currentDeadlines = remember(deadlines) {
        deadlines.asSequence().filter { !it.date.isBefore(today) }.take(5).toList()
    }

    if (currentDeadlines.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Upcoming Submissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Tabular Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Report Type",
                    modifier = Modifier.weight(1.5f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Deadline",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Tabular Rows
            currentDeadlines.forEach { deadline ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deadline.report,
                        modifier = Modifier.weight(1.5f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = deadline.displayDate,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(value = false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = { },
            label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.clickable { expanded = true }
                )
            }
        )
        // Invisible box to catch clicks on the whole field
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun SubmissionDeadlinesReminderPreview() {
    IAAAppTheme {
        SubmissionDeadlinesReminder()
    }
}

@ThemePreviews
@Composable
fun DropdownFieldPreview() {
    IAAAppTheme {
        DropdownField(
            label = "Sample Label",
            options = listOf("Option 1", "Option 2", "Option 3"),
            selectedOption = "Option 1",
        ) { }
    }
}

fun calculateTotalSize(context: Context, uris: List<Uri>): Long {
    var totalSize = 0L
    uris.forEach { uri ->
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                totalSize += fd.length
            }
        } catch (_: Exception) { }
    }
    return totalSize
}
