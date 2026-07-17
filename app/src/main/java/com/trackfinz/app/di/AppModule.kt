package com.trackfinz.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trackfinz.app.data.database.*
import com.trackfinz.app.data.repository.MonthlyReportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ── Room migrations ───────────────────────────────────────────────────────────

/** v3 → v4: added getForCategoryAndMonth query — no schema change, just a new DAO method. */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) { /* no DDL change */ }
}

/** v4 → v5: added bill_reminders table. */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `bill_reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `dueDateMillis` INTEGER NOT NULL,
                `reminderTimeMillis` INTEGER NOT NULL,
                `frequency` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

/** v5 → v6: added monthly_reports table for AI Monthly Financial Report feature. */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `monthly_reports` (
                `id`            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `month`         INTEGER NOT NULL,
                `year`          INTEGER NOT NULL,
                `narrative`     TEXT NOT NULL,
                `isFallback`    INTEGER NOT NULL DEFAULT 0,
                `healthScore`   INTEGER NOT NULL,
                `totalIncome`   REAL NOT NULL,
                `totalExpenses` REAL NOT NULL,
                `savingsRate`   REAL NOT NULL,
                `generatedAt`   INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): TrackFinzDatabase =
        Room.databaseBuilder(ctx, TrackFinzDatabase::class.java, "trackfinz.db")
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .fallbackToDestructiveMigration()   // safety net for anything older than v3
            .build()

    @Provides fun provideUserDao(db: TrackFinzDatabase): UserDao = db.userDao()
    @Provides fun provideTransactionDao(db: TrackFinzDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideBudgetDao(db: TrackFinzDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideBudgetHistoryDao(db: TrackFinzDatabase): BudgetHistoryDao = db.budgetHistoryDao()
    @Provides fun provideGoalDao(db: TrackFinzDatabase): GoalDao = db.goalDao()
    @Provides fun provideGoalHistoryDao(db: TrackFinzDatabase): GoalHistoryDao = db.goalHistoryDao()
    @Provides fun provideChatMessageDao(db: TrackFinzDatabase): ChatMessageDao = db.chatMessageDao()
    @Provides fun provideBillReminderDao(db: TrackFinzDatabase): BillReminderDao = db.billReminderDao()
    @Provides fun provideMonthlyReportDao(db: TrackFinzDatabase): MonthlyReportDao = db.monthlyReportDao()

    @Provides
    @Singleton
    fun provideMonthlyReportRepository(dao: MonthlyReportDao): MonthlyReportRepository =
        MonthlyReportRepository(dao)
}
