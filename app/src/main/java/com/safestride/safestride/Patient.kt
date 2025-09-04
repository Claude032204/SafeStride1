package com.safestride.safestride

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.app.DatePickerDialog
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class Patient : AppCompatActivity() {

    private lateinit var fullNameEditText: EditText
    private lateinit var fullNameDisplay: TextView
    private lateinit var birthdateEditText: EditText
    private lateinit var calendarIcon: ImageView
    private lateinit var genderAutoCompleteTextView: AutoCompleteTextView
    private lateinit var bloodTypeAutoCompleteTextView: AutoCompleteTextView
    private lateinit var mobilityStatusAutoCompleteTextView: AutoCompleteTextView
    private lateinit var guardianEditText: EditText
    private lateinit var conditionEditText: EditText
    private lateinit var saveButton: Button

    private val calendar = Calendar.getInstance()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)

        val patientLayout = findViewById<RelativeLayout>(R.id.patient)
        ViewCompat.setOnApplyWindowInsetsListener(patientLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the back arrow icon by its ID
        val backArrowIcon: ImageView = findViewById(R.id.backArrowIcon)

        // Set a click listener to navigate back to the Dashboard
        backArrowIcon.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
            finish() // Optional: Close the current activity to avoid stacking
        }

        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("PatientProfile", MODE_PRIVATE)

        fullNameEditText = findViewById(R.id.fullNameEditText)
        fullNameDisplay = findViewById(R.id.fullName)
        birthdateEditText = findViewById(R.id.birthdateEditText)
        calendarIcon = findViewById(R.id.calendarIcon)
        genderAutoCompleteTextView = findViewById(R.id.genderAutoCompleteTextView)
        bloodTypeAutoCompleteTextView = findViewById(R.id.bloodTypeAutoCompleteTextView)
        mobilityStatusAutoCompleteTextView = findViewById(R.id.mobilityStatusAutoCompleteTextView)
        guardianEditText = findViewById(R.id.guardianEditText)
        conditionEditText = findViewById(R.id.conditionEditText)
        saveButton = findViewById(R.id.saveButton)

        loadPatientProfile()

        calendarIcon.setOnClickListener { openDatePickerDialog() }

        // Set up AutoCompleteTextView for Gender with options
        val genderOptions = arrayOf("Male", "Female")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        genderAutoCompleteTextView.setAdapter(genderAdapter)
        genderAutoCompleteTextView.threshold = 1 // filtering starts after 1 character

        // Set up AutoCompleteTextView for Blood Type with options
        val bloodTypeOptions = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val bloodTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodTypeOptions)
        bloodTypeAutoCompleteTextView.setAdapter(bloodTypeAdapter)
        bloodTypeAutoCompleteTextView.threshold = 1

        // Set up AutoCompleteTextView for Mobility Status with options
        val mobilityStatusOptions = arrayOf(
            "Wheelchair User",
            "Walker/Crutches User",
            "Cane User",
            "Non-Ambulatory (Unable to Walk)",
            "Limited Mobility (Can Walk with Assistance or Devices)"
        )
        val mobilityStatusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mobilityStatusOptions)
        mobilityStatusAutoCompleteTextView.setAdapter(mobilityStatusAdapter)
        mobilityStatusAutoCompleteTextView.threshold = 1

        // Attach onClickListeners for dropdown icons to reset filter and show all options
        val genderDropdownIcon = findViewById<ImageView>(R.id.dropdownIcon)
        genderDropdownIcon.setOnClickListener {
            genderAutoCompleteTextView.requestFocus() // Ensure the AutoCompleteTextView gets focus
            genderAutoCompleteTextView.showDropDown() // Show the dropdown
        }

        val bloodTypeDropdownIcon = findViewById<ImageView>(R.id.bloodTypeDropdownIcon)
        bloodTypeDropdownIcon.setOnClickListener {
            bloodTypeAutoCompleteTextView.requestFocus() // Ensure the AutoCompleteTextView gets focus
            bloodTypeAutoCompleteTextView.showDropDown() // Show the dropdown
        }

        val mobilityStatusDropdownIcon = findViewById<ImageView>(R.id.mobilityStatusDropdownIcon)
        mobilityStatusDropdownIcon.setOnClickListener {
            mobilityStatusAutoCompleteTextView.requestFocus() // Ensure the AutoCompleteTextView gets focus
            mobilityStatusAutoCompleteTextView.showDropDown() // Show the dropdown
        }

        // TextWatcher for fullNameEditText to update fullNameDisplay
        fullNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                fullNameDisplay.text = charSequence.toString()
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        // Save button action
        saveButton.setOnClickListener { savePatientProfile() }
    }

    private fun openDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)
                birthdateEditText.setText(formattedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun savePatientProfile() {
        val fullName = fullNameEditText.text.toString()
        val birthdate = birthdateEditText.text.toString()
        val gender = genderAutoCompleteTextView.text.toString()
        val bloodType = bloodTypeAutoCompleteTextView.text.toString()
        val mobilityStatus = mobilityStatusAutoCompleteTextView.text.toString()
        val condition = conditionEditText.text.toString()
        val guardian = guardianEditText.text.toString()

        val patientProfile = hashMapOf(
            "FullName" to fullName,
            "Birthdate" to birthdate,
            "Gender" to gender,
            "BloodType" to bloodType,
            "MobilityStatus" to mobilityStatus,
            "Condition" to condition,
            "Guardian" to guardian
        )

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("profiles")
                .document(userId)
                .set(patientProfile)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPatientProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("profiles")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("FullName")
                        val birthdate = document.getString("Birthdate")
                        val gender = document.getString("Gender")
                        val bloodType = document.getString("BloodType")
                        val mobilityStatus = document.getString("MobilityStatus")
                        val condition = document.getString("Condition")
                        val guardian = document.getString("Guardian")

                        // Set the values in the views
                        fullNameEditText.setText(fullName)
                        fullNameDisplay.text = fullName
                        birthdateEditText.setText(birthdate)
                        genderAutoCompleteTextView.setText(gender)
                        bloodTypeAutoCompleteTextView.setText(bloodType)
                        mobilityStatusAutoCompleteTextView.setText(mobilityStatus)
                        conditionEditText.setText(condition)
                        guardianEditText.setText(guardian)

                        // Reset the adapters to show the full list of options
                        val genderOptions = arrayOf("Male", "Female")
                        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
                        genderAutoCompleteTextView.setAdapter(genderAdapter)

                        val bloodTypeOptions = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
                        val bloodTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodTypeOptions)
                        bloodTypeAutoCompleteTextView.setAdapter(bloodTypeAdapter)

                        val mobilityStatusOptions = arrayOf(
                            "Wheelchair User",
                            "Walker/Crutches User",
                            "Cane User",
                            "Non-Ambulatory (Unable to Walk)",
                            "Limited Mobility (Can Walk with Assistance or Devices)"
                        )
                        val mobilityStatusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mobilityStatusOptions)
                        mobilityStatusAutoCompleteTextView.setAdapter(mobilityStatusAdapter)

                    } else {
                        Toast.makeText(this, "No profile data found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LandingPage::class.java)
        startActivity(intent)
        finish()
    }
}
