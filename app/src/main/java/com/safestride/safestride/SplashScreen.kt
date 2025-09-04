package com.safestride.safestride

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.animation.BounceInterpolator

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val landingLayout = findViewById<RelativeLayout>(R.id.splash)
        ViewCompat.setOnApplyWindowInsetsListener(landingLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the TextView for "SafeStride" and ImageView for the logo
        val splashText = findViewById<TextView>(R.id.splash_text)
        val splashLogo = findViewById<ImageView>(R.id.splash_logo)

        // Bounce animation for SafeStride text from the bottom to the middle
        val bounceTextAnimator = ObjectAnimator.ofFloat(splashText, "translationY", 1000f, 0f)
        bounceTextAnimator.duration = 1000  // Duration of the animation (1 second)
        bounceTextAnimator.interpolator = BounceInterpolator()  // Bounce effect

        // Bounce animation for Logo from the bottom to the middle
        val bounceLogoAnimator = ObjectAnimator.ofFloat(splashLogo, "translationY", 1000f, 0f)
        bounceLogoAnimator.duration = 1000  // Duration of the animation (1 second)
        bounceLogoAnimator.interpolator = BounceInterpolator()  // Bounce effect

        // Start both animations
        bounceTextAnimator.start()
        bounceLogoAnimator.start()

        Handler().postDelayed({
            // After the delay, start the LandingPageActivity
            val intent = Intent(this@SplashScreen, LandingPage::class.java)
            startActivity(intent)

            // Close SplashActivity to prevent going back to it
            finish()
        }, 5000)  // 5000 milliseconds = 5 seconds
    }
}
