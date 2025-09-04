package com.safestride.safestride

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Note : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotesAdapter
    private val notesList = mutableListOf<NoteModel>() // List of notes
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        val noteLayout = findViewById<RelativeLayout>(R.id.note)

        // Apply Window Insets listener
        ViewCompat.setOnApplyWindowInsetsListener(noteLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid

        // 🔹 Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerNotes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = NotesAdapter(
            notesList,
            onItemClick = { selectedNote ->
                val intent = Intent(this, AddNoteActivity::class.java)
                intent.putExtra("NOTE_ID", selectedNote.id)
                intent.putExtra("NOTE_CONTENT", selectedNote.content)
                startActivityForResult(intent, 2) // Request code 2 for editing notes
            },
            onDeleteClick = { noteToDelete ->
                noteToDelete.id?.let { deleteNoteFromFirestore(it) }
            }
        )
        recyclerView.adapter = adapter

        findViewById<View>(R.id.backArrowIcon).setOnClickListener {
            finish() // Close the activity
        }

        loadNotesFromFirestore() // Load notes from Firestore

        // 🔹 Swipe to delete functionality
        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition

                    // 🔹 Fix: Ensure valid position before deleting
                    if (position != RecyclerView.NO_POSITION && position < notesList.size) {
                        val noteToDelete = notesList[position]

                        noteToDelete.id?.let { noteId ->
                            deleteNoteFromFirestore(noteId)

                            // 🔹 Remove from local list and update UI immediately
                            notesList.removeAt(position)
                            adapter.notifyItemRemoved(position)
                        }
                    } else {
                        // 🔹 Prevent invalid access
                        adapter.notifyItemChanged(position)
                    }
                }
            }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // 🔹 Floating button to add note
        val fabAddNote = findViewById<FloatingActionButton>(R.id.fabAddNote)
        fabAddNote.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            startActivityForResult(intent, 1) // Request code 1 for adding new note
        }
    }

    private fun loadNotesFromFirestore() {
        if (userId != null) {
            db.collection("profiles").document(userId!!)
                .collection("notes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error loading notes: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val newNotesList = mutableListOf<NoteModel>()
                    if (snapshots != null) {
                        for (document in snapshots.documents) {
                            val note = NoteModel(
                                id = document.id,
                                content = document.getString("content") ?: "",
                                timestamp = document.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                            newNotesList.add(note)
                        }

                        // 🔹 Debug Log: Show real-time updates
                        android.util.Log.d("FirestoreDebug", "Real-time update: ${newNotesList.size} notes retrieved")

                        // 🔹 Update RecyclerView instantly
                        notesList.clear()
                        notesList.addAll(newNotesList)
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    // 🔹 Delete Note from Firestore
    private fun deleteNoteFromFirestore(noteId: String) {
        if (userId != null) {
            db.collection("profiles").document(userId!!)
                .collection("notes").document(noteId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error deleting note: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            android.util.Log.d("FirestoreDebug", "Reloading notes after adding/editing") // Debugging log
            loadNotesFromFirestore() // 🔹 Ensures notes update when coming back from AddNoteActivity
        }
    }

}

