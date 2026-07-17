package com.trackfinz.app.utils

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.trackfinz.app.data.model.BudgetEntity
import com.trackfinz.app.data.model.GoalEntity
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.model.TransactionEntity
import com.trackfinz.app.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ── Data models ───────────────────────────────────────────────────────────────

enum class HealthLabel { EXCELLENT, GOOD, FAIR, NEEDS_IMPROVEMENT }

enum class RecommendationType {
    POSITIVE_HABIT,
    OVER_BUDGET,
    TREND_ALERT,
    MISSING_BUDGET,
    LOW_SAVINGS,
    GOAL_AT_RISK,
    ENGAGEMENT
}

data class CategorySummary(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Double,          // % of total expenses
    val priorAmount: Double,         // prior-month amount for same category
    val deltaPercent: Double,        // MoM % change (positive = increased spending)
    val budgetLimit: Double?,        // null if no budget set
    val budgetPercent: Double?       // amount / limit * 100, null if no budget
)

data class MonthlyRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val actionLabel: String? = null,
    val actionCategory: TransactionCategory? = null, // for MISSING_BUDGET apply action
    val suggestedBudgetAmount: Double? = null
)

data class MonthlyAnalysis(
    val income: Double,
    val expenses: Double,
    val netBalance: Double,
    val savingsRate: Double,
    val priorIncome: Double,
    val priorExpenses: Double,
    val priorSavingsRate: Double,
    val healthScore: Int,
    val healthLabel: HealthLabel,
    val topCategories: List<CategorySummary>,
    val budgetedCategories: List<CategorySummary>,
    val goals: List<GoalEntity>,
    val recommendations: List<MonthlyRecommendation>,
    val narrative: String,
    val isNarrativeFallback: Boolean
)

// ── Main analyzer object ──────────────────────────────────────────────────────

object AIReportAnalyzer {

    private const val TAG = "AIReportAnalyzer"
    private const val GEMINI_API_KEY = "AIzaSyBXgLHhek2M6rDD9sA77TQ9Tqva7W_jgAs"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.6f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 600
            }
        )
    }

    // ── Public entry point ────────────────────────────────────────────────────

    suspend fun analyze(
        month: Int,
        year: Int,
        allTransactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        currency: String,
        languageName: String = "English"
    ): MonthlyAnalysis = withContext(Dispatchers.Default) {

        val monthStart = monthStartMillis(month, year)
        val monthEnd   = monthEndMillis(month, year)

        val (priorMonth, priorYear) = priorMonthYear(month, year)
        val priorStart = monthStartMillis(priorMonth, priorYear)
        val priorEnd   = monthEndMillis(priorMonth, priorYear)

        // Filter transactions
        val current = allTransactions.filter { it.date in monthStart..monthEnd }
        val prior   = allTransactions.filter { it.date in priorStart..priorEnd }

        val income    = current.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenses  = current.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val net       = income - expenses
        val savingsRate = if (income > 0.0) ((income - expenses) / income * 100.0) else 0.0

        val priorIncome   = prior.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val priorExpenses = prior.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val priorSavingsRate = if (priorIncome > 0.0) ((priorIncome - priorExpenses) / priorIncome * 100.0) else 0.0

        // Category spending
        val categoryMap = current.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val priorCategoryMap = prior.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val budgetMap = budgets.associateBy { it.category }

        val topCategories = categoryMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (cat, amount) ->
                val prior = priorCategoryMap[cat] ?: 0.0
                val delta = if (prior > 0.0) ((amount - prior) / prior * 100.0) else 0.0
                val budgetLimit = budgetMap[cat]?.limit
                CategorySummary(
                    category       = cat,
                    amount         = amount,
                    percentage     = if (expenses > 0.0) amount / expenses * 100.0 else 0.0,
                    priorAmount    = prior,
                    deltaPercent   = delta,
                    budgetLimit    = budgetLimit,
                    budgetPercent  = budgetLimit?.let { if (it > 0) amount / it * 100.0 else null }
                )
            }

        // Budgeted categories (for budget compliance section)
        val budgetedCategories = budgets.map { budget ->
            val spent = categoryMap[budget.category] ?: 0.0
            val prior = priorCategoryMap[budget.category] ?: 0.0
            val delta = if (prior > 0.0) ((spent - prior) / prior * 100.0) else 0.0
            CategorySummary(
                category      = budget.category,
                amount        = spent,
                percentage    = if (expenses > 0.0) spent / expenses * 100.0 else 0.0,
                priorAmount   = prior,
                deltaPercent  = delta,
                budgetLimit   = budget.limit,
                budgetPercent = if (budget.limit > 0) spent / budget.limit * 100.0 else null
            )
        }

        // Health score
        val healthScore = computeHealthScore(savingsRate, budgetedCategories, goals)
        val healthLabel = labelForScore(healthScore)

        // Recommendations
        val recommendations = buildRecommendations(
            currentMonth     = month,
            categoryMap      = categoryMap,
            priorCategoryMap = priorCategoryMap,
            budgetMap        = budgetMap,
            savingsRate      = savingsRate,
            goals            = goals,
            currentTxCount   = current.size,
            allTransactions  = allTransactions
        )

        // AI narrative (with fallback)
        val (narrative, isFallback) = withContext(Dispatchers.IO) {
            generateNarrative(
                month          = month,
                year           = year,
                income         = income,
                expenses       = expenses,
                savingsRate    = savingsRate,
                topCategories  = topCategories,
                healthScore    = healthScore,
                currency       = currency,
                languageName   = languageName
            )
        }

        MonthlyAnalysis(
            income              = income,
            expenses            = expenses,
            netBalance          = net,
            savingsRate         = savingsRate,
            priorIncome         = priorIncome,
            priorExpenses       = priorExpenses,
            priorSavingsRate    = priorSavingsRate,
            healthScore         = healthScore,
            healthLabel         = healthLabel,
            topCategories       = topCategories,
            budgetedCategories  = budgetedCategories,
            goals               = goals,
            recommendations     = recommendations,
            narrative           = narrative,
            isNarrativeFallback = isFallback
        )
    }

    // ── Health score ──────────────────────────────────────────────────────────

    fun computeHealthScore(
        savingsRate: Double,
        budgetedCategories: List<CategorySummary>,
        goals: List<GoalEntity>
    ): Int {
        val savingsScore = (savingsRate / 20.0).coerceIn(0.0, 1.0) * 100.0

        val budgetScore = if (budgetedCategories.isEmpty()) 50.0 else {
            val under = budgetedCategories.count { (it.budgetPercent ?: 0.0) <= 100.0 }
            under.toDouble() / budgetedCategories.size * 100.0
        }

        val goalScore = if (goals.isEmpty()) 50.0 else {
            goals.map { g ->
                if (g.targetAmount > 0) (g.savedAmount / g.targetAmount).coerceIn(0.0, 1.0) else 0.0
            }.average() * 100.0
        }

        return (savingsScore * 0.40 + budgetScore * 0.35 + goalScore * 0.25)
            .toInt().coerceIn(0, 100)
    }

    fun labelForScore(score: Int): HealthLabel = when {
        score >= 80 -> HealthLabel.EXCELLENT
        score >= 60 -> HealthLabel.GOOD
        score >= 40 -> HealthLabel.FAIR
        else        -> HealthLabel.NEEDS_IMPROVEMENT
    }

    // ── Recommendation engine ─────────────────────────────────────────────────

    private fun buildRecommendations(
        currentMonth: Int,
        categoryMap: Map<TransactionCategory, Double>,
        priorCategoryMap: Map<TransactionCategory, Double>,
        budgetMap: Map<TransactionCategory, BudgetEntity>,
        savingsRate: Double,
        goals: List<GoalEntity>,
        currentTxCount: Int,
        allTransactions: List<TransactionEntity>
    ): List<MonthlyRecommendation> {
        val recs = mutableListOf<MonthlyRecommendation>()

        // R1: Over-budget categories (highest priority)
        categoryMap.forEach { (cat, spent) ->
            val budget = budgetMap[cat] ?: return@forEach
            if (spent > budget.limit) {
                val over = spent - budget.limit
                recs.add(MonthlyRecommendation(
                    type        = RecommendationType.OVER_BUDGET,
                    title       = "Over Budget: ${cat.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    description = "You spent ${formatAmount(spent)} on ${cat.name.lowercase()}, which is ${formatAmount(over)} over your ${formatAmount(budget.limit)} budget. Consider reducing spending in this category next month.",
                    actionLabel = "View Analytics"
                ))
            }
        }

        // R2: Trend alerts — category up ≥ 20% vs prior month
        categoryMap.forEach { (cat, current) ->
            val prior = priorCategoryMap[cat] ?: return@forEach
            if (prior > 0 && current > prior) {
                val delta = (current - prior) / prior * 100.0
                if (delta >= 20.0) {
                    recs.add(MonthlyRecommendation(
                        type        = RecommendationType.TREND_ALERT,
                        title       = "${cat.name.lowercase().replaceFirstChar { it.uppercase() }} Spending Up ${delta.toInt()}%",
                        description = "Your ${cat.name.lowercase()} spending is ${formatAmount(current)} this month — ${delta.toInt()}% higher than last month (${formatAmount(prior)}). Review your habits to avoid overspending.",
                        actionLabel = "View Analytics"
                    ))
                }
            }
        }

        // R3: Missing budget suggestions
        categoryMap.forEach { (cat, spent) ->
            if (!budgetMap.containsKey(cat) && spent > 0) {
                recs.add(MonthlyRecommendation(
                    type                 = RecommendationType.MISSING_BUDGET,
                    title                = "No Budget for ${cat.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    description          = "You spent ${formatAmount(spent)} on ${cat.name.lowercase()} this month but have no budget set. Setting one helps you stay on track.",
                    actionLabel          = "Set Budget: ${formatAmount(spent)}",
                    actionCategory       = cat,
                    suggestedBudgetAmount = spent
                ))
            }
        }

        // R4: Low savings rate
        if (savingsRate < 10.0 && savingsRate >= 0.0) {
            recs.add(MonthlyRecommendation(
                type        = RecommendationType.LOW_SAVINGS,
                title       = "Savings Rate Is Low",
                description = "Your current savings rate is ${String.format("%.1f", savingsRate)}%. Aim for at least 10–20% by reducing optional spending like entertainment and shopping.",
                actionLabel = "View Goals"
            ))
        }

        // R5: Goals at risk
        val now = System.currentTimeMillis()
        val sixtyDaysMs = 60L * 24 * 60 * 60 * 1000
        goals.forEach { goal ->
            val deadline = goal.deadline ?: return@forEach
            if (deadline > now && deadline - now <= sixtyDaysMs) {
                val progress = if (goal.targetAmount > 0) goal.savedAmount / goal.targetAmount else 0.0
                if (progress < 0.5) {
                    val shortfall = goal.targetAmount - goal.savedAmount
                    recs.add(MonthlyRecommendation(
                        type        = RecommendationType.GOAL_AT_RISK,
                        title       = "${goal.emoji} ${goal.title} Is at Risk",
                        description = "You're ${(progress * 100).toInt()}% toward your goal with less than 60 days left. You still need ${formatAmount(shortfall)} to reach your target.",
                        actionLabel = "Add to Goal"
                    ))
                }
            }
        }

        // R6: Engagement nudge — no transactions recently
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val recentCount = allTransactions.count { it.date >= sevenDaysAgo }
        if (recentCount == 0 && currentTxCount > 0) {
            recs.add(MonthlyRecommendation(
                type        = RecommendationType.ENGAGEMENT,
                title       = "No Recent Transactions",
                description = "You haven't logged any transactions in the past 7 days. Keep your records up-to-date for accurate insights.",
                actionLabel = "Add Transaction"
            ))
        }

        // R7: Positive habit — high savings rate
        if (savingsRate >= 25.0) {
            recs.add(MonthlyRecommendation(
                type        = RecommendationType.POSITIVE_HABIT,
                title       = "Outstanding Savings Rate! 🎉",
                description = "Your ${String.format("%.1f", savingsRate)}% savings rate this month is excellent. You're building strong financial habits. Keep it up!"
            ))
        }

        return recs.take(6)
    }

    // ── Gemini narrative ──────────────────────────────────────────────────────

    private suspend fun generateNarrative(
        month: Int,
        year: Int,
        income: Double,
        expenses: Double,
        savingsRate: Double,
        topCategories: List<CategorySummary>,
        healthScore: Int,
        currency: String,
        languageName: String,
        retryCount: Int = 2
    ): Pair<String, Boolean> {
        val topCatText = topCategories.take(3)
            .joinToString(", ") { "${it.category.name}: ${formatAmount(it.amount)}" }

        val prompt = """
You are a personal finance advisor writing a monthly summary.

DATA:
- Month: ${monthName(month)} $year
- Total income: $currency${String.format("%.2f", income)}
- Total expenses: $currency${String.format("%.2f", expenses)}
- Net balance: $currency${String.format("%.2f", income - expenses)}
- Savings rate: ${String.format("%.1f", savingsRate)}%
- Financial health score: $healthScore/100
- Top 3 expense categories: $topCatText

Write a warm, encouraging 2-paragraph financial summary in $languageName.
Paragraph 1: Overall performance (mention savings rate and net balance).
Paragraph 2: One specific, actionable improvement tip based on the data.
Keep the total response under 100 words. Use friendly, conversational language.
""".trimIndent()

        var lastException: Exception? = null
        for (attempt in 0 until retryCount) {
            try {
                Log.d(TAG, "Gemini call attempt ${attempt + 1}")
                val response = generativeModel.generateContent(prompt)
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    Log.d(TAG, "Gemini narrative generated successfully")
                    return Pair(text, false)
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Gemini error attempt ${attempt + 1}: ${e.message}")
                if (attempt < retryCount - 1) delay(2000L)
            }
        }

        Log.w(TAG, "Falling back to template narrative. Last error: ${lastException?.message}")
        return Pair(buildFallbackNarrative(month, year, income, expenses, savingsRate, topCategories, currency), true)
    }

    private fun buildFallbackNarrative(
        month: Int,
        year: Int,
        income: Double,
        expenses: Double,
        savingsRate: Double,
        topCategories: List<CategorySummary>,
        currency: String
    ): String {
        val net = income - expenses
        val netWord = if (net >= 0) "surplus" else "deficit"
        val topCat = topCategories.firstOrNull()

        val para1 = "In ${monthName(month)} $year, your total income was $currency${String.format("%.2f", income)} " +
            "and expenses were $currency${String.format("%.2f", expenses)}, " +
            "giving you a net $netWord of $currency${String.format("%.2f", Math.abs(net))} " +
            "and a savings rate of ${String.format("%.1f", savingsRate)}%."

        val para2 = if (topCat != null) {
            "Your top spending category was ${topCat.category.name.lowercase().replaceFirstChar { it.uppercase() }} " +
                "($currency${String.format("%.2f", topCat.amount)}). " +
                "Review this category regularly to find opportunities to save more each month."
        } else {
            "Track your spending categories consistently to identify patterns and discover opportunities to improve your savings rate."
        }

        return "$para1\n\n$para2"
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun priorMonthYear(month: Int, year: Int): Pair<Int, Int> =
        if (month == 1) Pair(12, year - 1) else Pair(month - 1, year)

    private fun formatAmount(amount: Double): String = String.format("$%.2f", amount)

    private fun monthName(month: Int): String = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ).getOrElse(month - 1) { "Month $month" }
}
