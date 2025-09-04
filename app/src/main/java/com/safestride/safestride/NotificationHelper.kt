package com.safestride.safestride

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "REMINDER_CHANNEL"

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(CHANNEL_ID) == null
        ) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    fun showNotification(context: Context, title: String) {
        showNotification(context, title, "Your scheduled reminder is due.")
    }

    fun showNotification(context: Context, title: String, body: String) {
        ensureChannel(context)

        // 🔹 Decide which Activity to open based on title
        val targetIntent: Intent = when {
            title.contains("Emergency", ignoreCase = true) -> {
                Intent(context, EmergencyLogs::class.java)
            }
            title.contains("Assistance", ignoreCase = true) -> {
                Intent(context, AssistanceLogs::class.java)
            }
            else -> {
                Intent(context, Notification::class.java)
            }
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getActivity(context, 0, targetIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification) // make sure drawable exists
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
