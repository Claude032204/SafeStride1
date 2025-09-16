package com.safestride.wearos.presentation

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.wearable.Wearable
import com.safestride.safestride.shared.WearPaths

class WatchLocationService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var callback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)

        // Tight, smooth, no batching
        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(1f)
            .setMaxUpdateDelayMillis(0L)
            .build()

        callback = object : LocationCallback() {
            private var lastLat = Double.NaN
            private var lastLng = Double.NaN

            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val acc = if (loc.hasAccuracy()) loc.accuracy else 999f
                if (acc > 50f) return  // ignore poor fixes

                // de-jitter: only send if moved ≥ 3m from last sent
                if (!lastLat.isNaN()) {
                    val d = haversineMeters(lastLat, lastLng, loc.latitude, loc.longitude)
                    if (d < 3) return
                }
                lastLat = loc.latitude
                lastLng = loc.longitude

                val payload = "${loc.latitude},${loc.longitude}".toByteArray(Charsets.UTF_8)
                Wearable.getNodeClient(this@WatchLocationService).connectedNodes
                    .addOnSuccessListener { nodes ->
                        nodes.forEach { node ->
                            Wearable.getMessageClient(this@WatchLocationService)
                                .sendMessage(node.id, WearPaths.LOCATION, payload)
                        }
                    }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Permission guard (prevents SecurityException on Android 14+)
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            Log.e("WatchService", "Missing location permission, stopping.")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, createNotification())
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        return START_STICKY
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val id = "watch_location_channel"
        val name = "SafeStride Watch Location"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, id)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("SafeStride Watch")
            .setContentText("Streaming location…")
            .setOngoing(true)
            .build()
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2)*Math.sin(dLon/2)
        return 2 * R * Math.asin(Math.sqrt(a))
    }

    companion object { private const val NOTIF_ID = 1001 }
}
