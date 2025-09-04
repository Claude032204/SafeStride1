package com.safestride.safestride

import java.io.Serializable

data class ReminderClass(
    val title: String = "",  // 🔹 Default empty string
    val date: String = "",   // 🔹 Default empty string
    val time: String = ""    // 🔹 Default empty string
) : Serializable {
    constructor() : this("", "", "") // 🔹 No-argument constructor for Firestore
}
