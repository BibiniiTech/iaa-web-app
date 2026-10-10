package com.bibiniitech.iaaapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.bibiniitech.iaaapp.FirebaseHelper
import com.bibiniitech.iaaapp.R
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: (isProfileComplete: Boolean, email: String?, name: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(value = false) }
    var isLoading by remember { mutableStateOf(value = false) }
    var isResettingPassword by remember { mutableStateOf(value = false) }
    var resetCooldownSeconds by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showMockGoogleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(resetCooldownSeconds) {
        if (resetCooldownSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            resetCooldownSeconds--
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sign In", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            Image(
                painter = painterResource(id = R.drawable.new_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Access Your Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "This app is designed for use by Internal Auditors of MDAs, SOEs, RCCs & MMDAs in Ghana under the supervision of the Internal Audit Agency ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
            )

            // Email Input
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

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val icon = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(icon, contentDescription = "Toggle password visibility")
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    enabled = !isResettingPassword && resetCooldownSeconds == 0,
                    onClick = {
                        if (email.isBlank()) {
                            errorMessage = "Please enter your email to reset your password."
                        } else {
                            scope.launch {
                                isResettingPassword = true
                                val success = FirebaseHelper.sendPasswordResetEmail(email.trim())
                                isResettingPassword = false
                                if (success) {
                                    resetCooldownSeconds = 60
                                    Toast.makeText(context, "Reset link sent to your email.", Toast.LENGTH_LONG).show()
                                } else {
                                    errorMessage = "Failed to send reset link. Verify your email."
                                }
                            }
                        }
                    }
                ) {
                    val resetLabel = when {
                        isResettingPassword -> "Sending..."
                        resetCooldownSeconds > 0 -> "Forgot Password? (${resetCooldownSeconds}s)"
                        else -> "Forgot Password?"
                    }
                    Text(resetLabel, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign In Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter both email and password."
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val success = FirebaseHelper.signIn(email.trim(), password.trim())
                        if (success) {
                            val user = FirebaseHelper.getCurrentUser()
                            if (user != null) {
                                val complete = FirebaseHelper.isProfileComplete(user.uid)
                                isLoading = false
                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(complete, user.email, user.firstName)
                            } else {
                                isLoading = false
                                onLoginSuccess(true, email.trim(), "User")
                            }
                        } else {
                            isLoading = false
                            errorMessage = "Authentication failed. Please verify credentials."
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
                    Text("Sign In")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (FirebaseHelper.isMockEnabled()) {
                        showMockGoogleDialog = true
                    } else {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(context.getString(R.string.default_web_client_id))
                                    .setAutoSelectEnabled(false)
                                    .build()
                                    
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                                    
                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = context
                                )
                                
                                val credential = result.credential
                                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val idToken = googleIdTokenCredential.idToken
                                    val success = FirebaseHelper.signInWithGoogleCredential(idToken)
                                    if (success) {
                                        val user = FirebaseHelper.getCurrentUser()
                                        if (user != null) {
                                            val complete = FirebaseHelper.isProfileComplete(user.uid)
                                            isLoading = false
                                            Toast.makeText(context, "Signed in with Google successfully!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(complete, user.email, user.firstName)
                                        } else {
                                            isLoading = false
                                            errorMessage = "Google sign-in succeeded but failed to retrieve user."
                                        }
                                    } else {
                                        isLoading = false
                                        errorMessage = "Firebase authentication with Google failed."
                                    }
                                } else {
                                    isLoading = false
                                    errorMessage = "Unexpected credential type."
                                }
                            } catch (e: GetCredentialException) {
                                isLoading = false
                                val msg = e.localizedMessage ?: e.message ?: ""
                                android.util.Log.e("LoginScreen", "Google Sign-In Error: ${e.type}", e)
                                errorMessage = if (msg.contains("No credentials available", ignoreCase = true)) {
                                    "Google Sign-In failed: No credentials available.\n\nPrerequisites to fix:\n1. A Google account must be signed in on this device.\n2. The Web Client ID must be configured in strings.xml.\n3. The app's SHA-1 fingerprint must be registered in the Firebase console."
                                } else {
                                    "Google Sign-In failed: $msg"
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "An unexpected error occurred: ${e.localizedMessage ?: e.message}"
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider for visual separation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Call to Action card for new users
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Click below to create an account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onNavigateToSignUp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Sign Up Here",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showMockGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showMockGoogleDialog = false },
            title = { Text("Select Mock Google Account", fontWeight = FontWeight.Bold) },
            text = { Text("Choose a mock user scenario to test Google Sign-in and Profile Completion.") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            showMockGoogleDialog = false
                            isLoading = true
                            scope.launch {
                                val email = "new_google_user@gmail.com"
                                val name = "Kofi Mensah"
                                val profile = FirebaseHelper.signInWithGoogleMock(email, name)
                                val complete = FirebaseHelper.isProfileComplete(profile.uid)
                                isLoading = false
                                Toast.makeText(context, "Google Mock: Signed in as $name", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(complete, email, name)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("New User (Incomplete Profile)")
                    }
                    Button(
                        onClick = {
                            showMockGoogleDialog = false
                            isLoading = true
                            scope.launch {
                                val email = "existing_google_auditor@iaa.gov.gh"
                                val name = "Auditor Ama"
                                val profile = FirebaseHelper.signInWithGoogleMock(email, name)
                                val completedProfile = profile.copy(
                                    firstName = "Ama",
                                    surname = "Osei",
                                    phone = "0244112233",
                                    institution = "Ministry of Health",
                                    isProfileComplete = true
                                )
                                FirebaseHelper.updateUserProfile(completedProfile)
                                val complete = FirebaseHelper.isProfileComplete(profile.uid)
                                isLoading = false
                                Toast.makeText(context, "Google Mock: Signed in as $name", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(complete, email, name)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Existing User (Completed Profile)")
                    }
                    OutlinedButton(
                        onClick = { showMockGoogleDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@ThemePreviews
@Composable
fun LoginScreenPreview() {
    IAAAppTheme {
        LoginScreen({ }, { _, _, _ -> })
    }
}
