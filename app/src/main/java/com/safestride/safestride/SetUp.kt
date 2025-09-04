package com.safestride.safestride

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.animation.AnimationUtils

class SetUp : AppCompatActivity() {

    private val typingDelay: Long = 50 // Typing speed (adjust as needed)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_up)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Connect Device Button
        findViewById<Button>(R.id.connectDeviceButton).setOnClickListener {
            startActivity(Intent(this, Connect::class.java))
        }

        // Back Arrow Icon
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }

        // Logo Animation (Rotating)
        val logoImageView: ImageView = findViewById(R.id.logoImageView)
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_logo)
        logoImageView.startAnimation(rotateAnimation)

        // Instruction Text Typing Animation
        val instructionTextView: TextView = findViewById(R.id.deviceSetupInstructions)
        val fullText = "Setting up the Device requires Bluetooth or GPS and SafeStride Device."
        animateText(instructionTextView, fullText)
    }

    // Typing animation method
    private fun animateText(textView: TextView, text: String) {
        val handler = Handler(Looper.getMainLooper())
        textView.text = ""
        for (i in text.indices) {
            handler.postDelayed({
                textView.append(text[i].toString())
            }, typingDelay * i)
        }
    }
}
