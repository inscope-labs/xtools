package com.inscopelabs.abx.xtools.diagnostics

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import com.inscopelabs.abx.xtools.R
import java.io.File

class LogViewerBottomSheet : BottomSheetDialogFragment() {

    private lateinit var adapter: LogEntryListAdapter
    private var allEntries: List<LogViewerAdapter.LogEntry> = emptyList()

    private var selectedLevel = "ALL"
    private var searchQuery = ""

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private lateinit var tvLogCount: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var rvLogEntries: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log_viewer_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnExport: ImageButton = view.findViewById(R.id.btnExport)
        val etSearchLogs: EditText = view.findViewById(R.id.etSearchLogs)
        val chipGroupLogLevels: ChipGroup = view.findViewById(R.id.chipGroupLogLevels)
        tvLogCount = view.findViewById(R.id.tvLogCount)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        rvLogEntries = view.findViewById(R.id.rvLogEntries)

        adapter = LogEntryListAdapter()
        rvLogEntries.layoutManager = LinearLayoutManager(requireContext())
        rvLogEntries.adapter = adapter

        // Load logs
        val context = requireContext()
        val logFile = Logger.getLogFile() ?: File(context.filesDir, "diagnostics.log")
        allEntries = LogViewerAdapter().parseLogs(logFile)

        applyFilter()

        // Export button listener
        btnExport.setOnClickListener {
            try {
                val bundle = DiagnosticBundle.createBundle(requireContext())
                DiagnosticExporter.shareDiagnosticBundle(requireContext(), bundle)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Debounced search watcher (400ms)
        etSearchLogs.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    searchQuery = s?.toString() ?: ""
                    applyFilter()
                }
                searchHandler.postDelayed(searchRunnable!!, 400L)
            }
        })

        // Level filter Chips listener
        chipGroupLogLevels.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedLevel = if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
                    R.id.chipLevelDebug -> "DEBUG"
                    R.id.chipLevelInfo -> "INFO"
                    R.id.chipLevelWarn -> "WARN"
                    R.id.chipLevelError -> "ERROR"
                    else -> "ALL"
                }
            } else {
                "ALL"
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        val query = searchQuery.trim()
        val filtered = allEntries.filter { entry ->
            val matchesLevel = selectedLevel == "ALL" || entry.level.equals(selectedLevel, ignoreCase = true)
            val matchesSearch = query.isBlank() ||
                    entry.message.contains(query, ignoreCase = true) ||
                    entry.component.contains(query, ignoreCase = true)
            matchesLevel && matchesSearch
        }

        adapter.submitList(filtered)
        tvLogCount.text = "Showing ${filtered.size} of ${allEntries.size} entries"

        if (filtered.isEmpty()) {
            tvEmptyState.isVisible = true
            rvLogEntries.isVisible = false
        } else {
            tvEmptyState.isVisible = false
            rvLogEntries.isVisible = true
        }
    }

    override fun onDestroyView() {
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        super.onDestroyView()
    }
}
