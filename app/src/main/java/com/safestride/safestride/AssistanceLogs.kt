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

class AssistanceLogs : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AssistanceLogsAdapter
    private val logsList = mutableListOf<AssistanceLogModel>() // Logs list
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistance_logs)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid

        // 🔹 Apply Window Insets listener for proper layout adjustment
        val assistanceLayout = findViewById<RelativeLayout>(R.id.assistance)
        ViewCompat.setOnApplyWindowInsetsListener(assistanceLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 Back Arrow Click Listener
        findViewById<View>(R.id.backArrowIcon).setOnClickListener {
            finish() // Close the activity
        }

        // 🔹 RecyclerView setup
        recyclerView = findViewById(R.id.recyclerAssistanceLogs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AssistanceLogsAdapter(logsList)
        recyclerView.adapter = adapter

        // 🔹 Load assistance logs from Firestore
        loadAssistanceLogsFromFirestore()
    }

    // 🔹 Fetch assistance logs from Firestore in real-time
    private fun loadAssistanceLogsFromFirestore() {
        if (userId != null) {
            db.collection("profiles").document(userId!!)
                .collection("assistanceLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error loading logs: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val newLogsList = mutableListOf<AssistanceLogModel>()
                    if (snapshots != null) {
                        for (document in snapshots.documents) {
                            val log = AssistanceLogModel(
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
}
