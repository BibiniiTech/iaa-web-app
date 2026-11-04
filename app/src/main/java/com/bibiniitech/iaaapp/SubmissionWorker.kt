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
import androidx.core.net.toUri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SubmissionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val category = inputData.getString("category") ?: return Result.failure()
        val institutionType = inputData.getString("institutionType") ?: ""
        val institution = inputData.getString("institution") ?: ""
        val region = inputData.getString("region") ?: ""
        val senderName = inputData.getString("senderName") ?: ""
        val senderEmail = inputData.getString("senderEmail") ?: ""
        val reportName = inputData.getString("reportName") ?: ""
        val subject = inputData.getString("subject") ?: ""
        val body = inputData.getString("body") ?: ""
        val apiKey = inputData.getString("apiKey") ?: ""
        val toRecipients = inputData.getStringArray("toRecipients")?.toList() ?: emptyList()
        val ccRecipients = inputData.getStringArray("ccRecipients")?.toList() ?: emptyList()
        val bccRecipients = inputData.getStringArray("bccRecipients")?.toList() ?: emptyList()
        val attachmentsJson = inputData.getString("attachmentsJson") ?: "[]"
        val userId = inputData.getString("userId") ?: ""

        val finalToRecipients = toRecipients.toMutableList()
        if (senderEmail.isNotBlank() && !finalToRecipients.contains(senderEmail)) {
            finalToRecipients.add(senderEmail)
        }

        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
        val attachmentsList: List<Pair<String, String>> = Gson().fromJson(attachmentsJson, type)
        val attachments = attachmentsList.map { it.first.toUri() to it.second }

        val emailSender = EmailSender(applicationContext)
        val success = emailSender.sendEmailWithBrevo(
            to = finalToRecipients,
            cc = ccRecipients,
            bcc = bccRecipients,
            subject = subject,
            body = body,
            attachments = attachments,
            apiKey = apiKey
        )

        return if (success) {
            FirebaseHelper.logSubmission(
                category = category,
                institutionType = institutionType,
                institution = institution,
                region = region,
                senderName = senderName,
                senderEmail = senderEmail,
                fileNames = attachments.map { it.second },
                reportName = reportName,
                userId = userId
            )
            Result.success()
        } else {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
