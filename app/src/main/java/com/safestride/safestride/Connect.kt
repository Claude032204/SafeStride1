package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Connect : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        // Set padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle back arrow icon click
        val backArrowIcon: ImageView = findViewById(R.id.backArrowIcon)
        backArrowIcon.setOnClickListener {
            val intent = Intent(this, SetUp::class.java)
            startActivity(intent)
            finish() // Optional: Close the current activity
        }

        // Simulate Bluetooth connection and update UI
        val deviceInfoLayout: LinearLayout = findViewById(R.id.deviceInfoLayout)
        val deviceNameText: TextView = findViewById(R.id.deviceName)
        val deviceStatusText: TextView = findViewById(R.id.deviceStatus)

        // Hide device info layout initially
        deviceInfoLayout.visibility = View.GONE

        // Simulate device found after 5 seconds
        Handler().postDelayed({
            // Update device info
            deviceNameText.text = "Samsung Galaxy Watch 4"
            deviceStatusText.text = "Connected"

            // Show device info layout with animation
            deviceInfoLayout.apply {
                visibility = View.VISIBLE
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(1000) // 1 second fade-in animation
                    .start()
            }

            // Update the title text
            findViewById<TextView>(R.id.connectDeviceTitle).text = "Device Connected"

            // Hide the setup instructions
            findViewById<TextView>(R.id.deviceSetupInstructions).visibility = View.GONE
        }, 5000) // Delay of 5 seconds

        // Get Started button logic
        val connectButton: Button = findViewById(R.id.connectDeviceButton)

        // Ensure the button is enabled and clickable
        connectButton.isEnabled = true
        connectButton.isClickable = true

        // Set up onClickListener for "Get Started!" button
        connectButton.setOnClickListener {
            // Log to confirm button click (optional)
            println("Get Started button clicked")

            // Intent to navigate to the Dashboard
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
            finish() // Close the current activity to prevent going back
        }
    }
}
