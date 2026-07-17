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

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

object AIFinancialAssistant {

    private const val GEMINI_API_KEY = "AIzaSyBXgLHhek2M6rDD9sA77TQ9Tqva7W_jgAs"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1000
            }
        )
    }

    /**
     * Ask AI a question about user's finances
     */
    suspend fun askQuestion(
        question: String,
        transactions: List<TransactionEntity>,
        currency: String = "USD",
        retryCount: Int = 2
    ): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        for (attempt in 0 until retryCount) {
            try {
                val financialContext = buildFinancialContext(transactions, currency)
                val prompt = buildPrompt(question, financialContext)
                
                android.util.Log.d("AIFinancialAssistant", "Sending request to Gemini API (attempt ${attempt + 1})...")
                val response = generativeModel.generateContent(prompt)
                android.util.Log.d("AIFinancialAssistant", "Response received: ${response.text}")
                return@withContext response.text ?: "I couldn't generate a response. Please try again."
            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("AIFinancialAssistant", "Error calling Gemini API (attempt ${attempt + 1})", e)
                
                // Check if it's a rate limit or high demand error
                val errorMessage = e.message ?: ""
                if (errorMessage.contains("503") || errorMessage.contains("high demand") || 
                    errorMessage.contains("UNAVAILABLE")) {
                    if (attempt < retryCount - 1) {
                        android.util.Log.d("AIFinancialAssistant", "High demand detected, waiting before retry...")
                        delay(2000L * (attempt + 1)) // Exponential backoff
                        continue
                    }
                    return@withContext "The AI service is experiencing high demand right now. Please try again in a few moments."
                }
                
                // For other errors, don't retry
                if (attempt < retryCount - 1) {
                    delay(1000L)
                }
            }
        }
        
        // If all retries failed
        val errorMsg = lastException?.message ?: "Unknown error"
        android.util.Log.e("AIFinancialAssistant", "All retry attempts failed: $errorMsg")
        
        when {
            errorMsg.contains("network") || errorMsg.contains("connection") -> 
                "Unable to connect to the AI service. Please check your internet connection."
            errorMsg.contains("503") || errorMsg.contains("UNAVAILABLE") -> 
                "The AI service is temporarily unavailable due to high demand. Please try again later."
            errorMsg.contains("401") || errorMsg.contains("API key") -> 
                "There's an issue with the AI service configuration. Please contact support."
            else -> 
                "I'm having trouble processing your request right now. Please try again later."
        }
    }

    /**
     * Build financial context from transactions
     */
    private fun buildFinancialContext(transactions: List<TransactionEntity>, currency: String): String {
        if (transactions.isEmpty()) {
            return "No transaction data available yet."
        }

        val now = System.currentTimeMillis()
        val oneMonthAgo = now - (30 * 24 * 60 * 60 * 1000L)
        val threeMonthsAgo = now - (90 * 24 * 60 * 60 * 1000L)

        val recentTransactions = transactions.filter { it.date >= oneMonthAgo }
        val historicalTransactions = transactions.filter { it.date >= threeMonthsAgo }

        // Income & Expenses
        val monthlyIncome = recentTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        
        val monthlyExpenses = recentTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val savingsRate = if (monthlyIncome > 0) {
            ((monthlyIncome - monthlyExpenses) / monthlyIncome * 100)
        } else 0.0

        // Category breakdown
        val categorySpending = recentTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val topCategories = categorySpending.take(5)
            .joinToString(", ") { (cat, amount) -> 
                "${cat.name}: $currency${String.format("%.2f", amount)}" 
            }

        // Spending trends
        val lastMonthExpenses = historicalTransactions
            .filter { it.date >= (now - 60 * 24 * 60 * 60 * 1000L) && it.date < oneMonthAgo }
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val spendingTrend = if (lastMonthExpenses > 0) {
            val change = ((monthlyExpenses - lastMonthExpenses) / lastMonthExpenses * 100)
            when {
                change > 10 -> "increasing significantly (+${String.format("%.1f", change)}%)"
                change > 0 -> "slightly increasing (+${String.format("%.1f", change)}%)"
                change < -10 -> "decreasing significantly (${String.format("%.1f", change)}%)"
                change < 0 -> "slightly decreasing (${String.format("%.1f", change)}%)"
                else -> "stable"
            }
        } else "no previous data"

        // Average transaction
        val avgTransaction = if (recentTransactions.isNotEmpty()) {
            recentTransactions.sumOf { it.amount } / recentTransactions.size
        } else 0.0

        // Most expensive transaction
        val mostExpensive = recentTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .maxByOrNull { it.amount }

        val dateFormat = SimpleDateFormat("MMM dd", Locale.US)

        return """
FINANCIAL SUMMARY (Last 30 days):
- Monthly Income: $currency${String.format("%.2f", monthlyIncome)}
- Monthly Expenses: $currency${String.format("%.2f", monthlyExpenses)}
- Net Balance: $currency${String.format("%.2f", monthlyIncome - monthlyExpenses)}
- Savings Rate: ${String.format("%.1f", savingsRate)}%
- Total Transactions: ${recentTransactions.size}
- Average Transaction: $currency${String.format("%.2f", avgTransaction)}

TOP SPENDING CATEGORIES:
$topCategories

SPENDING TREND:
Your spending is $spendingTrend compared to last month.

${mostExpensive?.let { 
    "LARGEST EXPENSE:\n${it.title} - $currency${String.format("%.2f", it.amount)} on ${dateFormat.format(Date(it.date))}"
} ?: ""}

HISTORICAL DATA:
- 3-month total transactions: ${historicalTransactions.size}
- 3-month average spending: $currency${String.format("%.2f", historicalTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } / 3)}
        """.trimIndent()
    }

    /**
     * Build prompt for Gemini AI
     */
    private fun buildPrompt(question: String, financialContext: String): String {
        return """
You are a friendly and knowledgeable personal finance advisor. The user is asking you a question about their finances.

USER'S FINANCIAL DATA:
$financialContext

USER'S QUESTION:
"$question"

INSTRUCTIONS:
1. Answer the question directly and concisely (2-4 sentences max)
2. Use the financial data provided to give specific, personalized advice
3. Be encouraging and supportive
4. If the question is about spending, reference specific categories and amounts
5. If the question is about saving, provide actionable tips based on their data
6. If the data is insufficient, acknowledge it and give general advice
7. Use a warm, conversational tone like a helpful friend
8. Include specific numbers from their data when relevant
9. End with a brief actionable tip if appropriate

EXAMPLE GOOD RESPONSES:
- "You spend most on Food (${'$'}450) and Shopping (${'$'}320) this month. Consider meal prepping to reduce food costs by 20-30%."
- "Your savings rate is 15%, which is good! To save more, try reducing your Entertainment spending (${'$'}180) by setting a weekly budget."
- "Your spending increased 18% this month, mainly in Shopping. Review recent purchases and return what you don't need."

Now answer the user's question:
        """.trimIndent()
    }

    /**
     * Get suggested questions based on user's data
     */
    fun getSuggestedQuestions(transactions: List<TransactionEntity>): List<String> {
        if (transactions.isEmpty()) {
            return listOf(
                "How should I start tracking my finances?",
                "What's a good savings rate?",
                "How can I create a budget?"
            )
        }

        val hasIncome = transactions.any { it.type == TransactionType.INCOME }
        val hasExpenses = transactions.any { it.type == TransactionType.EXPENSE }

        return buildList {
            if (hasExpenses) {
                add("Where do I spend the most money?")
                add("How can I reduce my spending?")
                add("What category is increasing?")
            }
            if (hasIncome && hasExpenses) {
                add("Am I saving enough?")
                add("How can I save more money?")
            }
            if (transactions.size > 30) {
                add("What are my spending trends?")
                add("How does this month compare to last month?")
            }
            if (size < 3) {
                add("What's my biggest expense?")
                add("How much do I spend on average?")
            }
        }.take(6)
    }
}
