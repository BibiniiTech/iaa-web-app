package com.bibiniitech.iaaapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bibiniitech.iaaapp.*
import com.bibiniitech.iaaapp.ui.theme.IAAAppTheme
import com.bibiniitech.iaaapp.ui.theme.ThemePreviews
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var votingConfig by remember { mutableStateOf<VotingConfig?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var hasVoted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var votingResults by remember { mutableStateOf(VotingResults()) }

    var currentStep by remember { mutableIntStateOf(0) }
    val userSelections = remember { mutableStateMapOf<String, List<String>>() } // categoryId -> list of optionIds

    LaunchedEffect(Unit) {
        val config = FirebaseHelper.getVotingConfig()
        votingConfig = config
        
        val baseUser = FirebaseHelper.getCurrentUser()
        val profile = baseUser?.let { FirebaseHelper.fetchUserProfile(it.uid) ?: it }
        userProfile = profile
        if (profile != null) {
            hasVoted = FirebaseHelper.hasUserVoted(profile.uid)
        }
        votingResults = FirebaseHelper.getVotingResults()
        
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Voting Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (votingConfig?.isVisible != true) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Voting is currently closed. Please check back later.", textAlign = TextAlign.Center)
            }
        } else if (hasVoted) {
            VotingSuccessView(
                config = votingConfig,
                votingResults = votingResults,
                onBack = onBack,
                modifier = Modifier.padding(padding)
            )
        } else {
            val config = votingConfig!!
            val userEmail = (userProfile?.email ?: FirebaseHelper.getCurrentUser()?.email ?: "").trim().lowercase(java.util.Locale.ROOT)
            val eligibleCategories = remember(config, userEmail) {
                config.categories.filter { cat ->
                    cat.allowedVoterEmails.isEmpty() || cat.allowedVoterEmails.any { it.trim().equals(userEmail, ignoreCase = true) }
                }
            }

            if (config.categories.isNotEmpty() && eligibleCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HowToVote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Voting Restricted",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (userEmail.isNotEmpty())
                                    "Your account ($userEmail) is not authorized to vote in any active poll sections. If you believe this is in error, please contact your administrator."
                                else
                                    "Your account is not authorized to vote in any active poll sections. If you believe this is in error, please contact your administrator.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = onBack) {
                                Text("Back to Dashboard")
                            }
                        }
                    }
                }
            } else {
                val categories = eligibleCategories
                val totalSteps = categories.size + 1 // +1 for summary card

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = config.header,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    if (eligibleCategories.size < config.categories.size) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Showing ${eligibleCategories.size} of ${config.categories.size} section(s) available for your account.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { (currentStep + 1).toFloat() / totalSteps },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                                } else {
                                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                                }.using(SizeTransform(clip = false))
                            }, label = ""
                        ) { step ->
                            if (step < categories.size) {
                                val category = categories[step]
                                CategoryVotingCard(
                                    category = category,
                                    votingResults = votingResults,
                                    selectedOptions = userSelections[category.id] ?: emptyList(),
                                    onSelectionChange = { userSelections[category.id] = it }
                                )
                            } else {
                                VotingSummaryCard(
                                    categories = categories,
                                    selections = userSelections,
                                    isSubmitting = false,
                                    onSubmit = {
                                        scope.launch {
                                            val profile = userProfile
                                            if (profile != null) {
                                                val vote = VoteRecord(
                                                    userId = profile.uid,
                                                    firstName = profile.firstName,
                                                    surname = profile.surname,
                                                    email = profile.email,
                                                    phone = profile.phone,
                                                    institutionType = profile.institution, // Fallback if type not explicit
                                                    institution = profile.institution,
                                                    region = "", // Add if available
                                                    selections = userSelections.toMap()
                                                )
                                                val success = FirebaseHelper.submitVote(vote)
                                                if (success) {
                                                    votingResults = FirebaseHelper.getVotingResults(forceRefresh = true)
                                                    hasVoted = true
                                                } else {
                                                    Toast.makeText(context, "Failed to submit vote.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { if (currentStep > 0) currentStep-- },
                            enabled = currentStep > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Previous")
                        }

                        if (currentStep < totalSteps - 1) {
                            Button(
                                onClick = { currentStep++ },
                                enabled = (userSelections[categories[currentStep].id]?.isNotEmpty() == true)
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsSummaryCard(
    category: VotingCategory,
    results: VotingResults,
    modifier: Modifier = Modifier
) {
    val totalCategoryVotes = results.categoryTotals[category.id] ?: 0
    val optionCounts = results.optionCounts

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Real-Time Results", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("$totalCategoryVotes Total Votes cast for this category", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            category.options.forEach { option ->
                val count = optionCounts[option.id] ?: 0
                val percent = if (totalCategoryVotes > 0) (count.toFloat() / totalCategoryVotes * 100).toInt() else 0
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(option.text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("$count ($percent%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { if (totalCategoryVotes > 0) count.toFloat() / totalCategoryVotes else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun CategoryVotingCard(
    category: VotingCategory,
    votingResults: VotingResults,
    selectedOptions: List<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Stats Counter Card
        ResultsSummaryCard(category = category, results = votingResults)

        Text(
            text = category.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = if (category.allowMultiple) "You can select multiple options" else "Select only one option",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.heightIn(max = 1000.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false // Container is already scrollable
        ) {
            items(category.options) { option ->
                val isSelected = selectedOptions.contains(option.id)
                VotingOptionItem(
                    option = option,
                    isSelected = isSelected,
                    onClick = {
                        if (category.allowMultiple) {
                            if (isSelected) {
                                onSelectionChange(selectedOptions - option.id)
                            } else {
                                onSelectionChange(selectedOptions + option.id)
                            }
                        } else {
                            onSelectionChange(listOf(option.id))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun VotingOptionItem(
    option: VotingOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = option.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun VotingSummaryCard(
    categories: List<VotingCategory>,
    selections: Map<String, List<String>>,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Review Your Choices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        categories.forEach { category ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(category.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val selectedIds = selections[category.id] ?: emptyList()
                    val selectedTexts = category.options.filter { it.id in selectedIds }.map { it.text }
                    
                    if (selectedTexts.isEmpty()) {
                        Text("No selection", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(selectedTexts.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isSubmitting && categories.all { selections[it.id]?.isNotEmpty() == true },
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.HowToVote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm & Submit Vote")
            }
        }
        Text(
            "Note: Once submitted, you cannot change your vote.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun VotingSuccessView(
    config: VotingConfig?,
    votingResults: VotingResults,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF4CAF50)
            )
            Text(
                "Thank You for Voting!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Your selections have been recorded. Here are the latest live results:",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            config?.categories?.forEach { category ->
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    textAlign = TextAlign.Start
                )
                ResultsSummaryCard(category = category, results = votingResults)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Return to Dashboard", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@ThemePreviews
@Composable
fun VotingScreenPreview() {
    IAAAppTheme {
        VotingScreen(onBack = {})
    }
}
