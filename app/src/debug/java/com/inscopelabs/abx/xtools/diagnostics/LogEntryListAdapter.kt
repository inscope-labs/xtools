package com.inscopelabs.abx.xtools.diagnostics

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.inscopelabs.abx.xtools.R

class LogEntryListAdapter : RecyclerView.Adapter<LogEntryListAdapter.ViewHolder>() {

    private val entries = mutableListOf<LogViewerAdapter.LogEntry>()

    fun submitList(newEntries: List<LogViewerAdapter.LogEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLogLevel: TextView = itemView.findViewById(R.id.tvLogLevel)
        private val tvLogComponent: TextView = itemView.findViewById(R.id.tvLogComponent)
        private val tvLogTimestamp: TextView = itemView.findViewById(R.id.tvLogTimestamp)
        private val tvLogMessage: TextView = itemView.findViewById(R.id.tvLogMessage)
        private val tvLogMeta: TextView = itemView.findViewById(R.id.tvLogMeta)

        fun bind(entry: LogViewerAdapter.LogEntry) {
            tvLogLevel.text = entry.level.uppercase()
            val (textColor, bgColor) = when (entry.level.uppercase()) {
                "ERROR" -> Pair(Color.parseColor("#D32F2F"), Color.parseColor("#1FD32F2F"))
                "WARN" -> Pair(Color.parseColor("#F57C00"), Color.parseColor("#1FF57C00"))
                "INFO" -> Pair(Color.parseColor("#1976D2"), Color.parseColor("#1F1976D2"))
                "DEBUG" -> Pair(Color.parseColor("#388E3C"), Color.parseColor("#1F388E3C"))
                else -> Pair(Color.parseColor("#757575"), Color.parseColor("#1F757575"))
            }
            tvLogLevel.setTextColor(textColor)
            tvLogLevel.setBackgroundColor(bgColor)

            tvLogComponent.text = entry.component
            tvLogTimestamp.text = entry.timestamp
            tvLogMessage.text = entry.message

            val metaParts = mutableListOf<String>()
            if (entry.threadInfo.isNotBlank()) {
                metaParts.add("Thread: ${entry.threadInfo}")
            }
            if (entry.session.isNotBlank()) {
                metaParts.add("Session: ${entry.session}")
            }

            if (metaParts.isNotEmpty()) {
                tvLogMeta.text = metaParts.joinToString(" | ")
                tvLogMeta.isVisible = true
            } else {
                tvLogMeta.isVisible = false
            }
        }
    }
}
