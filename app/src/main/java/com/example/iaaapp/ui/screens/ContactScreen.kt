package com.example.iaaapp.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Desk
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.iaaapp.R
import com.example.iaaapp.ui.theme.IAAAppTheme

@Composable
fun ContactScreen() {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Contact Us",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        ContactInfoCard(
            icon = Icons.Default.LocationOn,
            title = "Head Office",
            content = "Tumu Avenue, Kanda Accra - Ghana\nDigital Address: GV-002-6511"
        )

        MapCard(
            locationQuery = "Internal+Audit+Agency+Tumu+Avenue+Kanda+Accra",
            onClick = {
                val gmmIntentUri = "geo:0,0?q=Internal+Audit+Agency+Tumu+Avenue+Kanda+Accra".toUri()
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                context.startActivity(mapIntent)
            }
        )
        
        ContactInfoCard(
            icon = Icons.Default.Phone,
            title = "Phone",
            content = "+233 (0) 362 196 941",
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, "tel:+233362196941".toUri())
                context.startActivity(intent)
            }
        )
        
        ContactInfoCard(
            icon = Icons.Default.Email,
            title = "Email",
            content = "info@iaa.gov.gh\niaamails@iaa.gov.gh",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, "mailto:info@iaa.gov.gh".toUri())
                context.startActivity(intent)
            }
        )

        ContactInfoCard(
            icon = Icons.Default.Desk,
            title = "Specific Desks",
            content = "CCCC: +233 (0) 362 196 941\nNACAP: +233 (0) 362 196 941\nTrainings: +233 (0) 362 196 941"
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Follow Us",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocialMediaIconButton(
                painter = if (isPreview) null else painterResource(id = R.drawable.ic_facebook),
                label = "f",
                backgroundColor = Color(0xFF1877F2),
                url = "https://web.facebook.com/internalauditagency/?_rdc=1&_rdr#"
            )
            Spacer(modifier = Modifier.width(24.dp))
            SocialMediaIconButton(
                painter = if (isPreview) null else painterResource(id = R.drawable.ic_instagram),
                label = "i",
                backgroundColor = Color(0xFFE4405F),
                url = "https://www.instagram.com/iaa.gov.gh/"
            )
            Spacer(modifier = Modifier.width(24.dp))
            SocialMediaIconButton(
                painter = if (isPreview) null else painterResource(id = R.drawable.ic_x),
                label = "X",
                backgroundColor = Color(0xFF000000),
                url = "https://x.com/IAA_Ghana"
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Extra spacing at the bottom so content isn't covered by the floating menu
        Spacer(modifier = Modifier.height(150.dp))
    }
}

@Composable
fun SocialMediaIconButton(
    painter: Painter? = null,
    label: String,
    backgroundColor: Color,
    url: String
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            },
        color = backgroundColor,
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp), // Slightly larger for custom icons
                    tint = Color.Unspecified // Keep original icon colors if they are not monochromatic
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapCard(locationQuery: String, onClick: () -> Unit) {
    val isPreview = LocalInspectionMode.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp) // Increased height to show the map
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (isPreview) {
                    // WebView often causes java.lang.NoSuchMethodError (getSettings) in Android Studio Preview
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Map Preview\n(WebView not supported in Preview)",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                loadUrl("https://www.google.com/maps/search/?api=1&query=$locationQuery")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Google Maps Location",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap to open in full app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Open in Maps",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ContactInfoCard(
    icon: ImageVector, 
    title: String, 
    content: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (onClick != null && title !in listOf("Phone", "Email")) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Open in Maps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactScreenPreview() {
    IAAAppTheme {
        ContactScreen()
    }
}
