package com.trackfinz.app.utils

import com.trackfinz.app.data.model.*

/** Seed data for first-launch demo */
object SampleData {
    fun transactions(): List<TransactionEntity> = listOf(
        TransactionEntity(title = "Monthly Salary",   amount = 4500.0, type = TransactionType.INCOME,  category = TransactionCategory.SALARY,        date = System.currentTimeMillis() - 86400000L * 2),
        TransactionEntity(title = "Grocery Store",    amount = 85.50,  type = TransactionType.EXPENSE, category = TransactionCategory.FOOD,          date = System.currentTimeMillis() - 86400000L * 3),
        TransactionEntity(title = "Netflix",          amount = 15.99,  type = TransactionType.EXPENSE, category = TransactionCategory.ENTERTAINMENT, date = System.currentTimeMillis() - 86400000L * 4),
        TransactionEntity(title = "Electricity Bill", amount = 120.0,  type = TransactionType.EXPENSE, category = TransactionCategory.BILLS,         date = System.currentTimeMillis() - 86400000L * 5),
        TransactionEntity(title = "Uber Ride",        amount = 22.0,   type = TransactionType.EXPENSE, category = TransactionCategory.TRAVEL,        date = System.currentTimeMillis() - 86400000L * 6),
        TransactionEntity(title = "Amazon Shopping",  amount = 67.30,  type = TransactionType.EXPENSE, category = TransactionCategory.SHOPPING,      date = System.currentTimeMillis() - 86400000L * 7),
        TransactionEntity(title = "Doctor Visit",     amount = 50.0,   type = TransactionType.EXPENSE, category = TransactionCategory.HEALTHCARE,    date = System.currentTimeMillis() - 86400000L * 8),
        TransactionEntity(title = "Freelance Work",   amount = 800.0,  type = TransactionType.INCOME,  category = TransactionCategory.SALARY,        date = System.currentTimeMillis() - 86400000L * 9)
    )

    fun budgets(): List<BudgetEntity> {
        val m = currentMonth(); val y = currentYear()
        return listOf(
            BudgetEntity(category = TransactionCategory.FOOD,          limit = 400.0,  month = m, year = y),
            BudgetEntity(category = TransactionCategory.SHOPPING,      limit = 200.0,  month = m, year = y),
            BudgetEntity(category = TransactionCategory.ENTERTAINMENT, limit = 100.0,  month = m, year = y),
            BudgetEntity(category = TransactionCategory.BILLS,         limit = 300.0,  month = m, year = y),
            BudgetEntity(category = TransactionCategory.TRAVEL,        limit = 150.0,  month = m, year = y)
        )
    }

    fun goals(): List<GoalEntity> = listOf(
        GoalEntity(title = "Emergency Fund",  targetAmount = 10000.0, savedAmount = 3500.0, emoji = "🛡️"),
        GoalEntity(title = "Vacation Trip",   targetAmount = 2500.0,  savedAmount = 800.0,  emoji = "✈️"),
        GoalEntity(title = "New Laptop",      targetAmount = 1500.0,  savedAmount = 1200.0, emoji = "💻"),
        GoalEntity(title = "Car Down Payment",targetAmount = 5000.0,  savedAmount = 500.0,  emoji = "🚗")
    )
}
