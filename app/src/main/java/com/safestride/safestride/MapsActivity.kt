package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Find the RelativeLayout by its ID
        val mapsLayout = findViewById<RelativeLayout>(R.id.maps)

        // Apply Window Insets listener to adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(mapsLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Return the insets
        }

        // Inside onCreate()
        findViewById<ImageView>(R.id.notificationIcon).setOnClickListener {
            startActivity(Intent(this, Notification::class.java))
        }


        // WebView setup for OpenStreetMap
        val webView: WebView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true

        val mapHtml = """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css"/>
                <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
            </head>
            <body style="margin:0;padding:0;">
                <div id="map" style="width:100%;height:100vh;"></div>
                <script>
                    var map = L.map('map').setView([14.5995, 120.9842], 13);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null)

        // Bottom Drawer setup
        val bottomDrawer: View = findViewById(R.id.bottomDrawer)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomDrawer)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        // Set click listeners for bottom icons
        findViewById<View>(R.id.trackIcon).setOnClickListener {
            Toast.makeText(this, "Track location", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.homeIcon).setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
        }

        // Inside onCreate()
        findViewById<View>(R.id.recordIcon).setOnClickListener {
            startActivity(Intent(this, Records::class.java))
        }

        findViewById<View>(R.id.settingsIcon).setOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }
    }
}
