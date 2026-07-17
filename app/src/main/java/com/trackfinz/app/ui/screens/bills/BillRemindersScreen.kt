package com.trackfinz.app.ui.screens.bills

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.data.model.BillFrequency
import com.trackfinz.app.data.model.BillReminderEntity
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.i18n.LocalLanguage
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.navigation.NavRoutes
import com.trackfinz.app.ui.components.TrackFinzBottomBar
import com.trackfinz.app.ui.theme.ExpenseRed
import com.trackfinz.app.ui.theme.IncomeGreen
import com.trackfinz.app.ui.theme.Teal500
import com.trackfinz.app.utils.formatCurrency
import com.trackfinz.app.utils.formatDate
import com.trackfinz.app.viewmodel.BillReminderViewModel
import com.trackfinz.app.viewmodel.SettingsViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillRemindersScreen(
    onNavigate: (String) -> Unit,
    vm: BillReminderViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val bills by vm.bills.collectAsStateWithLifecycle()
    val currency by settingsVm.currency.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editBill by remember { mutableStateOf<BillReminderEntity?>(null) }
    var payBill by remember { mutableStateOf<BillReminderEntity?>(null) }

    val now = System.currentTimeMillis()
    val overdue  = bills.filter { it.isActive && it.dueDateMillis < now }.sortedByDescending { it.dueDateMillis }
    val upcoming = bills.filter { it.isActive && it.dueDateMillis >= now }.sortedBy { it.dueDateMillis }
    val inactive = bills.filter { !it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(Str.BILL_REMINDERS_TITLE), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = s(Str.ADD_BILL))
                    }
                }
            )
        },
        bottomBar = { TrackFinzBottomBar(currentRoute = NavRoutes.BILL_REMINDERS, onNavigate = onNavigate) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Teal500) {
                Icon(Icons.Default.Add, contentDescription = s(Str.ADD_BILL), tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BillSummaryCard(bills = bills, currency = currency, s = s) }

            if (overdue.isNotEmpty()) {
                item { SectionHeader(title = s(Str.OVERDUE), count = overdue.size, color = ExpenseRed) }
                items(overdue, key = { it.id }) { bill ->
                    BillCard(bill = bill, currency = currency, isOverdue = true, s = s,
                        onMarkPaid = { payBill = bill },
                        onEdit = { editBill = bill },
                        onDelete = { vm.deleteBill(bill) },
                        onToggle = { vm.toggleActive(bill) })
                }
            }
            if (upcoming.isNotEmpty()) {
                item { SectionHeader(title = s(Str.UPCOMING), count = upcoming.size, color = Teal500) }
                items(upcoming, key = { it.id }) { bill ->
                    BillCard(bill = bill, currency = currency, isOverdue = false, s = s,
                        onMarkPaid = { payBill = bill },
                        onEdit = { editBill = bill },
                        onDelete = { vm.deleteBill(bill) },
                        onToggle = { vm.toggleActive(bill) })
                }
            }
            if (inactive.isNotEmpty()) {
                item { SectionHeader(title = s(Str.PAUSED), count = inactive.size,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
                items(inactive, key = { it.id }) { bill ->
                    BillCard(bill = bill, currency = currency, isOverdue = false, s = s,
                        onMarkPaid = { payBill = bill },
                        onEdit = { editBill = bill },
                        onDelete = { vm.deleteBill(bill) },
                        onToggle = { vm.toggleActive(bill) })
                }
            }
            if (bills.isEmpty()) { item { EmptyBillsState(s = s, onAdd = { showAddDialog = true }) } }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Mark as Paid confirm dialog ───────────────────────────────────────────
    payBill?.let { bill ->
        val currency2 by settingsVm.currency.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { payBill = null },
            icon = { Text("✅", fontSize = 32.sp) },
            title = { Text(s(Str.MARK_PAID_TITLE), fontWeight = FontWeight.Bold) },
            text = {
                Text(String.format(s(Str.MARK_PAID_DESC), formatCurrency(bill.amount, currency2)))
            },
            confirmButton = {
                Button(
                    onClick = { vm.markAsPaid(bill); payBill = null },
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s(Str.MARK_AS_PAID))
                }
            },
            dismissButton = { TextButton(onClick = { payBill = null }) { Text(s(Str.CANCEL)) } }
        )
    }

    if (showAddDialog) {
        AddEditBillDialog(existing = null, s = s,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, dueMs, remMs, freq, cat, note ->
                vm.addBill(name, amount, dueMs, remMs, freq, cat, note)
                showAddDialog = false
            })
    }
    editBill?.let { bill ->
        AddEditBillDialog(existing = bill, s = s,
            onDismiss = { editBill = null },
            onConfirm = { name, amount, dueMs, remMs, freq, cat, note ->
                vm.updateBill(bill.copy(name = name, amount = amount, dueDateMillis = dueMs,
                    reminderTimeMillis = remMs, frequency = freq, category = cat, note = note))
                editBill = null
            })
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun BillSummaryCard(bills: List<BillReminderEntity>, currency: String, s: (String) -> String) {
    val now = System.currentTimeMillis()
    val overdueCount  = bills.count { it.isActive && it.dueDateMillis < now }
    val upcomingCount = bills.count { it.isActive && it.dueDateMillis >= now }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF0288D1))))
            .padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(s(Str.BILL_REMINDERS_TITLE), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    BillStat(s(Str.TOTAL_BILLS), "${bills.size}", Color.White)
                    BillStat(s(Str.OVERDUE), "$overdueCount",
                        if (overdueCount > 0) Color(0xFFFFCDD2) else Color.White)
                    BillStat(s(Str.UPCOMING), "$upcomingCount", Color(0xFFB3E5FC))
                }
            }
        }
    }
}

@Composable
private fun BillStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.8f))
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(4.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.width(8.dp))
        Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
            Text("$count", style = MaterialTheme.typography.labelSmall, color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

// ── Bill card ─────────────────────────────────────────────────────────────────

@Composable
private fun BillCard(
    bill: BillReminderEntity,
    currency: String,
    isOverdue: Boolean,
    s: (String) -> String,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val now = System.currentTimeMillis()
    val daysUntil = ((bill.dueDateMillis - now) / (1000 * 60 * 60 * 24)).toInt()

    val accentColor = when {
        !bill.isActive -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isOverdue      -> ExpenseRed
        daysUntil <= 3 -> Color(0xFFFB8C00)
        else           -> Teal500
    }
    val dueLabelColor = when {
        isOverdue      -> ExpenseRed
        daysUntil <= 3 -> Color(0xFFFB8C00)
        else           -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    val dueLabel = when {
        isOverdue      -> if (-daysUntil == 1) s(Str.OVERDUE_BY_DAY)
                          else String.format(s(Str.OVERDUE_BY_DAYS), -daysUntil)
        daysUntil == 0 -> s(Str.DUE_TODAY)
        daysUntil == 1 -> s(Str.DUE_TOMORROW)
        else           -> String.format(s(Str.DUE_IN_DAYS), daysUntil)
    }
    val freqLabel = when (bill.frequency) {
        BillFrequency.ONCE    -> s(Str.FREQ_ONCE)
        BillFrequency.WEEKLY  -> s(Str.FREQ_WEEKLY)
        BillFrequency.MONTHLY -> s(Str.FREQ_MONTHLY)
        BillFrequency.YEARLY  -> s(Str.FREQ_YEARLY)
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) ExpenseRed.copy(alpha = 0.05f)
                             else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(bill.category.emoji, fontSize = 22.sp) }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bill.name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!bill.isActive) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Surface(color = accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                        Text(freqLabel, style = MaterialTheme.typography.labelSmall, color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(dueLabel, style = MaterialTheme.typography.bodySmall, color = dueLabelColor,
                    fontWeight = if (isOverdue || daysUntil <= 3) FontWeight.SemiBold else FontWeight.Normal)
                Text("${s(Str.DUE_DATE)}: ${formatDate(bill.dueDateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                if (bill.note.isNotBlank()) {
                    Text(bill.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(bill.amount, currency), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = accentColor)
                Spacer(Modifier.height(4.dp))
                // Mark as Paid button — only for active bills
                if (bill.isActive) {
                    OutlinedButton(
                        onClick = onMarkPaid,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IncomeGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = s(Str.MARK_AS_PAID),
                            tint = IncomeGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s(Str.PAID), style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                        Icon(if (bill.isActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = if (bill.isActive) s(Str.PAUSED) else s(Str.UPCOMING),
                            tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = s(Str.EDIT),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = s(Str.DELETE),
                            tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyBillsState(s: (String) -> String, onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔔", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(s(Str.NO_BILL_REMINDERS), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Text(s(Str.NO_BILLS_DESC), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Teal500)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(s(Str.ADD_FIRST_BILL))
            }
        }
    }
}

// ── Add / Edit dialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEditBillDialog(
    existing: BillReminderEntity?,
    s: (String) -> String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long, Long, BillFrequency, TransactionCategory, String) -> Unit
) {
    val lang = LocalLanguage.current
    val isEdit = existing != null

    var name      by remember { mutableStateOf(existing?.name ?: "") }
    var amount    by remember { mutableStateOf(if (existing != null) existing.amount.toString() else "") }
    var note      by remember { mutableStateOf(existing?.note ?: "") }
    var category  by remember { mutableStateOf(existing?.category ?: TransactionCategory.BILLS) }
    var frequency by remember { mutableStateOf(existing?.frequency ?: BillFrequency.MONTHLY) }
    var error     by remember { mutableStateOf<String?>(null) }

    val initDueCal = Calendar.getInstance().apply {
        if (existing != null) timeInMillis = existing.dueDateMillis else add(Calendar.DAY_OF_MONTH, 7)
    }
    var dueYear  by remember { mutableIntStateOf(initDueCal.get(Calendar.YEAR)) }
    var dueMonth by remember { mutableIntStateOf(initDueCal.get(Calendar.MONTH)) }
    var dueDay   by remember { mutableIntStateOf(initDueCal.get(Calendar.DAY_OF_MONTH)) }

    val initRemCal = Calendar.getInstance().apply {
        if (existing != null) timeInMillis = existing.reminderTimeMillis
        else { add(Calendar.DAY_OF_MONTH, 6); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }
    }
    var remHour  by remember { mutableIntStateOf(initRemCal.get(Calendar.HOUR_OF_DAY)) }
    var remMin   by remember { mutableIntStateOf(initRemCal.get(Calendar.MINUTE)) }
    var remDayOffset by remember { mutableIntStateOf(
        if (existing != null)
            ((existing.dueDateMillis - existing.reminderTimeMillis) / (1000 * 60 * 60 * 24)).toInt().coerceIn(0, 7)
        else 1
    )}
    var showDueDatePicker by remember { mutableStateOf(false) }

    val offsetValues = listOf(0, 1, 2, 3, 5, 7)
    val offsetLabels = listOf(
        s(Str.ON_DUE_DATE),
        s(Str.DAY_BEFORE),
        String.format(s(Str.DAYS_BEFORE), 2),
        String.format(s(Str.DAYS_BEFORE), 3),
        String.format(s(Str.DAYS_BEFORE), 5),
        String.format(s(Str.DAYS_BEFORE), 7)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) s(Str.EDIT_BILL_REMINDER) else s(Str.NEW_BILL_REMINDER),
            fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(s(Str.BILL_NAME)) },
                    placeholder = { Text(s(Str.BILL_NAME_HINT)) },
                    leadingIcon = { Icon(Icons.Default.Receipt, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                OutlinedTextField(value = amount, onValueChange = { amount = it },
                    label = { Text(s(Str.AMOUNT)) },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                Text(s(Str.DUE_DATE), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                OutlinedButton(onClick = { showDueDatePicker = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    val cal = Calendar.getInstance().apply { set(dueYear, dueMonth, dueDay) }
                    Text(formatDate(cal.timeInMillis))
                }

                Text(s(Str.REMIND_ME), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    offsetValues.forEachIndexed { i, v ->
                        FilterChip(selected = remDayOffset == v, onClick = { remDayOffset = v },
                            label = { Text(offsetLabels[i], style = MaterialTheme.typography.bodySmall) })
                    }
                }

                Text(s(Str.REMINDER_TIME), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(8, 9, 12, 18, 20).forEach { h ->
                        FilterChip(selected = remHour == h, onClick = { remHour = h; remMin = 0 },
                            label = { Text(String.format("%02d:00", h)) })
                    }
                }

                Text(s(Str.FREQUENCY), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    mapOf(BillFrequency.ONCE to s(Str.FREQ_ONCE), BillFrequency.WEEKLY to s(Str.FREQ_WEEKLY),
                        BillFrequency.MONTHLY to s(Str.FREQ_MONTHLY), BillFrequency.YEARLY to s(Str.FREQ_YEARLY)
                    ).forEach { (freq, label) ->
                        FilterChip(selected = frequency == freq, onClick = { frequency = freq },
                            label = { Text(label) })
                    }
                }

                Text(s(Str.CATEGORY), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransactionCategory.expenseCategories().forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat },
                            label = { Text("${cat.emoji} ${cat.getLabel(lang)}") })
                    }
                }

                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text(s(Str.NOTE_OPTIONAL)) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2)

                error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    when {
                        name.isBlank()          -> { error = s(Str.BILL_NAME_REQUIRED); return@Button }
                        amt == null || amt <= 0 -> { error = s(Str.ENTER_VALID_AMOUNT); return@Button }
                    }
                    val dueCal = Calendar.getInstance().apply {
                        set(dueYear, dueMonth, dueDay, 23, 59, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val remCal = Calendar.getInstance().apply {
                        timeInMillis = dueCal.timeInMillis
                        add(Calendar.DAY_OF_MONTH, -remDayOffset)
                        set(Calendar.HOUR_OF_DAY, remHour); set(Calendar.MINUTE, remMin)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(name.trim(), amt!!, dueCal.timeInMillis, remCal.timeInMillis,
                        frequency, category, note.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Teal500)
            ) { Text(if (isEdit) s(Str.UPDATE) else s(Str.ADD_REMINDER)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )

    if (showDueDatePicker) {
        val initMs = Calendar.getInstance().apply { set(dueYear, dueMonth, dueDay) }.timeInMillis
        val dpState = rememberDatePickerState(initialSelectedDateMillis = initMs)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { ms ->
                        val c = Calendar.getInstance().apply { timeInMillis = ms }
                        dueYear = c.get(Calendar.YEAR); dueMonth = c.get(Calendar.MONTH)
                        dueDay = c.get(Calendar.DAY_OF_MONTH)
                    }
                    showDueDatePicker = false
                }) { Text(s(Str.OK)) }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text(s(Str.CANCEL)) } }
        ) { DatePicker(state = dpState) }
    }
}
