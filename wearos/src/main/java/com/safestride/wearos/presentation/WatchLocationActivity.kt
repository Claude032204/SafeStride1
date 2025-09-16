package com.safestride.wearos.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.safestride.wearos.R

class WatchLocationActivity : ComponentActivity() {

    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var statusText: TextView

    private val askFine = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startStreaming() else toast("Location permission denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_location)

        startBtn = findViewById(R.id.startButton)
        stopBtn  = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)

        startBtn.setOnClickListener {
            val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            if (fine == PackageManager.PERMISSION_GRANTED) startStreaming()
            else askFine.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        stopBtn.setOnClickListener { stopStreaming() }

        updateUi(false)
    }

    private fun startStreaming() {
        startService(Intent(this, WatchLocationService::class.java))
        toast("Watch streaming started")
        updateUi(true)
    }

    private fun stopStreaming() {
        stopService(Intent(this, WatchLocationService::class.java))
        toast("Watch streaming stopped")
        updateUi(false)
    }

    private fun updateUi(streaming: Boolean) {
        statusText.text = if (streaming) "Streaming…" else "Not streaming"
        startBtn.isEnabled = !streaming
        stopBtn.isEnabled = streaming
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
