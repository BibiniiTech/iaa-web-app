package com.example.iaaapp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iaaapp.ui.theme.IAAAppTheme
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun PFMLeagueScreen() {
    val isPreview = LocalInspectionMode.current
    val remoteConfig = if (isPreview) null else Firebase.remoteConfig
    
    val heading = remember(remoteConfig) {
        remoteConfig?.getString("pfm_league_heading") ?: "PFM Compliance League Table"
    }
    
    val description = remember(remoteConfig) {
        remoteConfig?.getString("pfm_league_description") ?: "The Public Financial Management (PFM) Compliance League Table is a landmark transparency and accountability initiative aimed at strengthening fiscal discipline and improving the management of public resources. This fulfills the Government’s commitment in the 2025 Budget Statement to publish an objective, evidence-based assessment of how public institutions comply with the PFM Act, 2016 (Act 921), its Regulations and associated laws. The League Table serves as a performance benchmarking tool that measures the extent to which public institutions adhere to the rules and procedures governing the use of public funds."
    }

    val mdasJson = remember(remoteConfig) {
        remoteConfig?.getString("pfm_league_mdas") ?: ""
    }
    
    val mmdasJson = remember(remoteConfig) {
        remoteConfig?.getString("pfm_league_mmdas") ?: ""
    }

    val mdaData = remember(mdasJson) { parsePFMData(mdasJson, getDefaultMDAData()) }
    val mmdaData = remember(mmdasJson) { parsePFMData(mmdasJson, getDefaultMMDAData()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Default.Leaderboard,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = heading,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LeagueCategoryCard(title = "MDAs", sections = mdaData)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LeagueCategoryCard(title = "MMDAs", sections = mmdaData)
        
        Spacer(modifier = Modifier.height(150.dp))
    }
}

@Composable
fun LeagueCategoryCard(title: String, sections: List<PFMSection>) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            
            if (expanded) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    sections.forEach { section ->
                        PFMSectionView(section)
                    }
                }
            }
        }
    }
}

@Composable
fun PFMSectionView(section: PFMSection) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = TextAlign.Center
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = getStatusColor(section.status),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (section.institutions.isEmpty()) {
                Text(
                    text = "No institutions listed yet.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                section.institutions.forEach { institution ->
                    Text(
                        text = institution,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "HIGHLY COMPLIANT" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        "COMPLIANT" -> Color(0xFF8BC34A).copy(alpha = 0.2f)
        "MODERATELY COMPLIANT" -> Color(0xFFFFEB3B).copy(alpha = 0.2f)
        "LEAST COMPLIANT" -> Color(0xFFF44336).copy(alpha = 0.2f)
        else -> Color.Gray.copy(alpha = 0.1f)
    }
}

data class PFMSection(
    val title: String,
    val status: String,
    val institutions: List<String>
)

fun parsePFMData(json: String, default: List<PFMSection>): List<PFMSection> {
    if (json.isBlank()) return default
    return try {
        val type = object : TypeToken<List<PFMSection>>() {}.type
        Gson().fromJson(json, type)
    } catch (_: Exception) {
        default
    }
}

fun getDefaultMDAData() = listOf(
    PFMSection(
        "HIGHLY COMPLIANT INSTITUTIONS",
        "HIGHLY COMPLIANT",
        listOf(
            "1. Environmental Protection Authority",
            "2. Tema Oil Refinery",
            "3. Ministry of Energy and Green Transition",
            "4. Ghana National Petroleum Corporation",
            "5. Ministry of Finance",
            "6. Ghana Aids Commission",
            "7. Petroleum Hub Development Corporation"
        )
    ),
    PFMSection(
        "COMPLIANT INSTITUTIONS",
        "COMPLIANT",
        listOf(
            "8. Cocoa Marketing Company",
            "9. Petroleum Commission",
            "10. Office of the Registrar of Companies",
            "11. Ministry of Food and Agriculture",
            "12. Office of the Attorney-General and Ministry of Justice",
            "13. Ministry of Transport",
            "14. Commission on Human Rights and Administrative Justice",
            "15. Ministry of Communication Digital Technology and Innovation",
            "16. Office of the Head of the Local Government Service",
            "17. Ghana Airports Company Limited",
            "18. Ghana Education Trust Fund (GETFund)",
            "19. Forestry Commission",
            "20. National Pensions Regulatory Authority",
            "21. Ministry of Foreign Affairs",
            "22. National Service Secretariat",
            "23. Ghana Highway Authority",
            "24. Ghana Standards Authority",
            "25. Ministry of Fisheries and Aquaculture",
            "26. Ministry of Youth Development and Empowerment",
            "27. Ghana News Agency",
            "28. Council for Scientific and Industrial Research",
            "29. Rent Control Department",
            "30. Public Services Commission",
            "31. Ghana Shippers Authority",
            "32. National Insurance Commission",
            "33. Tema Development Corporation (TDC)",
            "34. Minerals Commission",
            "35. Ghana Police Service",
            "36. National Commission for Civic Education",
            "37. Right to Information Commission",
            "38. Office of the President, Jubilee House",
            "39. Institute of Local Government Studies",
            "40. Driver and Vehicle Licensing Authority",
            "41. Ministry of Environment, Science and Technology",
            "42. National Identification Authority",
            "43. Copyright Office",
            "44. Narcotics Control Commission",
            "45. National Petroleum Authority",
            "46. Ministry of Trade, Agribusiness and Industry",
            "47. Ghana Export Promotion Authority",
            "48. Ministry of Tourism, Culture and Creative Arts",
            "49. Ministry of Works, Housing, and Water Resources",
            "50. Ministry of Health",
            "51. Securities and Exchange Commission",
            "52. National Intelligence Bureau",
            "53. Ghana Atomic Energy Commission",
            "54. Controller & Accountant - General's Dept",
            "55. Cyber Security Authority",
            "56. Zongo Development Fund",
            "57. National Youth Authority",
            "58. Ministry of Local Government, Chieftaincy and Religious Affairs",
            "59. Ghana College of Physicians and Surgeons",
            "60. Ghana Immigration Service"
        )
    ),
    PFMSection(
        "MODERATELY COMPLIANT INSTITUTIONS",
        "MODERATELY COMPLIANT",
        listOf(
            "61. Department of Parks and Gardens",
            "62. Registrar-General’s Dept.",
            "63. Ghana Infrastructure and Investment Fund",
            "64. Ministry for Sports and Recreation",
            "65. Ghana Gas",
            "66. Ministry of Defence",
            "67. District Assemblies Common Fund",
            "68. Land Use and Spatial Planning Authority (LUSPA)",
            "69. National Labour Commission",
            "70. Ministry of Gender, Children and Social Protection",
            "71. Ministry of Labour, Jobs, and Employment",
            "72. Ghana Ports and Harbours Authority",
            "73. Ghana Health Service",
            "74. Ghana Prisons Service",
            "75. Metro Mass Transit Ltd.",
            "76. Ministry of Interior",
            "77. Cocoa Processing Company",
            "78. Ministry of Roads and Highways",
            "79. Economic and Organised Crime Office",
            "80. University for Professional Studies",
            "81. National Development Planning Commission",
            "82. Ghana Audit Service"
        )
    ),
    PFMSection(
        "LEAST COMPLIANT INSTITUTIONS",
        "LEAST COMPLIANT",
        listOf(
            "83. Office of Legal Aid Commission",
            "84. Minerals Income Investment Fund",
            "85. Ghana Commodity Exchange",
            "86. Venture Capital Trust Fund",
            "87. National Peace Council",
            "88. Financial Intelligence Centre (FIC)",
            "89. Korle Bu Teaching Hospital",
            "90. University of Ghana",
            "91. Ghana Integrated Iron & Steel Development Corporation",
            "92. National Council for Curriculum & Assessment",
            "93. Ghana Revenue Authority",
            "94. National Media Commission",
            "95. National Food Buffer Stock Company",
            "96. Office of the Head of the Civil Service",
            "97. Ministry of Education - HQ",
            "98. Ghana Infrastructure Fund for Electronic Communications",
            "99. Ghana National Fire Service",
            "100. NADMO",
            "101. National Communications Authority"
        )
    )
)

fun getDefaultMMDAData() = listOf(
    PFMSection("HIGHLY COMPLIANT INSTITUTIONS", "HIGHLY COMPLIANT", emptyList()),
    PFMSection("COMPLIANT INSTITUTIONS", "COMPLIANT", emptyList()),
    PFMSection("MODERATELY COMPLIANT INSTITUTIONS", "MODERATELY COMPLIANT", emptyList()),
    PFMSection("LEAST COMPLIANT INSTITUTIONS", "LEAST COMPLIANT", emptyList())
)

@Preview(showBackground = true)
@Composable
fun PFMLeagueScreenPreview() {
    IAAAppTheme {
        PFMLeagueScreen()
    }
}
