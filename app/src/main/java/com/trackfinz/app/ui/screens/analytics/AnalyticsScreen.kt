package com.trackfinz.app.ui.screens.analytics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.model.TransactionType
import com.trackfinz.app.navigation.NavRoutes
import com.trackfinz.app.ui.components.*
import com.trackfinz.app.ui.theme.*
import com.trackfinz.app.utils.*
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.viewmodel.SettingsViewModel
import com.trackfinz.app.viewmodel.TransactionViewModel
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

// ── Date range presets ────────────────────────────────────────────────────────

enum class DateRangePreset(val labelKey: String) {
    TODAY(Str.TODAY), WEEK(Str.WEEK), MONTH(Str.MONTH), YEAR(Str.YEAR), CUSTOM(Str.CUSTOM)
}

private fun presetRange(preset: DateRangePreset): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    val now = System.currentTimeMillis()
    return when (preset) {
        DateRangePreset.TODAY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to now
        }
        DateRangePreset.WEEK -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to now
        }
        DateRangePreset.MONTH -> monthStartMillis(currentMonth(), currentYear()) to now
        DateRangePreset.YEAR -> {
            cal.set(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to now
        }
        DateRangePreset.CUSTOM -> 0L to now
    }
}

// ── Chart type selector ───────────────────────────────────────────────────────

enum class ChartType(val labelKey: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DONUT(Str.CHART_DONUT,   Icons.Default.DonutLarge),
    PIE(Str.CHART_PIE,       Icons.Default.PieChart),
    BAR(Str.CHART_BAR,       Icons.Default.BarChart),
    LINE(Str.CHART_TREND,    Icons.Default.ShowChart),
    COMPARE(Str.CHART_COMPARE, Icons.Default.CompareArrows)
}

// ── Category colors ───────────────────────────────────────────────────────────

private val catColors = mapOf(
    TransactionCategory.FOOD          to Color(0xFFEF5350),
    TransactionCategory.GROCERIES     to Color(0xFFFF7043),
    TransactionCategory.SHOPPING      to Color(0xFFAB47BC),
    TransactionCategory.BILLS         to Color(0xFF42A5F5),
    TransactionCategory.TRAVEL        to Color(0xFF26A69A),
    TransactionCategory.GAS           to Color(0xFF8D6E63),
    TransactionCategory.SALARY        to Color(0xFF66BB6A),
    TransactionCategory.FREELANCE     to Color(0xFF26C6DA),
    TransactionCategory.INVESTMENT    to Color(0xFF5C6BC0),
    TransactionCategory.GIFT_IN       to Color(0xFFEC407A),
    TransactionCategory.REFUND        to Color(0xFF29B6F6),
    TransactionCategory.ENTERTAINMENT to Color(0xFFFF7043),
    TransactionCategory.HEALTHCARE    to Color(0xFFEC407A),
    TransactionCategory.OTHER         to Color(0xFF78909C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigate: (String) -> Unit,
    vm: TransactionViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val currency by settingsVm.currency.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val lang = com.trackfinz.app.i18n.LocalLanguage.current

    var preset     by remember { mutableStateOf(DateRangePreset.MONTH) }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var chartType  by remember { mutableStateOf(ChartType.DONUT) }
    var customFrom by remember { mutableLongStateOf(monthStartMillis(currentMonth(), currentYear())) }
    var customTo   by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCustom by remember { mutableStateOf(false) }

    val rangeFrom by remember(preset, customFrom) {
        derivedStateOf { if (preset == DateRangePreset.CUSTOM) customFrom else presetRange(preset).first }
    }
    val rangeTo by remember(preset, customTo) {
        derivedStateOf { if (preset == DateRangePreset.CUSTOM) customTo else presetRange(preset).second }
    }

    // Update the ViewModel's analytics range whenever rangeFrom/rangeTo changes
    LaunchedEffect(rangeFrom, rangeTo) {
        vm.setAnalyticsRange(rangeFrom, rangeTo)
    }

    // Collect from the stable flows in the ViewModel
    val txInRange      by vm.analyticsTransactions.collectAsStateWithLifecycle()
    val incomeInRange  by vm.analyticsIncome.collectAsStateWithLifecycle()
    val expenseInRange by vm.analyticsExpense.collectAsStateWithLifecycle()

    val filteredTx = remember(txInRange, typeFilter) {
        when (typeFilter) {
            TransactionType.INCOME  -> txInRange.filter { it.type == TransactionType.INCOME }
            TransactionType.EXPENSE -> txInRange.filter { it.type == TransactionType.EXPENSE }
            else -> txInRange
        }
    }

    val expenseByCat = remember(filteredTx) {
        filteredTx.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }
    val incomeByCat = remember(filteredTx) {
        filteredTx.filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }

    val expenseSlices = remember(expenseByCat, lang) {
        expenseByCat.filter { it.value > 0 }.map { (cat, amt) ->
            PieSlice("${cat.emoji} ${cat.getLabel(lang)}", amt.toFloat(), catColors[cat] ?: SoftGray400)
        }
    }
    val incomeSlices = remember(incomeByCat, lang) {
        incomeByCat.filter { it.value > 0 }.map { (cat, amt) ->
            PieSlice("${cat.emoji} ${cat.getLabel(lang)}", amt.toFloat(), catColors[cat] ?: SoftGray400)
        }
    }

    // Bar entries for expense
    val barEntries = remember(expenseByCat) {
        expenseByCat.take(7).map { (cat, amt) -> BarEntry(cat.emoji, amt.toFloat()) }
    }

    // Daily trend line data
    val lineEntries = remember(filteredTx, preset) {
        buildDailyTrend(filteredTx.filter { it.type == TransactionType.EXPENSE }, rangeFrom, rangeTo)
    }

    // Grouped bar: income vs expense by month (last 6 months for year, or by day for week)
    val groupedEntries = remember(txInRange, preset) {
        buildGroupedEntries(txInRange, rangeFrom, rangeTo, preset)
    }

    val totalIncome  = incomeInRange ?: 0.0
    val totalExpense = expenseInRange ?: 0.0
    val netBalance   = totalIncome - totalExpense
    val savingsRate  = if (totalIncome > 0) (netBalance / totalIncome * 100).coerceAtLeast(0.0) else 0.0

    Scaffold(
        topBar = { TopAppBar(title = { Text(s(Str.ANALYTICS)) }) },
        bottomBar = { TrackFinzBottomBar(currentRoute = NavRoutes.ANALYTICS, onNavigate = onNavigate) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Period chips ──────────────────────────────────────────────────
            item {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
                    Text(s(Str.PERIOD), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateRangePreset.entries.forEach { p ->
                            FilterChip(
                                selected = preset == p,
                                onClick = { preset = p; if (p == DateRangePreset.CUSTOM) showCustom = true },
                                label = { Text(s(p.labelKey), maxLines = 1, overflow = TextOverflow.Clip) }
                            )
                        }
                    }
                    if (preset == DateRangePreset.CUSTOM) {
                        Spacer(Modifier.height(4.dp))
                        Text("${formatDate(rangeFrom)} – ${formatDate(rangeTo)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ── Type filter ───────────────────────────────────────────────────
            item {
                Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to s(Str.ALL), TransactionType.INCOME to s(Str.INCOME), TransactionType.EXPENSE to s(Str.EXPENSE))
                        .forEach { (type, label) ->
                            val col = when (type) {
                                TransactionType.INCOME  -> IncomeGreen
                                TransactionType.EXPENSE -> ExpenseRed
                                else -> MaterialTheme.colorScheme.primary
                            }
                            FilterChip(
                                selected = typeFilter == type,
                                onClick = { typeFilter = type },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = col.copy(alpha = 0.15f),
                                    selectedLabelColor = col
                                )
                            )
                        }
                }
            }

            // ── KPI cards ─────────────────────────────────────────────────────
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(s(Str.INCOME),  formatCurrency(totalIncome, currency),  IncomeGreen,  Icons.Default.TrendingUp,   Modifier.weight(1f))
                        KpiCard(s(Str.EXPENSE), formatCurrency(totalExpense, currency), ExpenseRed,   Icons.Default.TrendingDown,  Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(s(Str.NET_BALANCE), formatCurrency(netBalance, currency),
                            if (netBalance >= 0) IncomeGreen else ExpenseRed, Icons.Default.AccountBalance, Modifier.weight(1f))
                        KpiCard(s(Str.SAVINGS_RATE), "${String.format("%.1f", savingsRate)}%",
                            if (savingsRate >= 20) IncomeGreen else WarningAmber, Icons.Default.Savings, Modifier.weight(1f))
                    }
                }
            }

            // ── Transaction count pills ───────────────────────────────────────
            item {
                Card(Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceAround) {
                        StatPill(s(Str.ALL),     "${filteredTx.size}",                                          MaterialTheme.colorScheme.primary)
                        VerticalDivider(Modifier.height(36.dp))
                        StatPill(s(Str.INCOME),  "${filteredTx.count { it.type == TransactionType.INCOME }}",  IncomeGreen)
                        VerticalDivider(Modifier.height(36.dp))
                        StatPill(s(Str.EXPENSE), "${filteredTx.count { it.type == TransactionType.EXPENSE }}", ExpenseRed)
                    }
                }
            }

            // ── Chart type selector ───────────────────────────────────────────
            item {
                Card(Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(s(Str.CHART_TYPE), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChartType.entries.forEach { ct ->
                                FilterChip(
                                    selected = chartType == ct,
                                    onClick = { chartType = ct },
                                    leadingIcon = { Icon(ct.icon, null, modifier = Modifier.size(16.dp)) },
                                    label = { Text(s(ct.labelKey)) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Chart panel ───────────────────────────────────────────────────
            if (filteredTx.isNotEmpty()) {
                item {
                    Card(Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            when (chartType) {

                                ChartType.DONUT -> {
                                    Text(s(Str.EXPENSE_BREAKDOWN_DONUT),
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(16.dp))
                                    if (expenseSlices.isEmpty()) {
                                        EmptyChartHint(s(Str.NO_EXPENSE_DATA))
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            DonutChart(
                                                slices = expenseSlices,
                                                modifier = Modifier.size(160.dp),
                                                centerLabel = formatCurrency(totalExpense, currency).take(8),
                                                centerSubLabel = s(Str.SPENT_LABEL)
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            ChartLegend(expenseSlices.take(7), Modifier.weight(1f), showValues = true)
                                        }
                                    }
                                }

                                ChartType.PIE -> {
                                    Text(s(Str.EXPENSE_BREAKDOWN_PIE),
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(16.dp))
                                    if (expenseSlices.isEmpty()) {
                                        EmptyChartHint(s(Str.NO_EXPENSE_DATA))
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            PieChart(slices = expenseSlices, modifier = Modifier.size(160.dp))
                                            Spacer(Modifier.width(16.dp))
                                            ChartLegend(expenseSlices.take(7), Modifier.weight(1f), showValues = true)
                                        }
                                        if (incomeSlices.isNotEmpty()) {
                                            Spacer(Modifier.height(20.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                            Spacer(Modifier.height(16.dp))
                                            Text(s(Str.INCOME_BREAKDOWN_LABEL),
                                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.height(12.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                PieChart(slices = incomeSlices, modifier = Modifier.size(140.dp))
                                                Spacer(Modifier.width(16.dp))
                                                ChartLegend(incomeSlices.take(5), Modifier.weight(1f), showValues = true)
                                            }
                                        }
                                    }
                                }

                                ChartType.BAR -> {
                                    Text(s(Str.TOP_EXPENSE_BAR),
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(16.dp))
                                    if (barEntries.isEmpty()) {
                                        EmptyChartHint(s(Str.NO_EXPENSE_DATA))
                                    } else {
                                        BarChart(entries = barEntries, barColor = Teal500,
                                            modifier = Modifier.fillMaxWidth(), showValues = true)
                                        Spacer(Modifier.height(12.dp))
                                        expenseByCat.take(7).forEach { (cat, amt) ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                                Text("${cat.emoji} ${cat.getLabel(lang)}", style = MaterialTheme.typography.bodySmall)
                                                Text(formatCurrency(amt, currency), style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                                            }
                                        }
                                    }
                                }

                                ChartType.LINE -> {
                                    Text(s(Str.DAILY_TREND_LINE),
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(16.dp))
                                    if (lineEntries.isEmpty()) {
                                        EmptyChartHint(s(Str.NOT_ENOUGH_DATA))
                                    } else {
                                        LineChart(entries = lineEntries, lineColor = ExpenseRed,
                                            fillColor = ExpenseRed.copy(alpha = 0.1f),
                                            modifier = Modifier.fillMaxWidth())
                                    }
                                }

                                ChartType.COMPARE -> {
                                    Text(s(Str.INCOME_VS_EXPENSE),
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(16.dp))
                                    if (groupedEntries.isEmpty()) {
                                        EmptyChartHint(s(Str.NOT_ENOUGH_DATA))
                                    } else {
                                        GroupedBarChart(entries = groupedEntries, incomeColor = IncomeGreen,
                                            expenseColor = ExpenseRed, modifier = Modifier.fillMaxWidth())
                                        Spacer(Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            LegendDot(IncomeGreen, s(Str.INCOME))
                                            LegendDot(ExpenseRed, s(Str.EXPENSE))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (expenseByCat.isNotEmpty()) {
                item {
                    Card(Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text(s(Str.EXPENSE_BY_CATEGORY), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            expenseByCat.forEach { (cat, amount) ->
                                val pct = if (totalExpense > 0) amount / totalExpense * 100 else 0.0
                                CategoryRow(cat.emoji, cat.getLabel(lang),
                                    "${String.format("%.1f", pct)}${s(Str.PCT_OF_EXPENSES)}",
                                    formatCurrency(amount, currency), ExpenseRed)
                            }
                        }
                    }
                }
            }

            if (incomeByCat.isNotEmpty()) {
                item {
                    Card(Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text(s(Str.INCOME_BY_CATEGORY), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            incomeByCat.forEach { (cat, amount) ->
                                CategoryRow(cat.emoji, cat.getLabel(lang), null,
                                    formatCurrency(amount, currency), IncomeGreen)
                            }
                        }
                    }
                }
            }

            if (filteredTx.isNotEmpty()) {
                item {
                    Card(Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("${s(Str.TOP_TRANSACTIONS)} (${filteredTx.size})",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            filteredTx.take(15).forEach { tx ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Text(tx.category.emoji, fontSize = 18.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(tx.title, style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(formatDate(tx.date), style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${if (tx.type == TransactionType.INCOME) "+" else "-"}${formatCurrency(tx.amount, currency)}",
                                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                        color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            }
                            if (filteredTx.size > 15) {
                                Spacer(Modifier.height(8.dp))
                                Text("+ ${filteredTx.size - 15} ${s(Str.MORE_ITEMS)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            if (filteredTx.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.BarChart, null, modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f))
                            Spacer(Modifier.height(8.dp))
                            Text(s(Str.NO_DATA_PERIOD), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    if (showCustom) {
        CustomDateRangeDialog(customFrom, customTo,
            onDismiss = { showCustom = false },
            onConfirm = { from, to -> customFrom = from; customTo = to; showCustom = false })
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun KpiCard(label: String, value: String, color: Color,
                    icon: androidx.compose.ui.graphics.vector.ImageVector,
                    modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun CategoryRow(emoji: String, label: String, subtitle: String?, amount: String, amountColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            }
        }
        Text(amount, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, color = amountColor)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
}

@Composable
private fun EmptyChartHint(msg: String) {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

// ── Data builders ─────────────────────────────────────────────────────────────

private fun buildDailyTrend(
    expenses: List<com.trackfinz.app.data.model.TransactionEntity>,
    from: Long, to: Long
): List<LineEntry> {
    if (expenses.isEmpty()) return emptyList()
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    val result = mutableListOf<LineEntry>()
    cal.timeInMillis = from
    val end = Calendar.getInstance().apply { timeInMillis = to }
    while (!cal.after(end)) {
        val dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayEnd = cal.timeInMillis - 1
        val total = expenses.filter { it.date in dayStart..dayEnd }.sumOf { it.amount }.toFloat()
        result.add(LineEntry(sdf.format(Date(dayStart)), total))
    }
    // Trim leading/trailing zeros for cleaner chart, keep at least 2 points
    return if (result.size > 2) result else result
}

private fun buildGroupedEntries(
    txList: List<com.trackfinz.app.data.model.TransactionEntity>,
    from: Long, to: Long,
    preset: DateRangePreset
): List<GroupedBarEntry> {
    if (txList.isEmpty()) return emptyList()
    return when (preset) {
        DateRangePreset.YEAR -> {
            // Group by month
            val months = DateFormatSymbols().shortMonths
            (1..12).mapNotNull { m ->
                val mFrom = monthStartMillis(m, currentYear())
                val mTo   = monthEndMillis(m, currentYear())
                val inc = txList.filter { it.type == TransactionType.INCOME && it.date in mFrom..mTo }.sumOf { it.amount }.toFloat()
                val exp = txList.filter { it.type == TransactionType.EXPENSE && it.date in mFrom..mTo }.sumOf { it.amount }.toFloat()
                if (inc > 0 || exp > 0) GroupedBarEntry(months[m - 1].take(3), inc, exp) else null
            }
        }
        DateRangePreset.MONTH -> {
            // Group by week of month
            val cal = Calendar.getInstance()
            (1..5).mapNotNull { w ->
                cal.timeInMillis = from
                cal.set(Calendar.WEEK_OF_MONTH, w)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val wFrom = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7)
                val wTo = cal.timeInMillis - 1
                val inc = txList.filter { it.type == TransactionType.INCOME && it.date in wFrom..wTo }.sumOf { it.amount }.toFloat()
                val exp = txList.filter { it.type == TransactionType.EXPENSE && it.date in wFrom..wTo }.sumOf { it.amount }.toFloat()
                if (inc > 0 || exp > 0) GroupedBarEntry("W$w", inc, exp) else null
            }
        }
        else -> {
            // Group by day
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            val cal = Calendar.getInstance()
            val result = mutableListOf<GroupedBarEntry>()
            cal.timeInMillis = from
            val end = Calendar.getInstance().apply { timeInMillis = to }
            while (!cal.after(end)) {
                val dFrom = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val dTo = cal.timeInMillis - 1
                val inc = txList.filter { it.type == TransactionType.INCOME && it.date in dFrom..dTo }.sumOf { it.amount }.toFloat()
                val exp = txList.filter { it.type == TransactionType.EXPENSE && it.date in dFrom..dTo }.sumOf { it.amount }.toFloat()
                if (inc > 0 || exp > 0) result.add(GroupedBarEntry(sdf.format(Date(dFrom)).take(2), inc, exp))
            }
            result
        }
    }
}

// ── Custom date range dialog ──────────────────────────────────────────────────

@Composable
private fun CustomDateRangeDialog(fromMillis: Long, toMillis: Long,
                                   onDismiss: () -> Unit, onConfirm: (Long, Long) -> Unit) {
    val fromCal = remember { Calendar.getInstance().apply { timeInMillis = fromMillis } }
    val toCal   = remember { Calendar.getInstance().apply { timeInMillis = toMillis } }
    var fromMonth by remember { mutableIntStateOf(fromCal.get(Calendar.MONTH) + 1) }
    var fromYear  by remember { mutableIntStateOf(fromCal.get(Calendar.YEAR)) }
    var toMonth   by remember { mutableIntStateOf(toCal.get(Calendar.MONTH) + 1) }
    var toYear    by remember { mutableIntStateOf(toCal.get(Calendar.YEAR)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Date Range", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("From", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                MonthYearRow(fromMonth, fromYear, { fromMonth = it }, { fromYear = it })
                HorizontalDivider()
                Text("To", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                MonthYearRow(toMonth, toYear, { toMonth = it }, { toYear = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(monthStartMillis(fromMonth, fromYear), monthEndMillis(toMonth, toYear))
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MonthYearRow(month: Int, year: Int, onMonthChange: (Int) -> Unit, onYearChange: (Int) -> Unit) {
    val shortMonths = DateFormatSymbols().shortMonths
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            if (month > 1) onMonthChange(month - 1) else { onMonthChange(12); onYearChange(year - 1) }
        }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(18.dp)) }
        Text(shortMonths[month - 1], modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        IconButton(onClick = {
            if (month < 12) onMonthChange(month + 1) else { onMonthChange(1); onYearChange(year + 1) }
        }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { onYearChange(year - 1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(18.dp)) }
        Text("$year", modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        IconButton(onClick = { onYearChange(year + 1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp)) }
    }
}
