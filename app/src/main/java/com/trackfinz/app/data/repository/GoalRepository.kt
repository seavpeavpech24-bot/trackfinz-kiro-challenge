package com.trackfinz.app.data.repository

import com.trackfinz.app.data.database.GoalDao
import com.trackfinz.app.data.database.GoalHistoryDao
import com.trackfinz.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val dao: GoalDao,
    private val historyDao: GoalHistoryDao
) {
    val allGoals = dao.getAll()
    fun getAllHistory() = historyDao.getAll()
    fun getHistoryForGoal(goalId: Int) = historyDao.getForGoal(goalId)

    suspend fun add(goal: GoalEntity) = dao.insert(goal)

    suspend fun contribute(goal: GoalEntity, amount: Double) {
        val before = goal.savedAmount
        val after = (before + amount).coerceAtMost(goal.targetAmount)
        dao.update(goal.copy(savedAmount = after, isCompleted = after >= goal.targetAmount))
        historyDao.insert(GoalHistoryEntity(
            goalId = goal.id, goalTitle = goal.title,
            amount = amount, action = GoalFundAction.ADDED,
            balanceBefore = before, balanceAfter = after
        ))
    }

    suspend fun removeFunds(goal: GoalEntity, amount: Double) {
        val before = goal.savedAmount
        val after = (before - amount).coerceAtLeast(0.0)
        dao.update(goal.copy(savedAmount = after, isCompleted = false))
        historyDao.insert(GoalHistoryEntity(
            goalId = goal.id, goalTitle = goal.title,
            amount = amount, action = GoalFundAction.REMOVED,
            balanceBefore = before, balanceAfter = after
        ))
    }

    suspend fun update(goal: GoalEntity) = dao.update(goal)

    suspend fun delete(goal: GoalEntity) = dao.delete(goal)
}
