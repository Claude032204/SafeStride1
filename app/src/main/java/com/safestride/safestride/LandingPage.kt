package com.safestride.safestride

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LandingPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)

        val landingLayout = findViewById<RelativeLayout>(R.id.landing)
        ViewCompat.setOnApplyWindowInsetsListener(landingLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✅ Request Notification Permission for Android 13+
        requestNotificationPermission()

        // ✅ Always Fetch & Store FCM Token
        fetchAndSaveFCMToken()

        // Initialize and set the typing animation
        val taglineText = findViewById<TextView>(R.id.taglineText)
        val fullText = "Ensuring safety and support with every step, SafeStride connects PWDs and caregivers in real time"
        startTypingAnimation(taglineText, fullText)

        // Log In Button Click Listener
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }

        // Sign Up Button Click Listener
        findViewById<Button>(R.id.signupButton).setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
    }

    // 🔹 **Request Notification Permission for Android 13+**
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101
                )
            }
        }
    }

    // 🔹 **Fetch & Store FCM Token (Only if User is Logged In)**
    private fun fetchAndSaveFCMToken() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }

                // ✅ Successfully retrieved FCM Token
                val token = task.result
                Log.d("FCM", "FCM Token: $token") // ✅ Should appear in Logcat

                // ✅ Save token to Firestore under "users" collection
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(user.uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "FCM Token successfully saved to Firestore!")
                    }
                    .addOnFailureListener {
                        Log.w("FCM", "Failed to save FCM Token", it)
                    }
            }
        } else {
            Log.d("FCM", "User is not logged in, skipping FCM token save.")
        }
    }

    // ✅ Handle Permission Request Result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("FCM", "Notification permission granted!")
            } else {
                Log.w("FCM", "Notification permission denied!")
            }
        }
    }

    // 🔹 **Start Typing Animation for the Tagline Text**
    private fun startTypingAnimation(taglineText: TextView, fullText: String) {
        // Create a ValueAnimator that updates the TextView's text gradually
        val animator = ValueAnimator.ofInt(0, fullText.length)
        animator.duration = 3000  // Duration of the typing effect (in milliseconds)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            taglineText.text = fullText.substring(0, value)
        }
        animator.start()
    }
}
