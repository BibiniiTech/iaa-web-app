package com.bibiniitech.iaaapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.iaaapp.FirebaseHelper
import com.bibiniitech.iaaapp.SubmissionRecord
import com.bibiniitech.iaaapp.data.InstitutionData
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class MasterInstitution(
    val name: String,
    val type: String, // "MDA", "SOE", "RCC", "MMDA"
    val region: String = ""
)

data class DefaulterItem(
    val institution: MasterInstitution,
    val daysOverdue: Long
)

data class LateSubmissionItem(
    val institution: MasterInstitution,
    val submission: SubmissionRecord,
    val daysLate: Long
)

data class OnTimeSubmissionItem(
    val institution: MasterInstitution,
    val submission: SubmissionRecord
)

data class ReportDef(
    val id: String,
    val title: String,
    val category: String, // "quarterly", "soi", "ccc", "annual"
    val defaultDeadline: String, // "YYYY-MM-DD"
    val quarterIdentifier: String = ""
)

data class CategoryGroupDef(
    val id: String,
    val title: String,
    val reports: List<ReportDef>
)

val CATEGORY_GROUPS_DEF = listOf(
    CategoryGroupDef(
        id = "quarterly",
        title = "Quarterly Reports",
        reports = listOf(
            ReportDef("q_4", "4th Quarter Report", "quarterly", "2026-01-31", "Fourth"),
            ReportDef("q_1", "1st Quarter Report", "quarterly", "2026-04-30", "First"),
            ReportDef("q_2", "2nd Quarter Report", "quarterly", "2026-07-31", "Second"),
            ReportDef("q_3", "3rd Quarter Report", "quarterly", "2026-10-31", "Third")
        )
    ),
    CategoryGroupDef(
        id = "soi",
        title = "Status of Implementations",
        reports = listOf(
            ReportDef("soi_3", "3rd Quarter Status of Implementation Report", "soi", "2026-01-31", "Third"),
            ReportDef("soi_4", "4th Quarter Status of Implementation Report", "soi", "2026-04-30", "Fourth"),
            ReportDef("soi_1", "1st Quarter Status of Implementation Report", "soi", "2026-07-31", "First"),
            ReportDef("soi_2", "2nd Quarter Status of Implementation Report", "soi", "2026-10-31", "Second")
        )
    ),
    CategoryGroupDef(
        id = "ccc",
        title = "CCC Review Reports",
        reports = listOf(
            ReportDef("ccc_1", "1st Quarter CCC Review Report", "ccc", "2026-04-10", "First"),
            ReportDef("ccc_2", "2nd Quarter CCC Review Report", "ccc", "2026-07-10", "Second"),
            ReportDef("ccc_3", "3rd Quarter CCC Review Report", "ccc", "2026-10-10", "Third"),
            ReportDef("ccc_4", "4th Quarter CCC Review Report", "ccc", "2027-01-10", "Fourth")
        )
    ),
    CategoryGroupDef(
        id = "workplan",
        title = "Annual Workplan",
        reports = listOf(
            ReportDef("annual_workplan", "Annual Workplan", "annual", "2026-01-31")
        )
    ),
    CategoryGroupDef(
        id = "strategic",
        title = "Strategic Plan",
        reports = listOf(
            ReportDef("strategic_plan", "Strategic Plan", "annual", "2026-01-31")
        )
    ),
    CategoryGroupDef(
        id = "performance",
        title = "Annual IA Performance Report",
        reports = listOf(
            ReportDef("annual_ia_perf", "Annual IA Performance Report", "annual", "2026-06-30")
        )
    )
)

private fun normalizeText(str: String?): String {
    if (str == null) return ""
    return str.lowercase().replace(Regex("[^a-z0-9]"), "")
}

private fun matchesReport(submission: SubmissionRecord, report: ReportDef): Boolean {
    if (submission.category.isNotEmpty() && submission.category != report.category) {
        return false
    }

    val normRepName = normalizeText(submission.reportName)
    val normCategory = normalizeText(submission.category)

    if (report.category == "annual") {
        if (report.id == "annual_workplan") {
            return normRepName.contains("workplan") || normCategory.contains("workplan")
        }
        if (report.id == "strategic_plan") {
            return normRepName.contains("strategic") || normCategory.contains("strategic")
        }
        if (report.id == "annual_ia_perf") {
            return normRepName.contains("performance") ||
                    normRepName.contains("internalaudit") ||
                    normRepName.contains("statementonstatuses") ||
                    normCategory.contains("performance") ||
                    normCategory.contains("internalaudit")
        }
    }

    val targetQuarter = report.quarterIdentifier.lowercase()
    val altQuarter = when {
        report.id.contains("1") -> "1st"
        report.id.contains("2") -> "2nd"
        report.id.contains("3") -> "3rd"
        else -> "4th"
    }

    val matchesQuarter = (targetQuarter.isNotEmpty() && (normCategory.contains(targetQuarter) || normRepName.contains(targetQuarter))) ||
            normCategory.contains(altQuarter) ||
            normRepName.contains(altQuarter)

    if (!matchesQuarter) return false

    if (report.category == "ccc") {
        return normRepName.contains("ccc") || submission.category == "ccc"
    }
    if (report.category == "soi") {
        return normRepName.contains("soi") || normRepName.contains("status") || submission.category == "soi"
    }
    if (report.category == "quarterly") {
        return !normRepName.contains("soi") && !normRepName.contains("ccc")
    }

    return true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultersScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val allInstitutions = remember {
        val list = mutableListOf<MasterInstitution>()
        val seen = mutableSetOf<String>()

        for (mda in InstitutionData.MDA_LIST) {
            val clean = mda.trim()
            val n = normalizeText(clean)
            if (clean.isNotEmpty() && seen.add(n)) {
                list.add(MasterInstitution(clean, "MDA"))
            }
        }
        for (soe in InstitutionData.SOE_LIST) {
            val clean = soe.trim()
            val n = normalizeText(clean)
            if (clean.isNotEmpty() && seen.add(n)) {
                list.add(MasterInstitution(clean, "SOE"))
            }
        }
        for (region in InstitutionData.REGIONS) {
            val rccName = InstitutionData.RCC_DATA[region]
            if (rccName != null) {
                val clean = rccName.trim()
                val n = normalizeText(clean)
                if (clean.isNotEmpty() && seen.add(n)) {
                    list.add(MasterInstitution(clean, "RCC", region))
                }
            }
        }
        for (region in InstitutionData.REGIONS) {
            val mmdaList = InstitutionData.MMDA_DATA[region] ?: emptyList()
            for (mmda in mmdaList) {
                val clean = mmda.trim()
                val n = normalizeText(clean)
                if (clean.isNotEmpty() && seen.add(n)) {
                    list.add(MasterInstitution(clean, "MMDA", region))
                }
            }
        }
        list.sortedBy { it.name }
    }

    val submissions = remember { mutableStateListOf<SubmissionRecord>() }
    val deadlinesMap = remember { mutableStateMapOf<String, String>() }

    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val expandedSubsets = remember { mutableStateMapOf<String, Boolean>() }

    val activeTabs = remember { mutableStateMapOf<String, String>() } // repId -> "defaulters" | "late" | "ontime"
    val searchTerms = remember { mutableStateMapOf<String, String>() }
    val typeFilters = remember { mutableStateMapOf<String, String>() }
    val regionFilters = remember { mutableStateMapOf<String, String>() }

    fun loadData(refresh: Boolean = false) {
        scope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            try {
                val deadlinesList = FirebaseHelper.getDeadlines()
                deadlinesMap.clear()
                for (d in deadlinesList) {
                    if (d.report.isNotEmpty() && d.dateString.isNotEmpty()) {
                        deadlinesMap[normalizeText(d.report)] = d.dateString
                    }
                }

                val subs = FirebaseHelper.getSubmissions(limit = 1000, forceRefresh = refresh)
                submissions.clear()
                submissions.addAll(subs)
            } catch (e: Exception) {
                android.util.Log.e("DefaultersScreen", "Error loading data", e)
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // Process compliance data per report definition
    val today = remember { LocalDate.now() }

    val complianceData = remember(submissions.size, deadlinesMap.size, allInstitutions) {
        val result = mutableMapOf<String, ReportCompliance>()

        for (group in CATEGORY_GROUPS_DEF) {
            for (rep in group.reports) {
                val normTitle = normalizeText(rep.title)
                val dateString = deadlinesMap[normTitle] ?: rep.defaultDeadline
                val deadlineDate = try {
                    LocalDate.parse(dateString)
                } catch (_: Exception) {
                    LocalDate.parse(rep.defaultDeadline)
                }

                val isElapsed = today.isAfter(deadlineDate)

                val defaulters = mutableListOf<DefaulterItem>()
                val lateSubmissions = mutableListOf<LateSubmissionItem>()
                val onTimeSubmissions = mutableListOf<OnTimeSubmissionItem>()

                if (isElapsed) {
                    val deadlineEpochMs = deadlineDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                    for (inst in allInstitutions) {
                        val normInst = normalizeText(inst.name)
                        val matching = submissions.filter { s ->
                            val sNorm = normalizeText(s.institution)
                            (sNorm.isNotEmpty() && (sNorm == normInst || sNorm.contains(normInst) || normInst.contains(sNorm))) &&
                                    matchesReport(s, rep)
                        }.sortedBy { it.timestamp }

                        if (matching.isNotEmpty()) {
                            val earliest = matching.first()
                            if (earliest.timestamp <= deadlineEpochMs) {
                                onTimeSubmissions.add(OnTimeSubmissionItem(inst, earliest))
                            } else {
                                val subDate = java.time.Instant.ofEpochMilli(earliest.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                                val daysLate = maxOf(1L, ChronoUnit.DAYS.between(deadlineDate, subDate))
                                lateSubmissions.add(LateSubmissionItem(inst, earliest, daysLate))
                            }
                        } else {
                            val daysOverdue = maxOf(1L, ChronoUnit.DAYS.between(deadlineDate, today))
                            defaulters.add(DefaulterItem(inst, daysOverdue))
                        }
                    }
                }

                result[rep.id] = ReportCompliance(
                    report = rep,
                    dateString = dateString,
                    deadlineDate = deadlineDate,
                    isElapsed = isElapsed,
                    defaulters = defaulters,
                    lateSubmissions = lateSubmissions,
                    onTimeSubmissions = onTimeSubmissions
                )
            }
        }
        result
    }

    val totalElapsedReports = complianceData.values.count { it.isElapsed }
    val totalDefaulters = complianceData.values.filter { it.isElapsed }.sumOf { it.defaulters.size }
    val totalLate = complianceData.values.filter { it.isElapsed }.sumOf { it.lateSubmissions.size }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Defaulters' List", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadData(refresh = true) }, enabled = !isRefreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro text
                Text(
                    text = "Tracking institutions that have made late submissions or have not submitted mandatory statutory reports under the Public Financial Management Act and Regulations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Stats Overview Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Monitored",
                        value = allInstitutions.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Elapsed Deadlines",
                        value = totalElapsedReports.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Defaulters",
                        value = totalDefaulters.toString(),
                        isDanger = true,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Late Submissions",
                        value = totalLate.toString(),
                        isWarning = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Info banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Categories and subsets are collapsed by default. Reports whose deadlines have not yet elapsed do not show defaulters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Categories Accordion Stack
                CATEGORY_GROUPS_DEF.forEach { group ->
                    val isCatExpanded = expandedCategories[group.id] == true

                    // Calculate category totals
                    val groupReports = group.reports.mapNotNull { complianceData[it.id] }
                    val activeGroupCount = groupReports.count { it.isElapsed }
                    val groupDefaulters = groupReports.filter { it.isElapsed }.sumOf { it.defaulters.size }
                    val groupLate = groupReports.filter { it.isElapsed }.sumOf { it.lateSubmissions.size }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                        )
                    ) {
                        Column {
                            // Category Header Button (no icon)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategories[group.id] = !isCatExpanded
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (activeGroupCount > 0)
                                            "$groupDefaulters Defaulters • $groupLate Late Submissions"
                                        else
                                            "No elapsed deadlines yet",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Icon(
                                    imageVector = if (isCatExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Subsets Stack
                            if (isCatExpanded) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    group.reports.forEach { rep ->
                                        val data = complianceData[rep.id] ?: return@forEach
                                        val isSubsetExpanded = expandedSubsets[rep.id] == true

                                        SubsetCard(
                                            data = data,
                                            isExpanded = isSubsetExpanded,
                                            onToggleExpand = { expandedSubsets[rep.id] = !isSubsetExpanded },
                                            activeTab = activeTabs[rep.id] ?: "defaulters",
                                            onTabChanged = { activeTabs[rep.id] = it },
                                            searchTerm = searchTerms[rep.id] ?: "",
                                            onSearchChanged = { searchTerms[rep.id] = it },
                                            typeFilter = typeFilters[rep.id] ?: "ALL",
                                            onTypeFilterChanged = { typeFilters[rep.id] = it },
                                            regionFilter = regionFilters[rep.id] ?: "ALL",
                                            onRegionFilterChanged = { regionFilters[rep.id] = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

data class ReportCompliance(
    val report: ReportDef,
    val dateString: String,
    val deadlineDate: LocalDate,
    val isElapsed: Boolean,
    val defaulters: List<DefaulterItem>,
    val lateSubmissions: List<LateSubmissionItem>,
    val onTimeSubmissions: List<OnTimeSubmissionItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubsetCard(
    data: ReportCompliance,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    activeTab: String,
    onTabChanged: (String) -> Unit,
    searchTerm: String,
    onSearchChanged: (String) -> Unit,
    typeFilter: String,
    onTypeFilterChanged: (String) -> Unit,
    regionFilter: String,
    onRegionFilterChanged: (String) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val formattedDeadline = remember(data.deadlineDate) {
        data.deadlineDate.format(dateFormatter)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.report.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Deadline: $formattedDeadline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!data.isElapsed) {
                        Surface(
                            color = Color(0xFFE0F2FE),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "⏳ Not Due Yet",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(color = Color(0xFFFEE2E2), shape = CircleShape) {
                            Text("🔴 ${data.defaulters.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                        Surface(color = Color(0xFFFEF3C7), shape = CircleShape) {
                            Text("🟡 ${data.lateSubmissions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                        Surface(color = Color(0xFFDCFCE7), shape = CircleShape) {
                            Text("🟢 ${data.onTimeSubmissions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Body
            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    if (!data.isElapsed) {
                        Surface(
                            color = Color(0xFFF0F9FF),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⏳", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Submission date is not due yet.",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF0369A1)
                                    )
                                    Text(
                                        text = "The deadline for ${data.report.title} is $formattedDeadline. Defaulters and late submissions will not be shown until the deadline has elapsed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF0369A1)
                                    )
                                }
                            }
                        }
                    } else {
                        // Tabs row (Defaulters, Late, On-Time) - NO CSV export button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TabButton(
                                text = "🔴 Defaulters (${data.defaulters.size})",
                                isSelected = activeTab == "defaulters",
                                modifier = Modifier.weight(1f),
                                onClick = { onTabChanged("defaulters") }
                            )
                            TabButton(
                                text = "🟡 Late (${data.lateSubmissions.size})",
                                isSelected = activeTab == "late",
                                modifier = Modifier.weight(1f),
                                onClick = { onTabChanged("late") }
                            )
                            TabButton(
                                text = "🟢 On-Time (${data.onTimeSubmissions.size})",
                                isSelected = activeTab == "ontime",
                                modifier = Modifier.weight(1f),
                                onClick = { onTabChanged("ontime") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search and Filter controls
                        OutlinedTextField(
                            value = searchTerm,
                            onValueChange = onSearchChanged,
                            placeholder = { Text("Search institution name...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Type filter dropdown
                        var typeMenuExpanded by remember { mutableStateOf(false) }
                        val typeOptions = listOf("ALL", "MDA", "SOE", "RCC", "MMDA")

                        ExposedDropdownMenuBox(
                            expanded = typeMenuExpanded,
                            onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = if (typeFilter == "ALL") "All Institution Types" else "$typeFilter Only",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeMenuExpanded,
                                onDismissRequest = { typeMenuExpanded = false }
                            ) {
                                typeOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(if (opt == "ALL") "All Institution Types" else "$opt Only") },
                                        onClick = {
                                            onTypeFilterChanged(opt)
                                            typeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Region filter dropdown (for RCC/MMDA)
                        if (typeFilter != "MDA" && typeFilter != "SOE") {
                            Spacer(modifier = Modifier.height(8.dp))
                            var regionMenuExpanded by remember { mutableStateOf(false) }
                            val regionOptions = listOf("ALL") + InstitutionData.REGIONS

                            ExposedDropdownMenuBox(
                                expanded = regionMenuExpanded,
                                onExpandedChange = { regionMenuExpanded = !regionMenuExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = if (regionFilter == "ALL") "All Regions" else regionFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionMenuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = regionMenuExpanded,
                                    onDismissRequest = { regionMenuExpanded = false }
                                ) {
                                    regionOptions.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text(if (r == "ALL") "All Regions" else r) },
                                            onClick = {
                                                onRegionFilterChanged(r)
                                                regionMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filter items
                        val items = remember(activeTab, searchTerm, typeFilter, regionFilter, data) {
                            var list: List<DisplayRowItem> = when (activeTab) {
                                "defaulters" -> data.defaulters.map {
                                    DisplayRowItem(it.institution.name, it.institution.type, it.institution.region, "${it.daysOverdue} days overdue", Color(0xFFDC2626))
                                }
                                "late" -> data.lateSubmissions.map {
                                    DisplayRowItem(it.institution.name, it.institution.type, it.institution.region, "${it.daysLate} days late", Color(0xFFD97706))
                                }
                                else -> data.onTimeSubmissions.map {
                                    DisplayRowItem(it.institution.name, it.institution.type, it.institution.region, "Compliant", Color(0xFF166534))
                                }
                            }

                            if (typeFilter != "ALL") {
                                list = list.filter { it.type == typeFilter }
                            }
                            if (regionFilter != "ALL") {
                                list = list.filter { it.region == regionFilter }
                            }
                            if (searchTerm.trim().isNotEmpty()) {
                                val normQ = searchTerm.trim().lowercase()
                                list = list.filter { it.name.lowercase().contains(normQ) }
                            }
                            list
                        }

                        if (items.isEmpty()) {
                            Text(
                                text = if (searchTerm.isNotEmpty() || typeFilter != "ALL" || regionFilter != "ALL")
                                    "No institutions matched your search/filters."
                                else if (activeTab == "defaulters")
                                    "🎉 Excellent! No institutions are defaulting on this report."
                                else
                                    "No submissions recorded under this section.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items.forEachIndexed { idx, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${idx + 1}.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                TypeBadge(type = item.type)
                                                if (item.region.isNotEmpty()) {
                                                    Text(item.region, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        }
                                        Text(
                                            text = item.statusLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = item.statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DisplayRowItem(
    val name: String,
    val type: String,
    val region: String,
    val statusLabel: String,
    val statusColor: Color
)

@Composable
fun TypeBadge(type: String) {
    val (bgColor, textColor) = when (type) {
        "MDA" -> Color(0xFFE0E7FF) to Color(0xFF3730A3)
        "SOE" -> Color(0xFFF3E8FF) to Color(0xFF6B21A8)
        "RCC" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        else -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = type,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    isDanger: Boolean = false,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val valColor = when {
        isDanger -> Color(0xFFDC2626)
        isWarning -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = valColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 10.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline, lineHeight = 12.sp)
        }
    }
}

@ThemePreviews
@Composable
fun DefaultersScreenPreview() {
    IAAAppTheme {
        DefaultersScreen()
    }
}
