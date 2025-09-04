package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth  // Firebase Auth instance
    private lateinit var db: FirebaseFirestore  // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // References to the fields in your layout
        val usernameEditText: EditText = findViewById(R.id.usernameField) // Username field
        val emailEditText: EditText = findViewById(R.id.emailField) // Email field
        val passwordEditText: EditText = findViewById(R.id.passwordField) // Password field
        val confirmPasswordEditText: EditText = findViewById(R.id.confirmPasswordField) // Confirm password field
        val signUpButton: Button = findViewById(R.id.signupButton)
        val alreadyHaveAccountText: TextView = findViewById(R.id.loginText)
        val eyeIconPassword: ImageView = findViewById(R.id.eyeIconPassword)
        val eyeIconConfirmPassword: ImageView = findViewById(R.id.eyeIconConfirmPassword)
        val passwordRequirementsText: TextView = findViewById(R.id.passwordRequirementsText)

        // TextWatcher to monitor form fields
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isFormFilled = usernameEditText.text.isNotEmpty() &&
                        emailEditText.text.isNotEmpty() &&
                        passwordEditText.text.isNotEmpty() &&
                        confirmPasswordEditText.text.isNotEmpty() &&
                        isValidEmail(emailEditText.text.toString()) &&
                        passwordEditText.text.toString() == confirmPasswordEditText.text.toString() // Ensure passwords match
                signUpButton.isEnabled = isFormFilled
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        // Attach TextWatcher to each field
        usernameEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Show password requirements only if password is not empty
                if (passwordEditText.text.isNotEmpty() && !isPasswordValid(passwordEditText.text.toString())) {
                    passwordRequirementsText.visibility = TextView.VISIBLE
                } else {
                    passwordRequirementsText.visibility = TextView.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        confirmPasswordEditText.addTextChangedListener(textWatcher)

        // Ensure password input is hidden initially
        passwordEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        confirmPasswordEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Eye Icon for password toggle
        eyeIconPassword.setOnClickListener {
            togglePasswordVisibility(passwordEditText, eyeIconPassword)
        }

        // Eye Icon for confirm password toggle
        eyeIconConfirmPassword.setOnClickListener {
            togglePasswordVisibility(confirmPasswordEditText, eyeIconConfirmPassword)
        }

        // Sign-Up Button Click Listener (Email/Password sign up)
        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else if (!isValidEmail(email)) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            } else if (!isPasswordValid(password)) {
                Toast.makeText(this, "Password does not meet the requirements", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // Sign out any previously logged-in user
                auth.signOut()

                // Proceed with Firebase sign-up logic
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                val user = hashMapOf(
                                    "username" to username,
                                    "email" to email
                                )

                                // Save user data to Firestore
                                db.collection("users").document(userId).set(user)
                                    .addOnSuccessListener {
                                        // Fetch the user data immediately after saving it
                                        fetchUserData(userId)
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(this, "Error saving user info: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }

                                // Send email verification
                                val currentUser = auth.currentUser
                                currentUser?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                                    if (verificationTask.isSuccessful) {
                                        // Show card that informs the user to check their email
                                        showVerificationCard()
                                    } else {
                                        Toast.makeText(this, "Failed to send verification email: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "User ID is null, sign up failed.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Navigate to Log In when "Already have an account?" is clicked
        alreadyHaveAccountText.setOnClickListener {
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }

        // Back Arrow Click Listener
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            val intent = Intent(this, LandingPage::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Basic email validation method
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Password validation method
    private fun isPasswordValid(password: String): Boolean {
        val regex =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\$%^&*(),.?\":{}|<>]{8,}\$"
        return password.matches(regex.toRegex())
    }

    // Toggle password visibility
    private fun togglePasswordVisibility(passwordEditText: EditText, eyeIcon: ImageView) {
        if (passwordEditText.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            passwordEditText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            eyeIcon.setImageResource(R.drawable.openeye)
        } else {
            passwordEditText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            eyeIcon.setImageResource(R.drawable.eyeclosed)
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    // Fetch user data after sign-up
    private fun fetchUserData(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "Unknown"
                    val email = document.getString("email") ?: "No email"
                    Toast.makeText(this, "Welcome, $username! Email: $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch user data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Show a dialog informing the user to verify their email
    private fun showVerificationCard() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_verification, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)  // Prevent the user from closing it until they verify
            .create()

        val confirmButton = dialogView.findViewById<Button>(R.id.confirmVerificationButton)
        val generateLinkButton = dialogView.findViewById<Button>(R.id.generateVerificationLinkButton)
        val timerTextView = dialogView.findViewById<TextView>(R.id.timerTextView)
        val closeButton = dialogView.findViewById<ImageView>(R.id.closeDialogButton)  // The "X" button

        // Set up the timer (1 minute countdown)
        val timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                timerTextView.text = "Time remaining: $secondsRemaining sec"
            }

            override fun onFinish() {
                // When timer finishes, enable "Generate new verification link" button
                generateLinkButton.isEnabled = true
            }
        }

        timer.start()

        // Button click listeners
        confirmButton.setOnClickListener {
            checkEmailVerificationStatus(dialog)
        }

        generateLinkButton.setOnClickListener {
            // Generate a new verification link after the timeout
            sendVerificationEmailAgain()
        }

        // Close button click listener (Dismiss the dialog)
        closeButton.setOnClickListener {
            dialog.dismiss()  // Dismiss the dialog when the "X" button is clicked
        }

        dialog.show()
    }


    // Send the verification email again
    private fun sendVerificationEmailAgain() {
        val currentUser = auth.currentUser
        currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email sent again!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to resend verification email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Check email verification status
    private fun checkEmailVerificationStatus(dialog: AlertDialog) {
        val currentUser = auth.currentUser
        currentUser?.reload()?.addOnCompleteListener { reloadTask ->
            if (reloadTask.isSuccessful) {
                if (currentUser.isEmailVerified) {
                    // Email verified, allow user to go to the Dashboard
                    val intent = Intent(this, Dashboard::class.java)
                    startActivity(intent)
                    finish()
                    dialog.dismiss()
                } else {
                    // Email not verified yet, show message again
                    Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to reload user data.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
