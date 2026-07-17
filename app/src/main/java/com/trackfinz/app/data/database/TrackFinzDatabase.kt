package com.trackfinz.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trackfinz.app.data.model.*

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        BudgetHistoryEntity::class,
        GoalEntity::class,
        GoalHistoryEntity::class,
        ChatMessageEntity::class,
        BillReminderEntity::class,
        MonthlyReportEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrackFinzDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetHistoryDao(): BudgetHistoryDao
    abstract fun goalDao(): GoalDao
    abstract fun goalHistoryDao(): GoalHistoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun billReminderDao(): BillReminderDao
    abstract fun monthlyReportDao(): MonthlyReportDao
}
