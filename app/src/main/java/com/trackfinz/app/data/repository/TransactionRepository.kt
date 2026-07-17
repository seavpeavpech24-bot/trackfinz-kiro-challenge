package com.trackfinz.app.data.repository

import com.trackfinz.app.data.database.TransactionDao
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.model.TransactionEntity
import com.trackfinz.app.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val dao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllFlow()
    val recentTransactions: Flow<List<TransactionEntity>> = dao.getRecent()
    val totalIncome: Flow<Double?> = dao.totalIncome()
    val totalExpense: Flow<Double?> = dao.totalExpense()

    suspend fun add(tx: TransactionEntity) = dao.insert(tx)
    suspend fun update(tx: TransactionEntity) = dao.update(tx)
    suspend fun delete(tx: TransactionEntity) = dao.delete(tx)

    fun getByType(type: TransactionType) = dao.getByType(type)
    fun getByCategory(cat: TransactionCategory) = dao.getByCategory(cat)
    fun getByDateRange(from: Long, to: Long) = dao.getByDateRange(from, to)
    fun getByTypeAndDateRange(type: TransactionType, from: Long, to: Long) =
        dao.getByTypeAndDateRange(type, from, to)
    fun totalIncomeInRange(from: Long, to: Long) = dao.totalIncomeInRange(from, to)
    fun totalExpenseInRange(from: Long, to: Long) = dao.totalExpenseInRange(from, to)
    fun search(query: String) = dao.search(query)

    suspend fun spentInCategory(cat: TransactionCategory, from: Long, to: Long): Double =
        dao.spentInCategory(cat, from, to) ?: 0.0
}
