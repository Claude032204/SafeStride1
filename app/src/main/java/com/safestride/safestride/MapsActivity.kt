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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

    // ✅ separate marker explicitly for the watch
    private var watchMarker: Marker? = null

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val pwdId by lazy { intent.getStringExtra("PWD_ID") ?: "pwd1" }
    private val watchDocRef by lazy { db.collection("pwds").document(pwdId) }
    private var watchListener: ListenerRegistration? = null

    // Demo path for presentation (optional)
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

        val mapsLayout = findViewById<RelativeLayout>(R.id.maps)
        ViewCompat.setOnApplyWindowInsetsListener(mapsLayout) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        val bottomDrawer: View = findViewById(R.id.bottomDrawer)
        BottomSheetBehavior.from(bottomDrawer).state = BottomSheetBehavior.STATE_EXPANDED

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomDrawer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bottomDrawer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (::map.isInitialized) map.setPadding(0, 0, 0, bottomDrawer.height)
            }
        })

        findViewById<ImageView>(R.id.notificationIcon).setOnClickListener {
            startActivity(Intent(this, Notification::class.java))
        }
        findViewById<View>(R.id.trackIcon).setOnClickListener {
            // If you’re not simulating, recentre on the watch if we have it.
            if (simulateJob == null && watchMarker != null) {
                centerOnWatch()
            } else if (simulateJob == null) {
                startSimulation()
            } else {
                stopSimulation()
            }
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
        enableMyLocation() // blue dot for caregiver

        // Start at a sensible camera; will jump once Firestore updates arrive
        val initial = LatLng(14.5995, 120.9842)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initial, 13f))

        startWatchRealtimeListener()
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

    // ✅ Listen to the watch doc and move a distinct "Watch" marker
    private fun startWatchRealtimeListener() {
        watchListener = watchDocRef.addSnapshotListener(this) { snap, _ ->
            val lat = snap?.getDouble("lat")
            val lng = snap?.getDouble("lng")
            if (lat != null && lng != null) {
                val pos = LatLng(lat, lng)
                if (watchMarker == null) {
                    watchMarker = map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title("Watch")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                } else {
                    watchMarker!!.position = pos
                    map.animateCamera(CameraUpdateFactory.newLatLng(pos))
                }
            }
        }
    }

    private fun centerOnWatch() {
        watchMarker?.position?.let { pos ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17f))
            Toast.makeText(this, "Centered on watch", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(this, "No watch location yet", Toast.LENGTH_SHORT).show()
    }

    // ---------- DEMO: simulate movement by writing to Firestore ----------
    private fun startSimulation() {
        if (simulateJob != null) return
        Toast.makeText(this, "Simulating watch movement…", Toast.LENGTH_SHORT).show()
        simulateJob = lifecycleScope.launch {
            var i = 0
            while (true) {
                val p = demoPath[i % demoPath.size]
                watchDocRef.set(
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
        watchListener?.remove()
        simulateJob?.cancel()
        super.onDestroy()
    }
}
