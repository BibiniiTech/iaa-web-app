package com.bibiniitech.iaaapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.bibiniitech.iaaapp.FirebaseHelper
import com.bibiniitech.iaaapp.UserProfile
import com.bibiniitech.iaaapp.data.InstitutionData
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(value = true) }

    // Form fields
    var firstName by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    var institutionType by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }

    val regions = InstitutionData.REGIONS
    val mmdaData = InstitutionData.MMDA_DATA
    val mdaData = InstitutionData.MDA_LIST
    val soeData = InstitutionData.SOE_LIST
    val institutionTypes = InstitutionData.INSTITUTION_TYPES

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val user = FirebaseHelper.getCurrentUser()
        if (user != null) {
            val fullProfile = FirebaseHelper.fetchUserProfile(user.uid) ?: user
            userProfile = fullProfile
            firstName = fullProfile.firstName
            surname = fullProfile.surname
            phone = fullProfile.phone
            institution = fullProfile.institution

            // Attempt to infer institution type and region
            if (mdaData.contains(institution)) {
                institutionType = "MDA"
            } else if (soeData.contains(institution)) {
                institutionType = "SOE"
            } else {
                // Check RCCs
                val rccEntry = InstitutionData.RCC_DATA.entries.find { it.value == institution }
                if (rccEntry != null) {
                    institutionType = "RCC"
                    selectedRegion = rccEntry.key
                } else {
                    // Check MMDAs
                    val mmdaEntry = mmdaData.entries.find { it.value.contains(institution) }
                    if (mmdaEntry != null) {
                        institutionType = "MMDA"
                        selectedRegion = mmdaEntry.key
                    }
                }
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Profile Header
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = userProfile?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Edit Profile Section
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text("Surname") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                DropdownField(
                    label = "Institution Type",
                    options = institutionTypes,
                    selectedOption = institutionType,
                    onOptionSelected = {
                        institutionType = it
                        institution = ""
                        selectedRegion = ""
                    }
                )

                if (institutionType == "MDA" || institutionType == "SOE") {
                    DropdownField(
                        label = if (institutionType == "MDA") "Institution (MDA)" else "Institution (SOE)",
                        options = if (institutionType == "MDA") mdaData else soeData,
                        selectedOption = institution,
                        onOptionSelected = { institution = it }
                    )
                } else if (institutionType == "RCC" || institutionType == "MMDA") {
                    DropdownField(
                        label = "Region",
                        options = regions,
                        selectedOption = selectedRegion
                    ) {
                        selectedRegion = it
                        institution = ""
                    }

                    if (selectedRegion.isNotEmpty()) {
                        val options = if (institutionType == "RCC") {
                            listOf(InstitutionData.RCC_DATA[selectedRegion] ?: "")
                        } else {
                            mmdaData[selectedRegion] ?: emptyList()
                        }
                        DropdownField(
                            label = if (institutionType == "RCC") "Institution (RCC)" else "Institution (MMDA)",
                            options = options,
                            selectedOption = institution
                        ) { institution = it }
                    }
                }

                Button(
                    onClick = {
                        val profile = userProfile?.copy(
                            firstName = firstName.trim(),
                            surname = surname.trim(),
                            phone = phone.trim(),
                            institution = institution.trim()
                        ) ?: return@Button
                        
                        isSaving = true
                        scope.launch {
                            val success = FirebaseHelper.updateUserProfile(profile)
                            isSaving = false
                            if (success) {
                                userProfile = profile
                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mandatory Requirements Section
                ProfileExpandableItem(
                    title = "Privacy Policy",
                    icon = Icons.Default.PrivacyTip
                ) {
                    Text(
                        text = "We value your privacy. This app collects basic profile information (name, institution, contact) to facilitate audit report submissions to the Internal Audit Agency (IAA). Data is stored securely in Firebase and is only used for official IAA purposes. We do not share your information with third parties for marketing. \n sites.google.com/view/iaaappprivacypolicy",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                ProfileExpandableItem(
                    title = "Terms of Use",
                    icon = Icons.Default.Description
                ) {
                    Text(
                        text = "By using the IAA App, you agree to submit accurate and truthful information. Misuse of the reporting system may lead to administrative action. The resources provided are for guidance and should be used in conjunction with the relevant laws and regulations of Ghana.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                ProfileExpandableItem(
                    title = "Account Management",
                    icon = Icons.Default.ManageAccounts
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "To delete your account and all associated data, click the button below. This action is permanent and cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        
                        if (!showDeleteConfirm) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Request Account Deletion")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val success = FirebaseHelper.deleteUserAccount()
                                            if (success) {
                                                Toast.makeText(context, "Account deleted.", Toast.LENGTH_SHORT).show()
                                                // Sign out happens automatically in deleteUserAccount
                                            } else {
                                                Toast.makeText(context, "Failed to delete account. You may need to sign in again for security.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Confirm Delete", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showDeleteConfirm = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                ProfileExpandableItem(
                    title = "App Information",
                    icon = Icons.Default.Info
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("App Name: IAA App", style = MaterialTheme.typography.bodySmall)
                        Text("Version: ${com.bibiniitech.iaaapp.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                        Text("Developer: Bibinii Tech \n for Internal Audit Agency", style = MaterialTheme.typography.bodySmall)

                    }
                }

                Spacer(modifier = Modifier.height(130.dp))
            }
        }
    }
}

@Composable
fun ProfileExpandableItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@ThemePreviews
@Composable
fun ProfileScreenPreview() {
    IAAAppTheme {
        ProfileScreen { }
    }
}
