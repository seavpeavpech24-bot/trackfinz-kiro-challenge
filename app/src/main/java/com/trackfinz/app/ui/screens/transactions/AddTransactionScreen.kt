package com.trackfinz.app.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.model.TransactionEntity
import com.trackfinz.app.data.model.TransactionType
import com.trackfinz.app.ui.components.GradientButton
import com.trackfinz.app.ui.theme.ExpenseRed
import com.trackfinz.app.ui.theme.IncomeGreen
import com.trackfinz.app.ui.theme.Teal500
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.utils.formatDate
import com.trackfinz.app.utils.SmartCategorizer
import com.trackfinz.app.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    transactionId: Int?,
    initialType: String = "EXPENSE",
    onBack: () -> Unit,
    vm: TransactionViewModel = hiltViewModel()
) {
    // Collect the live list so we always find the transaction even on first composition
    val allTx by vm.allTransactions.collectAsState()
    val existing = remember(transactionId, allTx) {
        transactionId?.let { id -> allTx.find { it.id == id } }
    }
    val isEdit = transactionId != null

    var selectedTab by remember { mutableIntStateOf(if (initialType == "INCOME") 1 else 0) }
    val currentType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
    val s = LocalStrings.current
    val lang = com.trackfinz.app.i18n.LocalLanguage.current

    var category by remember { mutableStateOf(TransactionCategory.FOOD) }
    var title    by remember { mutableStateOf("") }
    var amount   by remember { mutableStateOf("") }
    var note     by remember { mutableStateOf("") }
    var txDate   by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error    by remember { mutableStateOf<String?>(null) }
    var fieldsSeeded by remember { mutableStateOf(false) }
    var suggestedCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var showCategorySuggestion by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Seed fields once the existing transaction is loaded from the DB
    LaunchedEffect(existing) {
        if (existing != null && !fieldsSeeded) {
            selectedTab = if (existing.type == TransactionType.INCOME) 1 else 0
            title       = existing.title
            category    = existing.category
            amount      = existing.amount.toString()
            note        = existing.note
            txDate      = existing.date
            fieldsSeeded = true
        }
    }

    // Reset category when switching tabs on a new transaction
    LaunchedEffect(selectedTab) {
        if (!isEdit) category = if (selectedTab == 1) TransactionCategory.SALARY else TransactionCategory.FOOD
    }

    val gradientColors = if (currentType == TransactionType.EXPENSE)
        listOf(ExpenseRed, Color(0xFFB71C1C))
    else
        listOf(IncomeGreen, Teal500)

    val availableCategories = if (currentType == TransactionType.INCOME)
        TransactionCategory.incomeCategories()
    else
        TransactionCategory.expenseCategories()

    // Ensure selected category is valid for current type
    LaunchedEffect(currentType) {
        if (!isEdit && !availableCategories.contains(category)) {
            category = availableCategories.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when {
                        isEdit -> s(Str.EDIT_TRANSACTION)
                        currentType == TransactionType.INCOME -> s(Str.ADD_INCOME)
                        else -> s(Str.ADD_EXPENSE)
                    })
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Income / Expense tab switcher ─────────────────────────────────
            if (!isEdit) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = if (selectedTab == 0) ExpenseRed else IncomeGreen
                        )
                    }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = {
                        Text("💸 ${s(Str.EXPENSE)}",
                            color = if (selectedTab == 0) ExpenseRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = {
                        Text("💰 ${s(Str.INCOME)}",
                            color = if (selectedTab == 1) IncomeGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                    })
                }
            }

            // ── Amount hero ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(gradientColors))
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (currentType == TransactionType.EXPENSE) s(Str.HOW_MUCH_SPENT) else s(Str.HOW_MUCH_EARNED),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        placeholder = {
                            Text("0.00", fontSize = 36.sp, color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        },
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            color = Color.White, fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.7f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, null, tint = Color.White.copy(alpha = 0.8f))
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Title field with smart categorization ─────────────────────
                Text("Title", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                OutlinedTextField(
                    value = title,
                    onValueChange = { newTitle ->
                        title = newTitle
                        // Auto-suggest category when user types
                        if (newTitle.length >= 3 && !isEdit) {
                            scope.launch {
                                val amountValue = amount.toDoubleOrNull() ?: 0.0
                                val suggested = SmartCategorizer.suggestCategory(
                                    context, newTitle, amountValue, currentType
                                )
                                if (suggested != category) {
                                    suggestedCategory = suggested
                                    showCategorySuggestion = true
                                }
                            }
                        }
                    },
                    placeholder = { Text(if (currentType == TransactionType.EXPENSE) "e.g., Brown Coffee, Grab, Walmart" else "e.g., Salary, Freelance Project") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                    trailingIcon = if (title.isNotEmpty()) {
                        {
                            IconButton(onClick = { title = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null
                )

                // Smart category suggestion chip
                if (showCategorySuggestion && suggestedCategory != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Teal500.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Teal500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Smart Suggestion",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Teal500
                                    )
                                    Text(
                                        "${suggestedCategory!!.emoji} ${suggestedCategory!!.getLabel(lang)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Row {
                                TextButton(onClick = { showCategorySuggestion = false }) {
                                    Text("Dismiss")
                                }
                                Button(
                                    onClick = {
                                        category = suggestedCategory!!
                                        showCategorySuggestion = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Teal500
                                    )
                                ) {
                                    Text("Apply")
                                }
                            }
                        }
                    }
                }

                // ── Category grid ─────────────────────────────────────────────
                Text(s(Str.CATEGORY), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val lang = com.trackfinz.app.i18n.LocalLanguage.current
                    availableCategories.forEach { cat ->
                        val selected = category == cat
                        val chipBg = if (selected)
                            (if (currentType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                        val chipBorder = if (selected)
                            (if (currentType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen)
                        else Color.Transparent

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .border(1.5.dp, chipBorder, RoundedCornerShape(12.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.emoji, fontSize = 18.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    cat.getLabel(lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected)
                                        (if (currentType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ── Date picker ───────────────────────────────────────────────
                Text(s(Str.DATE), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(formatDate(txDate), style = MaterialTheme.typography.bodyLarge)
                }

                // ── Description ───────────────────────────────────────────────
                Text(s(Str.DESCRIPTION), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(s(Str.ADD_NOTE)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 4,
                    leadingIcon = {
                        Icon(Icons.Default.Notes, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                    }
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }

                GradientButton(
                    text = when {
                        isEdit -> s(Str.UPDATE_TRANSACTION)
                        currentType == TransactionType.INCOME -> s(Str.SAVE_INCOME)
                        else -> s(Str.SAVE_EXPENSE)
                    },
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (amt == null || amt <= 0) { error = s(Str.ENTER_VALID_AMOUNT); return@GradientButton }
                        if (title.isBlank()) { error = "Please enter a description"; return@GradientButton }
                        
                        val tx = TransactionEntity(
                            id = existing?.id ?: 0,
                            title = title.trim(),
                            amount = amt,
                            type = currentType,
                            category = category,
                            note = note.trim(),
                            date = txDate
                        )
                        
                        // Learn from user's category choice for smart categorization
                        if (!isEdit) {
                            scope.launch {
                                SmartCategorizer.learnFromCorrection(context, title.trim(), category)
                            }
                        }
                        
                        if (isEdit) vm.updateTransaction(tx) else vm.addTransaction(tx)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = gradientColors
                )
            }
        }
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = txDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { txDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
