package com.inscopelabs.abx.xtools.plugins.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Tree view of the open project. Renders a flat list (paths only) for
 * v1; a future revision will swap in a proper tree adapter. Tapping a
 * file navigates to the [CodeEditorFragment].
 */
class ProjectExplorerFragment : Fragment() {

    private var list: ListView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val id = resources.getIdentifier("fragment_project_explorer", "layout", requireContext().packageName)
        return inflater.inflate(id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = view.findViewById(resources.getIdentifier("project_list", "id", requireContext().packageName))
        list?.setOnItemClickListener { _, _, position, _ ->
            val current = StudioSession.state.value
            val root = current.projectRoot ?: return@setOnItemClickListener
            val entries = walkProject(root)
            val tapped = entries.getOrNull(position) ?: return@setOnItemClickListener
            val relPath = tapped.relativeTo(root).invariantSeparatorsPath
            StudioSession.setCurrentFile(relPath)
            studioNavController().navigate(StudioRoutes.codeEditor(relPath))
        }
        observeSession()
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                StudioSession.state.collectLatest { state ->
                    val root = state.projectRoot ?: return@collectLatest
                    val files = withContext(Dispatchers.IO) { walkProject(root) }
                    val labels = files.map { it.relativeTo(root).invariantSeparatorsPath }
                    list?.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        labels,
                    )
                }
            }
        }
    }

    private fun walkProject(root: File): List<File> =
        root.walkTopDown()
            .filter { it.isFile }
            .filter { !it.path.contains(".xtools/") }
            .filter { !it.path.contains("/build/") }
            .toList()
            .sortedBy { it.absolutePath }

    companion object {
        fun newInstance(): ProjectExplorerFragment = ProjectExplorerFragment()
    }
}
