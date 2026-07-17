package com.trackfinz.app.ui.screens.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.data.model.GoalEntity
import com.trackfinz.app.data.model.GoalFundAction
import com.trackfinz.app.navigation.NavRoutes
import com.trackfinz.app.ui.components.GradientButton
import com.trackfinz.app.ui.components.TrackFinzBottomBar
import com.trackfinz.app.ui.theme.Emerald500
import com.trackfinz.app.ui.theme.ExpenseRed
import com.trackfinz.app.ui.theme.IncomeGreen
import com.trackfinz.app.ui.theme.Teal500
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.utils.formatCurrency
import com.trackfinz.app.utils.formatDate
import com.trackfinz.app.viewmodel.GoalViewModel
import com.trackfinz.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(
    onNavigate: (String) -> Unit,
    vm: GoalViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val goals      by vm.goals.collectAsStateWithLifecycle()
    val allHistory by vm.allHistory.collectAsStateWithLifecycle()
    val currency   by settingsVm.currency.collectAsStateWithLifecycle()

    var showAdd        by remember { mutableStateOf(false) }
    var contributeGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var removeGoal     by remember { mutableStateOf<GoalEntity?>(null) }
    var historyGoal    by remember { mutableStateOf<GoalEntity?>(null) }
    var showAllHistory by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(Str.GOALS)) },
                actions = {
                    IconButton(onClick = { showAllHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Goal")
                    }
                }
            )
        },
        bottomBar = {
            TrackFinzBottomBar(currentRoute = NavRoutes.GOALS, onNavigate = onNavigate)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Goal cards ────────────────────────────────────────────────────
            if (goals.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(s(Str.NO_GOALS), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            GradientButton(s(Str.CREATE_GOAL), onClick = { showAdd = true }, modifier = Modifier.width(200.dp))
                        }
                    }
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        currency = currency,
                        onContribute = { contributeGoal = goal },
                        onRemove = { removeGoal = goal },
                        onHistory = { historyGoal = goal },
                        onDelete = { vm.deleteGoal(goal) }
                    )
                }
            }
        }
    }

    // ── All-history bottom sheet ──────────────────────────────────────────────
    if (showAllHistory) {
        AllGoalHistorySheet(
            history = allHistory,
            currency = currency,
            onDismiss = { showAllHistory = false }
        )
    }

    // ── Per-goal history bottom sheet ─────────────────────────────────────────
    historyGoal?.let { goal ->
        GoalHistorySheet(
            goal = goal,
            currency = currency,
            vm = vm,
            onDismiss = { historyGoal = null }
        )
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showAdd) {
        AddGoalDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, target, emoji ->
                vm.addGoal(GoalEntity(title = title, targetAmount = target, emoji = emoji))
                showAdd = false
            }
        )
    }

    contributeGoal?.let { goal ->
        FundDialog(
            title = "${s(Str.ADD_FUNDS)} — ${goal.title}",
            confirmLabel = s(Str.ADD),
            onDismiss = { contributeGoal = null },
            onConfirm = { amount ->
                vm.contribute(goal, amount)
                contributeGoal = null
            }
        )
    }

    removeGoal?.let { goal ->
        FundDialog(
            title = "${s(Str.REMOVE_FUNDS)} — ${goal.title}",
            confirmLabel = s(Str.REMOVE),
            onDismiss = { removeGoal = null },
            onConfirm = { amount ->
                vm.removeFunds(goal, amount)
                removeGoal = null
            }
        )
    }
}

// ── All-history bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllGoalHistorySheet(
    history: List<com.trackfinz.app.data.model.GoalHistoryEntity>,
    currency: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val s = LocalStrings.current
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s(Str.ALL_FUND_HISTORY), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    items(history) { h -> GoalFundHistoryRow(h = h, currency = currency, showGoalTitle = true) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Per-goal history bottom sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalHistorySheet(
    goal: GoalEntity,
    currency: String,
    vm: GoalViewModel,
    onDismiss: () -> Unit
) {
    val history by vm.getHistoryForGoal(goal.id).collectAsStateWithLifecycle(emptyList())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val s = LocalStrings.current
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(goal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        Text(s(Str.NO_FUND_HISTORY), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(history) { h -> GoalFundHistoryRow(h = h, currency = currency, showGoalTitle = false) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Shared history row ────────────────────────────────────────────────────────

@Composable
private fun GoalFundHistoryRow(
    h: com.trackfinz.app.data.model.GoalHistoryEntity,
    currency: String,
    showGoalTitle: Boolean
) {
    val s = LocalStrings.current
    val isAdded = h.action == GoalFundAction.ADDED
    val actionColor = if (isAdded) IncomeGreen else ExpenseRed

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
                    if (isAdded) s(Str.FUND_ADDED) else s(Str.FUND_REMOVED),
                    color = actionColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                if (showGoalTitle) {
                    Text(h.goalTitle, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                }
                Text(
                    "${formatCurrency(h.balanceBefore, currency)} → ${formatCurrency(h.balanceAfter, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCurrency(h.amount, currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = actionColor
            )
            Text(
                formatDate(h.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    currency: String,
    onContribute: () -> Unit,
    onRemove: () -> Unit,
    onHistory: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (goal.savedAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val animProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "goal_p")
    val s = LocalStrings.current

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.emoji, fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${formatCurrency(goal.savedAmount, currency)} / ${formatCurrency(goal.targetAmount, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                if (goal.isCompleted) {
                    Surface(color = IncomeGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text("✅ ${s(Str.GOAL_DONE)}", color = IncomeGreen, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                } else {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = Teal500)
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (goal.isCompleted) IncomeGreen else Emerald500,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                // History
                IconButton(onClick = onHistory, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.History, contentDescription = "History",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                if (!goal.isCompleted) {
                    // Remove funds
                    TextButton(onClick = onRemove, colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)) {
                        Text("- ${s(Str.REMOVE)}")
                    }
                    // Add funds
                    TextButton(onClick = onContribute) { Text("+ ${s(Str.ADD)}") }
                }
                // Delete
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun FundDialog(title: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text(s(Str.AMOUNT)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { amount.toDoubleOrNull()?.let { if (it > 0) onConfirm(it) } }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String) -> Unit) {
    var title  by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var emoji  by remember { mutableStateOf("🎯") }
    val emojis = listOf("🎯","✈️","🏠","🚗","💻","📱","🎓","💍","🛡️","🌴")
    val s = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(Str.NEW_GOAL), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text(s(Str.GOAL_TITLE)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it },
                    label = { Text(s(Str.TARGET_AMOUNT)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                Text(s(Str.PICK_EMOJI), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    emojis.forEach { e ->
                        FilterChip(selected = emoji == e, onClick = { emoji = e },
                            label = { Text(e, fontSize = 20.sp) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = target.toDoubleOrNull() ?: return@TextButton
                if (title.isNotBlank()) onConfirm(title, t, emoji)
            }) { Text(s(Str.CREATE)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(Str.CANCEL)) } }
    )
}
