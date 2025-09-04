package com.safestride.safestride

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(
    private var notes: List<NoteModel>,
    private val onItemClick: (NoteModel) -> Unit,  // Click listener for editing notes
    private val onDeleteClick: (NoteModel) -> Unit // Delete listener for swipe-to-delete
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteText: TextView = view.findViewById(R.id.noteText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false) // Uses the correct layout
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        // Show the full note content
        holder.noteText.text = note.content

        // Click listener for editing a note
        holder.itemView.setOnClickListener {
            onItemClick(note)
        }
    }

    override fun getItemCount() = notes.size

    // 🔹 Function to update notes efficiently using DiffUtil
    fun updateNotes(newNotes: List<NoteModel>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes = newNotes
        diffResult.dispatchUpdatesTo(this)
    }
}

// 🔹 DiffUtil for efficient updates
class NotesDiffCallback(
    private val oldList: List<NoteModel>,
    private val newList: List<NoteModel>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
