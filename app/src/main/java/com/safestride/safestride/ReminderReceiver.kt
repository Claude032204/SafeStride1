package com.safestride.safestride

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val message = intent.getStringExtra("REMINDER_MESSAGE") ?: "Reminder Alert!"
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()
        val notification = hashMapOf(
            "title" to "Reminder Alert!",
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )

        // 🔹 Store the reminder in Firestore when it is due
        db.collection("notifications").document(userId)
            .collection("reminders").add(notification)
            .addOnSuccessListener {
                Toast.makeText(context, "Reminder: $message", Toast.LENGTH_SHORT).show()
            }

        // 🔹 Show a real notification when the reminder is due
        showNotification(context, message)
    }

    private fun showNotification(context: Context, message: String) {
        val channelId = "ReminderChannel"
        val notificationId = System.currentTimeMillis().toInt()

        // Intent to open the app when notification is clicked
        val openIntent = Intent(context, Notification::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create Notification Channel (For Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Reminder Notifications"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // Build Notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logoo) // Replace with your icon
            .setContentTitle("Reminder Alert!")
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // ✅ Check if notification permission is granted before displaying notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(context, "Notification permission not granted!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // ✅ Show Notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
