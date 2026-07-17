package com.trackfinz.app.data.database

import androidx.room.*
import com.trackfinz.app.data.model.*
import kotlinx.coroutines.flow.Flow

// ─── User DAO ─────────────────────────────────────────────────────────────────

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Update
    suspend fun update(user: UserEntity)
}

// ─── Transaction DAO ──────────────────────────────────────────────────────────

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Update
    suspend fun update(tx: TransactionEntity)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :cat ORDER BY date DESC")
    fun getByCategory(cat: TransactionCategory): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type AND date BETWEEN :from AND :to ORDER BY date DESC")
    fun getByTypeAndDateRange(type: TransactionType, from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    fun totalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun totalExpense(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND date BETWEEN :from AND :to")
    fun totalIncomeInRange(from: Long, to: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :from AND :to")
    fun totalExpenseInRange(from: Long, to: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = 'EXPENSE' AND category = :cat AND date BETWEEN :from AND :to
    """)
    suspend fun spentInCategory(cat: TransactionCategory, from: Long, to: Long): Double?

    @Query("SELECT * FROM transactions WHERE title LIKE '%' || :q || '%' ORDER BY date DESC")
    fun search(q: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int = 5): Flow<List<TransactionEntity>>
}

// ─── Budget DAO ───────────────────────────────────────────────────────────────

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category AND month = :month AND year = :year LIMIT 1")
    suspend fun getForCategoryAndMonth(category: TransactionCategory, month: Int, year: Int): BudgetEntity?
}

// ─── Budget History DAO ───────────────────────────────────────────────────────

@Dao
interface BudgetHistoryDao {
    @Insert
    suspend fun insert(h: BudgetHistoryEntity)

    @Query("SELECT * FROM budget_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BudgetHistoryEntity>>

    @Query("SELECT * FROM budget_history WHERE budgetId = :budgetId ORDER BY timestamp DESC")
    fun getForBudget(budgetId: Int): Flow<List<BudgetHistoryEntity>>
}

// ─── Goal DAO ─────────────────────────────────────────────────────────────────

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAll(): Flow<List<GoalEntity>>
}

// ─── Goal History DAO ─────────────────────────────────────────────────────────

@Dao
interface GoalHistoryDao {
    @Insert
    suspend fun insert(h: GoalHistoryEntity)

    @Query("SELECT * FROM goal_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<GoalHistoryEntity>>

    @Query("SELECT * FROM goal_history WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getForGoal(goalId: Int): Flow<List<GoalHistoryEntity>>
}

// ─── Chat Message DAO ─────────────────────────────────────────────────────────

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllFlow(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAll(): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(message: ChatMessageEntity)
}

// ─── Bill Reminder DAO ────────────────────────────────────────────────────────

@Dao
interface BillReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: BillReminderEntity): Long

    @Update
    suspend fun update(bill: BillReminderEntity)

    @Delete
    suspend fun delete(bill: BillReminderEntity)

    @Query("SELECT * FROM bill_reminders ORDER BY dueDateMillis ASC")
    fun getAllFlow(): Flow<List<BillReminderEntity>>

    @Query("SELECT * FROM bill_reminders WHERE id = :id")
    suspend fun getById(id: Int): BillReminderEntity?

    @Query("SELECT * FROM bill_reminders WHERE isActive = 1 ORDER BY dueDateMillis ASC")
    suspend fun getActive(): List<BillReminderEntity>
}
