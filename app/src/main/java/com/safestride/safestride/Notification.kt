package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Notification : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationsList = mutableListOf<Notif>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val notificationLayout = findViewById<CoordinatorLayout>(R.id.notification)
        ViewCompat.setOnApplyWindowInsetsListener(notificationLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // RecyclerView setup
        recyclerView = findViewById(R.id.recyclerNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notificationsList)
        recyclerView.adapter = adapter

        // Load notifications in real-time
        loadNotificationsFromFirestore()

        // Set up swipe-to-delete functionality
        setupSwipeToDelete()

        // Back button
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            onBackPressed()
        }

        // Open settings
        findViewById<ImageView>(R.id.settingsIcon).setOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }
    }

    private fun loadNotificationsFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificationsRef = db.collection("notifications").document(userId)

        // Clear list to prevent duplicates
        notificationsList.clear()

        // 🔹 Fetch all notifications and update UI in real-time
        val notificationTypes = listOf("reminders", "emergencyLogs", "assistanceLogs")

        for (type in notificationTypes) {
            notificationsRef.collection(type)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Toast.makeText(this, "Failed to load $type: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        updateNotificationList(snapshots, getAlertType(type))
                    }
                }
        }
    }

    private fun updateNotificationList(snapshots: com.google.firebase.firestore.QuerySnapshot?, type: String) {
        if (snapshots != null) {
            val tempList = mutableListOf<Notif>()

            for (document in snapshots.documents) {
                val timestampLong = document.getLong("timestamp") ?: 0L
                val formattedTimestamp = formatTimestamp(timestampLong)

                val notification = Notif(
                    documentId = document.id,  // Add the document ID
                    title = type,
                    details = document.getString("message") ?: "No details available",
                    timestamp = formattedTimestamp
                )
                tempList.add(notification)
            }

            // ✅ Remove duplicates and update the main list
            notificationsList.removeAll { notif -> tempList.any { it.details == notif.details && it.timestamp == notif.timestamp } }
            notificationsList.addAll(tempList)
            notificationsList.sortByDescending { it.timestamp } // ✅ Sort by newest first
            adapter.notifyDataSetChanged()
        }
    }

    // 🔹 Convert Firestore collection names to user-friendly alert titles
    private fun getAlertType(collection: String): String {
        return when (collection) {
            "reminders" -> "Reminder Alert!"
            "emergencyLogs" -> "Emergency Alert!"
            "assistanceLogs" -> "Assistance Alert!"
            else -> "Notification"
        }
    }

    // 🔹 Convert timestamp (Long) to a readable format
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} mins ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
            else -> "${diff / 86_400_000} days ago"
        }
    }

    // 🔹 Swipe-to-delete functionality
    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Get the swiped notification
                val notification = notificationsList[viewHolder.adapterPosition]

                // Delete from Firestore
                deleteNotificationFromFirestore(notification)

                // Remove from the local list
                notificationsList.removeAt(viewHolder.adapterPosition)
                adapter.notifyItemRemoved(viewHolder.adapterPosition)

                // Optionally, show a Toast to confirm deletion
                Toast.makeText(this@Notification, "Notification deleted", Toast.LENGTH_SHORT).show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun deleteNotificationFromFirestore(notification: Notif) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificationsRef = db.collection("notifications").document(userId)

        // Delete the notification by its documentId
        notificationsRef.collection(getNotificationTypeCollection(notification.title))
            .document(notification.documentId)  // Use the document ID for deletion
            .delete()
            .addOnSuccessListener {
                // Optionally, show a success message if needed
                Toast.makeText(this, "Notification deleted from Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Handle errors (e.g., network failure)
                Toast.makeText(this, "Error deleting notification: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun getNotificationTypeCollection(title: String): String {
        return when (title) {
            "Reminder Alert!" -> "reminders"
            "Emergency Alert!" -> "emergencyLogs"
            "Assistance Alert!" -> "assistanceLogs"
            else -> "notifications"  // Default collection if title doesn't match
        }
    }
}
