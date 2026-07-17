package com.trackfinz.app.data.repository

import com.trackfinz.app.data.database.BudgetDao
import com.trackfinz.app.data.database.BudgetHistoryDao
import com.trackfinz.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: BudgetDao,
    private val historyDao: BudgetHistoryDao
) {
    fun getForMonth(month: Int, year: Int) = dao.getForMonth(month, year)
    fun getAll() = dao.getAll()
    fun getAllHistory() = historyDao.getAll()

    suspend fun add(budget: BudgetEntity): Long {
        val id = dao.insert(budget)
        historyDao.insert(BudgetHistoryEntity(
            budgetId = id.toInt(), category = budget.category,
            oldLimit = null, newLimit = budget.limit,
            month = budget.month, year = budget.year,
            action = BudgetHistoryAction.CREATED
        ))
        return id
    }

    /**
     * Insert or update a budget for the given category+month+year.
     * If one already exists, updates its limit instead of creating a duplicate.
     */
    suspend fun upsert(budget: BudgetEntity) {
        val existing = dao.getForCategoryAndMonth(budget.category, budget.month, budget.year)
        if (existing != null) {
            val updated = existing.copy(limit = budget.limit)
            dao.update(updated)
            historyDao.insert(BudgetHistoryEntity(
                budgetId = existing.id, category = existing.category,
                oldLimit = existing.limit, newLimit = budget.limit,
                month = existing.month, year = existing.year,
                action = BudgetHistoryAction.UPDATED
            ))
        } else {
            val id = dao.insert(budget)
            historyDao.insert(BudgetHistoryEntity(
                budgetId = id.toInt(), category = budget.category,
                oldLimit = null, newLimit = budget.limit,
                month = budget.month, year = budget.year,
                action = BudgetHistoryAction.CREATED
            ))
        }
    }

    suspend fun update(budget: BudgetEntity, oldLimit: Double) {
        dao.update(budget)
        historyDao.insert(BudgetHistoryEntity(
            budgetId = budget.id, category = budget.category,
            oldLimit = oldLimit, newLimit = budget.limit,
            month = budget.month, year = budget.year,
            action = BudgetHistoryAction.UPDATED
        ))
    }

    suspend fun delete(budget: BudgetEntity) {
        dao.delete(budget)
        historyDao.insert(BudgetHistoryEntity(
            budgetId = budget.id, category = budget.category,
            oldLimit = budget.limit, newLimit = null,
            month = budget.month, year = budget.year,
            action = BudgetHistoryAction.DELETED
        ))
    }
}
