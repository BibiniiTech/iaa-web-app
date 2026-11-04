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

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bibiniitech.iaaapp.CloudEmailManager
import com.bibiniitech.iaaapp.EmailConfig
import com.bibiniitech.iaaapp.EmailSender
import com.bibiniitech.iaaapp.FirebaseHelper
import com.bibiniitech.iaaapp.data.AppDatabase
import com.bibiniitech.iaaapp.data.InstitutionData
import com.bibiniitech.iaaapp.data.SubmissionDraft
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusOfImplementationSubmissionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val draftDao = remember { AppDatabase.getDatabase(context).submissionDraftDao() }

    var institutionType by remember { mutableStateOf("") }
    var institutionText by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("") }
    var selectedMmda by remember { mutableStateOf("") }
    var quarter by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }
    var senderEmail by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(value = false) }

    val uploadedFiles = remember { mutableStateListOf<Pair<Uri, String>>() }

    LaunchedEffect(Unit) {
        FirebaseHelper.ensureRecipientEmailsLoaded()
        val draft = draftDao.getDraftByCategory("soi")
        if (draft != null) {
            institutionType = draft.institutionType
            if (draft.institutionType == "MDA" || draft.institutionType == "SOE") {
                institutionText = draft.institution
            } else {
                selectedMmda = draft.institution
            }
            selectedRegion = draft.region
            quarter = draft.period
            senderName = draft.senderName
            senderEmail = draft.senderEmail
        } else {
            val user = FirebaseHelper.getCurrentUser()
            if (user != null) {
                val fullProfile = FirebaseHelper.fetchUserProfile(user.uid) ?: user
                senderName = "${fullProfile.firstName} ${fullProfile.surname}".trim()
                senderEmail = fullProfile.email
            }
        }
    }

    // Auto-save draft
    LaunchedEffect(institutionType, institutionText, selectedRegion, selectedMmda, quarter, senderName, senderEmail) {
        val institution = if (institutionType == "MDA" || institutionType == "SOE") institutionText else selectedMmda
        if (institution.isNotEmpty() || quarter.isNotEmpty()) {
            draftDao.saveOrUpdateDraft(
                SubmissionDraft(
                    category = "soi",
                    institutionType = institutionType,
                    institution = institution,
                    region = selectedRegion,
                    period = quarter,
                    senderName = senderName,
                    senderEmail = senderEmail,
                )
            )
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { selectedUri: Uri? ->
        selectedUri?.let { uri ->
            val totalSize = calculateTotalSize(context, uploadedFiles.map { it.first } + uri)
            if (totalSize > (25 * 1024 * 1024)) {
                Toast.makeText(context, "Total attachment size exceeds 25MB limit.", Toast.LENGTH_LONG).show()
                return@let
            }

            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: uri.path?.substringAfterLast('/') ?: "Unknown File"

            if (uploadedFiles.size < 5) {
                uploadedFiles.add(uri to fileName)
            }
        }
    }

    val regions = InstitutionData.REGIONS
    val mmdaData = InstitutionData.MMDA_DATA
    val mdaData = InstitutionData.MDA_LIST
    val soeData = InstitutionData.SOE_LIST
    val institutionTypes = InstitutionData.INSTITUTION_TYPES

    val quarters = listOf("First", "Second", "Third", "Fourth")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Status of Implementation Submission", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DropdownField(
                label = "Institution Type",
                options = institutionTypes,
                selectedOption = institutionType,
            ) {
                institutionType = it
                institutionText = ""
                selectedRegion = ""
                selectedMmda = ""
            }

            if (institutionType == "MDA" || institutionType == "SOE") {
                DropdownField(
                    label = if (institutionType == "MDA") "Institution (MDA)" else "Institution (SOE)",
                    options = if (institutionType == "MDA") mdaData else soeData,
                    selectedOption = institutionText,
                ) { institutionText = it }
            } else if (institutionType == "RCC" || institutionType == "MMDA") {
                DropdownField(
                    label = "Region",
                    options = regions,
                    selectedOption = selectedRegion,
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
                        selectedOption = selectedMmda,
                    ) { selectedMmda = it }
                }
            }

            if (institutionType.isNotEmpty()) {
                DropdownField(
                    label = "Quarter",
                    options = quarters,
                    selectedOption = quarter,
                ) { quarter = it }

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender's Name", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )

                OutlinedTextField(
                    value = senderEmail,
                    onValueChange = { senderEmail = it },
                    label = { Text("Sender's Email", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Upload Files (Max 5)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uploadedFiles.size < 5) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attach File")
                        }
                    }

                    uploadedFiles.forEachIndexed { index, pair ->
                        ListItem(
                            headlineContent = {
                                Text(pair.second, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            },
                            trailingContent = {
                                IconButton(onClick = { uploadedFiles.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Please use your normal submissions channel until the Agency approves the use of this app",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                        isSending = true
                        val institution = if (institutionType == "MDA" || institutionType == "SOE") institutionText else selectedMmda

                        val subject = "$quarter Quarter Status of Implementation on Audit Reports for $institution"
                        val body = """
                            Kindly find attached the submission of the $quarter Quarter Quarter Status of Implementation on Audit Reports for $institution
                            
                            Sent by $senderName $senderEmail
                        """.trimIndent()

                        val category = "soi"
                        val toRecipients = mutableListOf<String>()
                        val ccRecipients = mutableListOf<String>()
                        val bccRecipients = listOf(EmailConfig.BCC_RECIPIENT)

                        if (institutionType == "MDA" || institutionType == "SOE") {
                            toRecipients.addAll(CloudEmailManager.getMdaTo(category))
                            ccRecipients.addAll(CloudEmailManager.getMdaCc(category))
                        } else {
                            toRecipients.addAll(CloudEmailManager.getMmdaTo(category))
                            ccRecipients.addAll(CloudEmailManager.getRegionCc(category, selectedRegion))
                        }

                        // Add sender to recipients for a copy
                        if (senderEmail.isNotBlank() && !toRecipients.contains(senderEmail)) {
                            toRecipients.add(senderEmail)
                        }

                        if (toRecipients.isEmpty()) {
                            Toast.makeText(context, "No recipient emails configured for this category.", Toast.LENGTH_LONG).show()
                            isSending = false
                            return@Button
                        }

                        val apiKey = if (isPreview) "" else Firebase.remoteConfig.getString("brevo_api_key")

                        if (apiKey.isBlank()) {
                            Toast.makeText(context, "Email service is not configured. Please check your internet connection.", Toast.LENGTH_LONG).show()
                            isSending = false
                            return@Button
                        }

                        scope.launch {
                            val emailSender = EmailSender(context)
                            val success = emailSender.sendEmailWithBrevo(
                                to = toRecipients,
                                cc = ccRecipients,
                                bcc = bccRecipients,
                                subject = subject,
                                body = body,
                                attachments = uploadedFiles.toList(),
                                apiKey = apiKey
                            )

                            if (success) {
                                FirebaseHelper.logSubmission(
                                    category = category,
                                    institutionType = institutionType,
                                    institution = institution,
                                    region = selectedRegion,
                                    senderName = senderName,
                                    senderEmail = senderEmail,
                                    fileNames = uploadedFiles.map { it.second },
                                    reportName = "$quarter Quarter SOI Report"
                                )
                                draftDao.deleteDraftByCategory("soi")
                                Toast.makeText(context, "Report Submitted Successfully!", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "Failed to submit report. Please check your connection.", Toast.LENGTH_LONG).show()
                            }
                            isSending = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending && (
                        ((institutionType == "MDA" || institutionType == "SOE") && institutionText.isNotEmpty()) ||
                        ((institutionType == "RCC" || institutionType == "MMDA") && selectedMmda.isNotEmpty())
                    ) && quarter.isNotEmpty() && senderName.isNotEmpty() && senderEmail.isNotEmpty() && uploadedFiles.isNotEmpty()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Submit Status Report")
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun StatusOfImplementationSubmissionScreenPreview() {
    IAAAppTheme {
        StatusOfImplementationSubmissionScreen { }
    }
}
