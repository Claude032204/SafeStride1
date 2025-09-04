package com.safestride.safestride

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.CountDownTimer
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Dashboard : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var usernameTextView: TextView

    private val db = FirebaseFirestore.getInstance()

    // Inactivity timer
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val inactivityTimeout: Long = 90000 // 1 minute of inactivity
    private val countdownTimeout: Long = 30000 // 30 seconds countdown dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout)


        // Handle Menu Icon Click
        val menuIcon: ImageView = findViewById(R.id.menuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Handle Navigation Drawer Item Clicks
        val navigationView: NavigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleMenuSelection(menuItem)
            drawerLayout.closeDrawers()
            true
        }

        // Handle Add Device Card Click
        val addDeviceCard: LinearLayout = findViewById(R.id.addDeviceCard)
        addDeviceCard.setOnClickListener {
            startActivity(Intent(this, SetUp::class.java))
        }

        // Handle Notification Bell Click
        val notificationBell: ImageView = findViewById(R.id.notificationBell)
        notificationBell.setOnClickListener {
            startActivity(Intent(this, Notification::class.java))
        }

        // Handle Patient Card Click
        val patientCard: LinearLayout = findViewById(R.id.patientCard)
        patientCard.setOnClickListener {
            startActivity(Intent(this, Patient::class.java))
        }

        // Handle Records Card Click
        val cardRecords = findViewById<LinearLayout>(R.id.cardRecords)
        cardRecords.setOnClickListener {
            startActivity(Intent(this, Records::class.java))
        }

        // Handle Reminder Card Click
        val reminderCard: LinearLayout = findViewById(R.id.cardReminder)
        reminderCard.setOnClickListener {
            startActivity(Intent(this, Reminder::class.java))
        }

        // Handle Profile Section Click
        val navigationHeaderView = navigationView.getHeaderView(0)
        val profileSection: LinearLayout? = navigationHeaderView?.findViewById(R.id.profileSection)

        profileSection?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Get the username TextView from the header view
        usernameTextView = navigationHeaderView?.findViewById(R.id.usernameTextView) ?: return

        // Fetch the user ID from Firebase Authentication
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // If user ID is available, fetch user data (username) from Firestore
        if (userId != null) {
            fetchUsernameFromFirestore(userId)
        }

        // Initialize the handler and runnable for inactivity timeout
        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            showInactivityDialog() // Show the countdown dialog if inactivity timeout is reached
        }

        // Start inactivity timer
        startInactivityTimer()

        // Set up user activity listeners
        setUserActivityListener()
    }

    private fun setUserActivityListener() {
        // Detect user actions and reset inactivity timer
        val mainLayout = findViewById<LinearLayout>(R.id.main)
        mainLayout.setOnClickListener {
            resetInactivityTimer() // Reset timer on user click
        }
    }

    // Reset the inactivity timer after the user interacts or dismisses the dialog
    fun resetInactivityTimer() {
        handler.removeCallbacks(runnable)  // Remove any existing inactivity timers
        startInactivityTimer() // Start a new inactivity timer
    }

    private fun startInactivityTimer() {
        handler.postDelayed(runnable, inactivityTimeout) // Set inactivity timeout for 1 minute
    }

    private fun showInactivityDialog() {
        // Check if the activity is not finishing or destroyed
        if (!isFinishing && !isDestroyed) {
            // Ensure that we only show the dialog if the fragment manager is in a valid state
            val existingDialog = supportFragmentManager.findFragmentByTag("InactivityDialog")

            // Check if dialog is already being shown
            if (existingDialog == null) {
                // Delay the dialog show action until the activity is in a valid state
                if (!isFinishing && !isDestroyed) {
                    val dialog = CountdownDialogFragment(countdownTimeout) { timedOut ->
                        if (timedOut) {
                            logoutUser()  // Log the user out after the countdown finishes
                        }
                    }

                    // Pass the callback to reset the inactivity timer after the dialog is dismissed
                    dialog.setOnDialogDismissedListener {
                        resetInactivityTimer()  // Reset inactivity timer when dialog is dismissed
                    }

                    // Use a fragment transaction to show the dialog fragment
                    try {
                        dialog.show(supportFragmentManager, "InactivityDialog")
                    } catch (e: IllegalStateException) {
                        Log.e("Dashboard", "Unable to show inactivity dialog: ${e.message}")
                    }
                }
            } else {
                // If the dialog already exists, just show it again if it was hidden previously
                if (existingDialog.isHidden) {
                    supportFragmentManager.beginTransaction().show(existingDialog).commit()
                }
            }
        }
    }



    private fun fetchUsernameFromFirestore(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    if (!username.isNullOrEmpty()) {
                        usernameTextView.text = username
                    }
                } else {
                    Toast.makeText(this, "No user data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching username: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
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
                    Toast.makeText(this, "Enable GPS Tracking in Settings first!", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.nav_about -> startActivity(Intent(this, About::class.java))
            R.id.nav_sign_out -> logoutUser()
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
}
