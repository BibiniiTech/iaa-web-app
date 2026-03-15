package com.example.iaaapp.ui.screens

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
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SubmissionDeadline(
    val report: String,
    val date: LocalDate
) {
    val displayDate: String
        get() = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))

    companion object {
        private fun deadline(year: Int, month: Int, day: Int, report: String): SubmissionDeadline {
            return SubmissionDeadline(report, LocalDate.of(year, month, day))
        }

        val allDeadlines = listOf(
            deadline(2026, 4, 30, "1st Quarter Internal Audit Report"),
            deadline(2026, 7, 31, "2nd Quarter Internal Audit Report"),
            deadline(2026, 10, 31, "3rd Quarter Internal Audit Report"),
            deadline(2026, 4, 30, "Status of Implementation on 4th Quarter IA Report"),
            deadline(2026, 7, 31, "Status of Implementation on 1st Quarter IA Report"),
            deadline(2026, 10, 31, "Status of Implementation on 2nd Quarter IA Report"),
            deadline(2026, 4, 10, "1st Quarter Commitment Control Compliance (CCC) Review Report"),
            deadline(2026, 7, 10, "2nd Quarter Commitment Control Compliance (CCC) Review Report"),
            deadline(2026, 10, 10, "3rd Quarter Commitment Control Compliance (CCC) Review Report"),
            deadline(2026, 6, 30, "2025 Annual Statement on Status of Audit Recommendations Implementations"),
        ).sortedBy { it.date }
    }
}

@Composable
fun SubmissionDeadlinesReminder() {
    val today = LocalDate.now()

    // Filter out past deadlines and take only the next 5
    val currentDeadlines = SubmissionDeadline.allDeadlines.filter { !it.date.isBefore(today) }.take(5)

    if (currentDeadlines.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Tabular Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Report Type",
                    modifier = Modifier.weight(1.5f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Deadline",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))

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
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = deadline.displayDate,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
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
    var expanded by remember { mutableStateOf(false) }

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
