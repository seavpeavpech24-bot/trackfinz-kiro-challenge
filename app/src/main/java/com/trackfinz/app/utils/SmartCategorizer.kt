package com.trackfinz.app.utils

import android.content.Context
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Smart AI-powered transaction categorization system
 * Learns from user corrections to improve accuracy over time
 */
object SmartCategorizer {

    // Learning data stored in SharedPreferences
    private const val PREFS_NAME = "smart_categorizer_prefs"
    private const val KEY_LEARNED_PATTERNS = "learned_patterns"

    /**
     * Suggest category based on transaction details
     */
    suspend fun suggestCategory(
        context: Context,
        title: String,
        amount: Double,
        type: TransactionType
    ): TransactionCategory = withContext(Dispatchers.Default) {
        val titleLower = title.lowercase().trim()

        // First check learned patterns
        val learnedCategory = checkLearnedPatterns(context, titleLower)
        if (learnedCategory != null) {
            return@withContext learnedCategory
        }

        // Then use built-in AI rules
        return@withContext if (type == TransactionType.INCOME) {
            categorizeIncome(titleLower, amount)
        } else {
            categorizeExpense(titleLower, amount)
        }
    }

    /**
     * Learn from user correction
     */
    suspend fun learnFromCorrection(
        context: Context,
        title: String,
        correctCategory: TransactionCategory
    ) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val learned = prefs.getStringSet(KEY_LEARNED_PATTERNS, mutableSetOf()) ?: mutableSetOf()
        
        val pattern = "${title.lowercase().trim()}:${correctCategory.name}"
        val updated = learned.toMutableSet().apply { add(pattern) }
        
        prefs.edit().putStringSet(KEY_LEARNED_PATTERNS, updated).apply()
    }

    /**
     * Check if we've learned this pattern before
     */
    private fun checkLearnedPatterns(context: Context, title: String): TransactionCategory? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val learned = prefs.getStringSet(KEY_LEARNED_PATTERNS, emptySet()) ?: emptySet()

        // Exact match
        learned.forEach { pattern ->
            val (learnedTitle, categoryName) = pattern.split(":")
            if (title == learnedTitle) {
                return try {
                    TransactionCategory.valueOf(categoryName)
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Partial match (contains)
        learned.forEach { pattern ->
            val (learnedTitle, categoryName) = pattern.split(":")
            if (title.contains(learnedTitle) || learnedTitle.contains(title)) {
                return try {
                    TransactionCategory.valueOf(categoryName)
                } catch (e: Exception) {
                    null
                }
            }
        }

        return null
    }

    /**
     * Categorize income transactions
     */
    private fun categorizeIncome(title: String, amount: Double): TransactionCategory {
        return when {
            // Salary patterns
            title.matches(Regex(".*\\b(salary|wage|payroll|income|pay)\\b.*")) -> 
                TransactionCategory.SALARY

            // Freelance patterns
            title.matches(Regex(".*\\b(freelance|contract|gig|project|client|upwork|fiverr)\\b.*")) -> 
                TransactionCategory.FREELANCE

            // Investment patterns
            title.matches(Regex(".*\\b(dividend|interest|stock|investment|profit|return|crypto|bitcoin)\\b.*")) -> 
                TransactionCategory.INVESTMENT

            // Gift patterns
            title.matches(Regex(".*\\b(gift|present|bonus|reward|prize)\\b.*")) -> 
                TransactionCategory.GIFT_IN

            // Refund patterns
            title.matches(Regex(".*\\b(refund|return|reimbursement|cashback)\\b.*")) -> 
                TransactionCategory.REFUND

            // Large amounts likely salary
            amount > 1000 -> TransactionCategory.SALARY

            else -> TransactionCategory.OTHER
        }
    }

    /**
     * Categorize expense transactions with AI
     */
    private fun categorizeExpense(title: String, amount: Double): TransactionCategory {
        return when {
            // Food & Dining
            title.matches(Regex(".*\\b(restaurant|cafe|coffee|starbucks|mcdonald|kfc|pizza|burger|food|lunch|dinner|breakfast|eat|dining|brown|bistro|kitchen|grill)\\b.*")) -> 
                TransactionCategory.FOOD

            // Groceries
            title.matches(Regex(".*\\b(grocery|supermarket|market|walmart|target|costco|safeway|whole foods|trader joe|aldi|lidl|tesco|carrefour|fresh|mart)\\b.*")) -> 
                TransactionCategory.GROCERIES

            // Transportation
            title.matches(Regex(".*\\b(grab|uber|lyft|taxi|cab|transport|bus|train|metro|subway|parking|toll|ride)\\b.*")) -> 
                TransactionCategory.TRAVEL

            // Gas & Fuel
            title.matches(Regex(".*\\b(gas|fuel|petrol|shell|chevron|exxon|bp|mobil|station|pump)\\b.*")) -> 
                TransactionCategory.GAS

            // Shopping
            title.matches(Regex(".*\\b(amazon|ebay|shop|store|mall|retail|clothing|fashion|nike|adidas|zara|h&m|uniqlo)\\b.*")) -> 
                TransactionCategory.SHOPPING

            // Entertainment
            title.matches(Regex(".*\\b(cinema|movie|theater|netflix|spotify|youtube|game|gaming|steam|playstation|xbox|concert|show|ticket)\\b.*")) -> 
                TransactionCategory.ENTERTAINMENT

            // Bills & Utilities
            title.matches(Regex(".*\\b(electric|electricity|water|internet|phone|mobile|bill|utility|rent|mortgage|insurance|subscription)\\b.*")) -> 
                TransactionCategory.BILLS

            // Healthcare
            title.matches(Regex(".*\\b(pharmacy|hospital|clinic|doctor|medical|health|medicine|drug|cvs|walgreens|dental|dentist)\\b.*")) -> 
                TransactionCategory.HEALTHCARE

            // Amount-based heuristics
            amount > 500 -> TransactionCategory.BILLS // Large amounts likely bills
            amount < 10 -> TransactionCategory.FOOD   // Small amounts likely food/drinks

            else -> TransactionCategory.OTHER
        }
    }

    /**
     * Get confidence score for suggestion (0-100)
     */
    fun getConfidenceScore(
        context: Context,
        title: String,
        suggestedCategory: TransactionCategory
    ): Int {
        val titleLower = title.lowercase().trim()

        // High confidence if learned
        if (checkLearnedPatterns(context, titleLower) != null) {
            return 95
        }

        // Check keyword match strength
        val keywords = getKeywordsForCategory(suggestedCategory)
        val matchCount = keywords.count { titleLower.contains(it) }

        return when {
            matchCount >= 2 -> 90
            matchCount == 1 -> 75
            else -> 50
        }
    }

    /**
     * Get keywords for a category
     */
    private fun getKeywordsForCategory(category: TransactionCategory): List<String> {
        return when (category) {
            TransactionCategory.FOOD -> listOf("restaurant", "cafe", "coffee", "food", "eat", "dining")
            TransactionCategory.GROCERIES -> listOf("grocery", "supermarket", "market", "fresh", "mart")
            TransactionCategory.TRAVEL -> listOf("grab", "uber", "taxi", "transport", "ride")
            TransactionCategory.GAS -> listOf("gas", "fuel", "petrol", "shell", "station")
            TransactionCategory.SHOPPING -> listOf("shop", "store", "mall", "amazon", "retail")
            TransactionCategory.ENTERTAINMENT -> listOf("cinema", "movie", "netflix", "game", "concert")
            TransactionCategory.BILLS -> listOf("electric", "water", "internet", "bill", "rent")
            TransactionCategory.HEALTHCARE -> listOf("pharmacy", "hospital", "doctor", "medical", "health")
            TransactionCategory.SALARY -> listOf("salary", "wage", "payroll", "income")
            TransactionCategory.FREELANCE -> listOf("freelance", "contract", "client", "project")
            TransactionCategory.INVESTMENT -> listOf("dividend", "stock", "investment", "profit")
            TransactionCategory.GIFT_IN -> listOf("gift", "bonus", "reward", "prize")
            TransactionCategory.REFUND -> listOf("refund", "return", "cashback")
            else -> emptyList()
        }
    }

    /**
     * Clear all learned patterns (for testing or reset)
     */
    suspend fun clearLearnedPatterns(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LEARNED_PATTERNS).apply()
    }

    /**
     * Get statistics about learned patterns
     */
    fun getLearnedPatternsCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val learned = prefs.getStringSet(KEY_LEARNED_PATTERNS, emptySet()) ?: emptySet()
        return learned.size
    }
}
