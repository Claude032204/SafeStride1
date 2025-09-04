package com.safestride.safestride

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.util.Calendar

class AddReminderActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_reminder)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val reminderMessageInput = findViewById<EditText>(R.id.reminderMessageInput)
        val datePicker = findViewById<DatePicker>(R.id.datePicker)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val addReminderButton = findViewById<Button>(R.id.addReminderButton)
        val toggleDateTimeButton = findViewById<ImageView>(R.id.toggleDateTimeButton)

        timePicker.setIs24HourView(true)

        // Toggle Date and Time Picker Visibility
        toggleDateTimeButton.setOnClickListener {
            if (datePicker.visibility == View.GONE) {
                datePicker.visibility = View.VISIBLE
                timePicker.visibility = View.VISIBLE
            } else {
                datePicker.visibility = View.GONE
                timePicker.visibility = View.GONE
            }
        }

        // Back Arrow Click Listener
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            finish()
        }

        // 🔹 Request Notification Permission (Android 13+)
        requestNotificationPermission()

        // Add Reminder Button Click Listener
        addReminderButton.setOnClickListener {
            val message = reminderMessageInput.text.toString()
            val day = datePicker.dayOfMonth
            val month = datePicker.month
            val year = datePicker.year
            val hour = timePicker.hour
            val minute = timePicker.minute

            if (message.isNotEmpty() && userId != null) {
                val reminderTime = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                }

                val reminder = hashMapOf(
                    "title" to message,
                    "date" to "$day/${month + 1}/$year",
                    "time" to "$hour:$minute",
                    "timestamp" to reminderTime.timeInMillis
                )

                db.collection("reminders").document(userId)
                    .collection("caregiverReminders").add(reminder)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reminder Added!", Toast.LENGTH_SHORT).show()

                        // ✅ Schedule Local Notification
                        if (checkAndRequestAlarmPermission()) {
                            scheduleNotification(reminderTime, message)
                        }

                        // ✅ Send FCM Push Notification
                        sendReminderToFCM(message)

                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please enter a reminder message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 Request Notification Permission (Android 13+)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                )
            }
        }
    }

    // 🔹 Check and Request Alarm Permission
    private fun checkAndRequestAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                Toast.makeText(this, "Enable 'Schedule Exact Alarms' in Settings", Toast.LENGTH_LONG).show()
                return false
            }
        }
        return true
    }

    // 🔹 Schedule Local Notification
    private fun scheduleNotification(reminderTime: Calendar, message: String) {
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_MESSAGE", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime.timeInMillis,
            pendingIntent
        )
    }

    // 🔹 Send FCM Push Notification
    private fun sendReminderToFCM(message: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")

                if (!fcmToken.isNullOrEmpty()) {
                    val notificationData = mapOf(
                        "to" to fcmToken,
                        "data" to mapOf(
                            "title" to "Reminder Alert!",
                            "message" to message
                        )
                    )

                    sendPushNotification(notificationData)
                }
            }
    }

    // 🔹 Send Push Notification via FCM API
    private fun sendPushNotification(notificationData: Map<String, Any>) {
        val jsonBody = JSONObject(notificationData)

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            "https://fcm.googleapis.com/fcm/send",
            jsonBody,
            { response ->
                println("FCM Notification sent: $response")
            },
            { error ->
                println("Error sending FCM Notification: ${error.message}")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "key=YOUR_SERVER_KEY"  // 🔹 Replace with your Firebase Server Key
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}
