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

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bibiniitech.iaaapp.FirebaseHelper
import com.bibiniitech.iaaapp.UserProfile
import com.bibiniitech.iaaapp.data.InstitutionData
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCompletionScreen(
    prefilledEmail: String,
    prefilledName: String,
    onProfileCompleteSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val names = remember(prefilledName) { prefilledName.split(" ") }
    var firstName by remember { mutableStateOf(names.getOrNull(0) ?: "") }
    var surname by remember { mutableStateOf(names.drop(1).joinToString(" ")) }
    var email by remember { mutableStateOf(prefilledEmail) }
    var phone by remember { mutableStateOf("") }
    
    var institutionType by remember { mutableStateOf("") }
    var institutionText by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("") }
    var selectedMmda by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val regions = InstitutionData.REGIONS
    val mmdaData = InstitutionData.MMDA_DATA
    val mdaData = InstitutionData.MDA_LIST
    val soeData = InstitutionData.SOE_LIST
    val institutionTypes = InstitutionData.INSTITUTION_TYPES

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Complete Your Profile", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to IAA App!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Please complete your profile details to unlock all audit submission and resource features.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // First Name
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it; errorMessage = null },
                label = { Text("First Name", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            // Surname
            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it; errorMessage = null },
                label = { Text("Surname", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            // Email (Read Only)
            OutlinedTextField(
                value = email,
                onValueChange = {},
                readOnly = true,
                label = { Text("Email Address", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = { 
                    if (it.length <= 10) {
                        phone = it.filter { char -> char.isDigit() }
                        errorMessage = null 
                    }
                },
                label = { Text("Contact Number (e.g. 024XXXXXXX)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            DropdownField(
                label = "Institution Type",
                options = institutionTypes,
                selectedOption = institutionType,
                onOptionSelected = {
                    institutionType = it
                    institutionText = ""
                    selectedRegion = ""
                    selectedMmda = ""
                }
            )

            if (institutionType == "MDA" || institutionType == "SOE") {
                DropdownField(
                    label = if (institutionType == "MDA") "Institution (MDA)" else "Institution (SOE)",
                    options = if (institutionType == "MDA") mdaData else soeData,
                    selectedOption = institutionText,
                    onOptionSelected = { institutionText = it; errorMessage = null }
                )
            } else if (institutionType == "RCC" || institutionType == "MMDA") {
                DropdownField(
                    label = "Region",
                    options = regions,
                    selectedOption = selectedRegion
                ) { 
                    selectedRegion = it
                    selectedMmda = ""
                }

                if (selectedRegion.isNotEmpty()) {
                    val options = if (institutionType == "RCC") {
                        listOf(InstitutionData.RCC_DATA[selectedRegion] ?: "")
                    } else {
                        mmdaData[selectedRegion] ?: emptyList()
                    }
                    DropdownField(
                        label = if (institutionType == "RCC") "Institution (RCC)" else "Institution (MMDA)",
                        options = options,
                        selectedOption = selectedMmda
                    ) { selectedMmda = it }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save / Complete Button
            Button(
                onClick = {
                    val selectedInstitution = if (institutionType == "MDA" || institutionType == "SOE") institutionText else selectedMmda
                    if (firstName.isBlank() || surname.isBlank() || email.isBlank() || phone.isBlank() || institutionType.isEmpty() || selectedInstitution.isEmpty()) {
                        errorMessage = "Please fill in all fields. All fields are mandatory."
                        return@Button
                    }
                    
                    // Validate Phone Number
                    val ghPhoneRegex = Regex("^0[0-9]{9}$")
                    if (!ghPhoneRegex.matches(phone)) {
                        errorMessage = "Invalid contact number. Must be 10 digits starting with '0'."
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        val currentUser = FirebaseHelper.getCurrentUser()
                        if (currentUser != null) {
                            val profile = UserProfile(
                                uid = currentUser.uid,
                                firstName = firstName.trim(),
                                surname = surname.trim(),
                                email = email.trim(),
                                institution = selectedInstitution.trim(),
                                phone = phone.trim(),
                                role = currentUser.role.ifBlank { "user" },
                                isProfileComplete = true
                            )
                            val success = FirebaseHelper.updateUserProfile(profile)
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "Profile completed successfully!", Toast.LENGTH_SHORT).show()
                                onProfileCompleteSuccess()
                            } else {
                                errorMessage = "Failed to update profile. Please try again."
                            }
                        } else {
                            isLoading = false
                            errorMessage = "Session expired. Please sign in again."
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Complete Profile")
                }
            }
        }
    }
}
