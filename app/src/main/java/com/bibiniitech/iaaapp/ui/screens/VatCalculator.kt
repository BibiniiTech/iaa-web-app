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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import java.util.Locale

@Composable
fun VatCalculator() {
    val standardSchemeOption = "Standard Scheme (21.9% Effective)"
    val flatRateOption = "Flat Rate Scheme - VFRS (4.0%)"
    val schemeOptions = listOf(standardSchemeOption, flatRateOption)

    var selectedScheme by remember { mutableStateOf(standardSchemeOption) }
    var baseInput by remember { mutableStateOf("") }
    var grossInput by remember { mutableStateOf("") }
    var isUpdatingFromBase by remember { mutableStateOf(true) }

    val isStandard = selectedScheme == standardSchemeOption

    // Compute base and gross values reactively
    val baseVal = if (isUpdatingFromBase) {
        baseInput.toDoubleOrNull() ?: 0.0
    } else {
        val g = grossInput.toDoubleOrNull() ?: 0.0
        if (isStandard) g / 1.219 else g / 1.04
    }

    val grossVal = if (isUpdatingFromBase) {
        val b = baseInput.toDoubleOrNull() ?: 0.0
        if (isStandard) b * 1.219 else b * 1.04
    } else {
        grossInput.toDoubleOrNull() ?: 0.0
    }

    // Individual itemized levies & taxes
    val nhilVal = if (isStandard) baseVal * 0.025 else 0.0
    val getfundVal = if (isStandard) baseVal * 0.025 else 0.0
    val covidVal = baseVal * 0.010
    val totalLevies = nhilVal + getfundVal + covidVal

    val vatBase = if (isStandard) baseVal + totalLevies else baseVal
    val vatVal = if (isStandard) vatBase * 0.15 else baseVal * 0.03
    val totalTaxAndLevies = totalLevies + vatVal

    // Sync input fields when scheme toggles
    LaunchedEffect(selectedScheme) {
        if (isUpdatingFromBase) {
            val b = baseInput.toDoubleOrNull() ?: 0.0
            val g = if (isStandard) b * 1.219 else b * 1.04
            grossInput = if (b > 0) String.format(Locale.US, "%.2f", g) else ""
        } else {
            val g = grossInput.toDoubleOrNull() ?: 0.0
            val b = if (isStandard) g / 1.219 else g / 1.04
            baseInput = if (g > 0) String.format(Locale.US, "%.2f", b) else ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "VAT & Statutory Levies Calculator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        DropdownField(
            label = "Tax Scheme",
            options = schemeOptions,
            selectedOption = selectedScheme
        ) {
            selectedScheme = it
        }

        // Taxable Base Supply Input Field (Forward)
        OutlinedTextField(
            value = if (isUpdatingFromBase) baseInput else if (baseVal > 0) String.format(Locale.US, "%.2f", baseVal) else "",
            onValueChange = {
                isUpdatingFromBase = true
                baseInput = it
                val b = it.toDoubleOrNull() ?: 0.0
                val g = if (isStandard) b * 1.219 else b * 1.04
                grossInput = if (b > 0) String.format(Locale.US, "%.2f", g) else ""
            },
            label = { Text("Taxable Base Supply (Excl. VAT & Levies)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            placeholder = { Text("0.00", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Gross Invoice Amount Input Field (Reverse)
        OutlinedTextField(
            value = if (!isUpdatingFromBase) grossInput else if (grossVal > 0) String.format(Locale.US, "%.2f", grossVal) else "",
            onValueChange = {
                isUpdatingFromBase = false
                grossInput = it
                val g = it.toDoubleOrNull() ?: 0.0
                val b = if (isStandard) g / 1.219 else g / 1.04
                baseInput = if (g > 0) String.format(Locale.US, "%.2f", b) else ""
            },
            label = { Text("Gross Invoice Amount (Incl. VAT & Levies)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            placeholder = { Text("0.00", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Calculation Breakdown Card
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
                    text = "Statutory Breakdown (GRA Standard)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                // Base Supply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Taxable Base Supply:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", baseVal),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isStandard) {
                    // NHIL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NHIL (2.5%):", style = MaterialTheme.typography.bodySmall)
                        Text(
                            String.format(Locale.US, "GH₵ %,.2f", nhilVal),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // GETFund
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GETFund Levy (2.5%):", style = MaterialTheme.typography.bodySmall)
                        Text(
                            String.format(Locale.US, "GH₵ %,.2f", getfundVal),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // COVID-19 Health Recovery Levy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("COVID-19 Levy (1.0%):", style = MaterialTheme.typography.bodySmall)
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", covidVal),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Total Levies Subtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (isStandard) "Total Levies (6.0%):" else "Total Levies (1.0%):",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", totalLevies),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // VAT (15% on VAT Base or 3% Flat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (isStandard) "VAT (15% on Base + Levies):" else "VAT Flat Rate (3.0%):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", vatVal),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Total Tax & Levies
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total Tax & Levies (${if (isStandard) "21.9%" else "4.0%"}):",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", totalTaxAndLevies),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

                // Gross Payable / Total Invoice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Gross Invoice:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        String.format(Locale.US, "GH₵ %,.2f", grossVal),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isStandard)
                        "Statutory basis: Ghana Value Added Tax Act, 2013 (Act 870) as amended. NHIL (2.5%), GETFund (2.5%), and COVID-19 (1.0%) levies form part of the taxable base for 15% VAT."
                    else
                        "Statutory basis: VAT Flat Rate Scheme (VFRS) under Act 870 as amended. Applied at 3% VAT + 1% COVID-19 Health Recovery Levy for eligible retailers and wholesalers.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Justify
                )
            }
        }

        Button(
            onClick = {
                baseInput = ""
                grossInput = ""
                isUpdatingFromBase = true
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
fun VatCalculatorPreview() {
    IAAAppTheme {
        VatCalculator()
    }
}
