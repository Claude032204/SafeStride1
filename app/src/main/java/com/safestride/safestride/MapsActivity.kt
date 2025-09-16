package com.safestride.safestride

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.math.max
import kotlin.math.pow

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    // Markers
    private var phoneMarker: Marker? = null
    private var watchMarker: Marker? = null

    // Phone location
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var initialCameraSet = false

    // Filters (de-jitter + smooth)
    private val phoneFilter = KalmanFilter2D()
    private val watchFilter = KalmanFilter2D()

    // Firestore
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val pwdId by lazy { intent.getStringExtra("PWD_ID") ?: "pwd1" }
    private val watchDocRef by lazy { db.collection("pwds").document(pwdId) }
    private var watchListener: ListenerRegistration? = null

    // ---- Permission + settings launchers ----
    private val askLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (!ok) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        ensureLocationSettingsAndStart()
    }

    private val askResolution = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            startPhoneLocationUpdates()
        } else {
            Toast.makeText(this, "Enable location to start tracking", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------- Lifecycle ----------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fused = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(1f)
            .setMaxUpdateDelayMillis(0L)   // no batching
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val acc = if (loc.hasAccuracy()) loc.accuracy else 999f
                if (acc > 75f) return // ignore very poor fixes

                val raw = LatLng(loc.latitude, loc.longitude)
                val smoothed = phoneFilter.update(raw.latitude, raw.longitude, acc.toDouble())
                val pos = LatLng(smoothed.first, smoothed.second)

                if (phoneMarker == null) {
                    phoneMarker = map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title("Phone")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                } else {
                    moveMarkerSmooth(phoneMarker!!, pos)
                }

                if (!initialCameraSet) {
                    initialCameraSet = true
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                }
            }
        }

        val root = findViewById<RelativeLayout>(R.id.maps)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        val bottomDrawer: View = findViewById(R.id.bottomDrawer)
        BottomSheetBehavior.from(bottomDrawer).state = BottomSheetBehavior.STATE_EXPANDED

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomDrawer.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bottomDrawer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (::map.isInitialized) map.setPadding(0, 0, 0, bottomDrawer.height)
            }
        })

        findViewById<ImageView>(R.id.notificationIcon).setOnClickListener {
            startActivity(Intent(this, Notification::class.java))
        }
        findViewById<View>(R.id.trackIcon).setOnClickListener {
            watchMarker?.position?.let {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
            } ?: Toast.makeText(this, "Waiting for watch location…", Toast.LENGTH_SHORT).show()
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
        requestLocationPermissionIfNeeded()
        startWatchRealtimeListener()
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentFixThenStartUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPhoneLocationUpdates()
    }

    override fun onDestroy() {
        watchListener?.remove()
        stopPhoneLocationUpdates()
        super.onDestroy()
    }

    // ---------------- Permissions & Settings ----------------

    private fun requestLocationPermissionIfNeeded() {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            ensureLocationSettingsAndStart()
        } else {
            askLocationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun ensureLocationSettingsAndStart() {
        map.isMyLocationEnabled = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val req = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        settingsClient.checkLocationSettings(req)
            .addOnSuccessListener {
                getCurrentFixThenStartUpdates()
            }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        val isr = IntentSenderRequest.Builder(e.resolution).build()
                        askResolution.launch(isr)
                    } catch (_: IntentSender.SendIntentException) {
                        Toast.makeText(this, "Enable Location Services", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Enable Location Services", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun getCurrentFixThenStartUpdates() {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return

        val token = com.google.android.gms.tasks.CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val acc = if (loc.hasAccuracy()) loc.accuracy else 999f
                    if (acc <= 75f) {
                        val pos = LatLng(loc.latitude, loc.longitude)
                        if (phoneMarker == null) {
                            phoneMarker = map.addMarker(
                                MarkerOptions()
                                    .position(pos)
                                    .title("Phone")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            )
                        } else {
                            phoneFilter.reset(pos.latitude, pos.longitude)
                            phoneMarker!!.position = pos
                        }
                        if (!initialCameraSet) {
                            initialCameraSet = true
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                        }
                    }
                }
                startPhoneLocationUpdates()
            }
            .addOnFailureListener { startPhoneLocationUpdates() }
    }

    // ---------------- Phone updates ----------------

    private fun startPhoneLocationUpdates() {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return
        fused.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun stopPhoneLocationUpdates() {
        fused.removeLocationUpdates(locationCallback)
    }

    // ---------------- Watch listener ----------------

    private fun startWatchRealtimeListener() {
        watchListener = watchDocRef.addSnapshotListener(this) { snap, err ->
            if (err != null) {
                Toast.makeText(this, "Watch listener error: ${err.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            val lat = snap?.getDouble("lat")
            val lng = snap?.getDouble("lng")
            if (lat != null && lng != null) {
                // Smooth the watch point too (assume ~35m accuracy when unknown)
                val est = watchFilter.update(lat, lng, 35.0)
                val pos = LatLng(est.first, est.second)

                if (watchMarker == null) {
                    watchMarker = map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title("Watch")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                } else {
                    moveMarkerSmooth(watchMarker!!, pos)
                }
            }
        }
    }

    // ---------------- Helpers ----------------

    private fun moveMarkerSmooth(marker: Marker, to: LatLng) {
        val from = marker.position
        if (from == to) return
        val animator = ValueAnimator.ofFloat(0f, 1f).apply { duration = 700 }
        animator.addUpdateListener { va ->
            val t = va.animatedValue as Float
            val lat = from.latitude + (to.latitude - from.latitude) * t
            val lng = from.longitude + (to.longitude - from.longitude) * t
            marker.position = LatLng(lat, lng)
        }
        animator.start()
    }
}

/** Very small 2D Kalman filter for Lat/Lng smoothing. */
private class KalmanFilter2D {
    private var hasState = false
    private var x = 0.0; private var y = 0.0
    private var vx = 0.0; private var vy = 0.0
    private var p = 50.0 // uncertainty

    fun reset(lat: Double, lng: Double) {
        hasState = true
        x = lat; y = lng; vx = 0.0; vy = 0.0; p = 10.0
    }

    /**
     * @param accMeters accuracy of the measurement (smaller = more trusted)
     * Returns smoothed (lat,lng)
     */
    fun update(lat: Double, lng: Double, accMeters: Double): Pair<Double, Double> {
        if (!hasState) reset(lat, lng)

        // Predict step: slightly increase uncertainty
        p += 1.0

        // Measurement noise R from accuracy (meters) -> rough scale to degrees near equator
        val metersToDeg = 1.0 / 111_000.0
        val r = max(1e-6, (accMeters * metersToDeg).pow(2.0))

        // Kalman gain
        val k = p / (p + r)
        x += k * (lat - x)
        y += k * (lng - y)
        p *= (1 - k)
        return x to y
    }
}
