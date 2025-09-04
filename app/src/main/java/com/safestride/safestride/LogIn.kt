package com.safestride.safestride

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LogIn : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var failedAttempts = 0
    private var isLockedOut = false
    private lateinit var lockoutTimer: CountDownTimer
    private lateinit var lockoutCountdownText: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private var lockoutEndTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        // Initialize lockoutCountdownText before checking lockout status.
        lockoutCountdownText = findViewById(R.id.lockoutCountdownText)

        // Check lockout status on app launch
        checkLockoutStatus()

        val emailEditText: EditText = findViewById(R.id.emailField)
        val passwordEditText: EditText = findViewById(R.id.passwordField)
        val logInButton: Button = findViewById(R.id.loginButton)
        val forgotPasswordText: TextView = findViewById(R.id.forgotPasswordText)
        val signUpLinkText: TextView = findViewById(R.id.signUpLinkText)
        val eyeIconPassword: ImageView = findViewById(R.id.eyeIconPassword)

        logInButton.setOnClickListener {
            if (isLockedOut) {
                // Prevent login attempts during lockout
                Toast.makeText(this, "Account is locked. Please wait.", Toast.LENGTH_SHORT).show()
            } else {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please fill in both email and password", Toast.LENGTH_SHORT).show()
                } else {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // Reset failed attempts on successful login
                                failedAttempts = 0
                                sharedPreferences.edit().putInt("failedAttempts", failedAttempts).apply()
                                val intent = Intent(this, Dashboard::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                failedAttempts++
                                sharedPreferences.edit().putInt("failedAttempts", failedAttempts).apply()
                                // When reaching 3 failures, show warning dialog.
                                if (failedAttempts == 3) {
                                    showFailedLoginDialog()
                                } else if (failedAttempts >= 4) {
                                    // On the 4th failure, trigger lockout with countdown
                                    triggerLockout()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Authentication Failed: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                }
            }
        }

        signUpLinkText.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }

        eyeIconPassword.setOnClickListener {
            if (passwordEditText.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                passwordEditText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                eyeIconPassword.setImageResource(R.drawable.openeye)
            } else {
                passwordEditText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                eyeIconPassword.setImageResource(R.drawable.eyeclosed)
            }
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            val intent = Intent(this, LandingPage::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkLockoutStatus() {
        lockoutEndTime = sharedPreferences.getLong("lockoutEndTime", 0)
        val currentTime = System.currentTimeMillis()

        if (lockoutEndTime > currentTime) {
            // If the lockout period is still active, show the remaining countdown.
            isLockedOut = true
            val remainingTime = lockoutEndTime - currentTime
            triggerLockoutWithCountdown(remainingTime)
        } else {
            // Reset lockout status if time has passed.
            isLockedOut = false
            lockoutCountdownText.visibility = View.INVISIBLE
        }
    }

    private fun triggerLockout() {
        isLockedOut = true
        val currentTime = System.currentTimeMillis()
        lockoutEndTime = currentTime + 60000 // Lockout for 1 minute.
        sharedPreferences.edit().putLong("lockoutEndTime", lockoutEndTime).apply()

        // Start countdown immediately.
        triggerLockoutWithCountdown(60000)
    }

    private fun triggerLockoutWithCountdown(remainingTime: Long) {
        // Make the countdown text visible.
        lockoutCountdownText.visibility = View.VISIBLE
        lockoutCountdownText.text = "Locked out for 1 minute"

        lockoutTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                lockoutCountdownText.text = "Locked out: ${millisUntilFinished / 1000} seconds remaining"
            }

            override fun onFinish() {
                isLockedOut = false
                failedAttempts = 0
                sharedPreferences.edit().putInt("failedAttempts", failedAttempts).apply()
                lockoutCountdownText.visibility = View.INVISIBLE
                Toast.makeText(this@LogIn, "You can now try logging in again.", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun showFailedLoginDialog() {
        // Inflate custom dialog view (warns user that the next failure will lock them out).
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_failed_login, null)
        val retryButton = dialogView.findViewById<Button>(R.id.retryButton)
        val forgotPasswordButton = dialogView.findViewById<Button>(R.id.forgotPasswordButton)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val dialog = builder.create()

        retryButton.setOnClickListener {
            // Dismiss the dialog so user can try again.
            // Do not reset failedAttempts – keep it at 3 so that if they fail again, lockout is triggered.
            dialog.dismiss()
        }

        forgotPasswordButton.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }

        dialog.show()
    }
}