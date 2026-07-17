package com.trackfinz.app.data.repository

import com.trackfinz.app.data.database.BillReminderDao
import com.trackfinz.app.data.model.BillReminderEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillReminderRepository @Inject constructor(
    private val dao: BillReminderDao
) {
    fun getAllFlow() = dao.getAllFlow()

    suspend fun add(bill: BillReminderEntity): Long = dao.insert(bill)

    suspend fun update(bill: BillReminderEntity) = dao.update(bill)

    suspend fun delete(bill: BillReminderEntity) = dao.delete(bill)

    suspend fun getById(id: Int) = dao.getById(id)

    suspend fun getActive() = dao.getActive()
}
