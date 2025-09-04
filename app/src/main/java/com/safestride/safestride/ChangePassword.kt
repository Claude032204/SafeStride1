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

class ChangePassword : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val changeLayout = findViewById<RelativeLayout>(R.id.change)

        // Apply Window Insets listener to adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(changeLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars
                ())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }

        // Back Arrow Click Listener
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            // Navigate to the Settings activity
            val intent = Intent(this, Settings::class.java)  // Replace with your Settings Activity
            startActivity(intent)
            finish()  // Optional: Close the ChangePassword activity to prevent going back
        }

        // Initialize the EditText for the email, old password, and repassword fields
        val emailEditText: EditText = findViewById(R.id.emailFieldForReset)
        val oldPasswordField: EditText = findViewById(R.id.repasswordField)
        val confirmEmailButton: Button = findViewById(R.id.confirmEmailButton)

        // Eye Icon for Password Visibility Toggle
        val eyeIconPassword: ImageView = findViewById(R.id.eyeIconPassword)
        val passwordField: EditText = findViewById(R.id.repasswordField)

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

        // Set click listener for the "Confirm Email" button
        confirmEmailButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val oldPassword = oldPasswordField.text.toString()

            // Check if the email field or password field is empty
            if (email.isEmpty() || oldPassword.isEmpty()) {
                // Show a toast message if any of the fields is empty
                Toast.makeText(this, "Please enter both your email and old password", Toast.LENGTH_SHORT).show()
            } else {
                // If both fields are filled, proceed with the logic
                val intent = Intent(this, SetNewPassword::class.java)
                intent.putExtra("user_email", email)  // Pass the email to the SetNewPassword Activity
                startActivity(intent)
                finish()  // Close the Change Password activity to prevent going back
            }
        }
    }
}
