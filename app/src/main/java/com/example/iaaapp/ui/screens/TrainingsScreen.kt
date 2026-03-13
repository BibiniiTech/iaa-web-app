package com.example.iaaapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.iaaapp.ui.theme.IAAAppTheme
import java.io.File
import java.io.FileOutputStream

@Composable
fun TrainingsScreen() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Training Portal",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        
        Text(
            text = "Keep up with professional training modules and seminars available for skills development.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        TrainingCategoryItem(
            title = "Video Tutorials",
            subtitle = "Step-by-step guides on relevant audit activities.",
            icon = Icons.Default.PlayCircleOutline,
            isExpandable = true
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                VideoListItem("IAA Audit Methodology 101", "https://www.youtube.com/results?search_query=internal+audit+methodology") { uriHandler.openUri(it) }
                VideoListItem("Risk Assessment Techniques", "https://www.youtube.com/results?search_query=audit+risk+assessment") { uriHandler.openUri(it) }
                VideoListItem("Reporting Best Practices", "https://www.youtube.com/results?search_query=internal+audit+reporting") { uriHandler.openUri(it) }
            }
        }
        
        TrainingCategoryItem(
            title = "Upcoming Seminars",
            subtitle = "Register for upcoming professional seminars.",
            icon = Icons.Default.Event,
            isExpandable = true
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                
                SeminarDetailItem(
                    "Leveraging the IIA Competency Framework to Build an Effective Internal Audit Function",
                    "Mar 26, 2026",
                    "02:00 PM - 04:00 PM",
                    "Online via Zoom",
                    "Free",
                    "https://us06web.zoom.us/meeting/register/CH2P5vejT7Gkf5VyGwwIbA"
                ) { uriHandler.openUri(it) }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SeminarDetailItem(
                    "Data Analytics for Auditors", 
                    "Nov 12, 2026",
                    "01:00 PM - 05:00 PM", 
                    "Online (Zoom)", 
                    "GH₵ 1,200.00",
                    "https://forms.gle/example2"
                ) { uriHandler.openUri(it) }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Don't see a topic you need?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { uriHandler.openUri("mailto:training@iaa.gov.gh") }) {
                    Text("Suggest a Training Topic")
                }
            }
        }
        
        TrainingCategoryItem(
            title = "Training Resources",
            subtitle = "Presentation files and resources.",
            icon = Icons.Default.CastForEducation,
            isExpandable = true
        ) {
            val files = getAssetsInFolder(context)
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                if (files.isEmpty()) {
                    Text(
                        text = "No training resources available",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    files.forEach { fileName ->
                        TrainingResourceFileItem(
                            title = fileName,
                            onClick = { openAssetFile(context, fileName) }
                        )
                    }
                }
            }
        }

        // Extra spacing at the bottom so content isn't covered by the floating menu
        Spacer(modifier = Modifier.height(150.dp))
    }
}

private fun getAssetsInFolder(context: Context): List<String> {
    val folder = "training_resources"
    return try {
        context.assets.list(folder)?.filter { !it.startsWith(".") && it != "gitkeep" }?.toList() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun openAssetFile(context: Context, fileName: String) {
    val folderName = "training_resources"
    try {
        val inputStream = context.assets.open("$folderName/$fileName")
        val outFile = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(outFile)
        
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Open File"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun TrainingCategoryItem(
    title: String, 
    subtitle: String, 
    icon: ImageVector,
    isExpandable: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { if (isExpandable) expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = if (isExpandable) {
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.ArrowForwardIos
                    },
                    contentDescription = null,
                    modifier = Modifier.size(if (isExpandable) 24.dp else 16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            
            if (isExpandable && expanded && content != null) {
                content()
            }
        }
    }
}

@Composable
fun VideoListItem(title: String, url: String, onVideoClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVideoClick(url) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TrainingResourceFileItem(title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.FileOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SeminarDetailItem(
    topic: String, 
    date: String, 
    time: String, 
    location: String, 
    fee: String,
    registrationUrl: String,
    onRegisterClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = topic,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        SeminarInfoRow(Icons.Default.CalendarToday, date)
        SeminarInfoRow(Icons.Default.Schedule, time)
        SeminarInfoRow(Icons.Default.LocationOn, location)
        SeminarInfoRow(Icons.Default.Payments, fee)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { onRegisterClick(registrationUrl) },
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Register", fontSize = 12.sp)
        }
    }
}

@Composable
fun SeminarInfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TrainingsScreenPreview() {
    IAAAppTheme {
        TrainingsScreen()
    }
}
