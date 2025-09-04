package com.safestride.safestride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var pwdMarker: Marker? = null

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val pwdId by lazy { intent.getStringExtra("PWD_ID") ?: "pwd1" }
    private val pwdDocRef by lazy { db.collection("pwds").document(pwdId) }
    private var pwdListener: ListenerRegistration? = null

    // Demo movement for tomorrow’s presentation
    private val demoPath = listOf(
        LatLng(14.59950, 120.98420),
        LatLng(14.60080, 120.98520),
        LatLng(14.60210, 120.98610),
        LatLng(14.60330, 120.98700),
        LatLng(14.60420, 120.98790),
        LatLng(14.60310, 120.98670),
        LatLng(14.60180, 120.98560),
        LatLng(14.60040, 120.98460)
    )
    private var simulateJob: Job? = null

    private val askLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) enableMyLocation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Handle system bar insets on your root layout
        val mapsLayout = findViewById<RelativeLayout>(R.id.maps)
        ViewCompat.setOnApplyWindowInsetsListener(mapsLayout) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Bottom drawer (keep your existing layout)
        val bottomDrawer: View = findViewById(R.id.bottomDrawer)
        BottomSheetBehavior.from(bottomDrawer).state = BottomSheetBehavior.STATE_EXPANDED

        // Map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Pad map so controls aren’t covered by the drawer
        bottomDrawer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bottomDrawer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (::map.isInitialized) map.setPadding(0, 0, 0, bottomDrawer.height)
            }
        })

        // Your existing icons
        findViewById<ImageView>(R.id.notificationIcon).setOnClickListener {
            startActivity(Intent(this, Notification::class.java))
        }
        findViewById<View>(R.id.trackIcon).setOnClickListener {
            // Toggle simulated movement for demo
            if (simulateJob == null) startSimulation() else stopSimulation()
        }
        findViewById<View>(R.id.homeIcon).setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
        }
        findViewById<View>(R.id.recordIcon).setOnClickListener {
            startActivity(Intent(this, Records::class.java))
        }
        findViewById<View>(R.id.settingsIcon).setOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableMyLocation() // optional blue dot

        // Start at a sensible camera; will jump once Firestore updates arrive
        val initial = LatLng(14.5995, 120.9842)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initial, 13f))

        startPwdRealtimeListener()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            askLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startPwdRealtimeListener() {
        // Expect Firestore fields: lat(Number), lng(Number)
        pwdListener = pwdDocRef.addSnapshotListener(this) { snap, _ ->
            val lat = snap?.getDouble("lat")
            val lng = snap?.getDouble("lng")
            if (lat != null && lng != null) {
                val pos = LatLng(lat, lng)
                if (pwdMarker == null) {
                    pwdMarker = map.addMarker(MarkerOptions().position(pos).title("PWD"))
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                } else {
                    pwdMarker!!.position = pos
                    map.animateCamera(CameraUpdateFactory.newLatLng(pos))
                }
            }
        }
    }

    // ---------- DEMO: simulate movement by writing to Firestore ----------
    private fun startSimulation() {
        if (simulateJob != null) return
        Toast.makeText(this, "Simulating PWD movement…", Toast.LENGTH_SHORT).show()
        simulateJob = lifecycleScope.launch {
            var i = 0
            while (true) {
                val p = demoPath[i % demoPath.size]
                pwdDocRef.set(
                    mapOf(
                        "lat" to p.latitude,
                        "lng" to p.longitude,
                        "ts"  to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                i++
                delay(1500)
            }
        }
    }

    private fun stopSimulation() {
        simulateJob?.cancel()
        simulateJob = null
        Toast.makeText(this, "Simulation stopped.", Toast.LENGTH_SHORT).show()
    }
    // --------------------------------------------------------------------

    override fun onDestroy() {
        pwdListener?.remove()
        simulateJob?.cancel()
        super.onDestroy()
    }
}
