package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge

class Records : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        findViewById<RelativeLayout>(R.id.records)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.records)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set click listeners for each card
        findViewById<View>(R.id.cardEmergencyLogs).setOnClickListener {
            startActivity(Intent(this, EmergencyLogs::class.java))
        }

        findViewById<View>(R.id.cardAssistanceLogs).setOnClickListener {
            startActivity(Intent(this, AssistanceLogs::class.java))
        }

        findViewById<View>(R.id.cardNotes).setOnClickListener {
            startActivity(Intent(this, Note::class.java))
        }

        // Find the back arrow icon by its ID
        val backArrowIcon: ImageView = findViewById(R.id.backArrowIcon)

        // Set a click listener to navigate back to the Dashboard
        backArrowIcon.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
            finish() // Optional: Close the current activity to avoid stacking
        }
    }
}
