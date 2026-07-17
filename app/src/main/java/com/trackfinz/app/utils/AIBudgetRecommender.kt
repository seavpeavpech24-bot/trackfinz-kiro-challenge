package com.trackfinz.app.utils

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.model.TransactionEntity
import com.trackfinz.app.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

data class BudgetRecommendation(
    val category: TransactionCategory,
    val recommendedAmount: Double,
    val currentSpending: Double,
    val reasoning: String,
    val priority: BudgetPriority
)

enum class BudgetPriority {
    ESSENTIAL,    // Food, Bills, Healthcare
    IMPORTANT,    // Transportation, Groceries
    FLEXIBLE      // Entertainment, Shopping
}

object AIBudgetRecommender {

    private const val GEMINI_API_KEY = "AIzaSyBXgLHhek2M6rDD9sA77TQ9Tqva7W_jgAs"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.5f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 800
            }
        )
    }

    suspend fun generateBudgetRecommendations(
        transactions: List<TransactionEntity>,
        countryCode: String = "US",
        retryCount: Int = 2
    ): List<BudgetRecommendation> = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext getDefaultRecommendations(countryCode)
        }

        val analysis = analyzeFinancialData(transactions)
        var lastException: Exception? = null
        
        for (attempt in 0 until retryCount) {
            try {
                val prompt = buildBudgetPrompt(analysis, countryCode)
                
                android.util.Log.d("AIBudgetRecommender", "Sending request to Gemini API (attempt ${attempt + 1})...")
                val response = generativeModel.generateContent(prompt)
                val recommendations = parseGeminiResponse(response.text ?: "", analysis)
                
                if (recommendations.isNotEmpty()) {
                    android.util.Log.d("AIBudgetRecommender", "Successfully generated ${recommendations.size} recommendations")
                    return@withContext recommendations
                }
            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("AIBudgetRecommender", "Error calling Gemini API (attempt ${attempt + 1})", e)
                
                val errorMessage = e.message ?: ""
                if (errorMessage.contains("503") || errorMessage.contains("high demand") || 
                    errorMessage.contains("UNAVAILABLE")) {
                    if (attempt < retryCount - 1) {
                        android.util.Log.d("AIBudgetRecommender", "High demand detected, waiting before retry...")
                        delay(2000L * (attempt + 1))
                        continue
                    }
                }
                
                if (attempt < retryCount - 1) {
                    delay(1000L)
                }
            }
        }
        
        // Fallback to rule-based recommendations
        android.util.Log.w("AIBudgetRecommender", "Using rule-based recommendations due to API issues")
        getRuleBasedRecommendations(analysis, countryCode)
    }

    private fun analyzeFinancialData(transactions: List<TransactionEntity>): FinancialAnalysis {
        val now = System.currentTimeMillis()
        val oneMonthAgo = now - (30 * 24 * 60 * 60 * 1000L)
        val threeMonthsAgo = now - (90 * 24 * 60 * 60 * 1000L)

        val recentTransactions = transactions.filter { it.date >= oneMonthAgo }
        val historicalTransactions = transactions.filter { it.date >= threeMonthsAgo }

        val monthlyIncome = recentTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val monthlyExpenses = recentTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val categorySpending = recentTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val historicalCategoryAvg = historicalTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } / 3.0 }

        return FinancialAnalysis(
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            categorySpending = categorySpending,
            historicalCategoryAvg = historicalCategoryAvg,
            savingsRate = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpenses) / monthlyIncome * 100) else 0.0,
            transactionCount = recentTransactions.size
        )
    }

    private fun getCountryBenchmarks(countryCode: String): Map<String, String> {
        return when (countryCode.uppercase()) {
            "KH", "CAMBODIA" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "400-600",
                "foodCost" to "100-150",
                "rentCost" to "150-300",
                "transportCost" to "30-50",
                "context" to "Cambodia: Lower cost of living, street food culture, motorbike transportation common"
            )
            "US", "USA" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "3500-5000",
                "foodCost" to "300-500",
                "rentCost" to "1200-2000",
                "transportCost" to "200-400",
                "context" to "USA: Higher cost of living, car-dependent, diverse dining options"
            )
            "VN", "VIETNAM" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "300-500",
                "foodCost" to "80-120",
                "rentCost" to "150-250",
                "transportCost" to "20-40",
                "context" to "Vietnam: Affordable street food, motorbike culture, growing middle class"
            )
            "TH", "THAILAND" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "500-800",
                "foodCost" to "120-180",
                "rentCost" to "200-400",
                "transportCost" to "40-80",
                "context" to "Thailand: Moderate cost of living, excellent street food, tourism economy"
            )
            "LA", "LAOS" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "250-400",
                "foodCost" to "70-100",
                "rentCost" to "100-200",
                "transportCost" to "20-40",
                "context" to "Laos: Low cost of living, simple lifestyle, developing economy"
            )
            "CN", "CHINA" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "800-1500",
                "foodCost" to "150-250",
                "rentCost" to "300-600",
                "transportCost" to "50-100",
                "context" to "China: Varies by city, affordable food, efficient public transport"
            )
            "JP", "JAPAN" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "2500-4000",
                "foodCost" to "300-500",
                "rentCost" to "800-1500",
                "transportCost" to "100-200",
                "context" to "Japan: High cost of living, excellent public transport, quality food culture"
            )
            "KR", "KOREA" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "2000-3500",
                "foodCost" to "250-400",
                "rentCost" to "500-1000",
                "transportCost" to "80-150",
                "context" to "South Korea: Urban lifestyle, cafe culture, efficient metro systems"
            )
            "MY", "MALAYSIA" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "600-1000",
                "foodCost" to "150-250",
                "rentCost" to "200-400",
                "transportCost" to "50-100",
                "context" to "Malaysia: Moderate costs, diverse food scene, car ownership common"
            )
            "ID", "INDONESIA" -> mapOf(
                "currency" to "USD",
                "avgIncome" to "300-600",
                "foodCost" to "100-150",
                "rentCost" to "150-300",
                "transportCost" to "30-60",
                "context" to "Indonesia: Affordable living, street food culture, motorbike transportation"
            )
            else -> mapOf(
                "currency" to "USD",
                "avgIncome" to "2000-3000",
                "foodCost" to "200-350",
                "rentCost" to "600-1000",
                "transportCost" to "100-200",
                "context" to "Global average: Moderate cost of living"
            )
        }
    }

    private fun buildBudgetPrompt(analysis: FinancialAnalysis, countryCode: String): String {
        val benchmarks = getCountryBenchmarks(countryCode)
        val categories = TransactionCategory.expenseCategories()
        val categoryData = categories.joinToString("\n") { cat ->
            val current = analysis.categorySpending[cat] ?: 0.0
            val historical = analysis.historicalCategoryAvg[cat] ?: 0.0
            "- ${cat.name}: Current $$${String.format("%.2f", current)}, 3-month avg $$${String.format("%.2f", historical)}"
        }

        return """
You are a financial advisor creating personalized budget recommendations.

FINANCIAL DATA:
- Monthly Income: $$${String.format("%.2f", analysis.monthlyIncome)}
- Monthly Expenses: $$${String.format("%.2f", analysis.monthlyExpenses)}
- Current Savings Rate: ${String.format("%.1f", analysis.savingsRate)}%

CATEGORY SPENDING:
$categoryData

COUNTRY CONTEXT (${countryCode.uppercase()}):
${benchmarks["context"]}
- Average Income: ${benchmarks["currency"]} ${benchmarks["avgIncome"]}
- Typical Food Cost: ${benchmarks["currency"]} ${benchmarks["foodCost"]}
- Typical Rent: ${benchmarks["currency"]} ${benchmarks["rentCost"]}
- Typical Transport: ${benchmarks["currency"]} ${benchmarks["transportCost"]}

RULES:
1. Recommend budgets for ALL expense categories (FOOD, GROCERIES, TRAVEL, GAS, SHOPPING, ENTERTAINMENT, BILLS, HEALTHCARE, OTHER)
2. Total recommended budgets should be 70-80% of monthly income (leave room for savings)
3. Base recommendations on: current spending, historical average, income level, AND country-specific costs
4. Essential categories (FOOD, GROCERIES, BILLS, HEALTHCARE) get priority
5. Adjust recommendations based on local cost of living
6. Each recommendation must include a brief reason (max 10 words)
7. Format: CATEGORY|AMOUNT|REASON

EXAMPLES:
FOOD|150.00|Based on your dining habits
GROCERIES|200.00|Essential household needs
ENTERTAINMENT|40.00|Balanced leisure spending
BILLS|300.00|Cover utilities and subscriptions
HEALTHCARE|100.00|Medical and wellness needs
TRAVEL|80.00|Commute and occasional trips
GAS|60.00|Fuel for regular driving
SHOPPING|100.00|Clothing and personal items
OTHER|50.00|Miscellaneous expenses

Generate budget recommendations now (one per line):
        """.trimIndent()
    }

    private fun parseGeminiResponse(
        response: String,
        analysis: FinancialAnalysis
    ): List<BudgetRecommendation> {
        val recommendations = mutableListOf<BudgetRecommendation>()
        
        response.lines().forEach { line ->
            val parts = line.trim().split("|")
            if (parts.size == 3) {
                try {
                    val category = TransactionCategory.valueOf(parts[0].trim().uppercase())
                    val amount = parts[1].trim().toDoubleOrNull() ?: 0.0
                    val reason = parts[2].trim()
                    
                    if (amount > 0 && category in TransactionCategory.expenseCategories()) {
                        recommendations.add(BudgetRecommendation(
                            category = category,
                            recommendedAmount = amount,
                            currentSpending = analysis.categorySpending[category] ?: 0.0,
                            reasoning = reason,
                            priority = getPriority(category)
                        ))
                    }
                } catch (e: Exception) {
                    // Skip invalid lines
                }
            }
        }
        
        return recommendations.sortedByDescending { it.priority }
    }

    private fun getRuleBasedRecommendations(analysis: FinancialAnalysis, countryCode: String): List<BudgetRecommendation> {
        val recommendations = mutableListOf<BudgetRecommendation>()
        val income = analysis.monthlyIncome
        
        val baseAmount = if (income > 0) income * 0.75 else analysis.monthlyExpenses * 1.2
        
        val budgetRules = mapOf(
            TransactionCategory.FOOD to 0.15,
            TransactionCategory.GROCERIES to 0.20,
            TransactionCategory.BILLS to 0.25,
            TransactionCategory.HEALTHCARE to 0.08,
            TransactionCategory.TRAVEL to 0.10,
            TransactionCategory.GAS to 0.07,
            TransactionCategory.SHOPPING to 0.08,
            TransactionCategory.ENTERTAINMENT to 0.05,
            TransactionCategory.OTHER to 0.02
        )

        budgetRules.forEach { (category, percentage) ->
            val recommended = baseAmount * percentage
            val current = analysis.categorySpending[category] ?: 0.0
            val historical = analysis.historicalCategoryAvg[category] ?: 0.0
            
            val adjusted = when {
                historical > recommended * 1.5 -> (recommended + historical) / 2
                historical > 0 -> (recommended * 0.7 + historical * 0.3)
                else -> recommended
            }
            
            recommendations.add(BudgetRecommendation(
                category = category,
                recommendedAmount = adjusted,
                currentSpending = current,
                reasoning = getReasoning(category, adjusted, current),
                priority = getPriority(category)
            ))
        }
        
        return recommendations.sortedByDescending { it.priority }
    }

    private fun getDefaultRecommendations(countryCode: String): List<BudgetRecommendation> {
        val benchmarks = getCountryBenchmarks(countryCode)
        val avgIncome = benchmarks["avgIncome"]?.split("-")?.get(1)?.toDoubleOrNull() ?: 2000.0
        
        val defaultBudgets = mapOf(
            TransactionCategory.FOOD to avgIncome * 0.15,
            TransactionCategory.GROCERIES to avgIncome * 0.20,
            TransactionCategory.BILLS to avgIncome * 0.25,
            TransactionCategory.HEALTHCARE to avgIncome * 0.08,
            TransactionCategory.TRAVEL to avgIncome * 0.10,
            TransactionCategory.GAS to avgIncome * 0.07,
            TransactionCategory.SHOPPING to avgIncome * 0.08,
            TransactionCategory.ENTERTAINMENT to avgIncome * 0.05,
            TransactionCategory.OTHER to avgIncome * 0.02
        )

        return defaultBudgets.map { (category, amount) ->
            BudgetRecommendation(
                category = category,
                recommendedAmount = amount,
                currentSpending = 0.0,
                reasoning = "Starter budget for ${category.name.lowercase()}",
                priority = getPriority(category)
            )
        }.sortedByDescending { it.priority }
    }

    private fun getPriority(category: TransactionCategory): BudgetPriority {
        return when (category) {
            TransactionCategory.FOOD,
            TransactionCategory.GROCERIES,
            TransactionCategory.BILLS,
            TransactionCategory.HEALTHCARE -> BudgetPriority.ESSENTIAL
            
            TransactionCategory.TRAVEL,
            TransactionCategory.GAS -> BudgetPriority.IMPORTANT
            
            else -> BudgetPriority.FLEXIBLE
        }
    }

    private fun getReasoning(category: TransactionCategory, recommended: Double, current: Double): String {
        return when {
            current == 0.0 -> "Recommended starting budget"
            current > recommended * 1.3 -> "Reduce spending in this category"
            current < recommended * 0.5 -> "Room to increase if needed"
            else -> "Maintain current spending level"
        }
    }

    private data class FinancialAnalysis(
        val monthlyIncome: Double,
        val monthlyExpenses: Double,
        val categorySpending: Map<TransactionCategory, Double>,
        val historicalCategoryAvg: Map<TransactionCategory, Double>,
        val savingsRate: Double,
        val transactionCount: Int
    )
}
