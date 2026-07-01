package com.bibiniitech.iaaapp

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class SubmissionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

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
        val smtpUser = inputData.getString("smtpUser") ?: ""
        val smtpPass = inputData.getString("smtpPass") ?: ""
        
        val toRecipients = inputData.getStringArray("toRecipients")?.toList() ?: emptyList()
        val ccRecipients = inputData.getStringArray("ccRecipients")?.toList() ?: emptyList()
        val bccRecipients = inputData.getStringArray("bccRecipients")?.toList() ?: emptyList()
        
        // Attachments are passed as a JSON list of Uri/Name pairs
        val attachmentsJson = inputData.getString("attachmentsJson") ?: "[]"
        val attachmentType = object : TypeToken<List<Pair<String, String>>>() {}.type
        val attachmentsData: List<Pair<String, String>> = Gson().fromJson(attachmentsJson, attachmentType)
        val attachments = attachmentsData.map { Uri.parse(it.first) to it.second }

        val emailSender = EmailSender(applicationContext)

        return try {
            Log.d("SubmissionWorker", "Attempting background submission for $institution")
            
            val success = emailSender.sendEmailWithAttachments(
                to = toRecipients,
                cc = ccRecipients,
                bcc = bccRecipients,
                subject = subject,
                body = body,
                attachments = attachments,
                smtpUser = smtpUser,
                smtpPass = smtpPass
            )

            if (success) {
                FirebaseHelper.logSubmission(
                    category = category,
                    institutionType = institutionType,
                    institution = institution,
                    region = region,
                    senderName = senderName,
                    senderEmail = senderEmail,
                    fileNames = attachments.map { it.second },
                    reportName = reportName
                )
                Log.d("SubmissionWorker", "Background submission successful")
                Result.success()
            } else {
                Log.w("SubmissionWorker", "Email sending failed, retrying...")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SubmissionWorker", "Error in SubmissionWorker", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
