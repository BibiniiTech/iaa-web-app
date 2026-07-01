package com.bibiniitech.iaaapp

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.google.gson.Gson

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
        smtpUser: String,
        smtpPass: String,
        toRecipients: List<String>,
        ccRecipients: List<String>,
        bccRecipients: List<String>,
        attachments: List<Pair<Uri, String>>
    ) {
        val attachmentsJson = Gson().toJson(attachments.map { it.first.toString() to it.second })

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
            "smtpUser" to smtpUser,
            "smtpPass" to smtpPass,
            "toRecipients" to toRecipients.toTypedArray(),
            "ccRecipients" to ccRecipients.toTypedArray(),
            "bccRecipients" to bccRecipients.toTypedArray(),
            "attachmentsJson" to attachmentsJson
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val submissionRequest = OneTimeWorkRequestBuilder<SubmissionWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .addTag("submission")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "submission_${System.currentTimeMillis()}", // Or use a more stable ID if needed
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            submissionRequest
        )
    }
}
