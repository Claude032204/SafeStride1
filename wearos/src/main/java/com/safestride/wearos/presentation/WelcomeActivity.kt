package com.safestride.wearos.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.safestride.wearos.R

class WelcomeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnOk: Button = findViewById(R.id.btn_ok)
        btnOk.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
