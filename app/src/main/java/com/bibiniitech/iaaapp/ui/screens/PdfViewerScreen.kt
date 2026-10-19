package com.bibiniitech.iaaapp.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(url: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(value = true) }
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "pdf_cache").apply { mkdirs() }
                val urlHash = MessageDigest.getInstance("SHA-256")
                    .digest(url.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                val file = File(cacheDir, "doc_$urlHash.pdf")

                if (!file.exists() || file.length() == 0L) {
                    val tempFile = File(cacheDir, "doc_${urlHash}.tmp")
                    URL(url).openStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.length() > 0L) {
                        tempFile.renameTo(file)
                    } else {
                        tempFile.delete()
                    }
                    // Evict oldest cached PDFs if cache exceeds 25 files
                    val cachedFiles = cacheDir.listFiles { f -> f.extension == "pdf" }
                    if (cachedFiles != null && cachedFiles.size > 25) {
                        cachedFiles.sortedBy { it.lastModified() }
                            .take(cachedFiles.size - 25)
                            .forEach { it.delete() }
                    }
                } else {
                    file.setLastModified(System.currentTimeMillis())
                }

                val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(input)
                val pages = mutableListOf<Bitmap>()
                
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pages.add(bitmap)
                    page.close()
                }
                
                renderer.close()
                input.close()
                
                pdfPages = pages
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load PDF: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center).padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pdfPages) { bitmap ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "PDF Page",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun PdfViewerScreenPreview() {
    IAAAppTheme {
        PdfViewerScreen(url = "", title = "Sample Document", onBack = {})
    }
}
