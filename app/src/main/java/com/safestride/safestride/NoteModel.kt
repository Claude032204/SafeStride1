package com.safestride.safestride

data class NoteModel(
    val id: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
