package com.safestride.safestride.wear

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.safestride.safestride.NotificationHelper
import com.safestride.safestride.shared.WearPaths

class WearMessageListenerService : WearableListenerService() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onMessageReceived(event: MessageEvent) {
        val body = String(event.data ?: ByteArray(0), Charsets.UTF_8)
        Log.d("WearMsgSvc", ">>> path=${event.path}, body=$body")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Wear msg: ${event.path}", Toast.LENGTH_SHORT).show()
        }

        when (event.path) {
            // ✅ watch GPS -> Firestore (doc the map already listens to)
            WearPaths.LOCATION -> {
                // body format: "lat,lng"
                val p = body.split(",")
                val lat = p.getOrNull(0)?.toDoubleOrNull()
                val lng = p.getOrNull(1)?.toDoubleOrNull()
                if (lat != null && lng != null) {
                    db.collection("pwds").document("pwd1")
                        .set(
                            mapOf(
                                "lat" to lat,
                                "lng" to lng,
                                "ts"  to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                } else {
                    Log.w("WearMsgSvc", "Bad location payload: $body")
                }
            }

            // your existing alerts (unchanged)
            WearPaths.ALERT_RED -> {
                NotificationHelper.showNotification(
                    this, "Emergency (RED)", "Immediate assistance requested. $body"
                )
                logToFirestore("emergencyLogs", "Emergency Alert! $body")
            }
            WearPaths.ALERT_YELLOW -> {
                NotificationHelper.showNotification(
                    this, "Assistance (YELLOW)", "Check-in needed. $body"
                )
                logToFirestore("assistanceLogs", "Assistance Alert! $body")
            }
        }
    }

    private fun logToFirestore(collection: String, message: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("notifications").document(uid)
            .collection(collection)
            .add(mapOf("message" to message, "timestamp" to System.currentTimeMillis()))
    }
}
