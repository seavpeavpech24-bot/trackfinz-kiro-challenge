package com.trackfinz.app.data.database

import androidx.room.*
import com.trackfinz.app.data.model.MonthlyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: MonthlyReportEntity): Long

    @Query("SELECT * FROM monthly_reports WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getForMonth(month: Int, year: Int): MonthlyReportEntity?

    @Query("SELECT * FROM monthly_reports ORDER BY year DESC, month DESC")
    fun getAllFlow(): Flow<List<MonthlyReportEntity>>

    @Query("DELETE FROM monthly_reports WHERE year < :year OR (year = :year AND month < :month)")
    suspend fun deleteOlderThan(month: Int, year: Int)
}
