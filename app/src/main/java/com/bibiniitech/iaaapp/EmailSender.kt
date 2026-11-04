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
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.concurrent.TimeUnit

class EmailSender(private val context: Context) {

    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        private val gson = Gson()
    }

    suspend fun sendEmailWithBrevo(
        to: List<String>,
        cc: List<String> = emptyList(),
        bcc: List<String> = emptyList(),
        subject: String,
        body: String,
        attachments: List<Pair<Uri, String>>,
        apiKey: String,
        senderEmail: String = "bibiniitech@gmail.com", // This should be a verified email in Brevo
        senderName: String = "IAA App"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val uniqueToEmails = to.map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
            val toPersons = uniqueToEmails.map { BrevoPerson(email = it) }
            if (toPersons.isEmpty()) {
                Log.e("EmailSender", "No valid 'TO' recipients provided")
                return@withContext false
            }

            val seenEmails = uniqueToEmails.map { it.lowercase() }.toMutableSet()
            val ccPersons = cc.map { it.trim() }
                .filter { it.isNotBlank() && seenEmails.add(it.lowercase()) }
                .map { BrevoPerson(email = it) }
                .ifEmpty { null }
            val bccPersons = bcc.map { it.trim() }
                .filter { it.isNotBlank() && seenEmails.add(it.lowercase()) }
                .map { BrevoPerson(email = it) }
                .ifEmpty { null }

            val brevoAttachments = attachments.mapNotNull { (uri, fileName) ->
                try {
                    val bytes = FileCompressionUtils.getCompressedAttachmentBytes(context, uri, fileName)
                    if (bytes != null) {
                        BrevoAttachment(
                            content = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP),
                            name = fileName
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e("EmailSender", "Failed to process attachment: $fileName", e)
                    null
                }
            }.ifEmpty { null }

            val htmlContent = "<html><body>${body.replace("\n", "<br>")}</body></html>"

            val brevoRequest = BrevoEmailRequest(
                sender = BrevoPerson(name = senderName, email = senderEmail),
                to = toPersons,
                cc = ccPersons,
                bcc = bccPersons,
                subject = subject,
                htmlContent = htmlContent,
                attachment = brevoAttachments
            )

            val json = gson.toJson(brevoRequest)
            val mediaType = "application/json".toMediaType()
            val requestBody = json.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.brevo.com/v3/smtp/email")
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("EmailSender", "Brevo API Error: ${response.code} - ${response.body?.string()}")
                    return@withContext false
                }
                Log.d("EmailSender", "Email sent successfully via Brevo")
                true
            }
        } catch (e: Exception) {
            Log.e("EmailSender", "Failed to send email via Brevo", e)
            false
        }
    }
}
