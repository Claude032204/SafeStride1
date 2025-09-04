package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddNoteActivity : AppCompatActivity() {

    private lateinit var noteInput: EditText
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private var noteId: String? = null  // Store Firestore document ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid

        noteInput = findViewById(R.id.noteInput)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve the note content and ID from the intent
        noteId = intent.getStringExtra("NOTE_ID")
        val noteContent = intent.getStringExtra("NOTE_CONTENT")
        noteInput.setText(noteContent)

        findViewById<View>(R.id.backArrowIcon).setOnClickListener {
            finish() // Navigate back to NotesActivity
        }

        findViewById<View>(R.id.saveIcon).setOnClickListener {
            saveNoteToFirestore()
        }

        findViewById<View>(R.id.deleteIcon).setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    // 🔹 Save or Update Note in Firestore
    private fun saveNoteToFirestore() {
        val noteText = noteInput.text.toString().trim()

        if (noteText.isEmpty()) {
            Toast.makeText(this, "Please enter a note", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId != null) {
            val noteData = hashMapOf(
                "content" to noteText,
                "timestamp" to System.currentTimeMillis()
            )

            if (noteId == null) {
                // Add new note
                db.collection("profiles").document(userId!!)
                    .collection("notes")
                    .add(noteData)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
                        finish() // Close activity
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save note: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Update existing note
                db.collection("profiles").document(userId!!)
                    .collection("notes").document(noteId!!)
                    .update(noteData as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show()
                        finish() // Close activity
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update note: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // 🔹 Delete Note from Firestore
    private fun deleteNoteFromFirestore() {
        if (userId != null && noteId != null) {
            db.collection("profiles").document(userId!!)
                .collection("notes").document(noteId!!)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
                    finish() // Close activity
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to delete note: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 🔹 Show Confirmation Dialog for Deleting Note
    private fun showDeleteConfirmationDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Discard Note?")
            .setMessage("Are you sure you want to discard this note?")
            .setPositiveButton("Discard") { _, _ -> deleteNoteFromFirestore() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
