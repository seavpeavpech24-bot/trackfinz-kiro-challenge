package com.trackfinz.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.trackfinz.app.MainActivity
import com.trackfinz.app.R
import com.trackfinz.app.data.model.BillFrequency
import com.trackfinz.app.utils.BillReminderScheduler
import com.trackfinz.app.utils.formatCurrency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.trackfinz.app.data.repository.BillReminderRepository

@AndroidEntryPoint
class BillReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var billRepo: BillReminderRepository

    companion object {
        const val EXTRA_BILL_ID     = "bill_id"
        const val EXTRA_BILL_NAME   = "bill_name"
        const val EXTRA_BILL_AMOUNT = "bill_amount"
        const val EXTRA_DUE_MILLIS  = "due_millis"
        const val EXTRA_FREQUENCY   = "frequency"

        private const val CHANNEL_ID = "trackfinz_alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val billId    = intent.getIntExtra(EXTRA_BILL_ID, -1)
        val billName  = intent.getStringExtra(EXTRA_BILL_NAME) ?: "Bill"
        val amount    = intent.getDoubleExtra(EXTRA_BILL_AMOUNT, 0.0)
        val dueMillis = intent.getLongExtra(EXTRA_DUE_MILLIS, 0L)
        val freqName  = intent.getStringExtra(EXTRA_FREQUENCY) ?: BillFrequency.MONTHLY.name
        val frequency = runCatching { BillFrequency.valueOf(freqName) }.getOrDefault(BillFrequency.MONTHLY)

        // Fire the notification
        postNotification(context, billId, billName, amount, dueMillis)

        // Reschedule for recurring bills and update DB
        if (frequency != BillFrequency.ONCE && billId != -1) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val bill = billRepo.getById(billId)
                    if (bill != null && bill.isActive) {
                        val nextDue = BillReminderScheduler.nextDueDate(bill.dueDateMillis, frequency)
                        // Keep reminder time offset the same (e.g. 1 day before)
                        val offset = bill.dueDateMillis - bill.reminderTimeMillis
                        val nextReminder = nextDue - offset
                        val updated = bill.copy(
                            dueDateMillis = nextDue,
                            reminderTimeMillis = nextReminder
                        )
                        billRepo.update(updated)
                        BillReminderScheduler.schedule(context, updated)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun postNotification(
        context: Context,
        billId: Int,
        billName: String,
        amount: Double,
        dueMillis: Long
    ) {
        val now = System.currentTimeMillis()
        val daysUntilDue = ((dueMillis - now) / (1000 * 60 * 60 * 24)).toInt()

        val title = "🔔 Bill Reminder: $billName"
        val body = when {
            daysUntilDue <= 0 -> "$billName is due today! Amount: ${formatCurrency(amount, "USD")}"
            daysUntilDue == 1 -> "$billName is due tomorrow. Amount: ${formatCurrency(amount, "USD")}"
            else -> "$billName is due in $daysUntilDue days. Amount: ${formatCurrency(amount, "USD")}"
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "bill_reminders")
        }
        val pi = PendingIntent.getActivity(
            context, billId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = Settings.System.DEFAULT_NOTIFICATION_URI

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify("bill_$billId".hashCode(), notification)
        } catch (_: SecurityException) { /* permission not granted */ }
    }
}
