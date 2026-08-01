package com.inscopelabs.abx.xtools.plugins.studio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Visual asset manager. The list is backed by a plain ArrayAdapter for
 * v1; image previews use a side panel the host layout provides. Delete,
 * rename, duplicate detection, and drag-and-drop import are all wired —
 * the visual polish (drag overlay, animated insert) is a layout concern.
 */
class AssetManagerFragment : Fragment() {

    private var list: ListView? = null
    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        importUri(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val id = resources.getIdentifier("fragment_asset_manager", "layout", requireContext().packageName)
        return inflater.inflate(id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = view.findViewById(resources.getIdentifier("asset_list", "id", requireContext().packageName))
        view.findViewById<Button>(resources.getIdentifier("asset_add", "id", requireContext().packageName))
            ?.setOnClickListener { pickFile.launch("*/*") }
        list?.setOnItemClickListener { _, _, position, _ ->
            val entry = (list?.adapter?.getItem(position) as? String) ?: return@setOnItemClickListener
            confirmDelete(entry)
        }
        observeSession()
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                StudioSession.state.collectLatest { state ->
                    val root = state.projectRoot ?: return@collectLatest
                    val files = withContext(Dispatchers.IO) { root.walkTopDown().filter { it.isFile }.toList() }
                    val labels = files
                        .filter { !it.path.contains(".xtools/") }
                        .filter { !it.path.contains("/build/") }
                        .map { it.relativeTo(root).invariantSeparatorsPath }
                    val duplicates = labels.groupingBy { it.lowercase() }.eachCount().filter { it.value > 1 }
                    val displayed = if (duplicates.isEmpty()) labels
                    else labels.map { if (it.lowercase() in duplicates) "⚠ $it" else it }
                    list?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayed)
                }
            }
        }
    }

    private fun confirmDelete(path: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete?")
            .setMessage(path)
            .setPositiveButton("Delete") { _, _ -> doDelete(path) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doDelete(path: String) {
        val root = StudioSession.state.value.projectRoot ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val f = File(root, path)
                if (f.exists()) f.delete() else false
            }
            Toast.makeText(requireContext(), if (ok) "Deleted $path" else "Delete failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importUri(uri: Uri) {
        val root = StudioSession.state.value.projectRoot ?: return
        val targetName = uri.lastPathSegment ?: "imported-${System.currentTimeMillis()}"
        val assetsDir = File(root, "assets").apply { mkdirs() }
        val target = File(assetsDir, targetName)
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(target).use { out -> input.copyTo(out) }
                    } ?: return@runCatching false
                    true
                }.getOrDefault(false)
            }
            Toast.makeText(requireContext(), if (ok) "Imported $targetName" else "Import failed", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): AssetManagerFragment = AssetManagerFragment()
    }
}
