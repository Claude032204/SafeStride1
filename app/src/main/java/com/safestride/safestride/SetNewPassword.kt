package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SetNewPassword : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_new_password)

        // Find the RelativeLayout by its ID
        val setnewLayout = findViewById<RelativeLayout>(R.id.setnew)

        // Apply Window Insets listener to adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(setnewLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        val passwordField: EditText = findViewById(R.id.passwordField)
        val confirmPasswordField: EditText = findViewById(R.id.confirmPasswordField)
        val confirmPasswordButton: Button = findViewById(R.id.confirmPasswordButton)

        val eyeIconPassword: ImageView = findViewById(R.id.eyeIconPassword)
        val eyeIconConfirmPassword: ImageView = findViewById(R.id.eyeIconConfirmPassword)

        // Back Arrow Click Listener
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            // Navigate to the Forgot Password activity
            val intent = Intent(this, ChangePassword::class.java)
            startActivity(intent)
            finish()  // Optional: Close the Verification activity to prevent going back
        }

        // Eye Icon Click to toggle visibility of Password
        eyeIconPassword.setOnClickListener {
            // Check if the current input type is password (hidden)
            if (passwordField.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                // Change to visible (normal text)
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                eyeIconPassword.setImageResource(R.drawable.openeye) // Update the icon to 'open eye'
            } else {
                // Change back to hidden (password)
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                eyeIconPassword.setImageResource(R.drawable.eyeclosed) // Update the icon to 'closed eye'
            }
            // Move the cursor to the end of the text
            passwordField.setSelection(passwordField.text.length)
        }

// Eye Icon Click to toggle visibility of Confirm Password
        eyeIconConfirmPassword.setOnClickListener {
            // Check if the current input type is password (hidden)
            if (confirmPasswordField.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                // Change to visible (normal text)
                confirmPasswordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                eyeIconConfirmPassword.setImageResource(R.drawable.openeye) // Update the icon to 'open eye'
            } else {
                // Change back to hidden (password)
                confirmPasswordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                eyeIconConfirmPassword.setImageResource(R.drawable.eyeclosed) // Update the icon to 'closed eye'
            }
            // Move the cursor to the end of the text
            confirmPasswordField.setSelection(confirmPasswordField.text.length)
        }

        // Confirm Password Button click listener
        confirmPasswordButton.setOnClickListener {
            val password = passwordField.text.toString()
            val confirmPassword = confirmPasswordField.text.toString()

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                // Logic to save the new password and proceed
                Toast.makeText(this, "Password successfully changed", Toast.LENGTH_SHORT).show()

                // Navigate to the LogIn page
                val intent = Intent(this, LogIn::class.java)
                startActivity(intent)
                finish()  // Close the NewPasswordActivity to prevent the user from going back
            }
        }
    }
}

