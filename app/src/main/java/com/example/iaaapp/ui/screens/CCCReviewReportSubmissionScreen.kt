package com.example.iaaapp.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.iaaapp.ui.theme.IAAAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CCCReviewReportSubmissionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var institutionType by remember { mutableStateOf("") }
    var institutionText by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("") }
    var selectedMmda by remember { mutableStateOf("") }
    var quarter by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }
    var senderEmail by remember { mutableStateOf("") }
    
    // Store pairs of Uri and FileName
    val uploadedFiles = remember { mutableStateListOf<Pair<Uri, String>>() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: it.path?.substringAfterLast('/') ?: "Unknown File"
            
            if (uploadedFiles.size < 5) {
                uploadedFiles.add(it to fileName)
            }
        }
    }

    val regions = listOf(
        "Ahafo", "Ashanti", "Bono", "Bono East", "Central", "Eastern", 
        "Greater Accra", "North East", "Northern", "Oti", "Savannah", 
        "Upper East", "Upper West", "Volta", "Western", "Western North"
    )
    
    val mmdaData = remember {
        mapOf(
            "Ahafo" to listOf(
                "Asunafo North Municipal", "Asunafo South District", "Asutifi North District", 
                "Asutifi South District", "Tano North Municipal", "Tano South Municipal"
            ),
            "Ashanti" to listOf(
                "Kumasi Metropolitan Assembly", "Ahafo Ano North Municipal", "Asante Akim Central Municipal", 
                "Asante Akim South Municipal", "Asokore Mampong Municipal", "Asokwa Municipal", 
                "Atwima Nwabiagya Municipal", "Bekwai Municipal", "Ejisu Municipal", 
                "Ejura Sekyeredumasi Municipal", "Juaben Municipal", "Kwabre East Municipal", 
                "Kwadaso Municipal", "Mampong Municipal", "Obuasi Municipal", "Offinso Municipal", 
                "Oforikrom Municipal", "Old Tafo Municipal", "Suame Municipal", "Adansi Asokwa District", 
                "Adansi North District", "Adansi South District", "Afigya Kwabre North District", 
                "Afigya Kwabre South District", "Ahafo Ano South East District", "Ahafo Ano South West District", 
                "Akrofuom District", "Amansie Central District", "Amansie South District", "Amansie West District", 
                "Asante Akim North District", "Atwima Kwanwoma District", "Atwima Mponua District", 
                "Atwima Nwabiagya North District", "Bosome Freho District", "Bosomtwe District", 
                "Obuasi East District", "Offinso North District", "Sekyere Afram Plains District", 
                "Sekyere Central District", "Sekyere East District", "Sekyere Kumawu District", "Sekyere South District"
            ),
            "Bono" to listOf(
                "Berekum East Municipal", "Dormaa Central Municipal", "Jaman South Municipal", 
                "Sunyani Municipal", "Sunyani West Municipal", "Wenchi Municipal", "Banda District", 
                "Berekum West District", "Dormaa East District", "Dormaa West District", 
                "Jaman North District", "Tain District"
            ),
            "Bono East" to listOf(
                "Atebubu-Amantin Municipal", "Kintampo North Municipal", "Nkoranza South Municipal", 
                "Techiman Municipal", "Kintampo South District", "Nkoranza North District", 
                "Pru East District", "Pru West District", "Sene East District", "Sene West District", 
                "Techiman North District"
            ),
            "Central" to listOf(
                "Cape Coast Metropolitan", "Agona West Municipal", "Assin Central Municipal", 
                "Awutu Senya East Municipal", "Effutu Municipal", "Komenda-Edina-Eguafo-Abirem Municipal", 
                "Mfantsiman Municipal", "Upper Denkyira East Municipal", "Abura/Asebu/Kwamankese District", 
                "Agona East District", "Ajumako/Enyan/Essiam District", "Asikuma-Odoben-Brakwa District", 
                "Assin North District", "Assin South District", "Awutu Senya West District", "Ekumfi District", 
                "Gomoa Central District", "Gomoa East District", "Gomoa West District", "Twifo Ati-Morkwa District", 
                "Twifo Hemang Lower Denkyira District", "Upper Denkyira West District"
            ),
            "Eastern" to listOf(
                "New Juaben South metropolitan", "Abuakwa North Municipal", "Abuakwa South Municipal", 
                "Akwapim North Municipal", "Birim Central Municipal", "Kwaebibirem Municipal", 
                "Kwahu West Municipal", "Lower Manya Krobo Municipal", "New Juaben North Municipal", 
                "Nsawam Adoagyiri Municipal", "Suhum Municipal", "West Akim Municipal", "Yilo Krobo Municipal", 
                "Achiase District", "Akuapem South District", "Akyemansa District", "Asene-Manso-Akroso District", 
                "Asuogyaman District", "Atiwa East District", "Atiwa West District", "Ayensuano District", 
                "Birim North District", "Birim South District", "Denkyembuor District", "Fanteakwa North District", 
                "Fanteakwa South District", "Kwahu Afram Plains North District", "Kwahu Afram Plains South District", 
                "Kwahu East District", "Kwahu South District", "Okere District", "Upper Manya Krobo District", 
                "Upper West Akim District"
            ),
            "Greater Accra" to listOf(
                "Accra Metropolitan", "Tema Metropolitan", "Ablekuma Central Municipal", "Ablekuma North Municipal", 
                "Ablekuma West Municipal", "Adenta Municipal", "Ashaiman Municipal", "Ayawaso Central Municipal", 
                "Ayawaso East Municipal", "Ayawaso North Municipal", "Ayawaso West Municipal", "Ga Central Municipal", 
                "Ga East Municipal", "Ga North Municipal", "Ga South Municipal", "Ga West Municipal", 
                "Korle Klottey Municipal", "Kpone Katamanso Municipal", "Krowor Municipal", 
                "La Dade-Kotopon Municipal", "La-Nkwantanang Madina Municipal", "Ledzokuku Municipal", 
                "Okaikwei North Municipal", "Tema West Municipal", "Weija-Gbawe Municipal", "Ada East District", 
                "Ada West District", "Ningo-Prampram District", "Shai-Osudoku District"
            ),
            "North East" to listOf(
                "East Mamprusi Municipal", "West Mamprusi Municipal", "Bunkpurugu-Nakpanduri District", 
                "Chereponi District", "Mamprugu-Moagduri District", "Yunyoo-Nasuan District"
            ),
            "Northern" to listOf(
                "Tamale Metropolitan", "Gushegu Municipal", "Nanumba North Municipal", "Sagnerigu Municipal", 
                "Savelugu Municipal", "Yendi Municipal", "Karaga District", "Kpandai District", 
                "Kumbungu District", "Mion District", "Nanton District", "Nanumba South District", 
                "Saboba District", "Tatale Sanguli District", "Tolon District", "Zabzugu District"
            ),
            "Oti" to listOf(
                "Krachi East Municipal", "Krachi West Municipal", "Nkwanta South Municipal", "Jasikan Municipal", 
                "Biakoye District", "Guan District", "Kadjebi District", "Krachi Nchumuru District", "Nkwanta North District"
            ),
            "Savannah" to listOf(
                "East Gonja Municipal", "West Gonja Municipal", "Bole District", "Central Gonja District", 
                "North Gonja District", "North East Gonja District", "Sawla-Tuna-Kalba District"
            ),
            "Upper East" to listOf(
                "Bawku Municipal", "Bolgatanga Municipal", "Builsa North Municipal", "Kassena Nankana East Municipal", 
                "Bawku West District", "Binduri District", "Bolgatanga East District", "Bongo District", 
                "Builsa South District", "Garu District", "Kassena Nankana West District", "Nabdam District", 
                "Pusiga District", "Talensi District", "Tempane District"
            ),
            "Upper West" to listOf(
                "Jirapa Municipal", "Lawra Municipal", "Nandom Municipal", "Sissala East Municipal", "Wa Municipal", 
                "Daffiama-Bussie-Issa District", "Lambussie District", "Nadowli-Kaleo District", "Sissala West District", 
                "Wa East District", "Wa West District"
            ),
            "Volta" to listOf(
                "Ho Metropolitan", "Akatsi South Municipal", "Hohoe Municipal", "Keta Municipal", 
                "Ketu North Municipal", "Ketu South Municipal", "Kpando Municipal", "South Tongu Municipal", 
                "Adaklu District", "Afadzato South District", "Agotime-Ziope District", "Akatsi North District", 
                "Anloga District", "Central Tongu District", "Ho West District", "North Dayi District", 
                "North Tongu District", "South Dayi District"
            ),
            "Western" to listOf(
                "Sekondi-Takoradi Metropolitan", "Ahanta West Municipal", "Amenfi West Municipal", 
                "Effia Kwesimintsim Municipal", "Jomoro Municipal", "Nzema East Municipal", 
                "Prestea Huni-Valley Municipal", "Tarkwa Nsuaem Municipal", "Wassa Amenfi East Municipal", 
                "Amenfi Central District", "Ellembelle District", "Mpohor District", "Shama District", "Wassa East District"
            ),
            "Western North" to listOf(
                "Aowin Municipal", "Bibiani-Anhwiaso-Bekwai Municipal", "Sefwi Wiawso Municipal", "Bia East District", 
                "Bia West District", "Bodi District", "Juaboso District", "Sefwi Akontombra District", "Suaman District"
            )
        )
    }
    
    val quarters = listOf("First", "Second", "Third", "Fourth")
    val institutionTypes = listOf("MDA", "MMDA")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CCC Review Report Submission", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Institution Type
            DropdownField(
                label = "Institution Type",
                options = institutionTypes,
                selectedOption = institutionType,
                onOptionSelected = { 
                    institutionType = it
                    institutionText = ""
                    selectedRegion = ""
                    selectedMmda = ""
                }
            )

            if (institutionType == "MDA") {
                // 2. Institution (MDA)
                OutlinedTextField(
                    value = institutionText,
                    onValueChange = { institutionText = it },
                    label = { Text("Institution", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )
            } else if (institutionType == "MMDA") {
                // 2. Region (MMDA)
                DropdownField(
                    label = "Region",
                    options = regions,
                    selectedOption = selectedRegion,
                    onOptionSelected = { 
                        selectedRegion = it
                        selectedMmda = ""
                    }
                )

                // 3. Institution (MMDA Dropdown)
                if (selectedRegion.isNotEmpty()) {
                    DropdownField(
                        label = "Institution (MMDA)",
                        options = mmdaData[selectedRegion] ?: emptyList(),
                        selectedOption = selectedMmda,
                        onOptionSelected = { selectedMmda = it }
                    )
                }
            }

            if (institutionType.isNotEmpty()) {
                // Quarter
                DropdownField(
                    label = "Quarter",
                    options = quarters,
                    selectedOption = quarter,
                    onOptionSelected = { quarter = it }
                )

                // Sender's Name
                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender's Name", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )

                // Sender's Email
                OutlinedTextField(
                    value = senderEmail,
                    onValueChange = { senderEmail = it },
                    label = { Text("Sender's Email", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )

                // Upload Files
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Upload Files (Max 5)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (uploadedFiles.size < 5) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attach File")
                        }
                    }

                    uploadedFiles.forEachIndexed { index, pair ->
                        ListItem(
                            headlineContent = { 
                                Text(
                                    pair.second,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                ) 
                            },
                            trailingContent = {
                                IconButton(onClick = { uploadedFiles.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = { /* Handle Submit */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (institutionType == "MDA" && institutionText.isNotEmpty() || (institutionType == "MMDA" && selectedMmda.isNotEmpty())) && 
                             quarter.isNotEmpty() && senderName.isNotEmpty() && senderEmail.isNotEmpty()
                ) {
                    Text("Submit Report")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CCCReviewReportSubmissionScreenPreview() {
    IAAAppTheme {
        CCCReviewReportSubmissionScreen(onBack = {})
    }
}
