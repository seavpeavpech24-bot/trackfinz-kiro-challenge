package com.trackfinz.app.ui.screens.budget

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackfinz.app.ui.theme.*
import com.trackfinz.app.utils.AIBudgetRecommender
import com.trackfinz.app.utils.BudgetPriority
import com.trackfinz.app.utils.BudgetRecommendation
import com.trackfinz.app.utils.formatCurrency
import com.trackfinz.app.viewmodel.BudgetViewModel
import com.trackfinz.app.viewmodel.SettingsViewModel
import com.trackfinz.app.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBudgetRecommendationsScreen(
    onBack: () -> Unit,
    onApplyBudgets: () -> Unit,
    txVm: TransactionViewModel = hiltViewModel(),
    budgetVm: BudgetViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val transactions by txVm.allTransactions.collectAsState()
    val currency by settingsVm.currency.collectAsState()
    var recommendations by remember { mutableStateOf<List<BudgetRecommendation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedRecommendations by remember { mutableStateOf<Set<BudgetRecommendation>>(emptySet()) }
    var isApplying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val s = com.trackfinz.app.i18n.LocalStrings.current

    // Generate recommendations on first load
    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        try {
            // Detect country from device locale
            val countryCode = java.util.Locale.getDefault().country
            recommendations = AIBudgetRecommender.generateBudgetRecommendations(transactions, countryCode)
            // Pre-select essential budgets
            selectedRecommendations = recommendations.filter { 
                it.priority == BudgetPriority.ESSENTIAL 
            }.toSet()
        } catch (e: Exception) {
            error = s(com.trackfinz.app.i18n.Str.FAILED_GENERATE_RECOMMENDATIONS)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(com.trackfinz.app.i18n.Str.AI_BUDGET_RECOMMENDATIONS)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s(com.trackfinz.app.i18n.Str.BACK))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val countryCode = java.util.Locale.getDefault().country
                                    recommendations = AIBudgetRecommender.generateBudgetRecommendations(transactions, countryCode)
                                } catch (e: Exception) {
                                    error = s(com.trackfinz.app.i18n.Str.FAILED_GENERATE_RECOMMENDATIONS)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedRecommendations.isNotEmpty() && !isLoading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${selectedRecommendations.size} ${s(com.trackfinz.app.i18n.Str.BUDGETS_SELECTED)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    "${s(com.trackfinz.app.i18n.Str.TOTAL)}: ${formatCurrency(selectedRecommendations.sumOf { it.recommendedAmount }, currency)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    if (!isApplying) {
                                        isApplying = true
                                        val toApply = selectedRecommendations.map { it.category to it.recommendedAmount }
                                        budgetVm.applyRecommendedBudgets(toApply)
                                        onApplyBudgets()
                                    }
                                },
                                enabled = !isApplying,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Teal500
                                )
                            ) {
                                if (isApplying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(s(com.trackfinz.app.i18n.Str.APPLY_BUDGETS))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF6A1B9A), Color(0xFF8E24AA))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    s(com.trackfinz.app.i18n.Str.SMART_BUDGET_PLANNING),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                s(com.trackfinz.app.i18n.Str.AI_BUDGET_SUBTITLE),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Loading state
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF8E24AA))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                s(com.trackfinz.app.i18n.Str.ANALYZING_FINANCES),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Error state
            error?.let { errorMsg ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                errorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Priority sections
            if (!isLoading && recommendations.isNotEmpty()) {
                // Essential budgets
                val essentials = recommendations.filter { it.priority == BudgetPriority.ESSENTIAL }
                if (essentials.isNotEmpty()) {
                    item {
                        PrioritySectionHeader("Essential", "Must-have expenses", Color(0xFFE53935))
                    }
                    items(essentials) { rec ->
                        BudgetRecommendationCard(
                            recommendation = rec,
                            currency = currency,
                            isSelected = rec in selectedRecommendations,
                            onToggle = {
                                selectedRecommendations = if (rec in selectedRecommendations) {
                                    selectedRecommendations - rec
                                } else {
                                    selectedRecommendations + rec
                                }
                            }
                        )
                    }
                }

                // Important budgets
                val important = recommendations.filter { it.priority == BudgetPriority.IMPORTANT }
                if (important.isNotEmpty()) {
                    item {
                        PrioritySectionHeader("Important", "Regular expenses", Color(0xFFFB8C00))
                    }
                    items(important) { rec ->
                        BudgetRecommendationCard(
                            recommendation = rec,
                            currency = currency,
                            isSelected = rec in selectedRecommendations,
                            onToggle = {
                                selectedRecommendations = if (rec in selectedRecommendations) {
                                    selectedRecommendations - rec
                                } else {
                                    selectedRecommendations + rec
                                }
                            }
                        )
                    }
                }

                // Flexible budgets
                val flexible = recommendations.filter { it.priority == BudgetPriority.FLEXIBLE }
                if (flexible.isNotEmpty()) {
                    item {
                        PrioritySectionHeader("Flexible", "Optional spending", Color(0xFF43A047))
                    }
                    items(flexible) { rec ->
                        BudgetRecommendationCard(
                            recommendation = rec,
                            currency = currency,
                            isSelected = rec in selectedRecommendations,
                            onToggle = {
                                selectedRecommendations = if (rec in selectedRecommendations) {
                                    selectedRecommendations - rec
                                } else {
                                    selectedRecommendations + rec
                                }
                            }
                        )
                    }
                }
            }

            // Empty state
            if (!isLoading && recommendations.isEmpty() && error == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No recommendations yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrioritySectionHeader(title: String, subtitle: String, color: Color) {
    val s = com.trackfinz.app.i18n.LocalStrings.current
    val translatedTitle = when(title) {
        "Essential" -> s(com.trackfinz.app.i18n.Str.ESSENTIAL)
        "Important" -> s(com.trackfinz.app.i18n.Str.IMPORTANT)
        "Flexible" -> s(com.trackfinz.app.i18n.Str.FLEXIBLE)
        else -> title
    }
    val translatedSubtitle = when(subtitle) {
        "Must-have expenses" -> s(com.trackfinz.app.i18n.Str.MUST_HAVE_EXPENSES)
        "Regular expenses" -> s(com.trackfinz.app.i18n.Str.REGULAR_EXPENSES)
        "Optional spending" -> s(com.trackfinz.app.i18n.Str.OPTIONAL_SPENDING)
        else -> subtitle
    }
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp, 24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    translatedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    translatedSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun BudgetRecommendationCard(
    recommendation: BudgetRecommendation,
    currency: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val lang = com.trackfinz.app.i18n.LocalLanguage.current
    val s = com.trackfinz.app.i18n.LocalStrings.current
    val borderColor = if (isSelected) Teal500 else Color.Transparent
    val backgroundColor = if (isSelected) 
        Teal500.copy(alpha = 0.08f) 
    else 
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon
                Text(
                    recommendation.category.emoji,
                    fontSize = 32.sp
                )
                Spacer(Modifier.width(16.dp))
                
                // Details
                Column {
                    Text(
                        recommendation.category.getLabel(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        recommendation.reasoning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // Current vs Recommended
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                s(com.trackfinz.app.i18n.Str.CURRENT),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                formatCurrency(recommendation.currentSpending, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Column {
                            Text(
                                s(com.trackfinz.app.i18n.Str.RECOMMENDED),
                                style = MaterialTheme.typography.labelSmall,
                                color = Teal500
                            )
                            Text(
                                formatCurrency(recommendation.recommendedAmount, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Teal500
                            )
                        }
                    }
                }
            }
            
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Teal500
                )
            )
        }
    }
}
