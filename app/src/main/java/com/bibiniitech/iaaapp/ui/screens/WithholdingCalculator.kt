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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import java.util.Locale

@Composable
fun WithholdingCalculator() {
    var selectedCategory by remember { mutableStateOf("Goods (3%)") }
    val taxRate = when (selectedCategory) {
        "Goods (3%)" -> 0.03
        "Works (5%)" -> 0.05
        "Services (7.5%)" -> 0.075
        "Rent - Residential (8%)" -> 0.08
        "Rent - Commercial (15%)" -> 0.15
        "Directors Fees (20%)" -> 0.20
        else -> 0.03
    }

    var grossInput by remember { mutableStateOf("") }
    var netInput by remember { mutableStateOf("") }
    var isUpdatingFromGross by remember { mutableStateOf(value = true) }

    // Perform calculations reactively
    val grossVal = if (isUpdatingFromGross) {
        grossInput.toDoubleOrNull() ?: 0.0
    } else {
        val netVal = netInput.toDoubleOrNull() ?: 0.0
        netVal / (1.0 - taxRate)
    }

    val netVal = if (isUpdatingFromGross) {
        val g = grossInput.toDoubleOrNull() ?: 0.0
        g * (1.0 - taxRate)
    } else {
        netInput.toDoubleOrNull() ?: 0.0
    }

    val taxVal = grossVal * taxRate

    // Sync input fields without circular loops
    LaunchedEffect(selectedCategory, taxRate) {
        if (isUpdatingFromGross) {
            val g = grossInput.toDoubleOrNull() ?: 0.0
            netInput = if (g > 0) String.format(Locale.US, "%.2f", g * (1.0 - taxRate)) else ""
        } else {
            val n = netInput.toDoubleOrNull() ?: 0.0
            grossInput = if (n > 0) String.format(Locale.US, "%.2f", n / (1.0 - taxRate)) else ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Withholding Tax (WHT) Calculator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        DropdownField(
            label = "Transaction Type",
            options = listOf(
                "Goods (3%)",
                "Works (5%)",
                "Services (7.5%)",
                "Rent - Residential (8%)",
                "Rent - Commercial (15%)",
                "Directors Fees (20%)"
            ),
            selectedOption = selectedCategory
        ) {
            selectedCategory = it
        }

        // Gross Input Field
        OutlinedTextField(
            value = if (isUpdatingFromGross) grossInput else if (grossVal > 0) String.format(Locale.US, "%.2f", grossVal) else "",
            onValueChange = {
                isUpdatingFromGross = true
                grossInput = it
                val g = it.toDoubleOrNull() ?: 0.0
                netInput = if (g > 0) String.format(Locale.US, "%.2f", g * (1.0 - taxRate)) else ""
            },
            label = { Text("Gross Invoice Amount (GHS)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Net Input Field
        OutlinedTextField(
            value = if (!isUpdatingFromGross) netInput else if (netVal > 0) String.format(Locale.US, "%.2f", netVal) else "",
            onValueChange = {
                isUpdatingFromGross = false
                netInput = it
                val n = it.toDoubleOrNull() ?: 0.0
                grossInput = if (n > 0) String.format(Locale.US, "%.2f", n / (1.0 - taxRate)) else ""
            },
            label = { Text("Net Pay Amount (GHS)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Calculation Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Gross Amount:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", grossVal),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("WHT Deducted (${String.format(Locale.US, "%.1f", taxRate * 100)}%):", style = MaterialTheme.typography.bodySmall)
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", taxVal),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Net Amount Paid:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", netVal),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Button(
            onClick = {
                grossInput = ""
                netInput = ""
                isUpdatingFromGross = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text("Clear")
        }
    }
}

@ThemePreviews
@Composable
fun WithholdingCalculatorPreview() {
    IAAAppTheme {
        WithholdingCalculator()
    }
}
