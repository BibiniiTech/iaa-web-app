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
