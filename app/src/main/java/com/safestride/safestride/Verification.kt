package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class Verification : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var resendCodeText: TextView
    private lateinit var confirmCodeButton: Button
    private var timer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 60000  // 1 minute
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        // Find the RelativeLayout by its ID
        val verifyLayout = findViewById<RelativeLayout>(R.id.verify)

        // Apply Window Insets listener to adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(verifyLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        // Back Arrow Click Listener
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
            finish()
        }

        // Retrieve the email passed from ForgotPasswordActivity
        val userEmail = intent.getStringExtra("user_email")

        // Set the email text dynamically in the TextView
        val verificationSentText: TextView = findViewById(R.id.verificationSentText)
        verificationSentText.text = "Verification sent to: $userEmail"

        confirmCodeButton = findViewById(R.id.confirmCodeButton)
        timerText = findViewById(R.id.timerText)
        resendCodeText = findViewById(R.id.resendCodeText)

        // Start the timer
        startTimer()

        // Resend code click listener
        resendCodeText.setOnClickListener {
            resendVerificationCode(userEmail)
            restartTimer()
        }

        // Confirm Code Button click listener
        confirmCodeButton.setOnClickListener {
            // Always allow the user to go to the login page
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
            finish()  // End current activity
        }
    }

    // Start the countdown timer
    private fun startTimer() {
        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                val seconds = (millisUntilFinished / 1000).toInt()
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                timerText.text = String.format("%02d:%02d", minutes, remainingSeconds)
            }

            override fun onFinish() {
                timerText.text = "00:00"
                resendCodeText.isClickable = true  // Enable the "Resend" text when timer finishes
                resendCodeText.setTextColor(android.graphics.Color.WHITE)  // Set the text color to white to indicate it is clickable
            }
        }.start()
    }

    // Restart the timer for resend functionality
    private fun restartTimer() {
        timeLeftInMillis = 60000  // Reset timer to 1 minute
        timer?.cancel()
        startTimer()  // Start the timer again
    }

    // Resend verification code logic
    private fun resendVerificationCode(email: String?) {
        if (!email.isNullOrEmpty()) {
            // Resend the email verification link
            auth.currentUser?.sendEmailVerification()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to resend email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
