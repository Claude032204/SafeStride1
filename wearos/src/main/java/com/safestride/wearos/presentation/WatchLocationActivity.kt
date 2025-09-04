package com.safestride.wearos.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.safestride.safestride.shared.WearPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WatchLocationActivity : AppCompatActivity() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient
    private var phoneNodeId: String? = null
    private var streaming = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            val payload = "${loc.latitude},${loc.longitude}".toByteArray()
            phoneNodeId?.let { id ->
                messageClient.sendMessage(id, WearPaths.LOCATION, payload)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val btn = Button(this).apply { text = "Start streaming to phone" }
        setContentView(btn)

        fused = LocationServices.getFusedLocationProviderClient(this)
        messageClient = Wearable.getMessageClient(this)
        nodeClient = Wearable.getNodeClient(this)

        // Resolve the connected phone once
        CoroutineScope(Dispatchers.Main).launch {
            phoneNodeId = nodeClient.connectedNodes.await()
                .firstOrNull { it.isNearby }?.id
            if (phoneNodeId == null) {
                Toast.makeText(this@WatchLocationActivity, "No phone connected", Toast.LENGTH_SHORT).show()
            }
        }

        btn.setOnClickListener {
            if (!streaming) startStreaming(btn) else stopStreaming(btn)
        }
    }

    private fun startStreaming(button: Button) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
        streaming = true
        button.text = "Stop streaming"
        Toast.makeText(this, "Streaming GPS to phone…", Toast.LENGTH_SHORT).show()
    }

    private fun stopStreaming(button: Button) {
        fused.removeLocationUpdates(callback)
        streaming = false
        button.text = "Start streaming to phone"
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (code == 100 && res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) {
            (window.decorView as? android.view.ViewGroup)?.getChildAt(0)?.let { v ->
                if (v is Button) startStreaming(v)
            }
        }
    }

    override fun onDestroy() {
        if (streaming) fused.removeLocationUpdates(callback)
        super.onDestroy()
    }
}
