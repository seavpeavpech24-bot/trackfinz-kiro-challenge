package com.trackfinz.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackfinz.app.data.datastore.UserPreferences
import com.trackfinz.app.data.model.*
import com.trackfinz.app.data.repository.BudgetRepository
import com.trackfinz.app.data.repository.TransactionRepository
import com.trackfinz.app.utils.NotificationHelper
import com.trackfinz.app.utils.currentMonth
import com.trackfinz.app.utils.currentYear
import com.trackfinz.app.utils.monthEndMillis
import com.trackfinz.app.utils.monthStartMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repo: TransactionRepository,
    private val budgetRepo: BudgetRepository,
    private val prefs: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val allTransactions = repo.allTransactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recentTransactions = repo.recentTransactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val totalIncome  = repo.totalIncome.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)
    val totalExpense = repo.totalExpense.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val balance: StateFlow<Double> = combine(totalIncome, totalExpense) { inc, exp ->
        (inc ?: 0.0) - (exp ?: 0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { q -> if (q.isBlank()) repo.allTransactions else repo.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun addTransaction(tx: TransactionEntity) = viewModelScope.launch {
        repo.add(tx)
        if (tx.type == TransactionType.EXPENSE) {
            checkNotifications(tx)
        }
    }

    fun updateTransaction(tx: TransactionEntity) = viewModelScope.launch { repo.update(tx) }
    fun deleteTransaction(tx: TransactionEntity) = viewModelScope.launch { repo.delete(tx) }

    fun getById(id: Int): TransactionEntity? = allTransactions.value.find { it.id == id }

    val expenseByCategory: StateFlow<Map<TransactionCategory, Double>> =
        allTransactions.map { list ->
            list.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ── Analytics date range ──────────────────────────────────────────────────

    private val _analyticsRange = MutableStateFlow(0L to System.currentTimeMillis())

    fun setAnalyticsRange(from: Long, to: Long) {
        _analyticsRange.value = from to to
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val analyticsTransactions: StateFlow<List<TransactionEntity>> = _analyticsRange
        .flatMapLatest { (from, to) -> repo.getByDateRange(from, to) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val analyticsIncome: StateFlow<Double?> = _analyticsRange
        .flatMapLatest { (from, to) -> repo.totalIncomeInRange(from, to) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val analyticsExpense: StateFlow<Double?> = _analyticsRange
        .flatMapLatest { (from, to) -> repo.totalExpenseInRange(from, to) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun getTransactionsInRange(from: Long, to: Long) =
        repo.getByDateRange(from, to).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getIncomeInRange(from: Long, to: Long) =
        repo.totalIncomeInRange(from, to).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun getExpenseInRange(from: Long, to: Long) =
        repo.totalExpenseInRange(from, to).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Notification checks ───────────────────────────────────────────────────

    private suspend fun checkNotifications(tx: TransactionEntity) {
        val notifyLarge    = prefs.notifyLargeExpense.first()
        val notifyBudget   = prefs.notifyOverBudget.first()
        val threshold      = prefs.largeExpenseThreshold.first()
        val currency       = prefs.currency.first()
        val languageCode   = prefs.language.first()
        val language       = com.trackfinz.app.i18n.AppLanguage.fromCode(languageCode)
        val categoryName   = tx.category.getLabel(language)

        // 1. Large expense alert
        if (notifyLarge && tx.amount >= threshold) {
            NotificationHelper.postLargeExpenseAlert(
                context      = context,
                categoryName = categoryName,
                amount       = tx.amount,
                currency     = currency
            )
        }

        // 2. Over-budget alert — check if this expense pushed the category over its limit
        if (notifyBudget) {
            val month = currentMonth()
            val year  = currentYear()
            val from  = monthStartMillis(month, year)
            val to    = monthEndMillis(month, year)

            // Find a budget for this category in the current month
            val budgets = budgetRepo.getForMonth(month, year).first()
            val budget  = budgets.find { it.category == tx.category } ?: return

            val spent = repo.spentInCategory(tx.category, from, to)
            if (spent >= budget.limit) {
                NotificationHelper.postOverBudgetAlert(
                    context      = context,
                    categoryName = categoryName,
                    spent        = spent,
                    limit        = budget.limit,
                    currency     = currency
                )
            }
        }
    }
}
