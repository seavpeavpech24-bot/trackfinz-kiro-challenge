package com.trackfinz.app.utils

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.trackfinz.app.data.model.TransactionEntity
import com.trackfinz.app.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class AIInsight(
    val title: String,
    val message: String,
    val type: InsightType,
    val icon: String
)

enum class InsightType {
    SPENDING_INCREASE,
    SPENDING_DECREASE,
    SAVING_TIP,
    CATEGORY_ALERT,
    POSITIVE_HABIT,
    WARNING
}

object AIInsightsGenerator {

    private const val GEMINI_API_KEY = "AIzaSyBXgLHhek2M6rDD9sA77TQ9Tqva7W_jgAs"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 500
            }
        )
    }

    /**
     * Generate AI-powered spending insights
     */
    suspend fun generateInsights(
        transactions: List<TransactionEntity>,
        retryCount: Int = 2
    ): List<AIInsight> = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) return@withContext emptyList()

        val analysis = analyzeTransactions(transactions)
        var lastException: Exception? = null
        
        for (attempt in 0 until retryCount) {
            try {
                val prompt = buildPrompt(analysis)
                
                android.util.Log.d("AIInsightsGenerator", "Sending request to Gemini API (attempt ${attempt + 1})...")
                val response = generativeModel.generateContent(prompt)
                val insights = parseGeminiResponse(response.text ?: "")
                
                if (insights.isNotEmpty()) {
                    android.util.Log.d("AIInsightsGenerator", "Successfully generated ${insights.size} insights")
                    return@withContext insights
                }
            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("AIInsightsGenerator", "Error calling Gemini API (attempt ${attempt + 1})", e)
                
                val errorMessage = e.message ?: ""
                if (errorMessage.contains("503") || errorMessage.contains("high demand") || 
                    errorMessage.contains("UNAVAILABLE")) {
                    if (attempt < retryCount - 1) {
                        android.util.Log.d("AIInsightsGenerator", "High demand detected, waiting before retry...")
                        delay(2000L * (attempt + 1))
                        continue
                    }
                }
                
                if (attempt < retryCount - 1) {
                    delay(1000L)
                }
            }
        }
        
        // Fallback to rule-based insights
        android.util.Log.w("AIInsightsGenerator", "Using rule-based insights due to API issues")
        generateRuleBasedInsights(transactions)
    }

    /**
     * Analyze transaction data
     */
    private fun analyzeTransactions(transactions: List<TransactionEntity>): TransactionAnalysis {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val twoWeeksAgo = now - (14 * 24 * 60 * 60 * 1000L)
        val oneMonthAgo = now - (30 * 24 * 60 * 60 * 1000L)

        val thisWeek = transactions.filter { it.date >= oneWeekAgo && it.type == TransactionType.EXPENSE }
        val lastWeek = transactions.filter { it.date >= twoWeeksAgo && it.date < oneWeekAgo && it.type == TransactionType.EXPENSE }
        val thisMonth = transactions.filter { it.date >= oneMonthAgo && it.type == TransactionType.EXPENSE }

        val categorySpending = thisMonth.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val weekdaySpending = thisMonth.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.DAY_OF_WEEK) in 2..6 // Monday to Friday
        }.sumOf { it.amount }

        val weekendSpending = thisMonth.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.DAY_OF_WEEK) in listOf(1, 7) // Sunday, Saturday
        }.sumOf { it.amount }

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME && it.date >= oneMonthAgo }
            .sumOf { it.amount }

        return TransactionAnalysis(
            thisWeekTotal = thisWeek.sumOf { it.amount },
            lastWeekTotal = lastWeek.sumOf { it.amount },
            thisMonthTotal = thisMonth.sumOf { it.amount },
            topCategories = categorySpending.take(3),
            weekdaySpending = weekdaySpending,
            weekendSpending = weekendSpending,
            totalIncome = totalIncome,
            transactionCount = thisMonth.size,
            averageTransaction = if (thisMonth.isNotEmpty()) thisMonth.sumOf { it.amount } / thisMonth.size else 0.0
        )
    }

    /**
     * Build prompt for Gemini AI
     */
    private fun buildPrompt(analysis: TransactionAnalysis): String {
        val weekChange = if (analysis.lastWeekTotal > 0) {
            ((analysis.thisWeekTotal - analysis.lastWeekTotal) / analysis.lastWeekTotal * 100).toInt()
        } else 0

        return """
You are a friendly financial coach analyzing spending patterns. Generate 3-4 concise, actionable insights.

SPENDING DATA:
- This week: ${'$'}${String.format("%.2f", analysis.thisWeekTotal)}
- Last week: ${'$'}${String.format("%.2f", analysis.lastWeekTotal)}
- Week change: ${if (weekChange > 0) "+" else ""}$weekChange%
- This month: ${'$'}${String.format("%.2f", analysis.thisMonthTotal)}
- Top categories: ${analysis.topCategories.joinToString(", ") { "${it.first.name} (${'$'}${String.format("%.2f", it.second)})" }}
- Weekday spending: ${'$'}${String.format("%.2f", analysis.weekdaySpending)}
- Weekend spending: ${'$'}${String.format("%.2f", analysis.weekendSpending)}
- Monthly income: ${'$'}${String.format("%.2f", analysis.totalIncome)}
- Avg transaction: ${'$'}${String.format("%.2f", analysis.averageTransaction)}

RULES:
1. Each insight must be ONE sentence (max 15 words)
2. Be specific with numbers and percentages
3. Mix positive reinforcement with actionable tips
4. Use friendly, encouraging tone
5. Format: TYPE|TITLE|MESSAGE

TYPES: INCREASE, DECREASE, TIP, ALERT, POSITIVE, WARNING

EXAMPLES:
INCREASE|Spending Up|Food spending increased 18% this week.
POSITIVE|Great Habit|You save more money on weekdays.
TIP|Smart Saving|Try meal prepping to reduce food costs.
ALERT|Budget Watch|Entertainment spending is 40% of your budget.

Generate 3-4 insights now:
        """.trimIndent()
    }

    /**
     * Parse Gemini response into insights
     */
    private fun parseGeminiResponse(response: String): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        response.lines().forEach { line ->
            val parts = line.trim().split("|")
            if (parts.size == 3) {
                val type = when (parts[0].uppercase()) {
                    "INCREASE" -> InsightType.SPENDING_INCREASE
                    "DECREASE" -> InsightType.SPENDING_DECREASE
                    "TIP" -> InsightType.SAVING_TIP
                    "ALERT" -> InsightType.CATEGORY_ALERT
                    "POSITIVE" -> InsightType.POSITIVE_HABIT
                    "WARNING" -> InsightType.WARNING
                    else -> InsightType.SAVING_TIP
                }
                
                val icon = when (type) {
                    InsightType.SPENDING_INCREASE -> "📈"
                    InsightType.SPENDING_DECREASE -> "📉"
                    InsightType.SAVING_TIP -> "💡"
                    InsightType.CATEGORY_ALERT -> "⚠️"
                    InsightType.POSITIVE_HABIT -> "✨"
                    InsightType.WARNING -> "🚨"
                }
                
                insights.add(AIInsight(
                    title = parts[1].trim(),
                    message = parts[2].trim(),
                    type = type,
                    icon = icon
                ))
            }
        }
        
        return insights.ifEmpty { generateRuleBasedInsights(emptyList()) }
    }

    /**
     * Fallback rule-based insights
     */
    private fun generateRuleBasedInsights(transactions: List<TransactionEntity>): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        if (transactions.isEmpty()) {
            insights.add(AIInsight(
                title = "Start Tracking",
                message = "Add your first transaction to get personalized insights.",
                type = InsightType.SAVING_TIP,
                icon = "💡"
            ))
            return insights
        }

        val analysis = analyzeTransactions(transactions)
        
        // Week-over-week comparison
        if (analysis.lastWeekTotal > 0) {
            val change = ((analysis.thisWeekTotal - analysis.lastWeekTotal) / analysis.lastWeekTotal * 100).toInt()
            if (change > 10) {
                insights.add(AIInsight(
                    title = "Spending Up",
                    message = "Spending increased ${change}% this week.",
                    type = InsightType.SPENDING_INCREASE,
                    icon = "📈"
                ))
            } else if (change < -10) {
                insights.add(AIInsight(
                    title = "Great Progress",
                    message = "You reduced spending by ${-change}% this week!",
                    type = InsightType.POSITIVE_HABIT,
                    icon = "✨"
                ))
            }
        }

        // Weekday vs weekend
        if (analysis.weekdaySpending > 0 && analysis.weekendSpending > 0) {
            if (analysis.weekdaySpending < analysis.weekendSpending) {
                insights.add(AIInsight(
                    title = "Weekday Saver",
                    message = "You spend less on weekdays. Keep it up!",
                    type = InsightType.POSITIVE_HABIT,
                    icon = "✨"
                ))
            }
        }

        // Top category alert
        if (analysis.topCategories.isNotEmpty()) {
            val topCategory = analysis.topCategories.first()
            val percentage = (topCategory.second / analysis.thisMonthTotal * 100).toInt()
            if (percentage > 40) {
                insights.add(AIInsight(
                    title = "Category Alert",
                    message = "${topCategory.first.name} is ${percentage}% of your spending.",
                    type = InsightType.CATEGORY_ALERT,
                    icon = "⚠️"
                ))
            }
        }

        // Savings tip
        insights.add(AIInsight(
            title = "Smart Tip",
            message = "Review your subscriptions to find hidden savings.",
            type = InsightType.SAVING_TIP,
            icon = "💡"
        ))

        return insights.take(4)
    }

    private data class TransactionAnalysis(
        val thisWeekTotal: Double,
        val lastWeekTotal: Double,
        val thisMonthTotal: Double,
        val topCategories: List<Pair<com.trackfinz.app.data.model.TransactionCategory, Double>>,
        val weekdaySpending: Double,
        val weekendSpending: Double,
        val totalIncome: Double,
        val transactionCount: Int,
        val averageTransaction: Double
    )
}
