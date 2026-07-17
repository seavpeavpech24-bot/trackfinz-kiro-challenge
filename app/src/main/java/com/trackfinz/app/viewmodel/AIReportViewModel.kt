package com.trackfinz.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackfinz.app.data.datastore.UserPreferences
import com.trackfinz.app.data.model.BudgetEntity
import com.trackfinz.app.data.model.GoalEntity
import com.trackfinz.app.data.model.MonthlyReportEntity
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.repository.BudgetRepository
import com.trackfinz.app.data.repository.GoalRepository
import com.trackfinz.app.data.repository.MonthlyReportRepository
import com.trackfinz.app.data.repository.TransactionRepository
import com.trackfinz.app.i18n.AppLanguage
import com.trackfinz.app.utils.AIReportAnalyzer
import com.trackfinz.app.utils.CategorySummary
import com.trackfinz.app.utils.HealthLabel
import com.trackfinz.app.utils.MonthlyAnalysis
import com.trackfinz.app.utils.MonthlyRecommendation
import com.trackfinz.app.utils.currentMonth
import com.trackfinz.app.utils.currentYear
import com.trackfinz.app.utils.monthEndMillis
import com.trackfinz.app.utils.monthStartMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ───────────────────────────────────────────────────────────────────

sealed class AIReportUiState {
    object Loading : AIReportUiState()
    data class Ready(
        val month: Int,
        val year: Int,
        val analysis: MonthlyAnalysis,
        val currency: String,
        val isRegenerating: Boolean = false
    ) : AIReportUiState()
    data class Error(val message: String) : AIReportUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AIReportViewModel @Inject constructor(
    private val txRepo: TransactionRepository,
    private val budgetRepo: BudgetRepository,
    private val goalRepo: GoalRepository,
    private val reportRepo: MonthlyReportRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIReportUiState>(AIReportUiState.Loading)
    val uiState: StateFlow<AIReportUiState> = _uiState.asStateFlow()

    private var selectedMonth: Int = currentMonth()
    private var selectedYear: Int  = currentYear()

    init {
        loadReport()
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun selectMonth(month: Int, year: Int) {
        selectedMonth = month
        selectedYear  = year
        loadReport()
    }

    fun regenerate() {
        val current = _uiState.value as? AIReportUiState.Ready ?: return
        _uiState.value = current.copy(isRegenerating = true)
        loadReport(forceRefresh = true)
    }

    fun applyBudgetRecommendation(category: TransactionCategory, amount: Double) {
        viewModelScope.launch {
            try {
                val month = selectedMonth
                val year  = selectedYear
                budgetRepo.upsert(
                    com.trackfinz.app.data.model.BudgetEntity(
                        category = category,
                        limit    = amount,
                        month    = month,
                        year     = year
                    )
                )
                // Refresh the report data to reflect the new budget
                loadReport(forceRefresh = false)
            } catch (e: Exception) {
                android.util.Log.e("AIReportViewModel", "applyBudgetRecommendation failed", e)
            }
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun loadReport(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (_uiState.value !is AIReportUiState.Ready) {
                    _uiState.value = AIReportUiState.Loading
                }

                val currency     = prefs.currency.first()
                val languageCode = prefs.language.first()
                val language     = AppLanguage.fromCode(languageCode)
                val languageName = language.displayName

                // Collect all data concurrently-style (sequential is fine on IO)
                val allTransactions = txRepo.allTransactions.first()

                val monthStart = monthStartMillis(selectedMonth, selectedYear)
                val monthEnd   = monthEndMillis(selectedMonth, selectedYear)
                val budgets    = budgetRepo.getForMonth(selectedMonth, selectedYear).first()
                val goals      = goalRepo.allGoals.first()

                // Try to use cached report if available and not forcing refresh
                val cached = if (!forceRefresh) {
                    reportRepo.getReportForMonth(selectedMonth, selectedYear)
                } else null

                val analysis: MonthlyAnalysis
                if (cached != null && !forceRefresh) {
                    // Use cached narrative but recompute live KPIs (budgets/goals may have changed)
                    analysis = AIReportAnalyzer.analyze(
                        month            = selectedMonth,
                        year             = selectedYear,
                        allTransactions  = allTransactions,
                        budgets          = budgets,
                        goals            = goals,
                        currency         = currency,
                        languageName     = languageName
                    ).copy(
                        narrative           = cached.narrative,
                        isNarrativeFallback = cached.isFallback
                    )
                } else {
                    analysis = AIReportAnalyzer.analyze(
                        month            = selectedMonth,
                        year             = selectedYear,
                        allTransactions  = allTransactions,
                        budgets          = budgets,
                        goals            = goals,
                        currency         = currency,
                        languageName     = languageName
                    )
                    // Persist the newly generated report
                    reportRepo.saveReport(
                        MonthlyReportEntity(
                            month        = selectedMonth,
                            year         = selectedYear,
                            narrative    = analysis.narrative,
                            isFallback   = analysis.isNarrativeFallback,
                            healthScore  = analysis.healthScore,
                            totalIncome  = analysis.income,
                            totalExpenses = analysis.expenses,
                            savingsRate  = analysis.savingsRate
                        )
                    )
                }

                _uiState.value = AIReportUiState.Ready(
                    month    = selectedMonth,
                    year     = selectedYear,
                    analysis = analysis,
                    currency = currency
                )
            } catch (e: Exception) {
                android.util.Log.e("AIReportViewModel", "loadReport failed", e)
                _uiState.value = AIReportUiState.Error(e.message ?: "Failed to generate report")
            }
        }
    }
}
