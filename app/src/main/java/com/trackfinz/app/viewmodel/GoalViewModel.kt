package com.trackfinz.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackfinz.app.data.model.GoalEntity
import com.trackfinz.app.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(private val repo: GoalRepository) : ViewModel() {

    val goals = repo.allGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allHistory = repo.getAllHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addGoal(goal: GoalEntity) = viewModelScope.launch { repo.add(goal) }

    fun contribute(goal: GoalEntity, amount: Double) =
        viewModelScope.launch { repo.contribute(goal, amount) }

    fun removeFunds(goal: GoalEntity, amount: Double) =
        viewModelScope.launch { repo.removeFunds(goal, amount) }

    fun deleteGoal(goal: GoalEntity) = viewModelScope.launch { repo.delete(goal) }

    fun getHistoryForGoal(goalId: Int) =
        repo.getHistoryForGoal(goalId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
