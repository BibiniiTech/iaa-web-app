package com.example.iaaapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.iaaapp.ui.theme.IAAAppTheme
import java.io.File
import java.io.FileOutputStream

@Composable
fun ResourcesScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Resources Portal",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        
        Text(
            text = "Access laws, guidelines, and templates to support your internal audit functions.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        ResourceCategoryItem(
            title = "Legislations",
            subtitle = "Acts, Regulations, L.I.s, etc.",
            icon = Icons.Default.Gavel
        ) {
            val folder = "legislations"
            val files = getAssetsInFolder(context, folder)
            if (files.isEmpty()) {
                Text(
                    text = "No legislations available",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                files.forEach { fileName ->
                    LawItem(
                        title = fileName,
                        onClick = { openAssetFile(context, folder, fileName) }
                    )
                }
            }
        }
        
        ResourceCategoryItem(
            title = "Guidelines & Manuals",
            subtitle = "Standard procedures, Directives and operational manuals.",
            icon = Icons.AutoMirrored.Filled.Rule
        ) {
            val folder = "guidelines"
            val files = getAssetsInFolder(context, folder)
            if (files.isEmpty()) {
                Text(
                    text = "No guidelines available",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                files.forEach { fileName ->
                    LawItem(
                        title = fileName,
                        onClick = { openAssetFile(context, folder, fileName) }
                    )
                }
            }
        }
        
        ResourceCategoryItem(
            title = "Reporting Templates",
            subtitle = "Standard formats for various reports.",
            icon = Icons.Default.Description
        ) {
            val folder = "templates"
            val files = getAssetsInFolder(context, folder)
            if (files.isEmpty()) {
                Text(
                    text = "No templates available",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                files.forEach { fileName ->
                    LawItem(
                        title = fileName,
                        onClick = { openAssetFile(context, folder, fileName) }
                    )
                }
            }
        }

        // Extra spacing at the bottom so content isn't covered by the floating menu
        Spacer(modifier = Modifier.height(150.dp))
    }
}

private fun getAssetsInFolder(context: Context, folder: String): List<String> {
    return try {
        context.assets.list(folder)?.filter { !it.startsWith(".") && it != "gitkeep" }?.toList() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun openAssetFile(context: Context, folder: String, fileName: String) {
    try {
        val inputStream = context.assets.open("$folder/$fileName")
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
fun ResourceCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .clickable { if (content != null) expanded = !expanded }
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (content != null) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            if (expanded && content != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun LawItem(title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
                contentDescription = "Document",
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
                contentDescription = "Open File",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResourcesScreenPreview() {
    IAAAppTheme {
        ResourcesScreen()
    }
}
