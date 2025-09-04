package com.safestride.safestride

data class Notif(
    val documentId: String, // Add this field to store the Firestore document ID
    val title: String,
    val details: String,
    val timestamp: String
)
