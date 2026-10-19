package com.bibiniitech.iaaapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bibiniitech.iaaapp.AuthResult
import com.bibiniitech.iaaapp.FirebaseHelper
import com.bibiniitech.iaaapp.data.InstitutionData
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    var institutionType by remember { mutableStateOf("") }
    var institutionText by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("") }
    var selectedMmda by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var isPasswordVisible by remember { mutableStateOf(value = false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sign Up", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Register Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create an account to track your audit submissions and view resources.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
            )

            val regions = InstitutionData.REGIONS
            val mmdaData = InstitutionData.MMDA_DATA
            val mdaData = InstitutionData.MDA_LIST
            val soeData = InstitutionData.SOE_LIST
            val institutionTypes = InstitutionData.INSTITUTION_TYPES

            // First Name
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it; errorMessage = null },
                label = { Text("First Name", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Surname
            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it; errorMessage = null },
                label = { Text("Surname", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Email Address", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = { 
                    if (it.length <= 10) {
                        phone = it.filter { char -> char.isDigit() }
                        errorMessage = null 
                    }
                },
                label = { Text("Contact Number (e.g. 024XXXXXXX)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            if (institutionType == "MDA" || institutionType == "SOE") {
                Spacer(modifier = Modifier.height(12.dp))
                DropdownField(
                    label = if (institutionType == "MDA") "Institution (MDA)" else "Institution (SOE)",
                    options = if (institutionType == "MDA") mdaData else soeData,
                    selectedOption = institutionText,
                    onOptionSelected = { institutionText = it; errorMessage = null }
                )
            } else if (institutionType == "RCC" || institutionType == "MMDA") {
                Spacer(modifier = Modifier.height(12.dp))
                DropdownField(
                    label = "Region",
                    options = regions,
                    selectedOption = selectedRegion
                ) { 
                    selectedRegion = it
                    selectedMmda = ""
                }

                if (selectedRegion.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val options = if (institutionType == "RCC") {
                        listOf(InstitutionData.RCC_DATA[selectedRegion] ?: "")
                    } else {
                        mmdaData[selectedRegion] ?: emptyList()
                    }
                    DropdownField(
                        label = if (institutionType == "RCC") "Institution (RCC)" else "Institution (MMDA)",
                        options = options,
                        selectedOption = selectedMmda
                    ) { selectedMmda = it }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val icon = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(icon, contentDescription = null)
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("Confirm Password", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val icon = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Icon(icon, contentDescription = null)
                    }
                },
                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Button
            Button(
                onClick = {
                    val selectedInstitution = if (institutionType == "MDA" || institutionType == "SOE") institutionText else selectedMmda
                    if (firstName.isBlank() || surname.isBlank() || email.isBlank() || phone.isBlank() || institutionType.isEmpty() || selectedInstitution.isEmpty() || password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "Please fill in all fields. All fields are mandatory."
                        return@Button
                    }
                    
                    // Validate Phone Number (10 digits starting with 0)
                    val ghPhoneRegex = Regex("^0[0-9]{9}$")
                    if (!ghPhoneRegex.matches(phone)) {
                        errorMessage = "Invalid contact number. Must be 10 digits starting with '0'."
                        return@Button
                    }

                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters long."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val result = FirebaseHelper.signUp(
                            email = email.trim().lowercase(),
                            password = password.trim(),
                            firstName = firstName.trim(),
                            surname = surname.trim(),
                            institution = selectedInstitution.trim(),
                            phone = phone.trim()
                        )
                        isLoading = false
                        when (result) {
                            is AuthResult.Success -> {
                                Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                onSignUpSuccess()
                            }
                            is AuthResult.Error -> {
                                errorMessage = result.message
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Register")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link to Login
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Already have an account? Sign In",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun SignUpScreenPreview() {
    IAAAppTheme {
        SignUpScreen(onNavigateToLogin = {}, onSignUpSuccess = {})
    }
}
