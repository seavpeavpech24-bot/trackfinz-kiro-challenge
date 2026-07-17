package com.trackfinz.app.ui.screens.report

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.i18n.LocalLanguage
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.ui.theme.*
import com.trackfinz.app.utils.*
import com.trackfinz.app.viewmodel.AIReportUiState
import com.trackfinz.app.viewmodel.AIReportViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIReportScreen(
    onBack: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    vm: AIReportViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val context = LocalContext.current

    // Month picker state
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Monthly Report",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s(Str.BACK))
                    }
                },
                actions = {
                    IconButton(onClick = { showMonthPicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Month")
                    }
                    if (uiState is AIReportUiState.Ready) {
                        IconButton(onClick = { vm.regenerate() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6A1B9A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AIReportUiState.Loading -> {
                    ReportLoadingState()
                }
                is AIReportUiState.Error -> {
                    ReportErrorState(
                        message = state.message,
                        onRetry = { vm.regenerate() }
                    )
                }
                is AIReportUiState.Ready -> {
                    ReportContent(
                        state               = state,
                        onApplyBudget       = { cat, amount -> vm.applyBudgetRecommendation(cat, amount) },
                        onNavigateToBudget  = onNavigateToBudget,
                        onNavigateToGoals   = onNavigateToGoals,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToTransactions = onNavigateToTransactions,
                        onShare             = {
                            val shareText = buildShareText(state)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Report"))
                        }
                    )
                    // Regenerating overlay
                    if (state.isRegenerating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF6A1B9A))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Regenerating…", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Month picker dialog
    if (showMonthPicker) {
        MonthPickerDialog(
            onDismiss = { showMonthPicker = false },
            onSelect   = { month, year ->
                showMonthPicker = false
                vm.selectMonth(month, year)
            }
        )
    }
}

// ── Loading state ─────────────────────────────────────────────────────────────

@Composable
private fun ReportLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF6A1B9A), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Analyzing your finances…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Error state ───────────────────────────────────────────────────────────────

@Composable
private fun ReportErrorState(message: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Failed to generate report",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Try Again")
                }
            }
        }
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun ReportContent(
    state: AIReportUiState.Ready,
    onApplyBudget: (com.trackfinz.app.data.model.TransactionCategory, Double) -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onShare: () -> Unit
) {
    val analysis = state.analysis
    val currency = state.currency
    val lang     = LocalLanguage.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Health Score Card
        item {
            HealthScoreCard(
                month       = state.month,
                year        = state.year,
                score       = analysis.healthScore,
                label       = analysis.healthLabel
            )
        }

        // 2. KPI Summary
        item {
            KpiSummaryCard(
                income       = analysis.income,
                expenses     = analysis.expenses,
                netBalance   = analysis.netBalance,
                priorIncome  = analysis.priorIncome,
                priorExpenses = analysis.priorExpenses,
                currency     = currency
            )
        }

        // 3. Savings Rate
        item {
            SavingsRateCard(
                savingsRate      = analysis.savingsRate,
                priorSavingsRate = analysis.priorSavingsRate,
                healthLabel      = analysis.healthLabel
            )
        }

        // 4. Top Categories
        if (analysis.topCategories.isNotEmpty()) {
            item {
                SectionHeader(title = "Top Spending Categories", icon = Icons.Default.PieChart)
            }
            items(analysis.topCategories) { cat ->
                CategoryRow(cat = cat, currency = currency, lang = lang)
            }
        }

        // 5. Budget Performance
        if (analysis.budgetedCategories.isNotEmpty()) {
            item {
                BudgetPerformanceCard(budgetedCategories = analysis.budgetedCategories, currency = currency, lang = lang)
            }
        }

        // 6. Goal Progress
        if (analysis.goals.isNotEmpty()) {
            item {
                GoalProgressCard(goals = analysis.goals, currency = currency)
            }
        }

        // 7. AI Narrative
        item {
            NarrativeCard(
                narrative   = analysis.narrative,
                isFallback  = analysis.isNarrativeFallback,
                onShare     = onShare
            )
        }

        // 8. Recommendations
        if (analysis.recommendations.isNotEmpty()) {
            item {
                SectionHeader(title = "AI Recommendations", icon = Icons.Default.AutoAwesome)
            }
            items(analysis.recommendations, key = { it.title }) { rec ->
                RecommendationCard(
                    rec                      = rec,
                    currency                 = currency,
                    onApplyBudget            = onApplyBudget,
                    onNavigateToBudget       = onNavigateToBudget,
                    onNavigateToGoals        = onNavigateToGoals,
                    onNavigateToAnalytics    = onNavigateToAnalytics,
                    onNavigateToTransactions = onNavigateToTransactions
                )
            }
        } else if (analysis.income == 0.0 && analysis.expenses == 0.0) {
            item { EmptyDataCard() }
        }

        // 9. Footer
        item {
            FooterNote(month = state.month, year = state.year)
        }
    }
}

// ── Health Score Card ─────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(month: Int, year: Int, score: Int, label: HealthLabel) {
    val monthNames = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )
    val monthName = monthNames.getOrElse(month - 1) { "Month $month" }

    val scoreColor = when (label) {
        HealthLabel.EXCELLENT       -> Emerald500
        HealthLabel.GOOD            -> Teal500
        HealthLabel.FAIR            -> WarningAmber
        HealthLabel.NEEDS_IMPROVEMENT -> ExpenseRed
    }
    val labelText = when (label) {
        HealthLabel.EXCELLENT       -> "Excellent"
        HealthLabel.GOOD            -> "Good"
        HealthLabel.FAIR            -> "Fair"
        HealthLabel.NEEDS_IMPROVEMENT -> "Needs Work"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF6A1B9A), Color(0xFF283593))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "$monthName $year Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Your Monthly AI-Powered Analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Financial Health Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$score",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                "/100",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier
                                .width(160.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = scoreColor,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scoreColor.copy(alpha = 0.25f)
                    ) {
                        Text(
                            labelText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
            }
        }
    }
}

// ── KPI Summary Card ──────────────────────────────────────────────────────────

@Composable
private fun KpiSummaryCard(
    income: Double,
    expenses: Double,
    netBalance: Double,
    priorIncome: Double,
    priorExpenses: Double,
    currency: String
) {
    val incomeDelta  = if (priorIncome > 0)   ((income   - priorIncome)   / priorIncome   * 100).toInt() else 0
    val expenseDelta = if (priorExpenses > 0) ((expenses - priorExpenses) / priorExpenses * 100).toInt() else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Income vs Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = IncomeGreen.copy(alpha = 0.10f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp, null,
                                tint = IncomeGreen, modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatCurrency(income, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                        if (incomeDelta != 0) {
                            DeltaChip(delta = incomeDelta, positiveIsGood = true)
                        }
                    }
                }
                // Expense chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = ExpenseRed.copy(alpha = 0.10f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingDown, null,
                                tint = ExpenseRed, modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Expenses", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatCurrency(expenses, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                        if (expenseDelta != 0) {
                            DeltaChip(delta = expenseDelta, positiveIsGood = false)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Net Balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                Text(
                    formatCurrency(netBalance, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance >= 0) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

@Composable
private fun DeltaChip(delta: Int, positiveIsGood: Boolean) {
    val isPositive = delta > 0
    val isGood = if (positiveIsGood) isPositive else !isPositive
    val color  = if (isGood) IncomeGreen else ExpenseRed
    val prefix = if (isPositive) "▲ +" else "▼ "
    Text(
        "$prefix$delta%",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(top = 2.dp)
    )
}

// ── Savings Rate Card ─────────────────────────────────────────────────────────

@Composable
private fun SavingsRateCard(
    savingsRate: Double,
    priorSavingsRate: Double,
    healthLabel: HealthLabel
) {
    val rateColor = when {
        savingsRate >= 20.0 -> Emerald500
        savingsRate >= 10.0 -> Teal500
        savingsRate >= 0.0  -> WarningAmber
        else                -> ExpenseRed
    }
    val rateLabel = when {
        savingsRate >= 20.0 -> "Excellent"
        savingsRate >= 10.0 -> "Good"
        savingsRate >= 0.0  -> "Fair"
        else                -> "Negative"
    }
    val delta = savingsRate - priorSavingsRate
    val progress = (savingsRate / 20.0).coerceIn(0.0, 1.0).toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        tint = rateColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Savings Rate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rateColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        rateLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = rateColor
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${String.format("%.1f", savingsRate)}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = rateColor
                )
                if (delta != 0.0 && priorSavingsRate > 0.0) {
                    Spacer(Modifier.width(8.dp))
                    val sign   = if (delta > 0) "▲ +" else "▼ "
                    val dColor = if (delta > 0) IncomeGreen else ExpenseRed
                    Text(
                        "$sign${String.format("%.1f", delta)}pp vs last month",
                        style = MaterialTheme.typography.bodySmall,
                        color = dColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = rateColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Target: 20% savings rate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Category Row ──────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(
    cat: CategorySummary,
    currency: String,
    lang: com.trackfinz.app.i18n.AppLanguage
) {
    val hasBudget   = cat.budgetLimit != null
    val isOverBudget = (cat.budgetPercent ?: 0.0) > 100.0
    val progress    = ((cat.budgetPercent ?: cat.percentage) / 100.0).coerceIn(0.0, 1.0).toFloat()
    val barColor    = if (isOverBudget) ExpenseRed else Teal500

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(cat.category.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            cat.category.getLabel(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        // MoM delta
                        if (cat.priorAmount > 0.0 && cat.deltaPercent != 0.0) {
                            val sign  = if (cat.deltaPercent > 0) "▲ +" else "▼ "
                            val dColor = if (cat.deltaPercent > 0) ExpenseRed else IncomeGreen
                            Text(
                                "$sign${String.format("%.0f", cat.deltaPercent)}% vs last month",
                                style = MaterialTheme.typography.labelSmall,
                                color = dColor
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatCurrency(cat.amount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${String.format("%.0f", cat.percentage)}% of total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Budget status
            if (hasBudget) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusIcon, statusText, statusColor) = when {
                        isOverBudget -> Triple(Icons.Default.Warning, "Over budget by ${formatCurrency(cat.amount - (cat.budgetLimit ?: 0.0), currency)}", ExpenseRed)
                        else         -> Triple(Icons.Default.CheckCircle, "Under budget (${formatCurrency(cat.budgetLimit ?: 0.0, currency)} limit)", IncomeGreen)
                    }
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }
        }
    }
}

// ── Budget Performance Card ───────────────────────────────────────────────────

@Composable
private fun BudgetPerformanceCard(
    budgetedCategories: List<CategorySummary>,
    currency: String,
    lang: com.trackfinz.app.i18n.AppLanguage
) {
    val underCount = budgetedCategories.count { (it.budgetPercent ?: 0.0) <= 100.0 }
    val overCount  = budgetedCategories.size - underCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountBalance,
                    null,
                    tint = Color(0xFF6A1B9A),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Budget Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = IncomeGreen.copy(alpha = 0.10f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$underCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                        Text(
                            "Under Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (overCount > 0) ExpenseRed.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$overCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (overCount > 0) ExpenseRed else MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                        Text(
                            "Over Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (overCount > 0) ExpenseRed else MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ── Goal Progress Card ────────────────────────────────────────────────────────

@Composable
private fun GoalProgressCard(
    goals: List<com.trackfinz.app.data.model.GoalEntity>,
    currency: String
) {
    val now = System.currentTimeMillis()
    val sixtyDaysMs = 60L * 24 * 60 * 60 * 1000

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    tint = Color(0xFF6A1B9A),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Goal Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))

            goals.take(4).forEach { goal ->
                val progress  = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
                val isAtRisk  = goal.deadline != null &&
                    goal.deadline > now &&
                    goal.deadline - now <= sixtyDaysMs &&
                    progress < 0.5
                val barColor  = when {
                    goal.isCompleted -> Emerald500
                    isAtRisk         -> ExpenseRed
                    else             -> Teal500
                }

                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(goal.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                goal.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (isAtRisk) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Warning,
                                    null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${formatCurrency(goal.savedAmount, currency)} of ${formatCurrency(goal.targetAmount, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ── AI Narrative Card ─────────────────────────────────────────────────────────

@Composable
private fun NarrativeCard(
    narrative: String,
    isFallback: Boolean,
    onShare: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SmartToy,
                        null,
                        tint = Color(0xFF6A1B9A),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI Financial Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share Report",
                        tint = Color(0xFF6A1B9A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text    = if (expanded || narrative.length <= 200) narrative else narrative.take(200) + "…",
                style   = MaterialTheme.typography.bodyMedium,
                color   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            if (narrative.length > 200) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (expanded) "Show less ▲" else "Show more ▼",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6A1B9A)
                    )
                }
            }

            if (isFallback) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WifiOff,
                        null,
                        tint = WarningAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Generated offline · Connect for personalized AI narrative",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Powered by Google Gemini",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ── Recommendation Card ───────────────────────────────────────────────────────

@Composable
private fun RecommendationCard(
    rec: MonthlyRecommendation,
    currency: String,
    onApplyBudget: (com.trackfinz.app.data.model.TransactionCategory, Double) -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = visible, exit = fadeOut()) {
        val (borderColor, iconVector, bgColor) = when (rec.type) {
            RecommendationType.POSITIVE_HABIT -> Triple(Emerald500, Icons.Default.Star,       Emerald500.copy(alpha = 0.08f))
            RecommendationType.OVER_BUDGET    -> Triple(ExpenseRed,  Icons.Default.Warning,    ExpenseRed.copy(alpha = 0.08f))
            RecommendationType.TREND_ALERT    -> Triple(WarningAmber, Icons.Default.TrendingUp, WarningAmber.copy(alpha = 0.08f))
            RecommendationType.MISSING_BUDGET -> Triple(Teal500,     Icons.Default.AddCircle,  Teal500.copy(alpha = 0.08f))
            RecommendationType.LOW_SAVINGS    -> Triple(WarningAmber, Icons.Default.Savings,   WarningAmber.copy(alpha = 0.08f))
            RecommendationType.GOAL_AT_RISK   -> Triple(ExpenseRed,  Icons.Default.Flag,       ExpenseRed.copy(alpha = 0.08f))
            RecommendationType.ENGAGEMENT     -> Triple(Teal500,     Icons.Default.EditNote,   Teal500.copy(alpha = 0.08f))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconVector, null, tint = borderColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            rec.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = borderColor
                        )
                    }
                    // Dismiss button
                    IconButton(
                        onClick = { visible = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    rec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                rec.actionLabel?.let { label ->
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            when (rec.type) {
                                RecommendationType.MISSING_BUDGET -> {
                                    val cat    = rec.actionCategory ?: return@OutlinedButton
                                    val amount = rec.suggestedBudgetAmount ?: return@OutlinedButton
                                    onApplyBudget(cat, amount)
                                }
                                RecommendationType.OVER_BUDGET,
                                RecommendationType.TREND_ALERT   -> onNavigateToAnalytics()
                                RecommendationType.LOW_SAVINGS,
                                RecommendationType.GOAL_AT_RISK  -> onNavigateToGoals()
                                RecommendationType.ENGAGEMENT    -> onNavigateToTransactions()
                                else                             -> onNavigateToBudget()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = borderColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Empty / Footer ────────────────────────────────────────────────────────────

@Composable
private fun EmptyDataCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BarChart,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No transactions this month",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Add transactions to see your monthly analysis",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun FooterNote(month: Int, year: Int) {
    val monthNames = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )
    val name = monthNames.getOrElse(month - 1) { "Month $month" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info,
            null,
            tint = MaterialTheme.colorScheme.onSurface.copy(0.35f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Analysis covers $name $year · Powered by AI",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    }
}

// ── Month Picker Dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (month: Int, year: Int) -> Unit
) {
    val now = remember { Calendar.getInstance() }
    var pickedYear  by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var pickedMonth by remember { mutableStateOf(now.get(Calendar.MONTH) + 1) } // 1-based

    val monthShorts = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { pickedYear-- }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
                }
                Text(
                    "$pickedYear",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { if (pickedYear < now.get(Calendar.YEAR)) pickedYear++ },
                    modifier = Modifier.size(32.dp),
                    enabled = pickedYear < now.get(Calendar.YEAR)
                ) {
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            // 3 × 4 month grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..2) {
                            val m = row * 3 + col + 1  // 1..12
                            val isFuture = pickedYear == now.get(Calendar.YEAR) && m > now.get(Calendar.MONTH) + 1
                            val isSelected = m == pickedMonth
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSelected -> Color(0xFF6A1B9A)
                                    isFuture   -> MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
                                    else       -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                onClick = {
                                    if (!isFuture) {
                                        pickedMonth = m
                                        onSelect(m, pickedYear)
                                    }
                                }
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        monthShorts[m - 1],
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            isFuture   -> MaterialTheme.colorScheme.onSurface.copy(0.3f)
                                            else       -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Share text builder ────────────────────────────────────────────────────────

private fun buildShareText(state: AIReportUiState.Ready): String {
    val monthNames = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )
    val monthName = monthNames.getOrElse(state.month - 1) { "Month ${state.month}" }
    val a = state.analysis
    val c = state.currency

    return """
📊 My $monthName ${state.year} Financial Report

💰 Income:   ${formatCurrency(a.income, c)}
💸 Expenses: ${formatCurrency(a.expenses, c)}
💵 Net:      ${formatCurrency(a.netBalance, c)}
📈 Savings:  ${String.format("%.1f", a.savingsRate)}%
🏆 Health Score: ${a.healthScore}/100 (${
        when (a.healthLabel) {
            HealthLabel.EXCELLENT        -> "Excellent"
            HealthLabel.GOOD             -> "Good"
            HealthLabel.FAIR             -> "Fair"
            HealthLabel.NEEDS_IMPROVEMENT -> "Needs Work"
        }
    })

${a.narrative}

Tracked with TrackFinz 📱
    """.trimIndent()
}
