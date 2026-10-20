package com.bibiniitech.iaaapp.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.compose.ui.unit.sp
import com.bibiniitech.iaaapp.*
import com.bibiniitech.iaaapp.data.InstitutionData
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val isPreview = LocalInspectionMode.current
    var selectedTab by remember { mutableIntStateOf(value = 0) }
    val syncToken by FirebaseHelper.syncToken.collectAsState()
    
    val tabs = listOf("Home & Contact", "Submission Emails", "PFM & Portal Docs", "Dates & Seminars", "Voting", "Submissions Monitor")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val showMockBanner = if (isPreview) true else FirebaseHelper.isMockEnabled()
            if (showMockBanner) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Running in Local Offline Mode. Changes are stored in memory.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                text = title, 
                                fontSize = 13.sp, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) 
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // We use Key to force re-composition only when syncToken changes, 
                // but we also need to consider that switching tabs kills the composable.
                // For a robust Admin panel, we'll let each tab handle its own state but 
                // load data when syncToken changes.
                when (selectedTab) {
                    0 -> AdminHomeContactTab(syncToken)
                    1 -> AdminRecipientEmailsTab(syncToken)
                    2 -> AdminPfmDocsTab(syncToken)
                    3 -> AdminDeadlinesSeminarsTab(syncToken)
                    4 -> AdminVotingTab(syncToken)
                    5 -> AdminSubmissionsMonitorTab(syncToken)
                }
            }
        }
    }
}

// ==========================================
// TAB 5: VOTING MANAGER
// ==========================================
@Composable
fun AdminVotingTab(syncToken: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isVisible by remember { mutableStateOf(false) }
    var mainHeader by remember { mutableStateOf("") }
    val categories = remember { mutableStateListOf<VotingCategory>() }
    
    var isSaving by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isInitialLoading by remember { mutableStateOf(true) }

    LaunchedEffect(syncToken, isSaving) {
        if (isSaving) return@LaunchedEffect

        val config = FirebaseHelper.getVotingConfig()
        isVisible = config.isVisible
        mainHeader = config.header
        categories.clear()
        categories.addAll(config.categories)
        isInitialLoading = false
    }

    if (isInitialLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visibility Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Voting Page Visibility", fontWeight = FontWeight.Bold)
                    Text("Toggle to show or hide the voting page from users.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = isVisible, onCheckedChange = { isVisible = it })
            }
        }

        HorizontalDivider()

        // Main Header
        OutlinedTextField(
            value = mainHeader,
            onValueChange = { mainHeader = it },
            label = { Text("Main Voting Header (e.g. Favorite Foods)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Categories List
        categories.forEachIndexed { catIndex, category ->
            VotingCategoryEditor(
                category = category,
                onCategoryChange = { updated -> categories[catIndex] = updated },
                onDelete = { categories.removeAt(catIndex) }
            )
        }

        Button(
            onClick = { 
                categories.add(VotingCategory(id = System.currentTimeMillis().toString(), name = "", options = listOf(VotingOption(id = "1", text = ""), VotingOption(id = "2", text = "")))) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Category")
        }

        HorizontalDivider()

        Button(
            onClick = {
                isSaving = true
                scope.launch {
                    val success = FirebaseHelper.saveVotingConfig(VotingConfig(isVisible, mainHeader, categories.toList()))
                    isSaving = false
                    if (success) {
                        Toast.makeText(context, "Voting configuration saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Save Voting Configuration")
            }
        }

        HorizontalDivider()

        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    exportVotingResults(context, categories.toList())
                    isExporting = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            enabled = !isExporting
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Voting Results to Excel (CSV)")
        }
    }
}

@Composable
fun VotingCategoryEditor(
    category: VotingCategory,
    onCategoryChange: (VotingCategory) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEmailListDialog by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val emails = parseEmailsFromCsv(context, uri)
                if (emails.isNotEmpty()) {
                    onCategoryChange(category.copy(allowedVoterEmails = emails))
                    Toast.makeText(context, "Loaded ${emails.size} voter email(s) for this section.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No valid email addresses found in the CSV file.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showEmailListDialog) {
        EditAllowedEmailsDialog(
            initialEmails = category.allowedVoterEmails,
            onDismiss = { showEmailListDialog = false },
            onSave = { updatedEmails ->
                onCategoryChange(category.copy(allowedVoterEmails = updatedEmails))
                showEmailListDialog = false
                Toast.makeText(context, "Updated voter list: ${updatedEmails.size} email(s)", Toast.LENGTH_SHORT).show()
            }
        )
    }

    CollapsibleAdminCard(title = "Category: ${category.name.ifEmpty { "New Category" }}") {
        OutlinedTextField(
            value = category.name,
            onValueChange = { onCategoryChange(category.copy(name = it)) },
            label = { Text("Category Heading (e.g. Best Fruit)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        category.options.forEachIndexed { optIndex, option ->
            VotingOptionEditor(
                option = option,
                onOptionChange = { updated ->
                    val newOptions = category.options.toMutableList()
                    newOptions[optIndex] = updated
                    onCategoryChange(category.copy(options = newOptions))
                },
                onDelete = {
                    val newOptions = category.options.toMutableList()
                    newOptions.removeAt(optIndex)
                    onCategoryChange(category.copy(options = newOptions))
                }
            )
        }

        Button(
            onClick = {
                val newOptions = category.options + VotingOption(id = (category.options.size + 1).toString())
                onCategoryChange(category.copy(options = newOptions))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Option")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Allow Multiple Selection", fontWeight = FontWeight.Medium)
            Switch(checked = category.allowMultiple, onCheckedChange = { onCategoryChange(category.copy(allowMultiple = it)) })
        }

        // Voter Access Control Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (category.allowedVoterEmails.isNotEmpty()) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (category.allowedVoterEmails.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Voter Restriction", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }

                if (category.allowedVoterEmails.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${category.allowedVoterEmails.size} emails loaded",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Open to All Voters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = if (category.allowedVoterEmails.isNotEmpty())
                    "Only registered accounts with these email addresses can view and vote in this section."
                else
                    "Anyone with an account can vote in this section. Upload a CSV to restrict access to specific emails (e.g. regional executives).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { csvLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (category.allowedVoterEmails.isEmpty()) "Upload CSV" else "Replace CSV", fontSize = 12.sp)
                }

                if (category.allowedVoterEmails.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showEmailListDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit (${category.allowedVoterEmails.size})", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = {
                            onCategoryChange(category.copy(allowedVoterEmails = emptyList()))
                            Toast.makeText(context, "Restrictions cleared. Section open to all.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onDelete, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Section")
            }
        }
    }
}

@Composable
fun EditAllowedEmailsDialog(
    initialEmails: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var rawText by remember { mutableStateOf(initialEmails.joinToString("\n")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Allowed Voter Emails", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter or paste voter email addresses (separated by newlines, commas, or semicolons). Only accounts with these emails will be permitted to vote in this section.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("e.g.\njohn.doe@iaa.gov.gh\nsarah.smith@iaa.gov.gh") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                    val parsed = emailRegex.findAll(rawText).map { it.value.trim().lowercase(java.util.Locale.ROOT) }.toList().distinct()
                    onSave(parsed)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private suspend fun parseEmailsFromCsv(context: Context, uri: Uri): List<String> = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
        val text = inputStream.bufferedReader().use { it.readText() }
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val matches = emailRegex.findAll(text).map { it.value.trim().lowercase(java.util.Locale.ROOT) }.toList()
        matches.distinct()
    } catch (e: Exception) {
        android.util.Log.e("AdminScreen", "Error reading CSV emails", e)
        emptyList()
    }
}

@Composable
fun VotingOptionEditor(
    option: VotingOption,
    onOptionChange: (VotingOption) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            isUploading = true
            scope.launch {
                val url = FirebaseHelper.uploadVotingImage(it, context)
                isUploading = false
                if (url != null) {
                    onOptionChange(option.copy(imageUrl = url))
                } else {
                    Toast.makeText(context, "Image upload failed. Check Firebase Storage setup.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(60.dp).clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (option.imageUrl.isNotEmpty()) {
                coil.compose.AsyncImage(
                    model = option.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.padding(16.dp))
                }
            }
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }

        OutlinedTextField(
            value = option.text,
            onValueChange = { onOptionChange(option.copy(text = it)) },
            label = { Text("Option Text") },
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Remove Option", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private suspend fun exportVotingResults(context: Context, categories: List<VotingCategory>) {
    val votes = FirebaseHelper.getVotes()
    if (votes.isEmpty()) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "No votes recorded yet.", Toast.LENGTH_SHORT).show()
        }
        return
    }

    val sb = StringBuilder()
    // Headings
    val headers = mutableListOf("Surname", "First Name", "Email", "Contact", "Institution Type", "Region", "Institution Name")
    categories.forEach { headers.add("\"${it.name}\"") }
    sb.append(headers.joinToString(",")).append("\n")

    // Data
    votes.forEach { vote ->
        val row = mutableListOf(
            "\"${vote.surname}\"",
            "\"${vote.firstName}\"",
            "\"${vote.email}\"",
            "\"${vote.phone}\"",
            "\"${vote.institutionType}\"",
            "\"${vote.region}\"",
            "\"${vote.institution}\""
        )
        categories.forEach { cat ->
            val selections = vote.selections[cat.id] ?: emptyList()
            val optionTexts = selections.map { optId ->
                cat.options.find { it.id == optId }?.text ?: optId
            }
            row.add("\"${optionTexts.joinToString("; ")}\"")
        }
        sb.append(row.joinToString(",")).append("\n")
    }

    withContext(Dispatchers.IO) {
        try {
            val fileName = "Voting_Results_${System.currentTimeMillis()}.csv"
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
            }
            
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Files.getContentUri("external")
            }
            
            val uri = resolver.insert(collection, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { os ->
                    os.write(sb.toString().toByteArray())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Results exported to Downloads as $fileName", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// ==========================================
// TAB 6: SUBMISSIONS MONITOR
// ==========================================
@Composable
fun AdminSubmissionsMonitorTab(syncToken: Long) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val submissions = remember { mutableStateListOf<SubmissionRecord>() }
    var isLoading by remember { mutableStateOf(value = true) }

    LaunchedEffect(syncToken) {
        isLoading = true
        val list = FirebaseHelper.getSubmissions()
        submissions.clear()
        submissions.addAll(list)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Submissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    scope.launch {
                        isLoading = true
                        val list = FirebaseHelper.getSubmissions(forceRefresh = true)
                        submissions.clear()
                        submissions.addAll(list)
                        isLoading = false
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            // MDAs MONITOR
            SubmissionsMonitorCard(
                title = "MDA Submissions",
                institutionType = "MDA",
                submissions = submissions
            )

            // SOEs MONITOR
            SubmissionsMonitorCard(
                title = "SOE Submissions",
                institutionType = "SOE",
                submissions = submissions
            )

            // RCCs MONITOR
            SubmissionsMonitorCard(
                title = "RCC Submissions",
                institutionType = "RCC",
                submissions = submissions,
                showRegionFilter = true
            )

            // MMDAs MONITOR
            SubmissionsMonitorCard(
                title = "MMDA Submissions",
                institutionType = "MMDA",
                submissions = submissions,
                showRegionFilter = true
            )
        }
    }
}

@Composable
fun SubmissionsMonitorCard(
    title: String,
    institutionType: String,
    submissions: List<SubmissionRecord>,
    showRegionFilter: Boolean = false
) {
    var selectedReportType by remember { mutableStateOf("All Reports") }
    var selectedRegion by remember { mutableStateOf("All Regions") }
    
    val regions = remember {
        listOf("All Regions") + InstitutionData.REGIONS
    }

    // Fixed comprehensive list of all various report types
    val reportTypes = remember {
        listOf(
            "All Reports",
            "First Quarter Report",
            "Second Quarter Report",
            "Third Quarter Report",
            "Fourth Quarter Report",
            "Annual Workplan",
            "Strategic Plan",
            "Annual Statement on Statuses",
            "Annual IA Performance Report",
            "CCC Review Report",
            "First Quarter SOI Report",
            "Second Quarter SOI Report",
            "Third Quarter SOI Report",
            "Fourth Quarter SOI Report",
            "Other Reports"
        )
    }

    CollapsibleAdminCard(title = title) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DropdownField(
                    label = "Filter Report",
                    options = reportTypes,
                    selectedOption = selectedReportType,
                    onOptionSelected = { selectedReportType = it }
                )
            }
            if (showRegionFilter) {
                Box(modifier = Modifier.weight(1f)) {
                    DropdownField(
                        label = "Filter Region",
                        options = regions,
                        selectedOption = selectedRegion,
                        onOptionSelected = { selectedRegion = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val filteredSubmissions = submissions.filter { sub ->
            val matchesReport = when (selectedReportType) {
                "All Reports" -> true
                "Other Reports" -> sub.category == "other"
                "CCC Review Report" -> sub.reportName == "CCC Review Report" || sub.reportName.contains("CCC Review Report") || sub.category == "ccc"
                else -> sub.reportName == selectedReportType
            }
            sub.institutionType == institutionType &&
            matchesReport &&
            (!showRegionFilter || selectedRegion == "All Regions" || sub.region == selectedRegion)
        }

        if (filteredSubmissions.isEmpty()) {
            Text(
                "No submissions found for the selected filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            // Group by institution to avoid duplicates if they submitted multiple times, 
            // but we want to show all submissions, so maybe group by institution and show count?
            // Actually the user said "display names of institutions... if clicked expand and show details".
            // If an institution submitted 3 times, we should show 3 entries or 1 entry with 3 details.
            // Let's show each submission individually as it's a "monitor".
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredSubmissions.forEach { sub ->
                    SubmissionItem(sub)
                }
            }
        }
    }
}

@Composable
fun SubmissionItem(submission: SubmissionRecord) {
    var expanded by remember { mutableStateOf(value = false) }
    @Suppress("NonObservableLocale")
    val sdf = remember { java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()) }
    val dateStr = sdf.format(java.util.Date(submission.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = getSubmissionBackgroundColor(submission)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = submission.institution,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = submission.reportName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                
                DetailRow(label = "Sender:", value = submission.senderName)
                DetailRow(label = "Email:", value = submission.senderEmail)
                DetailRow(label = "Date/Time:", value = dateStr)
                if (submission.region.isNotEmpty()) {
                    DetailRow(label = "Region:", value = submission.region)
                }
                if (submission.fileNames.isNotEmpty()) {
                    DetailRow(label = "Files:", value = submission.fileNames.joinToString(", "))
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun getSubmissionBackgroundColor(submission: SubmissionRecord): Color {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = submission.timestamp
    }
    val subYear = cal.get(java.util.Calendar.YEAR)
    val subMonth = cal.get(java.util.Calendar.MONTH) // 0-based
    val subDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
    
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    
    val greenTint = Color(0x284CAF50)
    val redTint = Color(0x28F44336)
    val defaultBg = MaterialTheme.colorScheme.surface
    
    // 1. Quarterly IA Reports
    if (submission.category == "quarterly" || 
        submission.reportName.contains("Quarter Report", ignoreCase = true)) {
        
        if (subYear == currentYear) {
            when {
                submission.reportName.contains("Fourth", ignoreCase = true) -> {
                    return if (subMonth == java.util.Calendar.JANUARY) greenTint else if (subMonth > java.util.Calendar.JANUARY) redTint else defaultBg
                }
                submission.reportName.contains("First", ignoreCase = true) -> {
                    return when {
                        subMonth == java.util.Calendar.APRIL -> greenTint
                        subMonth > java.util.Calendar.APRIL -> redTint
                        else -> defaultBg
                    }
                }
                submission.reportName.contains("Second", ignoreCase = true) -> {
                    return when {
                        subMonth == java.util.Calendar.JULY -> greenTint
                        subMonth > java.util.Calendar.JULY -> redTint
                        else -> defaultBg
                    }
                }
                submission.reportName.contains("Third", ignoreCase = true) -> {
                    return when {
                        subMonth == java.util.Calendar.OCTOBER -> greenTint
                        subMonth > java.util.Calendar.OCTOBER -> redTint
                        else -> defaultBg
                    }
                }
            }
        }
        return defaultBg
    }
    
    // 2. CCC Review Reports
    if (submission.category == "ccc" || 
        submission.reportName.contains("CCC Review", ignoreCase = true)) {
        
        val quarter = when {
            submission.reportName.contains("First", ignoreCase = true) || submission.reportName.contains("1st", ignoreCase = true) -> "First"
            submission.reportName.contains("Second", ignoreCase = true) || submission.reportName.contains("2nd", ignoreCase = true) -> "Second"
            submission.reportName.contains("Third", ignoreCase = true) || submission.reportName.contains("3rd", ignoreCase = true) -> "Third"
            submission.reportName.contains("Fourth", ignoreCase = true) || submission.reportName.contains("4th", ignoreCase = true) -> "Fourth"
            else -> {
                // Legacy fallback based on submission timestamp
                when (subMonth) {
                    java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY -> "Fourth"
                    java.util.Calendar.MARCH, java.util.Calendar.APRIL, java.util.Calendar.MAY -> "First"
                    java.util.Calendar.JUNE, java.util.Calendar.JULY, java.util.Calendar.AUGUST -> "Second"
                    else -> "Third"
                }
            }
        }
        
        if (subYear == currentYear) {
            when (quarter) {
                "Fourth" -> {
                    return if (subMonth == java.util.Calendar.JANUARY && subDay < 10) greenTint else redTint
                }
                "First" -> {
                    return when {
                        subMonth < java.util.Calendar.APRIL -> greenTint
                        subMonth == java.util.Calendar.APRIL -> if (subDay < 10) greenTint else redTint
                        else -> redTint
                    }
                }
                "Second" -> {
                    return when {
                        subMonth < java.util.Calendar.JULY -> greenTint
                        subMonth == java.util.Calendar.JULY -> if (subDay < 10) greenTint else redTint
                        else -> redTint
                    }
                }
                "Third" -> {
                    return when {
                        subMonth < java.util.Calendar.OCTOBER -> greenTint
                        subMonth == java.util.Calendar.OCTOBER -> if (subDay < 10) greenTint else redTint
                        else -> redTint
                    }
                }
            }
        }
        return defaultBg
    }
    
    return defaultBg
}

// ==========================================
// TAB 1: HOME & CONTACT
// ==========================================
@Composable
fun AdminHomeContactTab(syncToken: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var welcomeTitle by remember { mutableStateOf("") }
    var welcomeDesc by remember { mutableStateOf("") }
    var noticeTitle by remember { mutableStateOf("") }
    var noticeContent by remember { mutableStateOf("") }
    
    var contactAddress by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmails by remember { mutableStateOf("") }
    var contactDesks by remember { mutableStateOf("") }
    
    var showDisclaimer by remember { mutableStateOf(true) }
    var showPfmLeague by remember { mutableStateOf(true) }
    
    var isSaving by remember { mutableStateOf(value = false) }
    var isInitialLoading by remember { mutableStateOf(true) }

    LaunchedEffect(syncToken, isSaving) {
        if (isSaving) return@LaunchedEffect

        val welcome = FirebaseHelper.getWelcomeConfig()
        welcomeTitle = welcome.title
        welcomeDesc = welcome.description

        val notice = FirebaseHelper.getNoticeConfig()
        noticeTitle = notice.title
        noticeContent = notice.content

        val contact = FirebaseHelper.getContactConfig()
        contactAddress = contact.address
        contactPhone = contact.phone
        contactEmails = contact.emails
        contactDesks = contact.specificDesks
        
        showDisclaimer = FirebaseHelper.getShowDisclaimer()
        showPfmLeague = FirebaseHelper.getShowPfmLeague()
        isInitialLoading = false
    }

    if (isInitialLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Card Editor
        CollapsibleAdminCard(title = "Welcome Note") {
            OutlinedTextField(value = welcomeTitle, onValueChange = { welcomeTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = welcomeDesc, onValueChange = { welcomeDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        }

        // Notices Card Editor
        CollapsibleAdminCard(title = "Notices") {
            OutlinedTextField(value = noticeTitle, onValueChange = { noticeTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = noticeContent, onValueChange = { noticeContent = it }, label = { Text("Content") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }

        // Contact Info Card Editor
        CollapsibleAdminCard(title = "Contact & Location Details") {
            OutlinedTextField(value = contactAddress, onValueChange = { contactAddress = it }, label = { Text("Office Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Phone Contacts") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = contactEmails, onValueChange = { contactEmails = it }, label = { Text("Email Addresses (space separated)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = contactDesks, onValueChange = { contactDesks = it }, label = { Text("Specific Desks contacts") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }

        // Disclaimer Visibility Card Editor
        CollapsibleAdminCard(
            title = "Disclaimer Screen Settings",
            subtitle = "Enable or disable visibility of the Disclaimer screen for all users."
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show Disclaimer Screen",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = showDisclaimer,
                    onCheckedChange = { showDisclaimer = it }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show PFM League Screen",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = showPfmLeague,
                    onCheckedChange = { showPfmLeague = it }
                )
            }
        }

        Button(
            onClick = {
                isSaving = true
                scope.launch {
                    val s1 = FirebaseHelper.saveWelcomeConfig(WelcomeConfig(welcomeTitle, welcomeDesc), sync = false)
                    val s2 = FirebaseHelper.saveNoticeConfig(NoticeConfig(noticeTitle, noticeContent), sync = false)
                    
                    val currentContact = FirebaseHelper.getContactConfig()
                    val s3 = FirebaseHelper.saveContactConfig(currentContact.copy(
                        address = contactAddress,
                        phone = contactPhone,
                        emails = contactEmails,
                        specificDesks = contactDesks
                    ), sync = false)
                    
                    val s4 = FirebaseHelper.saveShowDisclaimer(showDisclaimer, sync = false)
                    val s5 = FirebaseHelper.saveShowPfmLeague(showPfmLeague, sync = false)
                    FirebaseHelper.triggerSync()

                    isSaving = false
                    if (s1 && s2 && s3 && s4 && s5) {
                        Toast.makeText(context, "Configurations saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error saving some configurations.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Save Home & Contact Config")
            }
        }
    }
}

// ==========================================
// TAB 2: SUBMISSION RECIPIENT EMAILS
// ==========================================
@Composable
fun AdminRecipientEmailsTab(syncToken: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Recipient emails parsed as a map of category -> CloudConfig
    var reportsEmailsMap by remember { mutableStateOf<Map<String, CloudConfig>>(emptyMap()) }
    var selectedCategory by remember { mutableStateOf("quarterly") }

    val regions = InstitutionData.REGIONS
    var selectedRegion by remember { mutableStateOf("Ashanti") }

    val mmdaData = InstitutionData.MMDA_DATA
    var selectedMmda by remember { mutableStateOf("Kumasi Metropolitan Assembly") }

    // Text fields for editing current category and region
    var mdaToText by remember { mutableStateOf("") }
    var mdaCcText by remember { mutableStateOf("") }
    var mmdaToText by remember { mutableStateOf("") }
    var regionCcText by remember { mutableStateOf("") }
    var mmdaInstitutionCcText by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(value = false) }

    val updateMdaToText: (String) -> Unit = { newValue ->
        mdaToText = newValue
        val currentConfig = reportsEmailsMap[selectedCategory] ?: CloudConfig()
        val updatedConfig = currentConfig.copy(
            mdaTo = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
        reportsEmailsMap = reportsEmailsMap + (selectedCategory to updatedConfig)
    }

    val updateMdaCcText: (String) -> Unit = { newValue ->
        mdaCcText = newValue
        val currentConfig = reportsEmailsMap[selectedCategory] ?: CloudConfig()
        val updatedConfig = currentConfig.copy(
            mdaCc = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
        reportsEmailsMap = reportsEmailsMap + (selectedCategory to updatedConfig)
    }

    val updateMmdaToText: (String) -> Unit = { newValue ->
        mmdaToText = newValue
        val currentConfig = reportsEmailsMap[selectedCategory] ?: CloudConfig()
        val updatedConfig = currentConfig.copy(
            mmdaTo = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
        reportsEmailsMap = reportsEmailsMap + (selectedCategory to updatedConfig)
    }

    val updateRegionCcText: (String) -> Unit = { newValue ->
        regionCcText = newValue
        val currentConfig = reportsEmailsMap[selectedCategory] ?: CloudConfig()
        val currentRegionCcMap = (currentConfig.regionCc ?: emptyMap()).toMutableMap()
        currentRegionCcMap[selectedRegion] = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val updatedConfig = currentConfig.copy(
            regionCc = currentRegionCcMap
        )
        reportsEmailsMap = reportsEmailsMap + (selectedCategory to updatedConfig)
    }

    val updateMmdaInstitutionCcText: (String) -> Unit = { newValue ->
        mmdaInstitutionCcText = newValue
        val currentConfig = reportsEmailsMap[selectedCategory] ?: CloudConfig()
        val currentMmdaInstitutionCcMap = (currentConfig.mmdaInstitutionCc ?: emptyMap()).toMutableMap()
        val regionMap = (currentMmdaInstitutionCcMap[selectedRegion] ?: emptyMap()).toMutableMap()
        regionMap[selectedMmda] = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        currentMmdaInstitutionCcMap[selectedRegion] = regionMap
        val updatedConfig = currentConfig.copy(
            mmdaInstitutionCc = currentMmdaInstitutionCcMap
        )
        reportsEmailsMap = reportsEmailsMap + (selectedCategory to updatedConfig)
    }

    val categoryOptions = listOf(
        "Quarterly Report",
        "Annual Report",
        "CCC Review Report",
        "Status of Implementation (SOI)",
        "Other Reports"
    )

    fun mapOptionToCatId(option: String): String = when(option) {
        "Quarterly Report" -> "quarterly"
        "Annual Report" -> "annual"
        "CCC Review Report" -> "ccc"
        "Status of Implementation (SOI)" -> "soi"
        "Other Reports" -> "other"
        else -> "quarterly"
    }

    fun mapCatIdToOption(id: String): String = when(id) {
        "quarterly" -> "Quarterly Report"
        "annual" -> "Annual Report"
        "ccc" -> "CCC Review Report"
        "soi" -> "Status of Implementation (SOI)"
        "other" -> "Other Reports"
        else -> "Quarterly Report"
    }

    val onCategoryChanged: (String) -> Unit = { newCat ->
        selectedCategory = newCat
        val config = reportsEmailsMap[newCat] ?: CloudConfig()
        mdaToText = config.mdaTo?.joinToString(", ") ?: ""
        mdaCcText = config.mdaCc?.joinToString(", ") ?: ""
        mmdaToText = config.mmdaTo?.joinToString(", ") ?: ""
        regionCcText = config.regionCc?.get(selectedRegion)?.joinToString(", ") ?: ""
        mmdaInstitutionCcText = config.mmdaInstitutionCc?.get(selectedRegion)?.get(selectedMmda)?.joinToString(", ") ?: ""
    }

    val onRegionChanged: (String) -> Unit = { newReg ->
        selectedRegion = newReg
        selectedMmda = mmdaData[newReg]?.firstOrNull() ?: ""
        val config = reportsEmailsMap[selectedCategory] ?: CloudConfig()
        regionCcText = config.regionCc?.get(newReg)?.joinToString(", ") ?: ""
        mmdaInstitutionCcText = config.mmdaInstitutionCc?.get(newReg)?.get(selectedMmda)?.joinToString(", ") ?: ""
    }

    val onMmdaChanged: (String) -> Unit = { newMmda ->
        selectedMmda = newMmda
        val config = reportsEmailsMap[selectedCategory] ?: CloudConfig()
        mmdaInstitutionCcText = config.mmdaInstitutionCc?.get(selectedRegion)?.get(newMmda)?.joinToString(", ") ?: ""
    }

    LaunchedEffect(syncToken, isSaving) {
        if (isSaving) return@LaunchedEffect

        // Load emails json configuration
        val emailsJson = FirebaseHelper.getRecipientEmailsJson()
        val parsed = parseRecipientEmails(emailsJson)
        reportsEmailsMap = parsed
        
        // Initial text field load for selected category
        val config = parsed[selectedCategory] ?: CloudConfig()
        mdaToText = config.mdaTo?.joinToString(", ") ?: ""
        mdaCcText = config.mdaCc?.joinToString(", ") ?: ""
        mmdaToText = config.mmdaTo?.joinToString(", ") ?: ""
        regionCcText = config.regionCc?.get(selectedRegion)?.joinToString(", ") ?: ""
        mmdaInstitutionCcText = config.mmdaInstitutionCc?.get(selectedRegion)?.get(selectedMmda)?.joinToString(", ") ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recipient Emails Card Editor (Multi-Report Category & Regional support)
        CollapsibleAdminCard(
            title = "Submission Recipient Emails",
            subtitle = "Configure dynamic recipient email addresses for different reports submissions."
        ) {
            DropdownField(
                label = "Report Submission Category",
                options = categoryOptions,
                selectedOption = mapCatIdToOption(selectedCategory),
                onOptionSelected = { option ->
                    onCategoryChanged(mapOptionToCatId(option))
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            OutlinedTextField(
                value = mdaToText,
                onValueChange = updateMdaToText,
                label = { Text("MDA/SOE Report Recipient Emails") },
                placeholder = { Text("e.g. officer@iaa.gov.gh, supervisor@iaa.gov.gh") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mdaCcText,
                onValueChange = updateMdaCcText,
                label = { Text("MDA/SOE Report CC Recipients") },
                placeholder = { Text("e.g. cc1@iaa.gov.gh, cc2@iaa.gov.gh") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mmdaToText,
                onValueChange = updateMmdaToText,
                label = { Text("RCC/MMDA Report Recipient Emails") },
                placeholder = { Text("e.g. mmda@iaa.gov.gh") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Regional CC configuration (for RCC/MMDA submissions)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

            DropdownField(
                label = "Select Ghana Region",
                options = regions,
                selectedOption = selectedRegion,
                onOptionSelected = { region ->
                    onRegionChanged(region)
                }
            )

            OutlinedTextField(
                value = regionCcText,
                onValueChange = updateRegionCcText,
                label = { Text("CC Recipients for $selectedRegion Region") },
                placeholder = { Text("e.g. regionalhead@iaa.gov.gh") },
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedCategory == "quarterly" || selectedCategory == "annual") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Institution-Specific CC (for RCC/MMDA submissions)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                val mmdaOptions = if (selectedRegion.isNotEmpty()) {
                    (listOf(InstitutionData.RCC_DATA[selectedRegion] ?: "") + (mmdaData[selectedRegion] ?: emptyList())).filter { it.isNotEmpty() }
                } else emptyList()

                DropdownField(
                    label = "Select Institution (RCC/MMDA)",
                    options = mmdaOptions,
                    selectedOption = selectedMmda,
                    onOptionSelected = { mmda ->
                        onMmdaChanged(mmda)
                    }
                )

                OutlinedTextField(
                    value = mmdaInstitutionCcText,
                    onValueChange = updateMmdaInstitutionCcText,
                    label = { Text("CC Recipients for $selectedMmda") },
                    placeholder = { Text("e.g. localadmin@mmda.gov.gh") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                isSaving = true
                scope.launch {
                    // Save all report submission categories to Firestore
                    val success = FirebaseHelper.saveRecipientEmailsJson(Gson().toJson(reportsEmailsMap))

                    isSaving = false
                    if (success) {
                        Toast.makeText(context, "Email configurations saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error saving email configurations.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Save Email Configurations")
            }
        }
    }
}

// ==========================================
// TAB 3: PFM CSV & PORTAL DOCUMENTS
// ==========================================
@Composable
fun AdminPfmDocsTab(syncToken: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isProcessingPfm by remember { mutableStateOf(value = false) }
    var selectedCategory by remember { mutableStateOf("legislations") }
    var isUploadingDoc by remember { mutableStateOf(value = false) }
    
    val selectedFiles = remember { mutableStateListOf<Pair<Uri, String>>() }
    
    val documentsList = remember { mutableStateListOf<DbDocument>() }

    LaunchedEffect(selectedCategory, syncToken) {
        documentsList.clear()
        documentsList.addAll(FirebaseHelper.getDocuments(selectedCategory))
    }

    // File Picker for CSV
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            isProcessingPfm = true
            scope.launch {
                val success = parseAndUploadPfmCsv(context, it)
                isProcessingPfm = false
                if (success) {
                    Toast.makeText(context, "PFM League table updated successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to parse CSV. Verify format (Category,InstitutionName,Status)", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Multiple File Picker for Portal Documents
    val docsPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: uri.path?.substringAfterLast('/') ?: "Document_File"
            
            if (selectedFiles.none { it.second == fileName }) {
                selectedFiles.add(uri to fileName)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PFM Table CSV Uploader
        CollapsibleAdminCard(
            title = "PFM League Table Update",
            subtitle = "Upload a CSV file containing the PFM Compliance Table values. File structure should be three comma-separated columns: Category,InstitutionName,Status (e.g. MDA,Ministry of Finance,Highly Compliant)"
        ) {
            Button(
                onClick = { csvPickerLauncher.launch("text/*") },
                enabled = !isProcessingPfm,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isProcessingPfm) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload CSV File")
                }
            }
        }

        // Document Uploader to Portal
        CollapsibleAdminCard(title = "Portal Document Uploader") {
            DropdownField(
                label = "Target Category",
                options = listOf("legislations", "guidelines", "templates", "checklists", "training_resources", "others"),
                selectedOption = selectedCategory,
                onOptionSelected = { selectedCategory = it }
            )

            Button(
                onClick = { docsPickerLauncher.launch("*/*") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Documents to Upload")
            }

            if (selectedFiles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    selectedFiles.forEachIndexed { index, (_, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 1)
                            IconButton(onClick = { selectedFiles.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        isUploadingDoc = true
                        scope.launch {
                            val errorMsg = FirebaseHelper.uploadDocuments(selectedCategory, selectedFiles.toList(), context)
                            isUploadingDoc = false
                            if (errorMsg == null) {
                                Toast.makeText(context, "Documents uploaded and logged successfully!", Toast.LENGTH_SHORT).show()
                                selectedFiles.clear()
                                // Refresh list
                                documentsList.clear()
                                documentsList.addAll(FirebaseHelper.getDocuments(selectedCategory))
                            } else {
                                Toast.makeText(context, "Failed to upload documents: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isUploadingDoc,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUploadingDoc) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Upload ${selectedFiles.size} Files")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Existing Documents in ${selectedCategory.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            if (documentsList.isEmpty()) {
                Text(
                    text = "No documents found in this category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                documentsList.forEach { doc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = doc.name,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, doc.downloadUrl.toUri())
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "View",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val success = FirebaseHelper.deleteDocument(doc)
                                        if (success) {
                                            documentsList.remove(doc)
                                            Toast.makeText(context, "Document deleted.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to delete document.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

/**
 * Parses the CSV file for compliance values and uploads them grouped by PFM Sections.
 */
private suspend fun parseAndUploadPfmCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String? = reader.readLine() // Skip header if there is one

        val mdaMap = mutableMapOf<String, MutableList<String>>()
        val mmdaMap = mutableMapOf<String, MutableList<String>>()

        // Initialize empty lists for statuses
        val statuses = listOf("HIGHLY COMPLIANT", "COMPLIANT", "MODERATELY COMPLIANT", "LEAST COMPLIANT")
        statuses.forEach {
            mdaMap[it] = mutableListOf()
            mmdaMap[it] = mutableListOf()
        }

        while (reader.readLine().also { line = it } != null) {
            val parts = line!!.split(",").map { it.trim() }
            if (parts.size >= 3) {
                val category = parts[0].uppercase()
                val name = parts[1]
                val status = parts[2].uppercase()

                if (status in statuses) {
                    if (category == "MDA") {
                        mdaMap[status]?.add(name)
                    } else if (category == "MMDA") {
                        mmdaMap[status]?.add(name)
                    }
                }
            }
        }
        reader.close()

        // Create PFM sections
        val mdaSections = statuses.map { status ->
            val title = when (status) {
                "HIGHLY COMPLIANT" -> "HIGHLY COMPLIANT INSTITUTIONS"
                "COMPLIANT" -> "COMPLIANT INSTITUTIONS"
                "MODERATELY COMPLIANT" -> "MODERATELY COMPLIANT INSTITUTIONS"
                else -> "LEAST COMPLIANT INSTITUTIONS"
            }
            PFMSection(title, status, mdaMap[status] ?: emptyList())
        }

        val mmdaSections = statuses.map { status ->
            val title = when (status) {
                "HIGHLY COMPLIANT" -> "HIGHLY COMPLIANT INSTITUTIONS"
                "COMPLIANT" -> "COMPLIANT INSTITUTIONS"
                "MODERATELY COMPLIANT" -> "MODERATELY COMPLIANT INSTITUTIONS"
                else -> "LEAST COMPLIANT INSTITUTIONS"
            }
            PFMSection(title, status, mmdaMap[status] ?: emptyList())
        }

        // Upload JSON structures (trigger sync once after both are saved)
        val s1 = FirebaseHelper.savePfmLeagueData("mda", Gson().toJson(mdaSections), sync = false)
        val s2 = FirebaseHelper.savePfmLeagueData("mmda", Gson().toJson(mmdaSections), sync = true)
        s1 && s2
    } catch (_: Exception) {
        false
    }
}

// ==========================================
// TAB 4: DEADLINES & SEMINARS
// ==========================================
@Composable
fun AdminDeadlinesSeminarsTab(syncToken: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val deadlinesList = remember { mutableStateListOf<SubmissionDeadline>() }
    val seminarsList = remember { mutableStateListOf<Seminar>() }

    // Deadline Inputs
    var newDeadlineName by remember { mutableStateOf("") }
    var newDeadlineDate by remember { mutableStateOf("") } // YYYY-MM-DD
    
    // Seminar Inputs
    var semTopic by remember { mutableStateOf("") }
    var semDate by remember { mutableStateOf("") }
    var semTime by remember { mutableStateOf("") }
    var semLocation by remember { mutableStateOf("") }
    var semFee by remember { mutableStateOf("") }
    var semUrl by remember { mutableStateOf("") }

    var isSavingDates by remember { mutableStateOf(value = false) }

    LaunchedEffect(syncToken, isSavingDates) {
        if (isSavingDates) return@LaunchedEffect

        deadlinesList.clear()
        deadlinesList.addAll(FirebaseHelper.getDeadlines())

        seminarsList.clear()
        seminarsList.addAll(FirebaseHelper.getSeminars())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Deadlines Editor
        CollapsibleAdminCard(title = "Submission Deadlines") {
            // Add deadline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newDeadlineName,
                    onValueChange = { newDeadlineName = it },
                    label = { Text("Report Name") },
                    modifier = Modifier.weight(1.5f)
                )
                OutlinedTextField(
                    value = newDeadlineDate,
                    onValueChange = { newDeadlineDate = it },
                    label = { Text("YYYY-MM-DD") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newDeadlineName.isNotEmpty() && newDeadlineDate.isNotEmpty()) {
                            deadlinesList.add(SubmissionDeadline(newDeadlineName, newDeadlineDate))
                            newDeadlineName = ""
                            newDeadlineDate = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add Deadline", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // List & Delete
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                deadlinesList.forEachIndexed { index, dl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dl.report, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(dl.displayDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { deadlinesList.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }

            Button(
                onClick = {
                    isSavingDates = true
                    scope.launch {
                        val success = FirebaseHelper.saveDeadlines(deadlinesList.toList())
                        isSavingDates = false
                        if (success) {
                            Toast.makeText(context, "Deadlines saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error saving deadlines.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isSavingDates,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSavingDates) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Confirm Deadlines Save")
                }
            }
        }

        // Seminars Manager
        CollapsibleAdminCard(title = "Manage Seminars") {
            // Add Seminar inputs
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = semTopic, onValueChange = { semTopic = it }, label = { Text("Topic Title") }, modifier = Modifier.fillMaxWidth())
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = semDate, onValueChange = { semDate = it }, label = { Text("Date (YYYY-MM-DD, e.g. 2026-03-26)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = semTime, onValueChange = { semTime = it }, label = { Text("Time (e.g. 02:00 PM)") }, modifier = Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = semLocation, onValueChange = { semLocation = it }, label = { Text("Location (Zoom/Accra)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = semFee, onValueChange = { semFee = it }, label = { Text("Fee (GHS / Free)") }, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = semUrl, onValueChange = { semUrl = it }, label = { Text("Registration URL Link") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        if (semTopic.isNotEmpty() && semDate.isNotEmpty() && semTime.isNotEmpty()) {
                            val s = Seminar("", semTopic, semDate, semTime, semLocation, semFee, semUrl)
                            scope.launch {
                                val success = FirebaseHelper.saveSeminar(s)
                                if (success) {
                                    Toast.makeText(context, "Seminar added!", Toast.LENGTH_SHORT).show()
                                    // Refresh list
                                    seminarsList.clear()
                                    seminarsList.addAll(FirebaseHelper.getSeminars())
                                    
                                    // Clear fields
                                    semTopic = ""
                                    semDate = ""
                                    semTime = ""
                                    semLocation = ""
                                    semFee = ""
                                    semUrl = ""
                                } else {
                                    Toast.makeText(context, "Error saving seminar.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Seminar")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // List Seminars
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                seminarsList.forEach { sem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sem.topic, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${sem.date} | ${sem.time} | ${sem.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val success = FirebaseHelper.deleteSeminar(sem.id)
                                    if (success) {
                                        seminarsList.remove(sem)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun AdminScreenPreview() {
    IAAAppTheme {
        AdminScreen(onBack = {})
    }
}

@ThemePreviews
@Composable
fun AdminHomeContactTabPreview() {
    IAAAppTheme {
        AdminHomeContactTab(0L)
    }
}

@ThemePreviews
@Composable
fun AdminPfmDocsTabPreview() {
    IAAAppTheme {
        AdminPfmDocsTab(0L)
    }
}

@ThemePreviews
@Composable
fun AdminDeadlinesSeminarsTabPreview() {
    IAAAppTheme {
        AdminDeadlinesSeminarsTab(0L)
    }
}

@ThemePreviews
@Composable
fun AdminRecipientEmailsTabPreview() {
    IAAAppTheme {
        AdminRecipientEmailsTab(0L)
    }
}

@Composable
fun CollapsibleAdminCard(
    title: String,
    subtitle: String? = null,
    isInitiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(isInitiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

private fun parseRecipientEmails(json: String): Map<String, CloudConfig> {
    val result = mutableMapOf<String, CloudConfig>()
    val gson = Gson()
    val categories = listOf("quarterly", "annual", "ccc", "soi", "other")
    
    // Seed defaults
    for (cat in categories) {
        val defaultConfig = when (cat) {
            "quarterly" -> CloudConfig(EmailConfig.Quarterly.MDA_TO, EmailConfig.Quarterly.MDA_CC, EmailConfig.Quarterly.MMDA_TO, EmailConfig.Quarterly.REGION_CC, EmailConfig.Quarterly.MMDA_INSTITUTION_CC)
            "annual" -> CloudConfig(EmailConfig.Annual.MDA_TO, EmailConfig.Annual.MDA_CC, EmailConfig.Annual.MMDA_TO, EmailConfig.Annual.REGION_CC, EmailConfig.Annual.MMDA_INSTITUTION_CC)
            "ccc" -> CloudConfig(EmailConfig.CCC.MDA_TO, EmailConfig.CCC.MDA_CC, EmailConfig.CCC.MMDA_TO, EmailConfig.CCC.REGION_CC)
            "soi" -> CloudConfig(EmailConfig.SOI.MDA_TO, EmailConfig.SOI.MDA_CC, EmailConfig.SOI.MMDA_TO, EmailConfig.SOI.REGION_CC)
            "other" -> CloudConfig(EmailConfig.Other.MDA_TO, EmailConfig.Other.MDA_CC, EmailConfig.Other.MMDA_TO, EmailConfig.Other.REGION_CC)
            else -> CloudConfig()
        }
        result[cat] = defaultConfig
    }
    
    if (json.isNotEmpty()) {
        try {
            val type = object : TypeToken<Map<String, CloudConfig>>() {}.type
            val parsedMap = gson.fromJson<Map<String, CloudConfig>>(json, type)
            if (parsedMap != null && parsedMap.keys.any { it in categories }) {
                for ((cat, config) in parsedMap) {
                    val existing = result[cat] ?: CloudConfig()
                    result[cat] = CloudConfig(
                        mdaTo = config.mdaTo ?: existing.mdaTo,
                        mdaCc = config.mdaCc ?: existing.mdaCc,
                        mmdaTo = config.mmdaTo ?: existing.mmdaTo,
                        regionCc = config.regionCc ?: existing.regionCc,
                        mmdaInstitutionCc = config.mmdaInstitutionCc ?: existing.mmdaInstitutionCc
                    )
                }
                return result
            }
        } catch (_: Exception) {}
        
        // Fallback to legacy format
        try {
            val legacyConfig = gson.fromJson(json, CloudConfig::class.java)
            if (legacyConfig != null && (legacyConfig.mdaTo != null || legacyConfig.mdaCc != null || legacyConfig.mmdaTo != null)) {
                val existing = result["quarterly"] ?: CloudConfig()
                result["quarterly"] = CloudConfig(
                    mdaTo = legacyConfig.mdaTo ?: existing.mdaTo,
                    mdaCc = legacyConfig.mdaCc ?: existing.mdaCc,
                    mmdaTo = legacyConfig.mmdaTo ?: existing.mmdaTo,
                    regionCc = legacyConfig.regionCc ?: existing.regionCc
                )
            }
        } catch (_: Exception) {}
    }
    return result
}
