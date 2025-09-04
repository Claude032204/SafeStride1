package com.safestride.wearos.presentation.ble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.safestride.wearos.R
import java.util.UUID

class BLEActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var deviceListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnScan = findViewById<Button>(R.id.btn_scan)
        deviceListView = findViewById(R.id.device_list)

        btnScan.setOnClickListener {
            if (hasPermissions()) {
                startBLEScan()
            } else {
                requestPermissionsIfNecessary()
            }
        }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            connectToDevice(device)
        }

        // Check location permissions and fetch location
        if (checkLocationPermissions()) {
            fetchLocation()
        } else {
            requestLocationPermissions()
        }
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissionsIfNecessary() {
        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            100
        )
    }

    private fun fetchLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Log.d("GPS", "Location: $latitude, $longitude")
                        sendLocationToPhone(latitude, longitude)
                    } else {
                        Log.e("GPS", "Location is null")
                        Toast.makeText(this, "Unable to fetch location.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Log.e("GPS", "Failed to fetch location: ${e.message}")
                    Toast.makeText(this, "Failed to fetch location.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("Permission", "Location permission not granted.")
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show()
                requestLocationPermissions()
            }
        } catch (e: SecurityException) {
            Log.e("GPS", "SecurityException: ${e.message}")
            Toast.makeText(this, "Permission issue occurred.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendLocationToPhone(latitude: Double, longitude: Double) {
        try {
            val data = "$latitude,$longitude".toByteArray()
            val characteristic = getCharacteristicForSendingData() // Replace with your BLE characteristic

            if (characteristic != null) {
                characteristic.value = data
                val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
                if (success) {
                    Log.d("BLE", "Location data sent successfully.")
                } else {
                    Log.e("BLE", "Failed to send location data.")
                    Toast.makeText(this, "Failed to send data.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("BLE", "Characteristic is null.")
                Toast.makeText(this, "BLE characteristic not found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Log.e("BLE", "SecurityException: ${e.message}")
            Toast.makeText(this, "Permission issue during BLE operation.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getCharacteristicForSendingData(): BluetoothGattCharacteristic? {
        bluetoothGatt?.services?.forEach { service ->
            service.characteristics.forEach { characteristic ->
                if (characteristic.uuid == UUID.fromString("YOUR_CHARACTERISTIC_UUID")) {
                    return characteristic
                }
            }
        }
        return null
    }

    private fun startBLEScan() {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission denied for scanning.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            bluetoothLeScanner.startScan(object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    if (!deviceList.contains(device) && device.name != null) {
                        deviceList.add(device)
                        updateDeviceList()
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Toast.makeText(this@BLEActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
                }
            })
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("BLEActivity", "SecurityException: ${e.message}")
        }
    }

    private fun updateDeviceList() {
        val deviceNames = deviceList.map { device ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                "Unknown Device (${device.address})"
            } else {
                "${device.name} (${device.address})"
            }
        }

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        deviceListView.adapter = adapter
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission denied for connecting.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            Log.d("BLEActivity", "Connected to ${device.name}")
                            runOnUiThread {
                                Toast.makeText(this@BLEActivity, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                            }
                            gatt?.discoverServices()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.d("BLEActivity", "Disconnected from ${device.name}")
                            runOnUiThread {
                                Toast.makeText(this@BLEActivity, "Disconnected from ${device.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d("BLEActivity", "Services discovered successfully.")
                        gatt?.services?.forEach { service ->
                            Log.d("BLEActivity", "Service UUID: ${service.uuid}")
                            service.characteristics.forEach { characteristic ->
                                Log.d("BLEActivity", "Characteristic UUID: ${characteristic.uuid}")
                            }
                        }
                        runOnUiThread {
                            Toast.makeText(this@BLEActivity, "Services discovered. Check logs for UUIDs.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("BLEActivity", "Failed to discover services. Status: $status")
                    }
                }
            })
        } catch (e: SecurityException) {
            Log.e("BLEActivity", "SecurityException during connectGatt: ${e.message}")
        }
    }
}
