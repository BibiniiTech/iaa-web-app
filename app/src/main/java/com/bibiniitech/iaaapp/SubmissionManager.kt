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

package com.bibiniitech.iaaapp

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

object SubmissionManager {
    fun scheduleSubmission(
        context: Context,
        category: String,
        institutionType: String,
        institution: String,
        region: String,
        senderName: String,
        senderEmail: String,
        reportName: String,
        subject: String,
        body: String,
        apiKey: String,
        toRecipients: List<String>,
        ccRecipients: List<String>,
        bccRecipients: List<String>,
        attachments: List<Pair<Uri, String>>,
        userId: String = ""
    ) {
        val attachmentsJson = Gson().toJson(attachments.map { it.first.toString() to it.second })
        val effectiveUserId = userId.ifEmpty { FirebaseHelper.getCurrentUser()?.uid ?: "" }

        val data = workDataOf(
            "category" to category,
            "institutionType" to institutionType,
            "institution" to institution,
            "region" to region,
            "senderName" to senderName,
            "senderEmail" to senderEmail,
            "reportName" to reportName,
            "subject" to subject,
            "body" to body,
            "apiKey" to apiKey,
            "toRecipients" to toRecipients.toTypedArray(),
            "ccRecipients" to ccRecipients.toTypedArray(),
            "bccRecipients" to bccRecipients.toTypedArray(),
            "attachmentsJson" to attachmentsJson,
            "userId" to effectiveUserId
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SubmissionWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "submission_${System.currentTimeMillis()}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
