package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var noteContentEditText: EditText
    private lateinit var saveIcon: ImageView
    private lateinit var deleteIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        noteContentEditText = findViewById(R.id.noteContent)
        saveIcon = findViewById(R.id.saveIcon)
        deleteIcon = findViewById(R.id.deleteIcon)

        // Back Arrow Click Listener
        findViewById<ImageView>(R.id.backArrowIcon).setOnClickListener {
            finish() // Navigate back to NotesActivity
        }

        // Get the full note content passed from NotesActivity
        val noteContent = intent.getStringExtra("NOTE_CONTENT")
        val notePosition = intent.getIntExtra("NOTE_POSITION", -1) // Get the position of the note to update

        noteContent?.let {
            noteContentEditText.setText(it) // Display the content in the EditText
        }

        // Save Icon Click Listener
        saveIcon.setOnClickListener {
            val updatedNote = noteContentEditText.text.toString()
            if (updatedNote.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("UPDATED_NOTE", updatedNote) // Updated content
                resultIntent.putExtra("NOTE_POSITION", notePosition) // Send the position to update the correct item
                setResult(RESULT_OK, resultIntent) // Set the result and return to NotesActivity
                finish() // Close NoteDetailActivity
            } else {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }

        // Delete Icon Click Listener
        deleteIcon.setOnClickListener {
            showDeleteConfirmationDialog(notePosition)
        }
    }

    private fun showDeleteConfirmationDialog(notePosition: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Note?")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                // Send back the position to delete the note from the list
                val resultIntent = Intent()
                resultIntent.putExtra("DELETE_NOTE_POSITION", notePosition)
                setResult(RESULT_OK, resultIntent)
                finish() // Close the activity
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
