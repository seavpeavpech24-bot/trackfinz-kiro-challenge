package com.trackfinz.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackfinz.app.data.model.BillFrequency
import com.trackfinz.app.data.model.BillReminderEntity
import com.trackfinz.app.data.model.TransactionCategory
import com.trackfinz.app.data.model.TransactionEntity
import com.trackfinz.app.data.model.TransactionType
import com.trackfinz.app.data.repository.BillReminderRepository
import com.trackfinz.app.data.repository.TransactionRepository
import com.trackfinz.app.utils.BillReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillReminderViewModel @Inject constructor(
    private val repo: BillReminderRepository,
    private val txRepo: TransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val bills = repo.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addBill(
        name: String,
        amount: Double,
        dueDateMillis: Long,
        reminderTimeMillis: Long,
        frequency: BillFrequency,
        category: TransactionCategory,
        note: String
    ) = viewModelScope.launch {
        val id = repo.add(
            BillReminderEntity(
                name = name, amount = amount,
                dueDateMillis = dueDateMillis, reminderTimeMillis = reminderTimeMillis,
                frequency = frequency, category = category, note = note
            )
        )
        val bill = repo.getById(id.toInt()) ?: return@launch
        BillReminderScheduler.schedule(context, bill)
    }

    fun updateBill(bill: BillReminderEntity) = viewModelScope.launch {
        repo.update(bill)
        BillReminderScheduler.cancel(context, bill.id)
        if (bill.isActive) BillReminderScheduler.schedule(context, bill)
    }

    fun deleteBill(bill: BillReminderEntity) = viewModelScope.launch {
        BillReminderScheduler.cancel(context, bill.id)
        repo.delete(bill)
    }

    fun toggleActive(bill: BillReminderEntity) = viewModelScope.launch {
        val updated = bill.copy(isActive = !bill.isActive)
        repo.update(updated)
        if (updated.isActive) BillReminderScheduler.schedule(context, updated)
        else BillReminderScheduler.cancel(context, bill.id)
    }

    /**
     * Mark a bill as paid:
     * 1. Records an expense transaction automatically.
     * 2. For recurring bills: advances due date to next cycle and reschedules alarm.
     * 3. For one-time bills: deactivates the reminder.
     */
    fun markAsPaid(bill: BillReminderEntity) = viewModelScope.launch {
        // 1. Record expense transaction
        txRepo.add(
            TransactionEntity(
                title = bill.name,
                amount = bill.amount,
                type = TransactionType.EXPENSE,
                category = bill.category,
                note = "Auto-recorded from bill reminder",
                date = System.currentTimeMillis()
            )
        )

        // 2. Advance or deactivate
        if (bill.frequency == BillFrequency.ONCE) {
            // One-time bill — deactivate after payment
            repo.update(bill.copy(isActive = false))
            BillReminderScheduler.cancel(context, bill.id)
        } else {
            // Recurring — advance to next cycle
            val nextDue = BillReminderScheduler.nextDueDate(bill.dueDateMillis, bill.frequency)
            val offset = bill.dueDateMillis - bill.reminderTimeMillis
            val nextReminder = nextDue - offset
            val updated = bill.copy(dueDateMillis = nextDue, reminderTimeMillis = nextReminder)
            repo.update(updated)
            BillReminderScheduler.cancel(context, bill.id)
            BillReminderScheduler.schedule(context, updated)
        }
    }
}
