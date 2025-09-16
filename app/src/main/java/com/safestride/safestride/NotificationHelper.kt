package com.safestride.safestride

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val REMINDER_CHANNEL_ID = "REMINDER_CHANNEL"
    private const val ALERT_CHANNEL_ID = "ALERT_CHANNEL_V2" // NEW id so Android recreates with correct settings

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (nm.getNotificationChannel(REMINDER_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT // no heads-up
                ).apply {
                    description = "General reminders and updates"
                    enableVibration(false)
                    setSound(null, null)
                }
            )
        }

        if (nm.getNotificationChannel(ALERT_CHANNEL_ID) == null) {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            nm.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Safety Alerts",
                    NotificationManager.IMPORTANCE_HIGH // heads-up enabled
                ).apply {
                    description = "Emergency and Assistance alerts"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 600, 700, 600) // buzz-pause-buzz
                    setSound(sound, attrs)
                    enableLights(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
        }
    }

    fun showNotification(context: Context, title: String) {
        showNotification(context, title, "Your scheduled reminder is due.")
    }

    fun showNotification(context: Context, title: String, body: String) {
        ensureChannels(context)

        // Decide destination
        val targetIntent: Intent = when {
            title.contains("Emergency", ignoreCase = true) -> Intent(context, EmergencyLogs::class.java)
            title.contains("Assistance", ignoreCase = true) -> Intent(context, AssistanceLogs::class.java)
            else -> Intent(context, Notification::class.java)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val contentPi = PendingIntent.getActivity(context, 0, targetIntent, flags)

        val isAlert = title.contains("Emergency", true) || title.contains("Assistance", true)
        val channelId = if (isAlert) ALERT_CHANNEL_ID else REMINDER_CHANNEL_ID

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification) // ensure drawable exists
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setCategory(
                if (title.contains("Emergency", true)) NotificationCompat.CATEGORY_ALARM
                else NotificationCompat.CATEGORY_EVENT
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Pre-O: force heads-up
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && isAlert) {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            builder.priority = NotificationCompat.PRIORITY_MAX
            builder.setSound(sound)
            builder.setVibrate(longArrayOf(0, 600, 700, 600))
        }

        // Optional: FULL-SCREEN INTENT for Emergency (most urgent)
        if (title.contains("Emergency", true)) {
            val fullScreenPi = PendingIntent.getActivity(context, 1, Intent(context, EmergencyLogs::class.java), flags)
            builder.setFullScreenIntent(fullScreenPi, true)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
