package com.safestride.safestride

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReminderAdapter(
    private val remindersList: MutableList<ReminderClass>,
    private val deleteReminder: (ReminderClass) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reminderTitle: TextView = itemView.findViewById(R.id.reminderTitle)
        val reminderDate: TextView = itemView.findViewById(R.id.reminderDate)
        val reminderTime: TextView = itemView.findViewById(R.id.reminderTime)
        val doneButton: Button = itemView.findViewById(R.id.doneButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.reminder_item, parent, false)
        return ReminderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = remindersList[position]
        holder.reminderTitle.text = reminder.title
        holder.reminderDate.text = reminder.date
        holder.reminderTime.text = reminder.time

        // Show "Done" button when clicked
        holder.itemView.setOnClickListener {
            holder.doneButton.visibility = if (holder.doneButton.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Delete reminder & send notification to Firestore when "Done" button is clicked
        holder.doneButton.setOnClickListener {
            deleteReminder(reminder)
            addReminderNotificationToFirestore(reminder)
        }
    }

    override fun getItemCount(): Int = remindersList.size

    // 🔹 Function to Send Reminder Notification to Firestore
    private fun addReminderNotificationToFirestore(reminder: ReminderClass) {
        if (userId == null) return

        val notificationData = hashMapOf(
            "title" to "Reminder Completed",
            "message" to "You completed the reminder: ${reminder.title}",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("notifications").document(userId)
            .collection("reminders")
            .add(notificationData)
            .addOnSuccessListener {
                println("🔔 Reminder notification added successfully!")
            }
            .addOnFailureListener { e ->
                println("❌ Error adding reminder notification: ${e.message}")
            }
    }
}
