package com.safestride.safestride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.animation.ObjectAnimator
import android.view.animation.BounceInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreen : AppCompatActivity() {

    // ask for POST_NOTIFICATIONS on Android 13+
    private val askNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // You could show a toast if denied; either way, proceed.
        if (!granted) {
            Toast.makeText(this, "Notifications may be limited until you enable them.", Toast.LENGTH_SHORT).show()
        }
        goNext()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val landingLayout = findViewById<RelativeLayout>(R.id.splash)
        ViewCompat.setOnApplyWindowInsetsListener(landingLayout) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        // Animations (same as your current file)
        val splashText = findViewById<TextView>(R.id.splash_text)
        val splashLogo = findViewById<ImageView>(R.id.splash_logo)

        ObjectAnimator.ofFloat(splashText, "translationY", 1000f, 0f).apply {
            duration = 1000
            interpolator = BounceInterpolator()
        }.start()

        ObjectAnimator.ofFloat(splashLogo, "translationY", 1000f, 0f).apply {
            duration = 1000
            interpolator = BounceInterpolator()
        }.start()

        // After a short delay, request permission (or proceed if already granted / not needed)
        Handler(Looper.getMainLooper()).postDelayed({
            maybeRequestNotificationPermission()
        }, 1200) // trigger after animations finish
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                askNotifPermission.launch(perm)
                return
            }
        }
        // pre-Android 13 or already granted
        goNext()
    }

    private fun goNext() {
        // Small pause so the splash doesn’t instantly disappear
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@SplashScreen, LandingPage::class.java))
            finish()
        }, 300) // feel free to change to 0 if you want it immediate
    }
}
