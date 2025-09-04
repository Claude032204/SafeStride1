package com.safestride.safestride.wear

import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.MessageEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.safestride.safestride.NotificationHelper
import com.safestride.safestride.shared.WearPaths

class WearMessageListenerService : WearableListenerService() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onMessageReceived(event: MessageEvent) {
        val body = String(event.data ?: ByteArray(0), Charsets.UTF_8)

        when (event.path) {
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
