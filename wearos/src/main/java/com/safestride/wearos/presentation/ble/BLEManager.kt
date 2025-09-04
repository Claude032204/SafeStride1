package com.safestride.wearos.presentation.ble


import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.*

class BLEManager(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null

    companion object {
        private const val TAG = "BLEManager"
    }

    fun startBLEScan(callback: BluetoothAdapter.LeScanCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission BLUETOOTH_SCAN not granted")
                return
            }
        } else if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission ACCESS_FINE_LOCATION not granted")
            return
        }
        bluetoothAdapter?.startLeScan(callback)
        Log.d(TAG, "BLE scan started.")
    }

    fun stopBLEScan(callback: BluetoothAdapter.LeScanCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission BLUETOOTH_SCAN not granted")
                return
            }
        } else if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission ACCESS_FINE_LOCATION not granted")
            return
        }
        bluetoothAdapter?.stopLeScan(callback)
        Log.d(TAG, "BLE scan stopped.")
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted")
                return
            }
        }
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server.")
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                gatt?.discoverServices()
                            } else {
                                Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted.")
                            }
                        } else {
                            gatt?.discoverServices()
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException: ${e.message}")
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server.")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered.")
                } else {
                    Log.e(TAG, "Failed to discover services: $status")
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Characteristic written successfully.")
                } else {
                    Log.e(TAG, "Failed to write characteristic: $status")
                }
            }
        })
    }

    fun sendDateTime() {
        val currentDateTime = Calendar.getInstance().time.toString()
        Log.d(TAG, "Sending date and time: $currentDateTime")
        // Add logic to write to a characteristic
    }

    fun disconnect() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt?.disconnect()
                    bluetoothGatt?.close()
                } else {
                    Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted.")
                }
            } else {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
        } finally {
            bluetoothGatt = null
            Log.d(TAG, "Bluetooth GATT connection closed.")
        }
    }

}
