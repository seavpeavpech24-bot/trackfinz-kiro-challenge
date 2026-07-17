package com.trackfinz.app.ui.screens.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackfinz.app.data.model.TransactionType
import com.trackfinz.app.navigation.NavRoutes
import com.trackfinz.app.ui.components.FinanceTextField
import com.trackfinz.app.ui.components.TrackFinzBottomBar
import com.trackfinz.app.ui.components.TransactionItem
import com.trackfinz.app.ui.theme.ExpenseRed
import com.trackfinz.app.ui.theme.IncomeGreen
import com.trackfinz.app.i18n.LocalStrings
import com.trackfinz.app.i18n.Str
import com.trackfinz.app.viewmodel.SettingsViewModel
import com.trackfinz.app.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigate: (String) -> Unit,
    onAdd: (String) -> Unit,
    onEdit: (Int) -> Unit,
    vm: TransactionViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val searchQuery   by vm.searchQuery.collectAsStateWithLifecycle()
    val currency      by settingsVm.currency.collectAsStateWithLifecycle()
    var selectedTab   by remember { mutableIntStateOf(0) }
    val s = LocalStrings.current

    val filtered = when (selectedTab) {
        1 -> searchResults.filter { it.type == TransactionType.INCOME }
        2 -> searchResults.filter { it.type == TransactionType.EXPENSE }
        else -> searchResults
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(s(Str.TRANSACTIONS)) })
        },
        bottomBar = {
            TrackFinzBottomBar(currentRoute = NavRoutes.TRANSACTIONS, onNavigate = onNavigate)
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Scan Receipt FAB
                SmallFloatingActionButton(
                    onClick = { onNavigate(NavRoutes.RECEIPT_SCANNER) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan Receipt", modifier = Modifier.size(18.dp))
                }
                // Add Income FAB
                SmallFloatingActionButton(
                    onClick = { onAdd("INCOME") },
                    containerColor = IncomeGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Income", modifier = Modifier.size(18.dp))
                }
                // Add Expense FAB
                FloatingActionButton(
                    onClick = { onAdd("EXPENSE") },
                    containerColor = ExpenseRed,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search
            FinanceTextField(
                value = searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                label = s(Str.SEARCH_TRANSACTIONS),
                leadingIcon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = when (selectedTab) {
                            1 -> IncomeGreen
                            2 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            ) {
                listOf(s(Str.ALL), s(Str.INCOME), s(Str.EXPENSE)).forEachIndexed { i, label ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(s(Str.NO_TRANSACTIONS_FOUND),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { tx ->
                        TransactionItem(
                            tx = tx,
                            currency = currency,
                            onEdit = { onEdit(tx.id) },
                            onDelete = { vm.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }
    }
}
