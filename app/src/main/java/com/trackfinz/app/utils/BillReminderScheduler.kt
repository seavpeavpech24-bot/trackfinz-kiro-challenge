package com.trackfinz.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.trackfinz.app.data.model.BillFrequency
import com.trackfinz.app.data.model.BillReminderEntity
import com.trackfinz.app.receiver.BillReminderReceiver

object BillReminderScheduler {

    fun schedule(context: Context, bill: BillReminderEntity) {
        if (!bill.isActive) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildIntent(context, bill)
        val pi = PendingIntent.getBroadcast(
            context,
            bill.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = bill.reminderTimeMillis
        if (triggerAt <= System.currentTimeMillis()) return  // already past

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                // Fall back to inexact alarm if exact not permitted
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            // Exact alarm permission not granted — use inexact
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context, billId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BillReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            billId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    /** Compute the next due date millis based on frequency. */
    fun nextDueDate(currentDueMillis: Long, frequency: BillFrequency): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = currentDueMillis }
        when (frequency) {
            BillFrequency.ONCE    -> { /* no next */ }
            BillFrequency.WEEKLY  -> cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            BillFrequency.MONTHLY -> cal.add(java.util.Calendar.MONTH, 1)
            BillFrequency.YEARLY  -> cal.add(java.util.Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun buildIntent(context: Context, bill: BillReminderEntity): Intent =
        Intent(context, BillReminderReceiver::class.java).apply {
            putExtra(BillReminderReceiver.EXTRA_BILL_ID,     bill.id)
            putExtra(BillReminderReceiver.EXTRA_BILL_NAME,   bill.name)
            putExtra(BillReminderReceiver.EXTRA_BILL_AMOUNT, bill.amount)
            putExtra(BillReminderReceiver.EXTRA_DUE_MILLIS,  bill.dueDateMillis)
            putExtra(BillReminderReceiver.EXTRA_FREQUENCY,   bill.frequency.name)
        }
}
