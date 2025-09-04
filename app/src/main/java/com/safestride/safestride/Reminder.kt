package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class Reminder : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReminderAdapter
    private val remindersList = mutableListOf<ReminderClass>()

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        findViewById<LinearLayout>(R.id.reminder)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reminder)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find TextView
        val dateTextView = findViewById<TextView>(R.id.dateText)

        // Get the current date
        val sdf = SimpleDateFormat("MMMM dd", Locale.getDefault()) // Example: "February 25"
        val currentDate = sdf.format(Date())

        // Set the current date to TextView
        dateTextView.text = currentDate

        // RecyclerView Setup
        recyclerView = findViewById(R.id.recyclerReminders)
        recyclerView.layoutManager = LinearLayoutManager(this)


        // Adapter Setup
        adapter = ReminderAdapter(remindersList) { deleteReminderFromFirestore(it) }
        recyclerView.adapter = adapter

        // Load saved reminders from Firestore
        loadRemindersFromFirestore()

        // Calendar Setup
        val calendarView: CalendarView = findViewById(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            Log.d("Calendar", "Selected Date: $dayOfMonth/${month + 1}/$year")
        }

        // Add Reminder Button
        findViewById<FloatingActionButton>(R.id.fabAddReminder).setOnClickListener {
            val intent = Intent(this, AddReminderActivity::class.java)
            startActivityForResult(intent, 1)
        }

        // Back Arrow Click Listener
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            finish()
        }
    }

    // Handle Result from AddReminderActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val reminderItem = data.getSerializableExtra("REMINDER") as? ReminderClass
            reminderItem?.let {
                saveReminderToFirestore(it)
            }
        }
    }

    // 🔹 Save Reminder to Firestore
    private fun saveReminderToFirestore(reminder: ReminderClass) {
        if (userId == null) return

        db.collection("reminders").document(userId)
            .collection("caregiverReminders")
            .add(reminder)
            .addOnSuccessListener {
                Toast.makeText(this, "Reminder Added", Toast.LENGTH_SHORT).show()
                loadRemindersFromFirestore()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadRemindersFromFirestore() {
        if (userId == null) return

        db.collection("reminders").document(userId)
            .collection("caregiverReminders")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load reminders: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    remindersList.clear()
                    for (document in snapshots) {
                        val reminder = document.toObject(ReminderClass::class.java)
                        remindersList.add(reminder)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }


    // 🔹 Delete Reminder from Firestore
    private fun deleteReminderFromFirestore(reminder: ReminderClass) {
        if (userId == null) return

        db.collection("reminders").document(userId)
            .collection("caregiverReminders")
            .whereEqualTo("title", reminder.title)
            .whereEqualTo("date", reminder.date)
            .whereEqualTo("time", reminder.time)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reminder Deleted", Toast.LENGTH_SHORT).show()
                            loadRemindersFromFirestore()
                        }
                }
            }
    }
}
