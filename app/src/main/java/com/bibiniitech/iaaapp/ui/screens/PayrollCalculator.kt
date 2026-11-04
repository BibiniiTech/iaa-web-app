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
fun PayrollCalculator() {
    var basicSalaryInput by remember { mutableStateOf("") }
    var allowancesInput by remember { mutableStateOf("") }

    val basicSalary = basicSalaryInput.toDoubleOrNull() ?: 0.0
    val allowances = allowancesInput.toDoubleOrNull() ?: 0.0

    // Calculations
    val grossSalary = basicSalary + allowances
    val employeeSsnit = basicSalary * 0.055
    val employerSsnit = basicSalary * 0.130
    
    val taxableIncome = maxOf(0.0, grossSalary - employeeSsnit)
    val paye = calculateGhanaPaye(taxableIncome)
    
    val totalDeductions = employeeSsnit + paye
    val netSalary = maxOf(0.0, grossSalary - totalDeductions)
    
    val totalCostOfLabor = grossSalary + employerSsnit

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Monthly Payroll & PAYE Calculator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        // Inputs
        OutlinedTextField(
            value = basicSalaryInput,
            onValueChange = { basicSalaryInput = it },
            label = { Text("Basic Salary (GHS)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        OutlinedTextField(
            value = allowancesInput,
            onValueChange = { allowancesInput = it },
            label = { Text("Allowances (GHS)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Results Card
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
                    text = "Deductions & Cost Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))

                // Gross
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Gross Salary:", style = MaterialTheme.typography.bodySmall)
                    Text(String.format(Locale.US, "GH₵ %,.2f", grossSalary), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }

                // Employee SSNIT
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Employee SSNIT (5.5%):", style = MaterialTheme.typography.bodySmall)
                    Text(String.format(Locale.US, "GH₵ %,.2f", employeeSsnit), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }

                // Taxable Income
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Taxable Income:", style = MaterialTheme.typography.bodySmall)
                    Text(String.format(Locale.US, "GH₵ %,.2f", taxableIncome), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }

                // PAYE Tax
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("PAYE Tax (GRA brackets):", style = MaterialTheme.typography.bodySmall)
                    Text(String.format(Locale.US, "GH₵ %,.2f", paye), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

                // Net Salary
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Net Take-Home Salary:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", netSalary),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))

                // Employer SSNIT
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Employer SSNIT (13%):", style = MaterialTheme.typography.bodySmall)
                    Text(String.format(Locale.US, "GH₵ %,.2f", employerSsnit), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }

                // Total Cost of Labor
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Total Cost of Labor:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(String.format(Locale.US, "GH₵ %,.2f", totalCostOfLabor), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Button(
            onClick = {
                basicSalaryInput = ""
                allowancesInput = ""
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

/**
 * Calculates Ghana Monthly PAYE Income Tax based on current GRA statutory brackets (Act 1094).
 * - First GHS 490.00 -> 0% (Cumulative: GHS 490.00)
 * - Next GHS 110.00 -> 5% (Cumulative: GHS 600.00)
 * - Next GHS 130.00 -> 10% (Cumulative: GHS 730.00)
 * - Next GHS 3,166.67 -> 17.5% (Cumulative: GHS 3,896.67)
 * - Next GHS 16,000.00 -> 25% (Cumulative: GHS 19,896.67)
 * - Next GHS 30,500.00 -> 30% (Cumulative: GHS 50,396.67)
 * - Exceeding GHS 50,396.67 -> 35%
 */
private fun calculateGhanaPaye(taxableIncome: Double): Double {
    var remaining = taxableIncome
    var tax = 0.0

    // Bracket 1: First 490 @ 0%
    val b1 = minOf(remaining, 490.0)
    tax += b1 * 0.0
    remaining -= b1
    if (remaining <= 0) return tax

    // Bracket 2: Next 110 @ 5%
    val b2 = minOf(remaining, 110.0)
    tax += b2 * 0.05
    remaining -= b2
    if (remaining <= 0) return tax

    // Bracket 3: Next 130 @ 10%
    val b3 = minOf(remaining, 130.0)
    tax += b3 * 0.10
    remaining -= b3
    if (remaining <= 0) return tax

    // Bracket 4: Next 3,166.67 @ 17.5%
    val b4 = minOf(remaining, 3166.67)
    tax += b4 * 0.175
    remaining -= b4
    if (remaining <= 0) return tax

    // Bracket 5: Next 16,000 @ 25%
    val b5 = minOf(remaining, 16000.0)
    tax += b5 * 0.25
    remaining -= b5
    if (remaining <= 0) return tax

    // Bracket 6: Next 30,500 @ 30%
    val b6 = minOf(remaining, 30500.0)
    tax += b6 * 0.30
    remaining -= b6
    if (remaining <= 0) return tax

    // Bracket 7: Exceeding 50,396.67 @ 35%
    tax += remaining * 0.35

    return tax
}

@ThemePreviews
@Composable
fun PayrollCalculatorPreview() {
    IAAAppTheme {
        PayrollCalculator()
    }
}
