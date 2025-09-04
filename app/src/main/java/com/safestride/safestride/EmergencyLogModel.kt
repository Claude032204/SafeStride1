package com.safestride.safestride

data class EmergencyLogModel(
    val id: String = "",
    val timestamp: Long = 0,
    val status: String = "",
    val location: String = "Unknown",
    val resolved: Boolean = false
)
