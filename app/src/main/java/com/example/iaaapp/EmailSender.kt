package com.example.iaaapp

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import java.io.File
import java.io.FileOutputStream

class EmailSender(private val context: Context) {

    suspend fun sendEmailWithAttachments(
        to: String,
        subject: String,
        body: String,
        attachments: List<Pair<Uri, String>>
    ): Boolean = withContext(Dispatchers.IO) {
        val user = "YOUR_EMAIL@gmail.com" // Replace with your email
        val password = "YOUR_APP_PASSWORD" // Replace with your app password

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(user, password)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(user))
                addRecipient(Message.RecipientType.TO, InternetAddress(to))
                setSubject(subject)
            }

            val multipart = MimeMultipart()

            // Text part
            val textPart = MimeBodyPart()
            textPart.setText(body)
            multipart.addBodyPart(textPart)

            // Attachment parts
            attachments.forEach { (uri, fileName) ->
                val attachmentPart = MimeBodyPart()
                val file = copyUriToFile(uri, fileName)
                if (file != null) {
                    val source = FileDataSource(file)
                    attachmentPart.dataHandler = DataHandler(source)
                    attachmentPart.fileName = fileName
                    multipart.addBodyPart(attachmentPart)
                }
            }

            message.setContent(multipart)
            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyUriToFile(uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
