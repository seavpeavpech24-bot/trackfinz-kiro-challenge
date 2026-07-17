package com.trackfinz.app.di

import android.util.Log
import com.trackfinz.app.data.repository.BudgetRepository
import com.trackfinz.app.data.repository.GoalRepository
import com.trackfinz.app.data.repository.TransactionRepository
import com.trackfinz.app.data.repository.UserRepository
import com.trackfinz.app.utils.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedDataInitializer @Inject constructor(
    private val txRepo: TransactionRepository,
    private val budgetRepo: BudgetRepository,
    private val goalRepo: GoalRepository,
    private val userRepo: UserRepository
) {
    fun seedIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Never seed if a real user has registered
                val existingUser = userRepo.getUser()
                if (existingUser != null) return@launch

                val existing = txRepo.allTransactions.first()
                if (existing.isEmpty()) {
                    SampleData.transactions().forEach { txRepo.add(it) }
                    SampleData.budgets().forEach { budgetRepo.add(it) }
                    SampleData.goals().forEach { goalRepo.add(it) }
                }
            } catch (e: Exception) {
                Log.e("SeedDataInitializer", "Seed failed (non-fatal)", e)
            }
        }
    }
}
