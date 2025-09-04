package com.safestride.safestride

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class EmergencyLogsAdapter(private val logs: List<EmergencyLogModel>) :
    RecyclerView.Adapter<EmergencyLogsAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logText: TextView = view.findViewById(R.id.logText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.log_item, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]

        // Format timestamp
        val formattedTime = formatTimestamp(log.timestamp)

        // Display log information
        holder.logText.text = "$formattedTime: ${log.status} (Resolved: ${if (log.resolved) "Yes" else "No"})"
    }

    override fun getItemCount(): Int = logs.size

    // 🔹 Helper function to format timestamp
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a, MMM dd yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
