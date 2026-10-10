package com.bibiniitech.iaaapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.ClipData
import android.widget.Toast
import androidx.annotation.Keep
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun FindingsCriteriaScreen(navController: androidx.navigation.NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    // AI Database states
    var findingsDatabase by remember { mutableStateOf<List<AuditFinding>>(emptyList()) }
    var lawDetailsDatabase by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingDatabase by remember { mutableStateOf(value = false) }
    var searchQuery by remember { mutableStateOf(value = "") }
    var activeCategoryFilter by remember { mutableStateOf<String?>(value = null) }

    // Pre-load AI database
    LaunchedEffect(Unit) {
        if (findingsDatabase.isEmpty()) {
            isLoadingDatabase = true
            withContext(Dispatchers.IO) {
                try {
                    val jsonString = context.assets.open("audit_findings.json")
                        .bufferedReader().use { it.readText() }
                    val listType = object : TypeToken<List<AuditFinding>>() {}.type
                    val findings: List<AuditFinding> = Gson().fromJson(jsonString, listType)

                    val lawsJsonString = context.assets.open("law_details.json")
                        .bufferedReader().use { it.readText() }
                    val mapType = object : TypeToken<Map<String, String>>() {}.type
                    val laws: Map<String, String> = Gson().fromJson(lawsJsonString, mapType)
                    
                    withContext(Dispatchers.Main) {
                        findingsDatabase = findings
                        lawDetailsDatabase = laws
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FindingsCriteriaScreen", "AI Database load failed", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoadingDatabase = false
                    }
                }
            }
        }
    }

    // IDF weights for search
    val wordWeights = remember(findingsDatabase) {
        if (findingsDatabase.isEmpty()) return@remember emptyMap<String, Float>()
        val counts = mutableMapOf<String, Int>()
        findingsDatabase.forEach { finding ->
            val words = (finding.observation + " " + finding.criteria.joinToString(" ")).lowercase(Locale.ROOT)
                .split(Regex("\\W+"))
                .filter { it.length > 2 }
                .distinct()
            words.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        }
        val total = findingsDatabase.size.toFloat()
        counts.mapValues { kotlin.math.ln(total / it.value.toDouble()).toFloat() + 1f }
    }

    // Computed Search Results
    val stopWords = remember {
        setOf("in", "to", "of", "on", "and", "the", "for", "a", "an", "is", "are", "that", "this", "which", "with", "at", "by", "from", "or", "as")
    }

    val (searchResults, showCategoryMismatchPrompt) = remember(searchQuery, activeCategoryFilter, findingsDatabase, wordWeights) {
        val filteredDatabase = if (activeCategoryFilter != null) {
            findingsDatabase.filter { it.category == activeCategoryFilter }
        } else {
            findingsDatabase
        }

        if (searchQuery.trim().isEmpty()) {
            val res = if (activeCategoryFilter != null) filteredDatabase.take(150) else emptyList()
            res to false
        } else {
            val rawQuery = searchQuery.lowercase(Locale.ROOT).trim()
            val expandedQuery = expandQueryWithSynonyms(rawQuery)
            val queryTerms = expandedQuery.split(Regex("\\W+"))
                .map { it.trim().lowercase(Locale.ROOT) }
                .filter { it.isNotEmpty() && !stopWords.contains(it) }

            val searchInDb = { db: List<AuditFinding> ->
                if (queryTerms.isEmpty()) {
                    db.filter { 
                        it.observation.contains(rawQuery, ignoreCase = true) || 
                        it.criteria.any { c -> c.contains(rawQuery, ignoreCase = true) } 
                    }.take(100)
                } else {
                    db.asSequence().map { finding ->
                        var score = 0f
                        val obsLower = finding.observation.lowercase(Locale.ROOT)
                        val critLower = finding.criteria.joinToString(" ").lowercase(Locale.ROOT)
                        val obsWords = obsLower.split(Regex("\\W+")).filter { it.isNotEmpty() }
                        val critWords = critLower.split(Regex("\\W+")).filter { it.isNotEmpty() }
                        
                        if (obsLower.contains(rawQuery)) score += 50f
                        if (critLower.contains(rawQuery)) score += 30f

                        for (term in queryTerms) {
                            val weight = wordWeights[term] ?: 1.0f
                            val isWholeWordObs = obsWords.contains(term)
                            val isWholeWordCrit = critWords.contains(term)
                            
                            if (isWholeWordObs) {
                                score += 15f * weight
                                if (obsLower.startsWith(term) || obsLower.contains(" $term")) score += 5f
                            } else if (obsLower.contains(term)) {
                                if (term.length >= 4) {
                                    score += 5f * weight
                                }
                            } else {
                                if (obsWords.any { isFuzzyMatch(it, term) }) {
                                    score += 5f * weight
                                }
                            }
                            if (isWholeWordCrit) {
                                score += 10f * weight
                            } else if (critLower.contains(term) && term.length >= 4) {
                                score += 4f * weight
                            }
                        }
                        finding to score
                    }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .map { it.first }
                    .take(100)
                    .toList()
                }
            }

            val categoryResults = searchInDb(filteredDatabase)
            
            val mismatch = if (categoryResults.isEmpty() && activeCategoryFilter != null) {
                val allResults = searchInDb(findingsDatabase)
                allResults.isNotEmpty()
            } else {
                false
            }

            categoryResults to mismatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Findings & Criteria",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Search audit observations, criteria/laws, and covered entities to find relevant regulatory references.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search panel
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("Search keywords or phrase (e.g. imprest, voucher, VAT)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Category chips row
                    val categoryFilters = listOf(
                        "All" to null,
                        "Expenditure" to "EXPENDITURE",
                        "Revenue" to "REVENUE",
                        "Procurement" to "PROCUREMENT/CONTRACT/PROJECT",
                        "Stores" to "STORES /LIBRARY",
                        "Transport" to "TRANSPORT",
                        "IT" to "IT",
                        "Asset" to "ASSET",
                        "Payroll" to "HUMAN RESOURCE / PAYROLL",
                        "Tax" to "TAX & STATUTORY DEDUCTIONS",
                        "Reporting" to "FINANCIAL REPORTING",
                        "Administrative" to "ADMINISTRATIVE / GOVERNANCE / OTHERS"
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoryFilters) { (label, value) ->
                            val isSelected = activeCategoryFilter == value
                            FilterChip(
                                selected = isSelected,
                                onClick = { activeCategoryFilter = value },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoadingDatabase) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text("Loading AI database...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (searchQuery.trim().isEmpty() && activeCategoryFilter == null) {
            // Onboarding state
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Enter a search query or select a category chip to begin exploring findings database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (searchResults.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                    Text("No results found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    if (showCategoryMismatchPrompt && activeCategoryFilter != null) {
                        Text(
                            text = "Matches were found in other categories. Would you like to clear the category filter?",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { activeCategoryFilter = null },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear Category Filter & Show All")
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Search Results (${searchResults.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(items = searchResults, key = { it.id }) { finding ->
                FindingItemCard(
                    finding = finding,
                    searchQuery = searchQuery,
                    lawDetailsDatabase = lawDetailsDatabase,
                    clipboard = clipboard,
                    scope = scope,
                    context = context
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}

@Composable
private fun FindingItemCard(
    finding: AuditFinding,
    searchQuery: String,
    lawDetailsDatabase: Map<String, String>,
    clipboard: androidx.compose.ui.platform.Clipboard,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    var itemExpanded by remember(finding.id) { mutableStateOf(value = false) }
    val highlightColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .animateContentSize()
            .border(1.dp, if (itemExpanded) MaterialTheme.colorScheme.primary.copy(0.5f) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { itemExpanded = !itemExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                Text(finding.category, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(text = highlightSearchTerms(finding.observation, searchQuery, highlightColor), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            if (itemExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Laws / Criteria Reference:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (law in finding.criteria) {
                            var lawExpanded by remember(law) { mutableStateOf(false) }
                            val lookupKey = resolveLawKey(law)
                            val fullText = if (lookupKey != null) lawDetailsDatabase[lookupKey] else null

                            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { lawExpanded = !lawExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Gavel, null, tint = MaterialTheme.colorScheme.primary.copy(0.7f), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                    Text(highlightSearchTerms(law, searchQuery, highlightColor), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = if (lawExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (lawExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .padding(start = 22.dp, bottom = 8.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = fullText ?: "Full section text not found in database.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    val copyText = if (fullText != null) "Law: $law\n\n$fullText" else law
                                                    scope.launch {
                                                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Law Reference", copyText)))
                                                    }
                                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("View ${finding.criteria.size} criteria & applicability", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// AI SEARCH UTILITIES
private fun expandQueryWithSynonyms(query: String): String {
    val synonyms = mapOf(
        "vat" to "value added tax",
        "pfm" to "public financial management",
        "pfmr" to "public financial management regulations li 2378",
        "ppa" to "public procurement act 663",
        "iaa" to "internal audit agency",
        "gifmis" to "ghana integrated financial management information system",
        "igf" to "internally generated funds",
        "gcr" to "general counterfoil receipt",
        "wht" to "withholding tax",
        "paye" to "pay as you earn",
        "act 921" to "public financial management act",
        "li 2378" to "public financial management regulations",
        
        // General financial and cash context synonyms
        "cash" to "money funds currency cheque bank float imprest",
        "money" to "cash funds currency cheque bank float imprest",
        "funds" to "cash money currency bank capital float imprest",
        "fund" to "cash money currency bank capital float imprest",
        
        // Audit specific terminology synonyms
        "payment" to "disbursement voucher cheque pv",
        "disbursement" to "payment voucher cheque pv",
        "voucher" to "pv payment receipt invoice document",
        "pv" to "voucher payment disbursement",
        "invoice" to "receipt bill voucher document",
        "receipt" to "gcr invoice voucher bill payment",
        "imprest" to "petty cash float advance funds",
        "advance" to "imprest float petty cash funds",
        "revenue" to "igf income tax collection receipt collection",
        "income" to "revenue igf collection",
        "stores" to "inventory stock items goods warehouse",
        "stock" to "stores inventory items goods warehouse",
        "inventory" to "stores stock items goods warehouse",
        "goods" to "items stock inventory stores asset asset",
        "purchases" to "procurement goods contract tender ppa",
        "contract" to "agreement procurement project tender ppa",
        "procurement" to "purchases contract tender ppa buy",
        "tax" to "gra wht paye levy duty tariff statutory",
        "salary" to "payroll wage paye ssnit compensation staff employee",
        "payroll" to "salary wage paye ssnit compensation staff employee",
        "wage" to "salary payroll ssnit compensation staff employee",
        "staff" to "employee human resource personnel hr worker officers",
        "employee" to "staff human resource personnel hr worker officer",
        "personnel" to "staff employee human resource hr worker officer",
        "vehicle" to "transport car automobile fuel logbook transport",
        "car" to "vehicle transport fuel logbook transport",
        "transport" to "vehicle car fuel logbook transit",
        "fuel" to "petrol diesel vehicle car transport coupon logbook",

        // Audit risk and irregularity synonyms
        "fictitious" to "fake forged fabricated false dummy simulated phantom non-existent",
        "fake" to "fictitious forged fabricated false dummy simulated",
        "forged" to "fictitious fake fabricated false copy altered simulated",
        "suspicious" to "dubious questionable fishy anomalous irregular unusual",
        "irregular" to "suspicious dubious questionable fishy anomalous unusual exception non-compliant",
        "fraudulent" to "dishonest corrupt illegal misappropriated unauthorized unapproved stolen missing theft",
        "corrupt" to "fraudulent dishonest illegal misappropriated bribe kickback",
        "illegal" to "unlawful non-compliant unauthorized unapproved prohibited fraudulent corrupt",
        "unauthorized" to "unapproved non-compliant illegal unlawful disallowed",
        "missing" to "lost stolen short count shortage unaccounted theft",
        "stolen" to "missing lost shortage misappropriated theft"
    )
    var expanded = query
    val words = query.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.isNotEmpty() }
    synonyms.forEach { (abbr, full) ->
        val isMatch = if (abbr.contains(" ")) {
            query.lowercase(Locale.ROOT).contains(abbr)
        } else {
            words.contains(abbr)
        }
        if (isMatch) {
            expanded += " $full"
        }
    }
    return expanded
}

private fun isFuzzyMatch(word: String, target: String): Boolean {
    if (word.length < 4 || target.length < 4) return word == target
    return levenshteinDistance(word, target) <= 1
}

private fun levenshteinDistance(s1: String, s2: String): Int {
    val d = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) d[i][0] = i
    for (j in 0..s2.length) d[0][j] = j
    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
        }
    }
    return d[s1.length][s2.length]
}

private fun resolveLawKey(lawString: String): String? {
    val cleanLaw = lawString.lowercase(Locale.ROOT)
    val actName = when {
        cleanLaw.contains("pfm regulation") || cleanLaw.contains("pfmr") || cleanLaw.contains("l.i. 2378") || cleanLaw.contains("li 2378") -> "PFMR"
        cleanLaw.contains("pfm act") || cleanLaw.contains("act 921") -> "PFM"
        cleanLaw.contains("procurement") || cleanLaw.contains("ppa") || cleanLaw.contains("act 663") || cleanLaw.contains("act 914") -> "PPA"
        cleanLaw.contains("local governance") || cleanLaw.contains("lga") || cleanLaw.contains("act 936") -> "LGA"
        cleanLaw.contains("internal audit agency") || cleanLaw.contains("iaa") || cleanLaw.contains("act 658") -> "IAA"
        else -> null
    } ?: return null
    val numberMatch = Regex("(?:section|regulation|reg|art|article|clause|l\\.i\\.?|li)\\s*(\\d+)", RegexOption.IGNORE_CASE).find(cleanLaw)
    val sectionNum = numberMatch?.groupValues?.get(1) ?: Regex("(\\d+)").find(cleanLaw)?.groupValues?.get(1)
    return if (sectionNum != null) "${actName}_$sectionNum" else null
}

@Composable
private fun highlightSearchTerms(text: String, query: String, color: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val wordsToHighlight = query.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.length > 2 }.toMutableSet()
    wordsToHighlight.add(query.lowercase(Locale.ROOT).trim())
    return buildAnnotatedString {
        var lastIndex = 0
        val lowerText = text.lowercase(Locale.ROOT)
        val occurrences = mutableListOf<IntRange>()
        wordsToHighlight.forEach { term ->
            if (term.isEmpty()) return@forEach
            var startIndex = lowerText.indexOf(term)
            while (startIndex != -1) {
                occurrences.add(startIndex until (startIndex + term.length))
                startIndex = lowerText.indexOf(term, startIndex + 1)
            }
        }
        val sortedRanges = occurrences.sortedBy { it.first }
        val mergedRanges = mutableListOf<IntRange>()
        if (sortedRanges.isNotEmpty()) {
            var current = sortedRanges[0]
            for (i in 1 until sortedRanges.size) {
                val next = sortedRanges[i]
                if (next.first <= current.last) current = current.first..maxOf(current.last, next.last)
                else { mergedRanges.add(current); current = next }
            }
            mergedRanges.add(current)
        }
        mergedRanges.forEach { range ->
            append(text.substring(lastIndex, range.first))
            withStyle(style = SpanStyle(background = color.copy(alpha = 0.2f), fontWeight = FontWeight.Bold)) {
                append(text.substring(range.first, range.last + 1))
            }
            lastIndex = range.last + 1
        }
        append(text.substring(lastIndex))
    }
}

@Keep
data class AuditFinding(
    val id: String,
    val number: String,
    val category: String,
    val observation: String,
    val criteria: List<String>,
    val applicableTo: String
)

@ThemePreviews
@Composable
fun FindingsCriteriaScreenPreview() {
    IAAAppTheme {
        FindingsCriteriaScreen()
    }
}
