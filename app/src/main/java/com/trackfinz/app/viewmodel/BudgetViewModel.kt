package com.trackfinz.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackfinz.app.data.model.BudgetEntity
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.repository.BudgetRepository
import com.trackfinz.app.data.repository.TransactionRepository
import com.trackfinz.app.utils.currentMonth
import com.trackfinz.app.utils.currentYear
import com.trackfinz.app.utils.monthEndMillis
import com.trackfinz.app.utils.monthStartMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetWithSpent(val budget: BudgetEntity, val spent: Double)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    private val txRepo: TransactionRepository
) : ViewModel() {

    private val _month = MutableStateFlow(currentMonth())
    private val _year  = MutableStateFlow(currentYear())

    val selectedMonth: StateFlow<Int> = _month.asStateFlow()
    val selectedYear: StateFlow<Int>  = _year.asStateFlow()

    val allHistory = budgetRepo.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetsWithSpent: StateFlow<List<BudgetWithSpent>> =
        combine(_month, _year) { m, y -> m to y }
            .flatMapLatest { (m, y) ->
                budgetRepo.getForMonth(m, y).map { budgets ->
                    val from = monthStartMillis(m, y)
                    val to   = monthEndMillis(m, y)
                    budgets.map { b ->
                        BudgetWithSpent(b, txRepo.spentInCategory(b.category, from, to))
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMonth(month: Int, year: Int) {
        _month.value = month
        _year.value  = year
    }

    fun addBudget(category: TransactionCategory, limit: Double) = viewModelScope.launch {
        budgetRepo.add(BudgetEntity(category = category, limit = limit, month = _month.value, year = _year.value))
    }

    fun applyRecommendedBudgets(recommendations: List<Pair<TransactionCategory, Double>>) = viewModelScope.launch {
        val month = currentMonth()
        val year = currentYear()
        recommendations.forEach { (category, limit) ->
            budgetRepo.upsert(BudgetEntity(category = category, limit = limit, month = month, year = year))
        }
    }

    fun updateBudget(budget: BudgetEntity, newLimit: Double) = viewModelScope.launch {
        budgetRepo.update(budget.copy(limit = newLimit), oldLimit = budget.limit)
    }

    fun deleteBudget(budget: BudgetEntity) = viewModelScope.launch { budgetRepo.delete(budget) }
}
