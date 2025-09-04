package com.safestride.safestride

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Settings : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var switchGpsTracking: Switch
    private lateinit var buttonViewLastKnownLocation: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseFirestore

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Find the RelativeLayout by its ID
        val settingsLayout = findViewById<RelativeLayout>(R.id.settings)

        // Apply Window Insets listener to adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(settingsLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        // Fetch and display the username in the navigation drawer
        fetchUsernameFromFirestore()

        // Handle the Menu Icon click to open the drawer
        val menuIcon: ImageView = findViewById(R.id.menuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START) // Open the navigation drawer
        }

        // Handle Profile Section Click
        val navigationHeaderView = navigationView.getHeaderView(0)
        val profileSection: LinearLayout? = navigationHeaderView?.findViewById(R.id.profileSection)

        profileSection?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Handle menu item selection
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleMenuSelection(menuItem)
            drawerLayout.closeDrawers() // Close drawer after selection
            true
        }

        // Switches
        val switchShowConnectedDevice: Switch = findViewById(R.id.switchShowConnectedDevice)
        switchGpsTracking = findViewById(R.id.switchGpsTracking)

        // Buttons
        val buttonReconnectWatch: Button = findViewById(R.id.buttonReconnectWatch)
        buttonViewLastKnownLocation = findViewById(R.id.buttonViewLastKnownLocation)
        val buttonEditProfile: Button = findViewById(R.id.buttonEditProfile)
        val buttonChangePassword: Button = findViewById(R.id.buttonChangePassword)
        val buttonLogout: Button = findViewById(R.id.buttonLogout)
        val buttonDeleteAccount: Button = findViewById(R.id.buttonDeleteAccount)

        // Load GPS Tracking status from SharedPreferences
        val isGpsEnabled = sharedPreferences.getBoolean("gps_tracking_enabled", false)
        switchGpsTracking.isChecked = isGpsEnabled
        buttonViewLastKnownLocation.isEnabled =
            isGpsEnabled // Enable only if GPS was enabled before

        // Handle GPS Tracking switch toggle
        switchGpsTracking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showLocationPermissionDialog()
            } else {
                saveGpsTrackingStatus(false)
                buttonViewLastKnownLocation.isEnabled = false // Disable button if switch is OFF
            }
        }

        // View Last Location (Google Maps) button
        buttonViewLastKnownLocation.setOnClickListener {
            if (switchGpsTracking.isChecked) {
                showPasswordConfirmationDialogForGps()
            } else {
                Toast.makeText(this, "Enable GPS Tracking first!", Toast.LENGTH_SHORT).show()
            }
        }

        // Reconnect Watch
        buttonReconnectWatch.setOnClickListener {
            startActivity(Intent(this, Connect::class.java))
        }

        // Edit Profile
        buttonEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Change Password
        buttonChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePassword::class.java))
        }

        // Logout Button
        buttonLogout.setOnClickListener {
            logoutUser()
        }
    }

    // Function to fetch username from Firestore and display it in the navigation drawer
    private fun fetchUsernameFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username")
                        if (!username.isNullOrEmpty()) {
                            // Fetch the navigation header view
                            val navigationHeaderView = navigationView.getHeaderView(0)

                            // Find the TextView in the header and set the username
                            val headerUsernameTextView =
                                navigationHeaderView?.findViewById<TextView>(R.id.usernameTextView)
                            headerUsernameTextView?.text =
                                username // Update the username in the drawer header
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Error fetching username: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun showLocationPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Allow \"Maps\" to use your location?")
        builder.setMessage("SafeStride uses location data to provide accurate GPS tracking.")

        builder.setPositiveButton("Allow") { _: DialogInterface, _: Int ->
            requestLocationPermission()
        }

        builder.setNegativeButton("Deny") { dialog: DialogInterface, _: Int ->
            switchGpsTracking.isChecked = false
            saveGpsTrackingStatus(false)
            buttonViewLastKnownLocation.isEnabled = false
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            switchGpsTracking.isChecked = true
            saveGpsTrackingStatus(true)
            buttonViewLastKnownLocation.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                switchGpsTracking.isChecked = true
                saveGpsTrackingStatus(true)
                buttonViewLastKnownLocation.isEnabled = true
            } else {
                switchGpsTracking.isChecked = false
                saveGpsTrackingStatus(false)
                buttonViewLastKnownLocation.isEnabled = false
            }
        }
    }

    private fun saveGpsTrackingStatus(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("gps_tracking_enabled", isEnabled).apply()
    }

    private fun handleMenuSelection(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.nav_home -> startActivity(Intent(this, Dashboard::class.java))
            R.id.nav_settings -> startActivity(Intent(this, Settings::class.java))
            R.id.nav_maps -> {
                val isGpsEnabled = sharedPreferences.getBoolean("gps_tracking_enabled", false)
                if (isGpsEnabled) {
                    startActivity(Intent(this, MapsActivity::class.java))
                } else {
                    Toast.makeText(
                        this,
                        "Enable GPS Tracking in Settings first!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            R.id.nav_about -> startActivity(Intent(this, About::class.java))
            R.id.nav_sign_out -> logoutUser() // Handle Sign Out from the Drawer
        }
    }

    // Logout function to handle Firebase sign-out
    private fun logoutUser() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // 🔹 Stop Firestore operations before logging out
        db.clearPersistence().addOnCompleteListener {
            db.terminate().addOnCompleteListener {
                // ✅ Sign out after Firestore has been stopped
                auth.signOut()

                // ✅ Clear SharedPreferences (if needed)
                sharedPreferences.edit().clear().apply()

                // ✅ Redirect to LandingPage or login screen
                val intent = Intent(this, LandingPage::class.java)
                startActivity(intent)
                finish() // ✅ Close current activity after logout
            }
        }
    }

    fun onDeleteAccountClick(view: View) {
        showDeleteConfirmationDialog()
    }

    // 🔹 Step 1: Show Delete Confirmation Dialog
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Account")
        builder.setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")

        builder.setPositiveButton("Yes") { _, _ ->
            showPasswordConfirmationDialog()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    // 🔹 Step 2: Show Password Confirmation Dialog
    private fun showPasswordConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = dialogView.findViewById<EditText>(R.id.confirmPasswordInput)
        val eyeIconPassword = dialogView.findViewById<ImageView>(R.id.eyeIconPassword)
        val eyeIconConfirmPassword = dialogView.findViewById<ImageView>(R.id.eyeIconConfirmPassword)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Password")
        builder.setView(dialogView)

        builder.setPositiveButton("Delete Account") { _, _ ->

            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            deleteUserAccount(password)
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()

        eyeIconPassword.setOnClickListener {
            togglePasswordVisibility(passwordInput, eyeIconPassword)
        }

        eyeIconConfirmPassword.setOnClickListener {
            togglePasswordVisibility(confirmPasswordInput, eyeIconConfirmPassword)
        }
    }

    // 🔹 Toggle Password Visibility
    private fun togglePasswordVisibility(editText: EditText, eyeIcon: ImageView) {
        if (editText.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            eyeIcon.setImageResource(R.drawable.openeye)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            eyeIcon.setImageResource(R.drawable.eyeclosed)
        }
        editText.setSelection(editText.text.length)
    }

    private fun deleteUserAccount(password: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, password)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    val userId = user.uid

                    db.collection("users").document(userId)
                        .delete()
                        .addOnSuccessListener {
                            user.delete()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Account deleted successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // ✅ Sign out and go to LandingPage
                                    FirebaseAuth.getInstance().signOut()

                                    val intent = Intent(this, LandingPage::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Failed to delete account: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Error deleting user data: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Incorrect password. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // 🔹 Method to trigger password confirmation for GPS access
    private fun showPasswordConfirmationDialogForGps() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = dialogView.findViewById<EditText>(R.id.confirmPasswordInput)
        val eyeIconPassword = dialogView.findViewById<ImageView>(R.id.eyeIconPassword)
        val eyeIconConfirmPassword = dialogView.findViewById<ImageView>(R.id.eyeIconConfirmPassword)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Password")
        builder.setView(dialogView)

        builder.setPositiveButton("Confirm") { _, _ ->

            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Authenticate the password with Firebase
            authenticatePasswordForGps(password)
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()

        eyeIconPassword.setOnClickListener {
            togglePasswordVisibility(passwordInput, eyeIconPassword)
        }

        eyeIconConfirmPassword.setOnClickListener {
            togglePasswordVisibility(confirmPasswordInput, eyeIconConfirmPassword)
        }
    }

    // Function to authenticate the password for GPS access
    private fun authenticatePasswordForGps(password: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, password)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // If password is correct, allow the user to view maps
                    startActivity(Intent(this, MapsActivity::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
