package com.bibiniitech.iaaapp

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.junit.Test
import java.io.File
import java.util.Locale

class DataParserUnitTest {

    data class AuditFinding(
        val id: String,
        val number: String,
        val category: String,
        val observation: String,
        val criteria: List<String>,
        val applicableTo: String
    )

    private class LineItem(val y: Float) {
        val characters = mutableListOf<TextPosition>()
    }

    // Helper to determine if a criteria string contains a specific reference to a law
    private fun isLaw(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        
        val nonLawKeywords = listOf(
            "circular", "contract", "agreement", "bond", "policy", "manual", 
            "guideline", "rates", "allowance", "conditions of service", "administrative rules",
            "instruction", "rules and instructions"
        )
        if (nonLawKeywords.any { lower.contains(it) }) {
            return false
        }
        
        val hasStandardLaw = listOf(
            "public financial management",
            "public procurement",
            "local governance",
            "internal audit agency",
            "stores regulations",
            "value added tax",
            "income tax",
            "labour act",
            "revenue administration",
            "audit service act",
            "constitution"
        ).any { lower.contains(it) }
        
        if (hasStandardLaw) {
            return true
        }
        
        if ((lower.contains("regulation") || lower.contains("regulations") || lower.contains("instrument")) && 
            (lower.contains("l.i") || lower.contains("li") || lower.contains("act") || lower.contains("20"))) {
            return true
        }
        
        return false
    }

    @Test
    fun parsePdfToJSON() {
        val pdfFile = File("../Observations.pdf").takeIf { it.exists() } ?: File("Observations.pdf")
        val assetsDir = File("../app/src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
        assetsDir.mkdirs()
        val outputFile = File(assetsDir, "audit_findings.json")

        val findings = mutableListOf<AuditFinding>()
        var currentCategory = "EXPENDITURE"
        var currentNumber = ""
        var expectedRowNumber = 1
        
        // Temp buffers for the current finding being aggregated
        val obsBuffer = StringBuilder()
        val critBuffer = StringBuilder()
        val appBuffer = StringBuilder()

        val categories = listOf(
            "EXPENDITURE", "REVENUE", "PROCUREMENT/CONTRACT/PROJECT",
            "STORES /LIBRARY", "TRANSPORT", "IT", "ASSET",
            "HUMAN RESOURCE / PAYROLL", "TAX & STATUTORY DEDUCTIONS",
            "FINANCIAL REPORTING", "ADMINISTRATIVE / GOVERNANCE / OTHERS"
        )

        fun flushFinding() {
            val obs = obsBuffer.toString().trim()
            val critText = critBuffer.toString().trim()
            val app = appBuffer.toString().trim()
            
            if (currentNumber.isNotEmpty() && (obs.isNotEmpty() || critText.isNotEmpty())) {
                // Split criteria text by bullets • or ❖
                val critList = critText.split(Regex("[•❖]"))
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { it.replace(Regex("\\s+"), " ") }
                    // Clean and validate criteria (returns null if not specific/invalid)
                    .mapNotNull { cleanAndValidateCriteria(it) }
                    .distinct()
                    .toList()

                // Only add if there are specific criteria/laws remaining
                if (critList.isNotEmpty()) {
                    val id = "${currentCategory.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "_")}_$currentNumber"
                    findings.add(
                        AuditFinding(
                            id = id,
                            number = currentNumber,
                            category = currentCategory,
                            observation = obs.replace(Regex("\\s+"), " "),
                            criteria = critList,
                            applicableTo = app.replace(Regex("\\s+"), " ")
                        )
                    )
                }
            }
            obsBuffer.setLength(0)
            critBuffer.setLength(0)
            appBuffer.setLength(0)
        }

        Loader.loadPDF(pdfFile).use { document ->
            val stripper = object : PDFTextStripper() {
                override fun writePage() {
                    // Extract characters line by line using y-coordinates
                    val linesMap = mutableMapOf<Int, LineItem>()
                    
                    val charactersList = charactersByArticle[0] as? List<*>
                    if (charactersList != null) {
                        for (item in charactersList) {
                            val textPos = item as? TextPosition ?: continue
                            // Round Y coordinate to group characters on the same line (tolerance of ~4 points)
                            val yKey = (textPos.yDirAdj / 4.0).toInt()
                            val lineItem = linesMap.getOrPut(yKey) { LineItem(textPos.yDirAdj) }
                            lineItem.characters.add(textPos)
                        }
                    }

                    // Sort lines by Y coordinate
                    val sortedLines = linesMap.values.sortedBy { it.y }

                    for (line in sortedLines) {
                        // Sort characters in line by X coordinate
                        val sortedChars = line.characters.sortedBy { it.xDirAdj }
                        
                        // Partition into text blocks based on horizontal gaps (> 6.0 points)
                        val blocks = mutableListOf<MutableList<TextPosition>>()
                        var currentBlock = mutableListOf<TextPosition>()
                        
                        for (char in sortedChars) {
                            if (currentBlock.isEmpty()) {
                                currentBlock.add(char)
                            } else {
                                val prev = currentBlock.last()
                                val gap = char.xDirAdj - (prev.xDirAdj + prev.widthDirAdj)
                                if (gap > 6.0f) {
                                    blocks.add(currentBlock)
                                    currentBlock = mutableListOf(char)
                                } else {
                                    currentBlock.add(char)
                                }
                            }
                        }
                        if (currentBlock.isNotEmpty()) blocks.add(currentBlock)

                        // Allocate text blocks to columns based on their starting X coordinates
                        val obsTextBuilder = StringBuilder()
                        val critTextBuilder = StringBuilder()
                        val appTextBuilder = StringBuilder()

                        for (block in blocks) {
                            val text = block.joinToString("") { it.unicode ?: "" }.trim()
                            if (text.isEmpty()) continue
                            
                            val startX = block.first().xDirAdj
                            when {
                                startX < 285f -> {
                                    if (obsTextBuilder.isNotEmpty()) obsTextBuilder.append(" ")
                                    obsTextBuilder.append(text)
                                }
                                startX in 285f..<630f -> {
                                    if (critTextBuilder.isNotEmpty()) critTextBuilder.append(" ")
                                    critTextBuilder.append(text)
                                }
                                else -> {
                                    if (appTextBuilder.isNotEmpty()) appTextBuilder.append(" ")
                                    appTextBuilder.append(text)
                                }
                            }
                        }

                        val obsText = obsTextBuilder.toString().trim()
                        val critText = critTextBuilder.toString().trim()
                        val appText = appTextBuilder.toString().trim()

                        val fullText = "$obsText $critText $appText".trim()

                        // Filter out headers/footers/page numbers
                        if (fullText.isEmpty() ||
                            fullText.contains("© TEAM INTERNAL AUDIT AID") ||
                            fullText.contains("SAMPLE OF AUDIT OBSERVATIONS") ||
                            fullText.contains("NO OBSERVATION/ FINDING") ||
                            fullText.contains("APPLICABLE CRITERIA / LAWS") ||
                            fullText.contains("WHICH COVERED ENTITY?") ||
                            fullText.contains("APPLICABLE TO WHICH COVERED ENTITY") ||
                            (fullText.startsWith("Compiled by") && fullText.length < 50) ||
                            (fullText.matches(Regex("\\d+")) && line.y > 540) // Page numbers at the bottom
                        ) {
                            continue
                        }

                        // Get entire line text without column-based splits to match centered category headers
                        val rawLineText = line.characters.sortedBy { it.xDirAdj }.joinToString("") { it.unicode ?: "" }.trim()
                        val normalizedLine = rawLineText.replace(Regex("\\s+"), "").uppercase(Locale.ROOT)
                        
                        // Check if it is a category header
                        val possibleCategory = categories.firstOrNull { cat ->
                            val normalizedCat = cat.replace(Regex("\\s+"), "").uppercase(Locale.ROOT)
                            normalizedLine == normalizedCat || 
                            (normalizedCat.startsWith("ADMINISTRATIVE") && normalizedLine.startsWith("ADMINISTRATIVE/GOVERNANCE"))
                        }
                        if (possibleCategory != null) {
                            flushFinding()
                            currentCategory = possibleCategory
                            currentNumber = ""
                            expectedRowNumber = 1
                            continue
                        }

                        // Check if a line starts a new observation row.
                        // Only count as a new row if it starts with the EXPECTED sequential row number or "1" or bullet "❖"
                        val matchResult = Regex("^(\\d+)\\b").find(obsText)
                        var startNewRow = false

                        if (matchResult != null) {
                            val parsedNum = matchResult.groupValues[1].toIntOrNull()
                            if (parsedNum != null && (parsedNum == expectedRowNumber || parsedNum == expectedRowNumber + 1 || parsedNum == 1)) {
                                startNewRow = true
                                expectedRowNumber = parsedNum + 1
                            }
                        } else if (obsText.startsWith("❖")) {
                            startNewRow = true
                        }

                        if (startNewRow) {
                            flushFinding()
                            if (matchResult != null) {
                                currentNumber = matchResult.groupValues[1]
                                // Remove number prefix from observation text
                                val remainingObsText = obsText.replaceFirst(Regex("^\\d+\\.?\\s*"), "").trim()
                                obsBuffer.append(remainingObsText)
                            } else {
                                // Bulleted row. Let's make an ID based on index
                                currentNumber = "bullet_${findings.size + 1}"
                                obsBuffer.append(obsText)
                            }
                            critBuffer.append(critText)
                            appBuffer.append(appText)
                        } else {
                            // Continuation of current row
                            if (obsText.isNotEmpty()) {
                                if (obsBuffer.isNotEmpty() && !obsBuffer.endsWith(" ")) obsBuffer.append(" ")
                                obsBuffer.append(obsText)
                            }
                            if (critText.isNotEmpty()) {
                                // Add a delimiter if it starts a new bullet in criteria
                                if (critText.startsWith("•")) {
                                    critBuffer.append(" •")
                                } else if (critBuffer.isNotEmpty() && !critBuffer.endsWith(" ") && !critText.startsWith("•")) {
                                    critBuffer.append(" ")
                                }
                                critBuffer.append(critText)
                            }
                            if (appText.isNotEmpty()) {
                                if (appBuffer.isNotEmpty() && !appBuffer.endsWith(" ")) appBuffer.append(" ")
                                appBuffer.append(appText)
                            }
                        }
                    }
                }
            }
            
            stripper.sortByPosition = true
            stripper.startPage = 3
            stripper.endPage = 95
            stripper.getText(document)
            
            // Flush the last finding
            flushFinding()
        }

        // Output results to JSON file
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonString = gson.toJson(findings)
        outputFile.writeText(jsonString)
        
        // Write stats to a separate file so we can view it in Antigravity
        val statsFile = File("../app_parse_stats.txt").takeIf { it.parentFile?.exists() == true } ?: File("app_parse_stats.txt")
        val sbStats = StringBuilder()
        sbStats.append("Successfully parsed PDF and generated JSON database\n")
        sbStats.append("Total observations parsed: ${findings.size}\n")
        findings.groupBy { it.category }.forEach { (cat, list) ->
            sbStats.append("- $cat: ${list.size} observations\n")
        }
        statsFile.writeText(sbStats.toString())
        
        println("Successfully parsed PDF and generated JSON database at ${outputFile.absolutePath}")
    }

    @Test
    fun findNonSpecificCriteria() {
        val assetsDir = File("../app/src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
        val file = File(assetsDir, "audit_findings.json")
        if (!file.exists()) {
            println("File does not exist")
            return
        }
        val jsonString = file.readText()
        val listType = object : TypeToken<List<AuditFinding>>() {}.type
        val findings: List<AuditFinding> = Gson().fromJson(jsonString, listType)
        
        val uniqueCriteria = mutableSetOf<String>()
        for (f in findings) {
            uniqueCriteria.addAll(f.criteria)
        }
        
        val outputFile = File("../all_criteria.txt").takeIf { it.parentFile?.exists() == true } ?: File("all_criteria.txt")
        val sb = StringBuilder()
        sb.append("Found ${uniqueCriteria.size} unique criteria:\n")
        uniqueCriteria.sorted().forEach { sb.append(" - $it\n") }
        outputFile.writeText(sb.toString())
        println("Saved ${uniqueCriteria.size} unique criteria to ${outputFile.absolutePath}")
    }

    @Test
    fun testCleanCriteria() {
        val assetsDir = File("../app/src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
        val file = File(assetsDir, "audit_findings.json")
        if (!file.exists()) {
            println("File does not exist")
            return
        }
        val jsonString = file.readText()
        val listType = object : TypeToken<List<AuditFinding>>() {}.type
        val findings: List<AuditFinding> = Gson().fromJson(jsonString, listType)
        
        val uniqueCriteria = mutableSetOf<String>()
        for (f in findings) {
            uniqueCriteria.addAll(f.criteria)
        }
        
        val outputFile = File("../cleaned_criteria.txt").takeIf { it.parentFile?.exists() == true } ?: File("cleaned_criteria.txt")
        val sb = StringBuilder()
        sb.append("Before and After Cleaning:\n\n")
        
        var keptCount = 0
        var removedCount = 0
        
        uniqueCriteria.sorted().forEach { crit ->
            val cleaned = cleanAndValidateCriteria(crit)
            if (cleaned != null) {
                sb.append("KEEP:\n  Original: $crit\n  Cleaned:  $cleaned\n\n")
                keptCount++
            } else {
                sb.append("REMOVE:\n  Original: $crit\n\n")
                removedCount++
            }
        }
        
        sb.append("Summary:\n")
        sb.append("Total unique criteria evaluated: ${uniqueCriteria.size}\n")
        sb.append("Total criteria kept: $keptCount\n")
        sb.append("Total criteria removed: $removedCount\n")
        
        outputFile.writeText(sb.toString())
        println("Saved clean report to ${outputFile.absolutePath}")
    }

    private fun getStandardActName(rawAct: String, isRegulation: Boolean): String? {
        val lower = rawAct.lowercase(Locale.ROOT)
        return when {
            lower.contains("pfm regulation") || lower.contains("pfmr") || lower.contains("l.i. 2378") || lower.contains("l.i 2378") || lower.contains("li 2378") || 
            (lower.contains("public financial management") && isRegulation) || (lower.contains("public financial") && isRegulation) -> 
                "the Public Financial Management Regulations, 2019 (L.I. 2378)"
            
            lower.contains("pfm act") || lower.contains("pfma") || lower.contains("act 921") || lower.contains("public financial management") || lower.contains("public financial") -> 
                "the Public Financial Management Act, 2016 (Act 921)"
                
            lower.contains("procurement") || lower.contains("ppa") -> {
                if (lower.contains("914") || lower.contains("amendment")) {
                    "the Public Procurement (Amendment) Act, 2016 (Act 914)"
                } else {
                    "the Public Procurement Act, 2003 (Act 663) as amended"
                }
            }
            
            lower.contains("local governance") || lower.contains("local government") || lower.contains("lga") || lower.contains("act 936") -> 
                "the Local Governance Act, 2016 (Act 936)"
                
            lower.contains("internal audit") || lower.contains("iaa") || lower.contains("act 658") -> 
                "the Internal Audit Agency Act, 2003 (Act 658)"
                
            lower.contains("stores regulation") || lower.contains("store regulation") || lower.contains("store's regulation") -> 
                "the Stores Regulations, 1984"
                
            lower.contains("vat act") || lower.contains("value added tax") || lower.contains("act 870") -> 
                "the Value Added Tax Act, 2013 (Act 870) as amended"
                
            lower.contains("income tax") || lower.contains("act 896") -> 
                "the Income Tax Act, 2015 (Act 896) as amended"
                
            lower.contains("labour act") || lower.contains("act 651") -> 
                "the Labour Act, 2003 (Act 651)"
                
            lower.contains("revenue administration") || lower.contains("act 915") -> 
                "the Revenue Administration Act, 2016 (Act 915)"
                
            lower.contains("audit service") || lower.contains("act 584") -> 
                "the Audit Service Act, 2000 (Act 584)"
                
            lower.contains("constitution") || lower.contains("constitutional") -> 
                "the 1992 Constitution of the Republic of Ghana"
                
            else -> null
        }
    }

    private fun replaceActSuffix(text: String, regex: Regex, replacement: String): String {
        val match = regex.find(text)
        if (match != null) {
            return text.substring(0, match.range.first) + replacement
        }
        return text
    }

    private fun cleanAndValidateCriteria(criteria: String): String? {
        var text = criteria.trim()
        // Remove leading bullets, spaces, dashes, or numbering
        text = text.replace(Regex("^[\\s•❖\\-*\\d.]+\\s*"), "")
        
        val lower = text.lowercase(Locale.ROOT)
        
        // Specific law keywords
        val specificWords = listOf(
            "section", "regulation", "reg", "paragraph", "para", 
            "article", "art", "chapter", "guideline", "clause", 
            "circular", "schedule", "instruction"
        )
        
        val hasKeyword = specificWords.any { lower.contains(it) }
                         
        if (!hasKeyword) {
            return null
        }
        
        // Filter out generic titles or placeholders
        if (lower == "act, 2016 (act 921)" || 
            lower == "act, 2016, (act 921)" ||
            lower == "(l.i.2378)" ||
            lower == "l.i. 2378" ||
            lower == "l.i 2378" ||
            lower == "regulations 2019, (l.i 2378)" ||
            lower == "regulations, 2019 (l.i. 2378)" ||
            lower == "public procurement act, 2003 (act 663) as amended" ||
            lower == "public procurement act, 2003 (act 663)" ||
            lower == "public procurement act" ||
            lower == "ppa regulation" ||
            lower == "as per contract agreement" ||
            lower == "as per conditions/clause in the contract agreement" ||
            lower == "as per contract" ||
            lower == "ent" ||
            lower == "ement" ||
            lower == "ct" ||
            lower == "t act," ||
            lower.length < 5
        ) {
            return null
        }

        val actKeywords = listOf(
            "public financial", "pfm", "public procurement", "ppa", 
            "local governance", "local government", "lga", "internal audit", 
            "iaa", "stores", "store's", "value added", "vat", "income tax", 
            "labour", "revenue administration", "audit service", "constitution"
        )
        
        val lawKeywords = listOf(
            "section", "regulation", "reg", "paragraph", "para", 
            "article", "art", "clause", "schedule", "instruction", "chapter"
        )
        
        // Find first Act keyword
        var actIdx = -1
        for (kw in actKeywords) {
            val idx = lower.indexOf(kw)
            if (idx != -1 && (actIdx == -1 || idx < actIdx)) {
                actIdx = idx
            }
        }
        
        // Find first Law keyword
        var lawIdx = -1
        for (kw in lawKeywords) {
            val idx = lower.indexOf(kw)
            if (idx != -1 && (lawIdx == -1 || idx < lawIdx)) {
                lawIdx = idx
            }
        }
        
        var cleanedResult: String? = null

        if (actIdx != -1 && lawIdx != -1) {
            val isReg = lower.substring(lawIdx).startsWith("reg")
            if (actIdx < lawIdx) {
                // Pattern B: Act is before Law (e.g. PFM Act Section 9)
                val actPart = text.substring(0, lawIdx)
                val lawPart = text.substring(lawIdx)
                val standardAct = getStandardActName(actPart, isReg)
                if (standardAct != null) {
                    val cleanLawPart = lawPart.replace(Regex("^[\\s,:]+"), "").trim()
                    // Extract identifier from cleanLawPart to check if it's specific
                    val numMatch = Regex("(\\d+|[a-zA-Z#_\\-()/]+)").find(cleanLawPart)
                    val id = numMatch?.value ?: ""
                    val isYear = id.matches(Regex("^\\d{4}$")) && id.toInt() in 1980..2030
                    if (!isYear && id.isNotEmpty()) {
                        cleanedResult = "$cleanLawPart of $standardAct"
                    }
                }
            } else {
                // Pattern A: Law is before Act (e.g. Section 9 of PFM Act)
                val lawPart = text.substring(0, actIdx)
                val actPart = text.substring(actIdx)
                val standardAct = getStandardActName(actPart, isReg)
                if (standardAct != null) {
                    val cleanLawPart = lawPart
                        .replace(Regex("(?i)\\b(of the|of|in|under|by|governing|the)\\s*$"), "")
                        .replace(Regex("\\b\\d{4}\\s*$"), "")
                        .replace(Regex("(?i)\\b(of the|of|in|under|by|governing|the)\\s*$"), "")
                        .trim()
                    // Extract identifier from cleanLawPart to check if it's specific
                    val numMatch = Regex("(\\d+|[a-zA-Z#_\\-()/]+)").find(cleanLawPart)
                    val id = numMatch?.value ?: ""
                    val isYear = id.matches(Regex("^\\d{4}$")) && id.toInt() in 1980..2030
                    if (!isYear && id.isNotEmpty()) {
                        cleanedResult = "$cleanLawPart of $standardAct"
                    }
                }
            }
        }

        if (cleanedResult == null) {
            // Fallback: If it matches our keyword regex and has a specific number
            val specificRegex = Regex(
                "\\b(section|regulation|reg|paragraph|para|article|art|chapter|guideline|clause|circular|schedule|instruction)\\s+([a-zA-Z0-9#_\\-()/.,&]+)", 
                RegexOption.IGNORE_CASE
            )
            val matchFallback = specificRegex.find(text)
            if (matchFallback != null) {
                val id = matchFallback.groupValues[2].trim()
                val isYear = id.matches(Regex("^\\d{4}$")) && id.toInt() in 1980..2030
                val isStopWord = listOf("of", "the", "and", "to", "for", "in", "by", "with", "on", "at", "a", "an", "is", "are", "that", "this", "which").contains(id.lowercase(Locale.ROOT))
                if (!isYear && !isStopWord && id.isNotEmpty()) {
                    var cleaned = text
                    if (lower.contains("pfm regulation") || lower.contains("pfmr") || 
                        ((lower.contains("public financial management") || lower.contains("public financial")) && (lower.contains("regulation") || lower.contains("reg.")))) {
                        cleaned = replaceActSuffix(cleaned, Regex("(of the\\s+)?(public financial management regulations?|pfm regulations?|pfmr|public financial management|public financial|pfm)(\\s*\\(?,?\\s*li\\s*\\d*\\)?)?\\s*$", RegexOption.IGNORE_CASE), "of the Public Financial Management Regulations, 2019 (L.I. 2378)")
                    } else if (lower.contains("pfm act") || lower.contains("public financial management act") || 
                        ((lower.contains("public financial management") || lower.contains("public financial")) && (lower.contains("act") || lower.contains("section")))) {
                        cleaned = replaceActSuffix(cleaned, Regex("(of the\\s+)?(public financial management act|pfm act|public financial management|public financial|pfm)\\s*(\\(?,?\\s*act\\s*\\d*\\)?)?\\s*$", RegexOption.IGNORE_CASE), "of the Public Financial Management Act, 2016 (Act 921)")
                    }
                    cleanedResult = cleaned
                }
            }
        }

        if (cleanedResult != null && isLaw(cleanedResult)) {
            return cleanedResult
        }

        return null
    }

    @Test
    fun analyzeUnreferencedLawsAndPdfPages() {
        val pdfFile = File("../Observations.pdf").takeIf { it.exists() } ?: File("Observations.pdf")
        val assetsDir = File("../app/src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
        
        val totalPages = Loader.loadPDF(pdfFile).use { it.numberOfPages }
        println("ANALYSIS_RESULT: Observations.pdf has $totalPages pages")
        
        val findingsFile = File(assetsDir, "audit_findings.json")
        val lawsFile = File(assetsDir, "law_details.json")
        
        if (!findingsFile.exists() || !lawsFile.exists()) {
            println("ANALYSIS_RESULT: Missing files")
            return
        }
        
        val findingsJson = findingsFile.readText()
        val findingsListType = object : TypeToken<List<AuditFinding>>() {}.type
        val findings: List<AuditFinding> = Gson().fromJson(findingsJson, findingsListType)
        
        val lawsJson = lawsFile.readText()
        val lawsMapType = object : TypeToken<Map<String, String>>() {}.type
        val laws: Map<String, String> = Gson().fromJson(lawsJson, lawsMapType)
        
        // Find referenced law keys
        val referencedKeys = mutableSetOf<String>()
        for (f in findings) {
            for (law in f.criteria) {
                val resolvedKey = resolveLawKeyLocal(law)
                if (resolvedKey != null) {
                    referencedKeys.add(resolvedKey)
                }
            }
        }
        
        val unreferencedKeys = laws.keys - referencedKeys
        println("ANALYSIS_RESULT: Total laws in database: ${laws.size}")
        println("ANALYSIS_RESULT: Referenced laws: ${referencedKeys.size}")
        println("ANALYSIS_RESULT: Unreferenced laws: ${unreferencedKeys.size}")
        
        println("ANALYSIS_RESULT: Unreferenced law keys sample:")
        unreferencedKeys.take(30).forEach { key ->
            println("ANALYSIS_RESULT:   - $key: ${laws[key]?.substringBefore("\n")}")
        }
    }
    
    private fun resolveLawKeyLocal(lawString: String): String? {
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

    @Test
    fun previewExpandedFindings() {
        val assetsDir = File("../app/src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
        val findingsFile = File(assetsDir, "audit_findings.json")
        val lawsFile = File(assetsDir, "law_details.json")
        
        if (!findingsFile.exists() || !lawsFile.exists()) {
            println("Missing files")
            return
        }
        
        val findingsJson = findingsFile.readText()
        val findingsListType = object : TypeToken<List<AuditFinding>>() {}.type
        val findings: List<AuditFinding> = Gson().fromJson(findingsJson, findingsListType)
        
        val lawsJson = lawsFile.readText()
        val lawsMapType = object : TypeToken<Map<String, String>>() {}.type
        val laws: Map<String, String> = Gson().fromJson(lawsJson, lawsMapType)
        
        // Find referenced law keys
        val referencedKeys = mutableSetOf<String>()
        for (f in findings) {
            for (law in f.criteria) {
                val resolvedKey = resolveLawKeyLocal(law)
                if (resolvedKey != null) {
                    referencedKeys.add(resolvedKey)
                }
            }
        }
        
        val unreferencedKeys = laws.keys - referencedKeys
        val previewFindings = mutableListOf<AuditFinding>()
        
        for (key in unreferencedKeys.sorted()) {
            val lawText = laws[key] ?: continue
            val category = getCategoryForLawLocal(key, lawText)
            val observation = generateObservationForLawLocal(key, lawText)
            val criteriaName = getCriteriaNameForLawLocal(key)
            val id = "generated_${key.lowercase(Locale.ROOT)}"
            
            previewFindings.add(
                AuditFinding(
                    id = id,
                    number = key.substringAfter("_"),
                    category = category,
                    observation = observation,
                    criteria = listOf(criteriaName),
                    applicableTo = "All covered entities"
                )
            )
        }
        
        // Write preview to build directory
        val previewDir = File("build").apply { mkdirs() }
        val previewFile = File(previewDir, "findings_expansion_preview.md")
        val sb = StringBuilder()
        sb.append("# Preview of Automatically Generated Findings & Criteria\n\n")
        sb.append("This document contains a preview of the **${previewFindings.size}** new audit findings generated from the unreferenced laws in `law_details.json`.\n\n")
        
        val grouped = previewFindings.groupBy { it.category }
        for ((cat, list) in grouped.entries.sortedBy { it.key }) {
            sb.append("## $cat (${list.size} findings)\n\n")
            for (f in list) {
                sb.append("### Finding ID: `${f.id}` (Ref: ${f.criteria.first()})\n")
                sb.append("**Observation/Finding:**\n> ${f.observation}\n\n")
                sb.append("**Applicable Law/Criteria:**\n* ${f.criteria.first()}\n\n")
                sb.append("**Applicable To:** ${f.applicableTo}\n\n")
                sb.append("---\n\n")
            }
        }
        
        previewFile.writeText(sb.toString())
        println("ANALYSIS_RESULT: Generated preview for ${previewFindings.size} findings at ${previewFile.absolutePath}")
    }

    @Test
    fun expandFindingsDatabase() {
        val assetsDir = File("../app/src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
        val findingsFile = File(assetsDir, "audit_findings.json")
        val lawsFile = File(assetsDir, "law_details.json")
        
        if (!findingsFile.exists() || !lawsFile.exists()) {
            println("Missing files")
            return
        }
        
        val findingsJson = findingsFile.readText()
        val findingsListType = object : TypeToken<List<AuditFinding>>() {}.type
        val originalFindings: List<AuditFinding> = Gson().fromJson(findingsJson, findingsListType)
        
        val lawsJson = lawsFile.readText()
        val lawsMapType = object : TypeToken<Map<String, String>>() {}.type
        val laws: Map<String, String> = Gson().fromJson(lawsJson, lawsMapType)
        
        // Find referenced law keys
        val referencedKeys = mutableSetOf<String>()
        for (f in originalFindings) {
            for (law in f.criteria) {
                val resolvedKey = resolveLawKeyLocal(law)
                if (resolvedKey != null) {
                    referencedKeys.add(resolvedKey)
                }
            }
        }
        
        val unreferencedKeys = laws.keys - referencedKeys
        val mergedFindings = originalFindings.toMutableList()
        
        for (key in unreferencedKeys.sorted()) {
            val lawText = laws[key] ?: continue
            val category = getCategoryForLawLocal(key, lawText)
            val observation = generateObservationForLawLocal(key, lawText)
            val criteriaName = getCriteriaNameForLawLocal(key)
            val id = "generated_${key.lowercase(Locale.ROOT)}"
            
            mergedFindings.add(
                AuditFinding(
                    id = id,
                    number = key.substringAfter("_"),
                    category = category,
                    observation = observation,
                    criteria = listOf(criteriaName),
                    applicableTo = "All covered entities"
                )
            )
        }
        
        // Output results to JSON file
        val gson = GsonBuilder().setPrettyPrinting().create()
        val newJsonString = gson.toJson(mergedFindings)
        findingsFile.writeText(newJsonString)
        
        // Write stats to stats file
        val statsFile = File("../app_parse_stats.txt").takeIf { it.parentFile?.exists() == true } ?: File("app_parse_stats.txt")
        val sbStats = StringBuilder()
        sbStats.append("Successfully expanded audit findings database!\n")
        sbStats.append("Original observations: ${originalFindings.size}\n")
        sbStats.append("Generated new observations: ${unreferencedKeys.size}\n")
        sbStats.append("Total observations now: ${mergedFindings.size}\n")
        mergedFindings.groupBy { it.category }.forEach { (cat, list) ->
            sbStats.append("- $cat: ${list.size} observations\n")
        }
        statsFile.writeText(sbStats.toString())
        
        println("ANALYSIS_RESULT: Successfully expanded database to ${mergedFindings.size} findings")
    }

    
    private fun getCategoryForLawLocal(key: String, text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("procure") || lower.contains("contract") || lower.contains("tender") || lower.contains("award") || key.startsWith("PPA") -> "PROCUREMENT/CONTRACT/PROJECT"
            lower.contains("payroll") || lower.contains("salary") || lower.contains("wage") || lower.contains("allowance") || lower.contains("hr ") || lower.contains("human resource") || lower.contains("staff") || lower.contains("employee") -> "HUMAN RESOURCE / PAYROLL"
            lower.contains("tax") || lower.contains("wht") || lower.contains("paye") || lower.contains("duty") || lower.contains("levy") || lower.contains("deduction") || lower.contains("tariffs") -> "TAX & STATUTORY DEDUCTIONS"
            lower.contains("stores") || lower.contains("storekeeping") || lower.contains("stock") || lower.contains("inventory") || lower.contains("warehouse") || lower.contains("library") -> "STORES /LIBRARY"
            lower.contains("vehicle") || lower.contains("transport") || lower.contains("car ") || lower.contains("fuel") || lower.contains("logbook") -> "TRANSPORT"
            lower.contains("asset") || lower.contains("property") || lower.contains("equipment") || lower.contains("land") || lower.contains("building") -> "ASSET"
            lower.contains("it ") || lower.contains("computer") || lower.contains("system") || lower.contains("software") || lower.contains("information technology") || lower.contains("electronic") || lower.contains("gifmis") -> "IT"
            lower.contains("revenue") || lower.contains("igf") || lower.contains("income") || lower.contains("collection") || lower.contains("receipt") || lower.contains("gcr") -> "REVENUE"
            lower.contains("financial statement") || lower.contains("account") || lower.contains("report") || lower.contains("audit") || lower.contains("reconciliation") || lower.contains("balance sheet") -> "FINANCIAL REPORTING"
            lower.contains("expenditure") || lower.contains("payment") || lower.contains("disbursement") || lower.contains("imprest") || lower.contains("voucher") || lower.contains("pv ") -> "EXPENDITURE"
            else -> {
                when {
                    key.startsWith("LGA") || key.startsWith("IAA") -> "ADMINISTRATIVE / GOVERNANCE / OTHERS"
                    else -> "ADMINISTRATIVE / GOVERNANCE / OTHERS"
                }
            }
        }
    }
    
    private fun cleanLawText(input: String): String {
        var text = input
        // Remove Part titles (e.g. PART III - FINANCIAL AND MISCELLANEOUS PROVISIONS)
        val partRegex = Regex("(?i)\\bPART\\s+(?:[IVXLCDM]+|\\w+)\\s*[–-]?\\s*[A-Z\\s_]+(?:AND\\s+[A-Z\\s_]+)?")
        text = text.replace(partRegex, "")
        
        // Remove other common OCR/Layout headers/footers
        text = text.replace(Regex("(?i)\\bDate of Gazette notification:.*$"), "")
        text = text.replace(Regex("(?i)\\bGPCL, ASSEMBLY PRESS, ACCRA.*$"), "")
        text = text.replace(Regex("(?i)\\bWebsite: www.ghpublishingcompany.co.*$"), "")
        text = text.replace(Regex("(?i)\\bE-mail: info@ghpublishingcompany.com.*$"), "")
        
        // Clean multiple spaces and return
        return text.replace(Regex("\\s+"), " ").trim()
    }
    
    private fun generateObservationForLawLocal(key: String, text: String): String {
        val cleanedText = cleanLawText(text)
        val lines = cleanedText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        val parts = key.split("_")
        val act = parts[0]
        val num = parts.getOrNull(1) ?: ""
        val prefix = if (act == "PFMR") "Regulation $num" else "Section $num"
        
        if (lines.isEmpty()) return "Failure to comply with the statutory requirements of $prefix."
        
        val firstLine = lines[0]
        var titlePart = if (firstLine.contains(":")) firstLine.substringBefore(":").trim() else firstLine
        titlePart = cleanLawText(titlePart).replace(Regex(":$"), "").trim()
        
        // If parsed title is too long or contains a full sentence, fallback to prefix
        if (titlePart.length > 90 || titlePart.lowercase(Locale.ROOT).contains("specifically") || titlePart.lowercase(Locale.ROOT).contains("shall")) {
            titlePart = prefix
        }
        
        var cleanTitleName = titlePart
            .replace(Regex("^(Regulation|Section|Act)\\s+\\d+\\s*-\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(Regulation|Section|Act)\\s+\\d+\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            
        if (cleanTitleName.length > 70 || cleanTitleName.isEmpty() || cleanTitleName == prefix) {
            cleanTitleName = "applicable requirements"
        }
        
        // Custom overrides for specific keys to make them highly realistic
        val overrides = mapOf(
            "IAA_1" to "Failure to align operations with the statutory object of the Internal Audit Agency",
            "IAA_2" to "Failure of the Agency to set standards or facilitate prevention and detection of fraud",
            "IAA_4" to "Improper constitution of the Internal Audit Board",
            "IAA_6" to "Failure of the Board to formulate policies or establish appropriate structures",
            "IAA_7" to "Non-compliance with tenure of office limits for Board members",
            "IAA_8" to "Failure to pay approved allowances or regulate Board meetings",
            "IAA_11" to "Failure of the Board to properly appoint committees or coordinate with the Director-General",
            "IAA_15" to "Failure to properly staff or administer the Agency's secretariat",
            "IAA_17" to "Failure to take appropriate action or recommend prosecution on internal audit reports",
            "IAA_18" to "Failure of internal auditors to exhibit professionalism or competence",
            "IAA_19" to "Failure of internal auditors to maintain integrity or avoid conflicts of interest",
            "IAA_20" to "Failure to maintain confidentiality of audit findings and investigations",
            "IAA_26" to "Failure to properly transition or assign internal audit staff",
            "IAA_27" to "Failure to properly transition or assign internal audit duties",
            
            "PFM_1" to "Failure of the Controller and Accountant-General to submit consolidated accounts",
            "PFM_2" to "Failure of Spending Officer to submit financial statements and accounts on time",
            "PFM_3" to "Failure of spending officers or public officers to discharge budget management responsibilities",
            "PFM_11" to "Failure to comply with parliamentary macroeconomic and fiscal policy oversight",
            "PFM_17" to "Failure of Cabinet to adhere to targets set out in the Fiscal Strategy Document",
            "PFM_18" to "Unauthorised suspension of fiscal rules or targets",
            "PFM_23" to "Unauthorised expenditure in advance of appropriation",
            "PFM_30" to "Obtaining a credit facility without approval or managing budget outside allocations",
            "PFM_31" to "Failure of spending officer to manage budget within allocations",
            "PFM_35" to "Failure to submit supplementary budget estimates for approval",
            "PFM_36" to "Unauthorised Contingency Fund payments or advancements",
            "PFM_44" to "Failure to transfer designated public bank funds into the Treasury Single Account",
            "PFM_53" to "Abandonment of claims or write-off of public funds without proper authorization",
            "PFM_56" to "Borrowing without parliamentary approval of terms and conditions",
            "PFM_57" to "Borrowing for unapproved or non-statutory purposes",
            "PFM_62" to "Unauthorised borrowing or debt management operations",
            "PFM_73" to "Unauthorised borrowing by local government authorities or public corporations",
            "PFM_77" to "Failure of public corporations or state-owned enterprises to comply with reporting requirements",
            "PFM_78" to "Failure to appoint agents in accordance with public accounts regulations",
            "PFM_85" to "Failure of Audit Committee to report on findings and recommendations",
            "PFM_87" to "Improper composition of the Audit Committee",
            "PFM_89" to "Failure of Local Government Minister to submit consolidated audited accounts on time",
            "PFM_90" to "Failure of public corporation governing body to manage resources prudently",
            "PFM_93" to "Failure of public corporation to submit annual financial plan on time",
            "PFM_94" to "Failure of public corporation to comply with financial directives",
            "PFM_95" to "Failure of public corporation governing body to submit audited financial statements on time",
            "PFM_96" to "Commission of public finance offences leading to statutory penalties",
            "PFM_97" to "Failure to recover loss or deficit of public funds through surcharge procedures",
            "PFM_98" to "Contravention of PFM Act provisions leading to administrative and statutory penalties",
            "PFM_102" to "Failure to comply with statutory accounting standards and definitions",
            "PFM_104" to "Failure to comply with transitional PFM regulations and schedules",
            
            "PFMR_1" to "Failure to comply with the preliminary provisions of PFM Regulations",
            "PFMR_2" to "Failure to align operations with the statutory object of PFM Regulations",
            "PFMR_5" to "Failure of spending officer to manage budget within allocations",
            "PFMR_6" to "Failure of spending officer to manage budget within allocations",
            "PFMR_7" to "Failure of spending officer to manage budget within allocations",
            "PFMR_8" to "Failure of spending officer to manage budget within allocations",
            "PFMR_9" to "Failure of spending officer to manage budget within allocations",
            "PFMR_10" to "Failure to establish or maintain a functional Internal Audit Unit",
            "PFMR_11" to "Failure to establish or maintain a functional Internal Audit Unit",
            "PFMR_12" to "Failure to establish or maintain a functional Internal Audit Unit",
            "PFMR_13" to "Improper composition or functioning of the Audit Committee",
            "PFMR_14" to "Improper composition or functioning of the Audit Committee",
            "PFMR_15" to "Improper composition or functioning of the Audit Committee",
            "PFMR_16" to "Improper composition or functioning of the Audit Committee",
            "PFMR_17" to "Improper composition or functioning of the Audit Committee",
            "PFMR_27" to "Unauthorised virement of funds without written approval",
            "PFMR_28" to "Unauthorised virement of funds without written approval",
            "PFMR_43" to "Collection or utilization of Internally Generated Funds without approval",
            "PFMR_48" to "Failure to deposit IGF collections within 24 hours",
            "PFMR_49" to "Opening bank accounts without written authorization or failing to reconcile monthly",
            "PFMR_50" to "Unauthorised deduction of commission or expenses from revenue",
            "PFMR_78" to "Failure to make payments against valid, authorized vouchers",
            "PFMR_79" to "Failure to verify goods, services or works before making payment",
            "PFMR_81" to "Failure to process payments to suppliers within contract timelines",
            "PFMR_82" to "Unauthorised commitment of public funds outside GIFMIS",
            "PFMR_83" to "Making unauthorised cash payments to third parties",
            "PFMR_100" to "Improper management or custody of cash and public assets",
            "PFMR_101" to "Improper management or custody of cash and public assets",
            "PFMR_102" to "Failure to retire special or petty cash imprests on time",
            "PFMR_103" to "Failure to retire special or petty cash imprests on time",
            "PFMR_104" to "Failure to retire special or petty cash imprests on time",
            "PFMR_105" to "Failure to retire special or petty cash imprests on time",
            "PFMR_106" to "Failure to retire special or petty cash imprests on time",
            "PFMR_232" to "Commission of financial misconduct leading to statutory sanctions",
            "PFMR_234" to "Failure to substantiate or justify questioned expenditure",
            
            "LGA_1" to "Failure to comply with planning system amendments and repeals",
            "LGA_4" to "Failure to declare planning areas in accordance with spatial planning laws",
            "LGA_8" to "Failure to properly compose or hold meetings of the planning committee",
            "LGA_9" to "Failure to preserve trees or regulate open spaces",
            "LGA_10" to "Failure to properly regulate public cemeteries and burial grounds",
            "LGA_11" to "Failure to use proper building scaffolding, hoardings, or safety machinery during construction",
            "LGA_12" to "Failure to satisfy site conditions required for building construction",
            "LGA_13" to "Failure to provide adequate sanitary arrangements and conveniences in new buildings",
            "LGA_14" to "Failure to comply with spatial planning requirements for dressing stations",
            "LGA_15" to "Failure to satisfy special building requirements on designated shopping streets",
            "LGA_16" to "Failure to establish a functional legal department at the district level",
            "LGA_19" to "Failure to preserve trees or regulate open spaces",
            "LGA_20" to "Failure to comply with duration and extension rules for development permits",
            "LGA_25" to "Failure to register or regulate public cemeteries and burial grounds",
            "LGA_29" to "Failure to consult District Planning Authority on land subdivision or use",
            "LGA_39" to "Failure of District Planning Authority to make or enforce building by-laws",
            "LGA_40" to "Failure to use proper building scaffolding or safety machinery during construction",
            "LGA_60" to "Failure to comply with spatial planning or development requirements",
            "LGA_64" to "Failure of District Planning Authority to prepare or submit development plans",
            "LGA_66" to "Failure to consult stakeholders in spatial planning development",
            "LGA_73" to "Undertaking building development without a valid development permit",
            "LGA_81" to "Failure to comply with spatial planning or development requirements",
            "LGA_88" to "Failure to comply with spatial planning or development requirements",
            "LGA_97" to "Failure to comply with spatial planning or development requirements",
            "LGA_98" to "Failure to properly handle claims for redress or appeals advisory committees",
            "LGA_101" to "Failure to recover betterment charges on urban land transactions",
            "LGA_103" to "Non-conforming land allocations or sub-dividing land without District Planning Authority consultation",
            "LGA_105" to "Constructing buildings or executing work without a valid building permit",
            "LGA_108" to "Unauthorised delegation of land use and spatial planning functions",
            "LGA_111" to "Failure to hold planning officers accountable for acts not done in good faith",
            "LGA_113" to "Failure of District Assembly to prepare response plans or make by-laws for emergency and disaster relief",
            "LGA_114" to "Failure to coordinate activities of designated agencies in emergency and disaster response",
            "LGA_115" to "Failure of designated agencies to perform emergency and disaster relief services",
            "LGA_116" to "Failure of District Assembly to exercise powers and coordinate fire services",
            "LGA_118" to "Failure of designated agency to close streets or regulate traffic during emergency fire outbreaks",
            "LGA_119" to "Obstruction of designated agencies in emergency, disaster prevention, or fire services",
            "LGA_120" to "Giving false alarms of fire to designated agencies",
            "LGA_121" to "Failure to set up enquiry committee for fire outbreak",
            "LGA_122" to "Failure to include required departments and development plans in the District Assembly budget",
            "LGA_123" to "Failure to submit detailed budget estimates to Regional Coordinating Council",
            "LGA_124" to "Failure to open and maintain bank accounts for revenues",
            "LGA_126" to "Failure to make required development fund allocations to District Assemblies",
            "LGA_129" to "Failure of Administrator to distribute Common Fund allocations or submit reports",
            "LGA_135" to "Failure to address complaints regarding Common Fund administration",
            "LGA_136" to "Failure to properly manage or transfer decentralised grants-in-aid",
            "LGA_137" to "Failure to collect licence fees from designated economic operators",
            "LGA_138" to "Failure to license vehicles or pay required vehicle licence fees",
            "LGA_139" to "Failure to obtain required licences or pay fees for entertainment activities",
            "LGA_140" to "Contravention of licence conditions or failure to produce licence for inspection",
            "LGA_143" to "Unauthorised investment of District Assembly moneys in safe securities",
            "LGA_144" to "Failure of rating authority to levy sufficient rates to meet expenditure",
            "LGA_147" to "Failure to publish notice of rate in the prescribed manner",
            "LGA_148" to "Failure to pay rates to authorised collectors or manage exemptions",
            "LGA_150" to "Failure to comply with ministerial guidelines when levying rates",
            "LGA_151" to "Failure to give priority to rate claims over other non-governmental claims",
            "LGA_152" to "Failure to enforce rate charges on premises or appoint Rate Assessment Committee",
            "LGA_157" to "Contravention of rating obligations through landlord-tenant agreements",
            "LGA_159" to "Inciting persons not to pay rates or collecting rates without authorization",
            "LGA_163" to "Failure to pay rates by public boards and government institutions",
            "LGA_164" to "Failure of public agencies or government to pay agreed rates on premises",
            "LGA_167" to "Failure of employer to deduct rates from employee remuneration",
            "LGA_174" to "Failure to provide access to records of District Assembly to authorised inspectors",
            "LGA_175" to "Failure to submit quarterly internal audit reports",
            "LGA_177" to "Failure to recover sums certified as due under Auditor-General surcharge",
            "LGA_180" to "Failure of District Assembly to make or submit by-laws",
            "LGA_184" to "Failure to deposit copies of by-laws at District Assembly offices for inspection",
            "LGA_185" to "Failure to establish Regional Coordinating Councils or perform coordinating functions",
            "LGA_186" to "Failure to properly compose or hold meetings of Regional Coordinating Councils",
            "LGA_193" to "Failure of the Office of Regional Coordinating Council to assist in coordinating functions",
            "LGA_197" to "Failure of Regional Departments to coordinate and provide quarterly reports",
            "LGA_198" to "Failure to collaborate and co-operate with other sectors of the public service",
            "LGA_202" to "Failure of Regional Minister to supervise and evaluate District Chief Executives",
            "LGA_203" to "Failure of Regional Coordinating Director to prepare annual report",
            "LGA_204" to "Failure to properly coordinate decentralisation policies through Inter-Ministerial Committee",
            "LGA_205" to "Failure to establish Technical Committee or Secretariat for Inter-Ministerial Committee",
            "LGA_212" to "Failure to properly describe ownership of property or manage power of entry",
            "LGA_215" to "Failure of occupier to provide specified particulars of the owner of premises",
            "LGA_218" to "Failure to properly authenticate or serve notices and orders on behalf of District Assembly",
            "LGA_222" to "Unauthorised sitting or voting by disqualified persons in District Assembly meetings",
            "LGA_224" to "Failure of officers to disclose interest in contracts or accept unauthorised fees",
            "LGA_225" to "Failure to properly handle legal proceedings or defend suits against District Assembly",
            "LGA_231" to "Failure to comply with spatial planning or development requirements",
            "LGA_233" to "Failure to comply with spatial planning or development requirements"
        )
        
        if (overrides.containsKey(key)) {
            return overrides[key]!!
        }
        
        val lowerTitle = cleanTitleName.lowercase(Locale.ROOT)
        return when {
            lowerTitle.startsWith("management of") -> "Failure to manage ${cleanTitleName.substring(13).trim()} properly"
            lowerTitle.startsWith("virement of") -> "Unauthorised virement of ${cleanTitleName.substring(11).trim()}"
            lowerTitle.startsWith("virement") -> "Unauthorised virement of funds"
            lowerTitle.startsWith("verification of") -> "Failure to verify ${cleanTitleName.substring(15).trim()}"
            lowerTitle.startsWith("submission of") -> "Failure to submit ${cleanTitleName.substring(13).trim()}"
            lowerTitle.startsWith("preparation of") -> "Failure to prepare ${cleanTitleName.substring(14).trim()}"
            lowerTitle.startsWith("keeping of") -> "Failure to keep ${cleanTitleName.substring(10).trim()}"
            lowerTitle.startsWith("reporting of") -> "Failure to report ${cleanTitleName.substring(12).trim()}"
            lowerTitle.startsWith("reporting") -> "Failure to comply with reporting requirements for ${cleanTitleName.substring(9).trim()}"
            lowerTitle.startsWith("oversight of") -> "Failure to comply with oversight requirements for ${cleanTitleName.substring(12).trim()}"
            lowerTitle.startsWith("suspension of") -> "Unauthorised suspension of ${cleanTitleName.substring(13).trim()}"
            lowerTitle.startsWith("borrowing by") -> "Unauthorised borrowing by ${cleanTitleName.substring(12).trim()}"
            lowerTitle.startsWith("borrowing") -> "Unauthorised borrowing of ${cleanTitleName.substring(9).trim()}"
            lowerTitle.startsWith("approval of") -> "Failure to obtain approval for ${cleanTitleName.substring(11).trim()}"
            lowerTitle.startsWith("payment of") -> "Irregular payment of ${cleanTitleName.substring(10).trim()}"
            lowerTitle.startsWith("collection of") -> "Failure to collect ${cleanTitleName.substring(13).trim()}"
            lowerTitle.startsWith("acquisition of") -> "Unauthorised acquisition of ${cleanTitleName.substring(14).trim()}"
            lowerTitle.startsWith("disposal of") -> "Unauthorised disposal of ${cleanTitleName.substring(11).trim()}"
            lowerTitle.startsWith("establishment of") -> "Failure to establish ${cleanTitleName.substring(16).trim()}"
            lowerTitle.startsWith("composition of") -> "Improper composition of ${cleanTitleName.substring(14).trim()}"
            lowerTitle.startsWith("enquiries by") -> "Failure to conduct enquiries by ${cleanTitleName.substring(12).trim()}"
            lowerTitle.startsWith("duties of") -> "Failure to perform duties of ${cleanTitleName.substring(9).trim()}"
            lowerTitle.startsWith("duty of") -> "Failure to perform duty of ${cleanTitleName.substring(7).trim()}"
            lowerTitle.startsWith("functions of") -> "Failure to perform functions of ${cleanTitleName.substring(12).trim()}"
            lowerTitle.startsWith("access to") -> "Failure to provide access to ${cleanTitleName.substring(9).trim()}"
            lowerTitle.startsWith("power to") -> "Failure to properly exercise power to ${cleanTitleName.substring(8).trim()}"
            lowerTitle.startsWith("laying of") -> "Failure to lay ${cleanTitleName.substring(9).trim()}"
            lowerTitle.startsWith("liability of") -> "Improper liability of ${cleanTitleName.substring(12).trim()}"
            lowerTitle.contains("reconciliation") -> "Failure to perform monthly bank reconciliations"
            lowerTitle.contains("reconcile") -> "Failure to reconcile accounts"
            
            cleanTitleName == "applicable requirements" -> "Failure to comply with the statutory requirements of $prefix."
            
            else -> {
                if (lowerTitle.startsWith("non-") || lowerTitle.startsWith("irregular") || lowerTitle.startsWith("unauthorised") || lowerTitle.startsWith("failure")) {
                    cleanTitleName.substring(0, 1).uppercase(Locale.ROOT) + cleanTitleName.substring(1)
                } else {
                    val cleanLower = lowerTitle.trim()
                    if (cleanLower.endsWith("s")) {
                        "Failure to comply with requirements governing $cleanTitleName."
                    } else {
                        "Failure to adhere to regulations regarding $cleanTitleName."
                    }
                }
            }
        }
    }
    
    private fun getCriteriaNameForLawLocal(key: String): String {
        val parts = key.split("_")
        val act = parts[0]
        val sectionNum = parts.getOrNull(1) ?: ""
        return when (act) {
            "PFMR" -> "Regulation $sectionNum of the Public Financial Management Regulations, 2019 (L.I. 2378)"
            "PFM" -> "Section $sectionNum of the Public Financial Management Act, 2016 (Act 921)"
            "PPA" -> "Section $sectionNum of the Public Procurement Act, 2003 (Act 663) as amended"
            "LGA" -> "Section $sectionNum of the Local Governance Act, 2016 (Act 936)"
            "IAA" -> "Section $sectionNum of the Internal Audit Agency Act, 2003 (Act 658)"
            else -> key
        }
    }

    @Test
    fun extractLawTextFromPdf() {
        val actsToParse = listOf(
            Triple("IAA.pdf", 3, "IAA"),
            Triple("PFM.pdf", 5, "PFM"),
            Triple("PPA.pdf", 5, "PPA"),
            Triple("LGA.pdf", 13, "LGA")
        )
        
        actsToParse.forEach { (fileName, startPage, actCode) ->
            // Look for files in the project root or one level up
            val pdfFile = File("../$fileName").takeIf { it.exists() } ?: File(fileName)

            if (pdfFile.exists()) {
                println("Parsing $fileName (starting at page $startPage)...")
                val sections = parseActPdf(pdfFile, actCode, startPage)
                
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(sections)
                
                val outFile = File("extracted_${actCode}.json")
                outFile.writeText(jsonString)
                println("Extracted ${sections.size} sections from $fileName to ${outFile.absolutePath}")
            } else {
                println("File not found: ${pdfFile.absolutePath}")
            }
        }
    }

    private fun parseActPdf(pdfFile: File, actCode: String, startPage: Int): Map<String, String> {
        val sections = mutableMapOf<String, String>()
        Loader.loadPDF(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            stripper.startPage = startPage
            stripper.endPage = document.numberOfPages
            val fullText = stripper.getText(document)
            
            // Find the start of the schedules to prevent section numbers in schedules from overwriting main sections.
            val scheduleRegex = Regex("(?mi)^\\s*(?:SCHEDULE|SCHEDULES|FIRST SCHEDULE|SCHEDULE 1)\\b")
            val scheduleMatch = scheduleRegex.find(fullText)
            val mainText = if (scheduleMatch != null) {
                fullText.substring(0, scheduleMatch.range.first)
            } else {
                fullText
            }
            
            // Matches headers like "Section 1.", "1.", "Regulation 12." at the start of a line
            val pattern = Regex("(?mi)^\\s*(?:Section|Regulation|Article)?\\s*(\\d+)\\.\\s+")
            val matches = pattern.findAll(mainText).toList()
            
            for (i in matches.indices) {
                val currentMatch = matches[i]
                val sectionNum = currentMatch.groupValues[1]
                val startIdx = currentMatch.range.last + 1
                val endIdx = if (i + 1 < matches.size) matches[i + 1].range.first else mainText.length
                
                var sectionBody = mainText.substring(startIdx, endIdx).trim()
                sectionBody = cleanSectionText(sectionBody, actCode)
                
                // Extract section title from the preceding text
                val preStart = if (i > 0) matches[i - 1].range.last + 1 else 0
                val preText = mainText.substring(preStart, currentMatch.range.first).trim()
                val preLines = preText.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
                
                val title = if (preLines.isNotEmpty()) {
                    val lastLine = preLines.last()
                    // Titles are usually short and don't end with sentence punctuation or numbers
                    if (lastLine.length < 85 && !lastLine.endsWith(".") && !lastLine.endsWith(";") && !lastLine.matches(Regex(".*\\d+$"))) {
                        lastLine
                    } else {
                        ""
                    }
                } else {
                    ""
                }
                
                val prefix = when(actCode) {
                    "PFMR" -> "Regulation"
                    "PFM" -> "Section"
                    "IAA" -> "Section"
                    "LGA" -> "Section"
                    "PPA" -> "Section"
                    else -> "Section"
                }

                val formattedText = if (title.isNotEmpty()) {
                    "$prefix $sectionNum - $title:\n$sectionBody"
                } else {
                    "$prefix $sectionNum:\n$sectionBody"
                }
                
                sections["${actCode}_$sectionNum"] = formattedText
            }
        }
        return sections
    }

    @Test
    fun mergeLawText() {
        val assetsDir = File("../app/src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
        val lawDetailsFile = File(assetsDir, "law_details.json")
        
        if (!lawDetailsFile.exists()) {
            println("law_details.json not found at ${lawDetailsFile.absolutePath}")
            return
        }

        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        val type = object : TypeToken<MutableMap<String, String>>() {}.type
        val lawDetails: MutableMap<String, String> = gson.fromJson(lawDetailsFile.readText(), type)

        val extractedFiles = listOf(
            "extracted_IAA.json",
            "extracted_LGA.json",
            "extracted_PFM.json",
            "extracted_PPA.json"
        )

        var mergedCount = 0
        var replacedCount = 0

        extractedFiles.forEach { fileName ->
            val file = File(fileName)
            if (file.exists()) {
                val extractedData: Map<String, String> = gson.fromJson(file.readText(), type)
                extractedData.forEach { (key, fullText) ->
                    val existing = lawDetails[key]
                    if (existing == null) {
                        lawDetails[key] = fullText
                        mergedCount++
                    } else if (fullText.length > existing.length + 10 || existing.lines().size < 2) {
                        // Replace if new text is significantly longer or existing is likely just a title
                        lawDetails[key] = fullText
                        replacedCount++
                    }
                }
            } else {
                println("Extracted file $fileName not found. Run extractLawTextFromPdf first.")
            }
        }

        // Final cleanup of the whole map
        lawDetails.keys.forEach { key ->
            lawDetails[key] = cleanSectionText(lawDetails[key] ?: "", key.substringBefore("_"))
        }

        lawDetailsFile.writeText(gson.toJson(lawDetails))
        println("Merged $mergedCount new sections and replaced $replacedCount existing sections.")
        println("Updated law_details.json at ${lawDetailsFile.absolutePath}")
    }

    private fun cleanSectionText(text: String, actCode: String): String {
        var cleaned = text
        
        // Remove specific Act footers/headers and OCR artifacts
        val patternsToRemove = listOf(
            "Internal Audit Agency Act, 2003 Act 658",
            "Internal Audit Agency Act, 2003",
            "Internal Audi gency Act, 2003",
            "Act 658",
            "Public Financial Management Act, 2016",
            "PUBLIC FINANCIAL MANAGEMENT ACT, 2016",
            "Act 921",
            "Local Governance Act, 2016",
            "LOCALLoc Gal OGVovEerRnNanAceN ACcEt, 2A0C16T, 2016 Act 936",
            "Act 936",
            "Public Procurement Act, 2003",
            "Act 663 Public Procurement Act, 2003",
            "Act 663",
            "Public Procurement (Amendment) Act, 2016",
            "Act 914",
            "GPCL, ASSEMBLY PRESS, ACCRA",
            "GPCL/A656/1,500/08/2016",
            "Website: www.ghpublishingcompany.co7m",
            "Website: www.ghpublishingcompany.com",
            "E-mail: info@ghpublishingcompany.com",
            "Date of Gazette notification: 26th August, 2016"
        )
        
        for (pattern in patternsToRemove) {
            cleaned = cleaned.replace(Regex("(?i)" + Regex.escape(pattern)), "")
        }
        
        // Remove running headers
        cleaned = cleaned.replace(Regex("(?i)LOCAL GOVERNANCE ACT, 2016"), "")
        cleaned = cleaned.replace(Regex("(?i)PUBLIC PROCUREMENT ACT, 2003"), "")
        cleaned = cleaned.replace(Regex("(?i)PUBLIC FINANCIAL MANAGEMENT ACT, 2016"), "")
        cleaned = cleaned.replace(Regex("(?i)INTERNAL AUDIT AGENCY ACT, 2003"), "")
        
        // Remove lines that are just numbers (page numbers)
        // Usually these are standalone numbers like " 71 ", " 72 ", etc.
        cleaned = cleaned.replace(Regex("(?m)^\\s*\\d+\\s*$"), "")
        // Also remove numbers within text if they look like page artifacts (e.g., " 71 " surrounded by spaces)
        cleaned = cleaned.replace(Regex("\\s+\\d{1,3}\\s+"), " ")

        // Clean up common OCR artifacts like "71 (i)", "72 (ii)" where the number is a page artifact
        cleaned = cleaned.replace(Regex("\\b\\d{2,3}\\s+\\("), "(")

        // Final formatting: remove multiple spaces and trim
        return cleaned.replace(Regex("\\s+"), " ").trim()
    }
}


