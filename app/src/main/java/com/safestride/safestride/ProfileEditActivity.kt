package com.safestride.safestride

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.*

class ProfileEditActivity : AppCompatActivity() {
    private lateinit var birthdateEditText: EditText
    private lateinit var calendarIcon: ImageView
    private lateinit var profileImageView: ImageView
    private lateinit var fullNameEditText: EditText
    private lateinit var usernameEditText: EditText  // Added username field
    private lateinit var emailAddressEditText: EditText
    private lateinit var contactNumberEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PROFILE_IMAGE_URI_KEY = "profileImageUri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        val profileLayout = findViewById<RelativeLayout>(R.id.profile)
        ViewCompat.setOnApplyWindowInsetsListener(profileLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.backArrowIcon).setOnClickListener { finish() }

        // Initialize UI elements
        birthdateEditText = findViewById(R.id.birthdateEditText)
        calendarIcon = findViewById(R.id.calendarIcon)
        profileImageView = findViewById(R.id.profileIcon)
        fullNameEditText = findViewById(R.id.fullNameEditText)
        usernameEditText = findViewById(R.id.usernameEditText)  // Initialize the username field
        emailAddressEditText = findViewById(R.id.emailAddressEditText)
        contactNumberEditText = findViewById(R.id.contactNumberEditText)
        addressEditText = findViewById(R.id.addressEditText)
        saveButton = findViewById(R.id.saveButton)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            emailAddressEditText.setText(user.email)
            sharedPreferences = getUserPreferences(user.uid)
        }

        loadUserDataLocally()
        loadUserDataFromFirestore()

        val dateClickListener = {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                birthdateEditText.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
            }, year, month, day).show()
        }

        birthdateEditText.setOnClickListener { dateClickListener() }
        calendarIcon.setOnClickListener { dateClickListener() }

        saveButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString()
            val username = usernameEditText.text.toString() // Get the username from EditText
            saveUserDataToFirestore(fullName, username)  // Pass the username

            val editor = sharedPreferences.edit()
            editor.putString("fullName", fullName)
            editor.putString("username", username)  // Save username locally
            editor.apply()

            val resultIntent = Intent().apply {
                putExtra("updatedFullName", fullName)
                putExtra("updatedUsername", username)  // Send updated username back
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        profileImageView.setOnClickListener { checkStoragePermission() }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PICK_IMAGE_REQUEST)
            } else {
                openImagePicker()
            }
        } else {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val selectedImageUri: Uri = data.data!!
            profileImageView.setImageURI(selectedImageUri)

            sharedPreferences.edit().putString(PROFILE_IMAGE_URI_KEY, selectedImageUri.toString()).apply()
        }
    }

    private fun loadUserDataLocally() {
        fullNameEditText.setText(sharedPreferences.getString("fullName", ""))
        usernameEditText.setText(sharedPreferences.getString("username", ""))  // Load username from sharedPreferences
        contactNumberEditText.setText(sharedPreferences.getString("contactNumber", ""))
        birthdateEditText.setText(sharedPreferences.getString("birthdate", ""))
        addressEditText.setText(sharedPreferences.getString("address", ""))
    }

    private fun saveUserDataToFirestore(fullName: String, username: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            val contactNumber = contactNumberEditText.text.toString()
            val birthdate = birthdateEditText.text.toString()
            val address = addressEditText.text.toString()

            saveUserDataLocally(fullName, contactNumber, birthdate, address, username)

            val userData: MutableMap<String, Any> = hashMapOf(
                "fullName" to fullName,
                "username" to username,  // Add username to the data being saved
                "contactNumber" to contactNumber,
                "birthdate" to birthdate,
                "address" to address
            )

            db.collection("users").document(userId)
                .update(userData)
                .addOnSuccessListener { Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun saveUserDataLocally(fullName: String, contactNumber: String, birthdate: String, address: String, username: String) {
        sharedPreferences.edit()
            .putString("fullName", fullName)
            .putString("username", username)  // Save username locally
            .putString("contactNumber", contactNumber)
            .putString("birthdate", birthdate)
            .putString("address", address)
            .apply()
    }

    private fun loadUserDataFromFirestore() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        fullNameEditText.setText(documentSnapshot.getString("fullName"))
                        usernameEditText.setText(documentSnapshot.getString("username"))  // Set username
                        contactNumberEditText.setText(documentSnapshot.getString("contactNumber"))
                        birthdateEditText.setText(documentSnapshot.getString("birthdate"))
                        addressEditText.setText(documentSnapshot.getString("address"))
                    }
                }
        }
    }

    private fun getUserPreferences(userId: String): SharedPreferences {
        return getSharedPreferences("UserProfileData_$userId", Context.MODE_PRIVATE)
    }
}
