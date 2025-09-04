package com.safestride.safestride

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AssistanceLogs : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AssistanceLogsAdapter
    private val logsList = mutableListOf<AssistanceLogModel>()
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistance_logs)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid

        // Insets handling
        val assistanceLayout = findViewById<RelativeLayout>(R.id.assistance)
        ViewCompat.setOnApplyWindowInsetsListener(assistanceLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button
        findViewById<View>(R.id.backArrowIcon).setOnClickListener {
            finish()
        }

        // RecyclerView setup
        recyclerView = findViewById(R.id.recyclerAssistanceLogs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AssistanceLogsAdapter(logsList)
        recyclerView.adapter = adapter

        // Load logs
        loadAssistanceLogsFromFirestore()
    }

    private fun loadAssistanceLogsFromFirestore() {
        if (userId != null) {
            db.collection("notifications").document(userId!!)
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
                                status = document.getString("status") ?: "Assistance Alert!",
                                location = document.getString("location") ?: "Unknown",
                                resolved = document.getBoolean("resolved") ?: false
                            )
                            newLogsList.add(log)
                        }

                        // Sort newest first before updating adapter
                        logsList.clear()
                        logsList.addAll(newLogsList.sortedByDescending { it.timestamp })
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }
}
