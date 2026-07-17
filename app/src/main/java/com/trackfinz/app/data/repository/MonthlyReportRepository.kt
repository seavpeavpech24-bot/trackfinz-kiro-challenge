package com.trackfinz.app.data.repository

import com.trackfinz.app.data.database.MonthlyReportDao
import com.trackfinz.app.data.model.MonthlyReportEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonthlyReportRepository @Inject constructor(
    private val dao: MonthlyReportDao
) {
    suspend fun getReportForMonth(month: Int, year: Int): MonthlyReportEntity? =
        dao.getForMonth(month, year)

    suspend fun saveReport(report: MonthlyReportEntity) = dao.insert(report)

    fun allReportsFlow(): Flow<List<MonthlyReportEntity>> = dao.getAllFlow()

    /** Keep only the most recent [keepMonths] reports. */
    suspend fun pruneOld(keepMonths: Int = 12) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -keepMonths)
        val month = cal.get(Calendar.MONTH) + 1
        val year  = cal.get(Calendar.YEAR)
        dao.deleteOlderThan(month, year)
    }
}
