package com.trackfinz.app.ui.screens.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.data.model.BudgetEntity
import com.trackfinz.app.data.model.BudgetHistoryAction
import com.trackfinz.app.data.model.BudgetHistoryEntity
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.navigation.NavRoutes
import com.trackfinz.app.ui.components.BudgetProgressBar
import com.trackfinz.app.ui.components.GradientButton
import com.trackfinz.app.ui.components.TrackFinzBottomBar
import com.trackfinz.app.ui.theme.ExpenseRed
import com.trackfinz.app.ui.theme.IncomeGreen
import com.trackfinz.app.ui.theme.Teal500
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.LocalLanguage
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.i18n.translatedFullMonths
import com.trackfinz.app.i18n.translatedShortMonths
import com.trackfinz.app.utils.currentMonth
import com.trackfinz.app.utils.currentYear
import com.trackfinz.app.utils.formatCurrency
import com.trackfinz.app.utils.formatDate
import com.trackfinz.app.viewmodel.BudgetViewModel
import com.trackfinz.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(
    onNavigate: (String) -> Unit,
    vm: BudgetViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val budgets      by vm.budgetsWithSpent.collectAsStateWithLifecycle()
    val currency     by settingsVm.currency.collectAsStateWithLifecycle()
    val selMonth     by vm.selectedMonth.collectAsStateWithLifecycle()
    val selYear      by vm.selectedYear.collectAsStateWithLifecycle()
    val history      by vm.allHistory.collectAsStateWithLifecycle()

    var showAddDialog   by remember { mutableStateOf(false) }
    var editBudget      by remember { mutableStateOf<BudgetEntity?>(null) }
    var showHistory     by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val s = LocalStrings.current
    val lang = LocalLanguage.current

    val fullMonths = translatedFullMonths(lang)
    val monthLabel = "${fullMonths[selMonth - 1]} $selYear"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(Str.BUDGET)) },
                actions = {
                    TextButton(onClick = { showMonthPicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(monthLabel, style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Budget")
                    }
                }
            )
        },
        bottomBar = {
            TrackFinzBottomBar(currentRoute = NavRoutes.BUDGET, onNavigate = onNavigate)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Month summary header ──────────────────────────────────────────
            item {
                val totalBudget = budgets.sumOf { it.budget.limit }
                val totalSpent  = budgets.sumOf { it.spent }
                Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BudgetStat(s(Str.BUDGETED), formatCurrency(totalBudget, currency), Teal500)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        BudgetStat(s(Str.SPENT), formatCurrency(totalSpent, currency), ExpenseRed)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        BudgetStat(s(Str.LEFT), formatCurrency((totalBudget - totalSpent).coerceAtLeast(0.0), currency), IncomeGreen)
                    }
                }
            }

            // ── AI Budget Recommendations Banner ──────────────────────────────
            item {
                Card(
                    onClick = { onNavigate(NavRoutes.AI_BUDGET_RECOMMENDATIONS) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF6A1B9A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    s(Str.AI_BUDGET_RECOMMENDATIONS),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    s(Str.GET_BUDGET_SUGGESTIONS),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ── Budget cards ──────────────────────────────────────────────────
            if (budgets.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(s(Str.NO_BUDGETS) + " $monthLabel",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            GradientButton(s(Str.CREATE_BUDGET), onClick = { showAddDialog = true }, modifier = Modifier.width(200.dp))
                        }
                    }
                }
            } else {
                items(budgets, key = { it.budget.id }) { bws ->
                    BudgetProgressBar(
                        budget = bws.budget,
                        spent = bws.spent,
                        currency = currency,
                        onEdit = { editBudget = bws.budget },
                        onDelete = { vm.deleteBudget(bws.budget) }
                    )
                }
            }
        }
    }

    // ── History bottom sheet ──────────────────────────────────────────────────
    if (showHistory) {
        BudgetHistorySheet(
            history = history,
            currency = currency,
            onDismiss = { showHistory = false }
        )
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, limit ->
                vm.addBudget(cat, limit)
                showAddDialog = false
            }
        )
    }

    editBudget?.let { budget ->
        EditBudgetDialog(
            budget = budget,
            currency = currency,
            onDismiss = { editBudget = null },
            onConfirm = { newLimit ->
                vm.updateBudget(budget, newLimit)
                editBudget = null
            }
        )
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = selMonth,
            currentYear = selYear,
            onDismiss = { showMonthPicker = false },
            onConfirm = { m, y ->
                vm.setMonth(m, y)
                showMonthPicker = false
            }
        )
    }
}

// ── Budget History bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetHistorySheet(
    history: List<com.trackfinz.app.data.model.BudgetHistoryEntity>,
    currency: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val s = LocalStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s(Str.BUDGET_HISTORY), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                    Text("${history.size} ${s(Str.ENTRIES)}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(s(Str.NO_HISTORY), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(history) { h -> BudgetHistoryRow(h = h, currency = currency) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BudgetHistoryRow(
    h: com.trackfinz.app.data.model.BudgetHistoryEntity,
    currency: String
) {
    val s = LocalStrings.current
    val (actionColor, actionLabel) = when (h.action) {
        BudgetHistoryAction.CREATED -> IncomeGreen to s(Str.BUDGET_CREATED)
        BudgetHistoryAction.UPDATED -> Teal500 to s(Str.BUDGET_UPDATED)
        BudgetHistoryAction.DELETED -> ExpenseRed to s(Str.BUDGET_DELETED)
    }
    val detail = when (h.action) {
        BudgetHistoryAction.CREATED -> "${s(Str.BUDGET_SET_TO)} ${formatCurrency(h.newLimit ?: 0.0, currency)}"
        BudgetHistoryAction.UPDATED -> "${formatCurrency(h.oldLimit ?: 0.0, currency)} → ${formatCurrency(h.newLimit ?: 0.0, currency)}"
        BudgetHistoryAction.DELETED -> "${s(Str.BUDGET_REMOVED)} ${formatCurrency(h.oldLimit ?: 0.0, currency)}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
                color = actionColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    actionLabel,
                    color = actionColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                val lang = LocalLanguage.current
                Text(
                    "${h.category.emoji} ${h.category.getLabel(lang)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            formatDate(h.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
}

@Composable
private fun BudgetStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBudgetDialog(onDismiss: () -> Unit, onConfirm: (TransactionCategory, Double) -> Unit) {
    var selectedCat by remember { mutableStateOf(TransactionCategory.FOOD) }
    var limit by remember { mutableStateOf("") }
    val s = LocalStrings.current
    val lang = LocalLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(Str.NEW_BUDGET), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(s(Str.CATEGORY), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransactionCategory.expenseCategories().forEach { cat ->
                        FilterChip(selected = selectedCat == cat, onClick = { selectedCat = cat },
                            label = { Text("${cat.emoji} ${cat.getLabel(lang)}") })
                    }
                }
                OutlinedTextField(
                    value = limit, onValueChange = { limit = it },
                    label = { Text(s(Str.MONTHLY_LIMIT)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { limit.toDoubleOrNull()?.let { onConfirm(selectedCat, it) } }) { Text(s(Str.CREATE)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}

@Composable
private fun EditBudgetDialog(
    budget: BudgetEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var newLimit by remember { mutableStateOf(budget.limit.toString()) }
    val s = LocalStrings.current
    val lang = LocalLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${s(Str.EDIT_BUDGET)} — ${budget.category.emoji} ${budget.category.getLabel(lang)}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${s(Str.CURRENT_LIMIT)}: ${formatCurrency(budget.limit, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                OutlinedTextField(
                    value = newLimit, onValueChange = { newLimit = it },
                    label = { Text(s(Str.NEW_LIMIT)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { newLimit.toDoubleOrNull()?.let { onConfirm(it) } }) { Text(s(Str.UPDATE)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}

@Composable
private fun MonthYearPickerDialog(
    currentMonth: Int,
    currentYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var month by remember { mutableIntStateOf(currentMonth) }
    var year  by remember { mutableIntStateOf(currentYear) }
    val s = LocalStrings.current
    val lang = LocalLanguage.current
    val shortMonths = translatedShortMonths(lang)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(Str.SELECT_MONTH), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { year-- }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text("$year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { year++ }) { Icon(Icons.Default.ChevronRight, null) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..3).forEach { col ->
                                val m = row * 4 + col + 1
                                FilterChip(
                                    selected = m == month,
                                    onClick = { month = m },
                                    label = { Text(shortMonths[m - 1]) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(month, year) }) { Text(s(Str.APPLY)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}
