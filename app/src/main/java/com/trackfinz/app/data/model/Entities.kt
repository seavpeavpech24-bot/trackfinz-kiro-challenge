package com.trackfinz.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trackfinz.app.i18n.AppLanguage
import com.trackfinz.app.i18n.translate

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val pin: String,
    val currency: String = "USD",
    val biometricEnabled: Boolean = false,
    val notifyOverBudget: Boolean = true,
    val notifyLargeExpense: Boolean = true,
    val largeExpenseThreshold: Double = 100.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val receiptImagePath: String? = null,
    val merchantName: String? = null,
    val scannedItems: String? = null // JSON string of items
)

enum class TransactionType { INCOME, EXPENSE }

enum class TransactionCategory(val labelKey: String, val emoji: String, val forIncome: Boolean, val forExpense: Boolean) {
    // Income
    SALARY        ("cat_salary",       "💼", true,  false),
    FREELANCE     ("cat_freelance",    "💻", true,  false),
    INVESTMENT    ("cat_investment",   "📈", true,  false),
    GIFT_IN       ("cat_gift_in",      "🎁", true,  false),
    REFUND        ("cat_refund",       "↩️", true,  false),
    // Expense
    FOOD          ("cat_food",         "🍔", false, true),
    GROCERIES     ("cat_groceries",    "🛒", false, true),
    SHOPPING      ("cat_shopping",     "🛍️", false, true),
    TRAVEL        ("cat_travel",       "✈️", false, true),
    GAS           ("cat_gas",          "⛽", false, true),
    ENTERTAINMENT ("cat_entertainment","🎬", false, true),
    BILLS         ("cat_bills",        "💡", false, true),
    HEALTHCARE    ("cat_healthcare",   "🏥", false, true),
    // Both
    OTHER         ("cat_other",        "📦", true,  true);

    /** Get translated label for this category */
    fun getLabel(language: AppLanguage): String = translate(labelKey, language)

    companion object {
        fun incomeCategories() = values().filter { it.forIncome }
        fun expenseCategories() = values().filter { it.forExpense }
    }
}

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: TransactionCategory,
    val limit: Double,
    val month: Int,
    val year: Int
)

enum class BudgetHistoryAction { CREATED, UPDATED, DELETED }

@Entity(tableName = "budget_history")
data class BudgetHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val budgetId: Int,
    val category: TransactionCategory,
    val oldLimit: Double?,
    val newLimit: Double?,
    val month: Int,
    val year: Int,
    val action: BudgetHistoryAction,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val emoji: String = "🎯",
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

enum class GoalFundAction { ADDED, REMOVED }

@Entity(tableName = "goal_history")
data class GoalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val goalTitle: String,
    val amount: Double,
    val action: GoalFundAction,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BillFrequency { ONCE, WEEKLY, MONTHLY, YEARLY }

@Entity(tableName = "bill_reminders")
data class BillReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val dueDateMillis: Long,          // next due date (epoch ms)
    val reminderTimeMillis: Long,     // exact time to fire notification (epoch ms)
    val frequency: BillFrequency = BillFrequency.MONTHLY,
    val category: TransactionCategory = TransactionCategory.BILLS,
    val note: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
