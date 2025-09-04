package com.safestride.safestride

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class EmergencyLogs : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EmergencyLogsAdapter
    private val logsList = mutableListOf<EmergencyLogModel>() // List of logs
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_logs)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid

        // 🔹 Apply Window Insets listener for proper layout adjustment
        val emergencyLayout = findViewById<RelativeLayout>(R.id.emergency)
        ViewCompat.setOnApplyWindowInsetsListener(emergencyLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 Back Arrow Click Listener
        findViewById<View>(R.id.backArrowIcon).setOnClickListener {
            finish() // Close the activity
        }

        // 🔹 RecyclerView setup
        recyclerView = findViewById(R.id.recyclerEmergencyLogs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = EmergencyLogsAdapter(logsList)
        recyclerView.adapter = adapter

        // 🔹 Load emergency logs from Firestore
        loadEmergencyLogsFromFirestore()
    }

    // 🔹 Fetch emergency logs from Firestore in real-time
    private fun loadEmergencyLogsFromFirestore() {
        if (userId != null) {
            db.collection("profiles").document(userId!!)
                .collection("emergencyLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error loading logs: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val newLogsList = mutableListOf<EmergencyLogModel>()
                    if (snapshots != null) {
                        for (document in snapshots.documents) {
                            val log = EmergencyLogModel(
                                id = document.id,
                                timestamp = document.getLong("timestamp") ?: 0,
                                status = document.getString("status") ?: "Unknown",
                                location = document.getString("location") ?: "Unknown",
                                resolved = document.getBoolean("resolved") ?: false
                            )
                            newLogsList.add(log)
                        }

                        // 🔹 Update UI only if data has changed
                        logsList.clear()
                        logsList.addAll(newLogsList)
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    // 🔹 Format timestamp into a readable date/time string
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a, MMM dd yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
