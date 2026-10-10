package com.bibiniitech.iaaapp.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.bibiniitech.iaaapp.DbDocument
import com.bibiniitech.iaaapp.FirebaseHelper
import com.bibiniitech.iaaapp.R
import com.bibiniitech.iaaapp.ui.navigation.Screen
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews

@Composable
fun ResourcesScreen(navController: androidx.navigation.NavController? = null, initialExpandedCategory: String? = null) {
    val context = LocalContext.current
    
    // Dynamic documents from Firestore
    val syncToken by FirebaseHelper.syncToken.collectAsState()
    var legislationDocs by remember { mutableStateOf<List<DbDocument>>(emptyList()) }
    var guidelineDocs by remember { mutableStateOf<List<DbDocument>>(emptyList()) }
    var templateDocs by remember { mutableStateOf<List<DbDocument>>(emptyList()) }
    var checklistDocs by remember { mutableStateOf<List<DbDocument>>(emptyList()) }
    var otherDocs by remember { mutableStateOf<List<DbDocument>>(emptyList()) }

    LaunchedEffect(syncToken) {
        legislationDocs = FirebaseHelper.getDocuments("legislations").sortedBy { it.name.lowercase() }
        guidelineDocs = FirebaseHelper.getDocuments("guidelines").sortedBy { it.name.lowercase() }
        templateDocs = FirebaseHelper.getDocuments("templates").sortedBy { it.name.lowercase() }
        checklistDocs = FirebaseHelper.getDocuments("checklists").sortedBy { it.name.lowercase() }
        otherDocs = FirebaseHelper.getDocuments("others").sortedBy { it.name.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = stringResource(id = R.string.resources_portal_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        
        Text(
            text = stringResource(id = R.string.resources_portal_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // 1. Legislations Card
        ResourceCategoryItem(
            title = stringResource(id = R.string.legislations_category),
            subtitle = stringResource(id = R.string.legislations_subtitle),
            icon = Icons.Default.Gavel,
            isInitiallyExpanded = initialExpandedCategory == "legislations"
        ) {
            if (legislationDocs.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_legislations),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                legislationDocs.forEach { doc ->
                    LawItem(
                        title = doc.name,
                        isOnline = true,
                        onClick = { 
                            if (navController != null && doc.downloadUrl.lowercase().endsWith(".pdf")) {
                                navController.navigate(Screen.PdfViewer.createRoute(doc.downloadUrl, doc.name))
                            } else {
                                openRemoteFile(context, doc.downloadUrl)
                            }
                        }
                    )
                }
            }
        }
        
        // 2. Guidelines Card
        ResourceCategoryItem(
            title = stringResource(id = R.string.guidelines_category),
            subtitle = stringResource(id = R.string.guidelines_subtitle),
            icon = Icons.AutoMirrored.Filled.Rule,
            isInitiallyExpanded = initialExpandedCategory == "guidelines"
        ) {
            if (guidelineDocs.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_guidelines),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                guidelineDocs.forEach { doc ->
                    LawItem(
                        title = doc.name,
                        isOnline = true,
                        onClick = { 
                            if (navController != null && doc.downloadUrl.lowercase().endsWith(".pdf")) {
                                navController.navigate(Screen.PdfViewer.createRoute(doc.downloadUrl, doc.name))
                            } else {
                                openRemoteFile(context, doc.downloadUrl)
                            }
                        }
                    )
                }
            }
        }
        
        // 3. Reporting Templates Card
        ResourceCategoryItem(
            title = stringResource(id = R.string.templates_category),
            subtitle = stringResource(id = R.string.templates_subtitle),
            icon = Icons.Default.Description,
            isInitiallyExpanded = initialExpandedCategory == "templates"
        ) {
            if (templateDocs.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_templates),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                templateDocs.forEach { doc ->
                    LawItem(
                        title = doc.name,
                        isOnline = true,
                        onClick = { 
                            if (navController != null && doc.downloadUrl.lowercase().endsWith(".pdf")) {
                                navController.navigate(Screen.PdfViewer.createRoute(doc.downloadUrl, doc.name))
                            } else {
                                openRemoteFile(context, doc.downloadUrl)
                            }
                        }
                    )
                }
            }
        }



        // 6. Checklists and Walk-Throughs Card
        ResourceCategoryItem(
            title = "Checklists & Walk-Throughs",
            subtitle = "Audit step checklists and templates.",
            icon = Icons.Default.Checklist,
            isInitiallyExpanded = false
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InteractiveChecklists()
                if (checklistDocs.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Downloadable Checklists & Manuals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
                    checklistDocs.forEach { doc ->
                        LawItem(title = doc.name, isOnline = true, onClick = { openRemoteFile(context, doc.downloadUrl) })
                    }
                }
            }
        }

        // 7. Other Resources Card
        ResourceCategoryItem(
            title = "Other Resources",
            subtitle = "Additional audit materials and miscellaneous files.",
            icon = Icons.Default.MoreHoriz,
            isInitiallyExpanded = false
        ) {
            if (otherDocs.isEmpty()) {
                Text("No additional resources available.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            } else {
                otherDocs.forEach { doc ->
                    LawItem(
                        title = doc.name,
                        isOnline = true,
                        onClick = { 
                            if (doc.downloadUrl.lowercase().endsWith(".pdf")) {
                                navController?.navigate(Screen.PdfViewer.createRoute(doc.downloadUrl, doc.name))
                            } else {
                                openRemoteFile(context, doc.downloadUrl)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(150.dp))
    }
}

@Composable
fun InteractiveChecklists() {
    var selectedChecklist by remember { mutableStateOf("Pre-Audit Planning") }
    val checklists = listOf("Pre-Audit Planning", "Fieldwork Walk-Through", "Quality Assurance Review")

    val preAuditItems = remember { mutableStateListOf(
        "Establish audit objective and scope" to false,
        "Request background financial documentation" to false,
        "Assess risk landscape & initial compliance criteria" to false,
        "Assign audit team roles and responsibilities" to false,
        "Schedule entry conference with auditee" to false
    ) }

    val fieldworkItems = remember { mutableStateListOf(
        "Conduct walk-through of transaction controls" to false,
        "Select sample vouchers and documentation" to false,
        "Test adherence to PFM Act (Act 921) rules" to false,
        "Document control exceptions and errors" to false,
        "Discuss findings with departmental heads" to false
    ) }

    val qaItems = remember { mutableStateListOf(
        "Review audit working papers against standards" to false,
        "Verify criteria source matches PFM guidelines" to false,
        "Confirm evidence supports each exception finding" to false,
        "Format draft audit report for review" to false,
        "Obtain sign-off from Head of Audit" to false
    ) }

    val activeItems = when (selectedChecklist) {
        "Pre-Audit Planning" -> preAuditItems
        "Fieldwork Walk-Through" -> fieldworkItems
        else -> qaItems
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Interactive Audit Playbooks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        DropdownField(
            label = "Select Playbook",
            options = checklists,
            selectedOption = selectedChecklist
        ) { selectedChecklist = it }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(12.dp)) {
                activeItems.forEachIndexed { index, (itemText, checked) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { activeItems[index] = itemText to !checked }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { activeItems[index] = itemText to it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(itemText, style = MaterialTheme.typography.bodyMedium, color = if (checked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

private fun openRemoteFile(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ResourceCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isInitiallyExpanded: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(isInitiallyExpanded) }
    LaunchedEffect(isInitiallyExpanded) { if (isInitiallyExpanded) expanded = true }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier.clickable { if (content != null) expanded = !expanded }.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (content != null) {
                    Icon(imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.outline)
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
            if (expanded && content != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
            }
        }
    }
}

@Composable
fun LawItem(title: String, isOnline: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (isOnline) Icons.Default.CloudDownload else Icons.Default.Description, contentDescription = null, tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(imageVector = if (isOnline) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@ThemePreviews
@Composable
fun ResourcesScreenPreview() {
    IAAAppTheme {
        ResourcesScreen()
    }
}


