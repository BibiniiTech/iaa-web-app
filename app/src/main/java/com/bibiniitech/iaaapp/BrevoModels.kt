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

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BrevoEmailRequest(
    @SerializedName("sender") val sender: BrevoPerson,
    @SerializedName("to") val to: List<BrevoPerson>,
    @SerializedName("cc") val cc: List<BrevoPerson>? = null,
    @SerializedName("bcc") val bcc: List<BrevoPerson>? = null,
    @SerializedName("subject") val subject: String,
    @SerializedName("htmlContent") val htmlContent: String,
    @SerializedName("attachment") val attachment: List<BrevoAttachment>? = null
)

@Keep
data class BrevoPerson(
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String
)

@Keep
data class BrevoAttachment(
    @SerializedName("content") val content: String,
    @SerializedName("name") val name: String
)
