package com.bibiniitech.iaaapp

data class BrevoEmailRequest(
    val sender: BrevoPerson,
    val to: List<BrevoPerson>,
    val cc: List<BrevoPerson>? = null,
    val bcc: List<BrevoPerson>? = null,
    val subject: String,
    val htmlContent: String,
    val attachment: List<BrevoAttachment>? = null
)

data class BrevoPerson(val name: String? = null, val email: String)

data class BrevoAttachment(val content: String, val name: String)
