package com.trackfinz.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.trackfinz.app.MainActivity
import com.trackfinz.app.R

object NotificationHelper {

    private const val CHANNEL_ID   = "trackfinz_alerts"
    private const val CHANNEL_NAME = "Finance Alerts"
    private const val CHANNEL_DESC = "Budget and expense alerts from TrackFinz"

    fun createChannel(context: Context) {
        val soundUri = Settings.System.DEFAULT_NOTIFICATION_URI

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH          // heads-up + sound
        ).apply {
            description = CHANNEL_DESC
            setSound(soundUri, audioAttrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    fun postOverBudgetAlert(
        context: Context,
        categoryName: String,
        spent: Double,
        limit: Double,
        currency: String
    ) {
        val title = "⚠️ Over Budget: $categoryName"
        val body  = "You've spent ${formatCurrency(spent, currency)} of your " +
                    "${formatCurrency(limit, currency)} $categoryName budget this month."
        post(context, notificationId = ("budget_$categoryName").hashCode(), title, body)
    }

    fun postLargeExpenseAlert(
        context: Context,
        categoryName: String,
        amount: Double,
        currency: String
    ) {
        val title = "💸 Large Expense Recorded"
        val body  = "${formatCurrency(amount, currency)} spent on $categoryName."
        post(context, notificationId = System.currentTimeMillis().toInt(), title, body)
    }

    private fun post(context: Context, notificationId: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
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
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }
}
