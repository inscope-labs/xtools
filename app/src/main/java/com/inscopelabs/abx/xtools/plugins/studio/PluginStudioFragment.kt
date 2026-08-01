package com.inscopelabs.abx.xtools.plugins.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.inscopelabs.abx.xtools.plugins.sdk.templates.BuiltInTemplateRegistry
import com.inscopelabs.abx.xtools.plugins.sdk.templates.Template
import com.inscopelabs.abx.xtools.plugins.sdk.templates.TemplateProjectWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Top-level container that hosts the Studio's tab navigation
 * (Explorer / Manifest / Code / Assets / Preview / Build). The actual
 * tabs and the host [androidx.fragment.app.FragmentContainerView] are
 * inflated from a layout named `fragment_plugin_studio.xml` that the
 * host app must provide — this class only owns the wiring.
 */
class PluginStudioFragment : Fragment() {

    private lateinit var projectsRoot: File
    private var newProjectButton: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // The host layout is required. The Studio lives in the same
        // module as the app, so the inflated id resolves at runtime.
        val resName = "fragment_plugin_studio"
        val id = resources.getIdentifier(resName, "layout", requireContext().packageName)
        require(id != 0) { "Layout $resName.xml is missing" }
        return inflater.inflate(id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        projectsRoot = File(requireContext().filesDir, "plugins-projects").apply { mkdirs() }
        newProjectButton = view.findViewById(resources.getIdentifier("new_project", "id", requireContext().packageName))
        newProjectButton?.setOnClickListener { showTemplatePicker() }
        observeSession()
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                StudioSession.state.collectLatest { state ->
                    val titleView = view?.findViewById<android.widget.TextView>(
                        resources.getIdentifier("studio_title", "id", requireContext().packageName)
                    )
                    titleView?.text = state.manifest?.name ?: "Plugin Studio"
                }
            }
        }
    }

    private fun showTemplatePicker() {
        val templates: List<Template> = BuiltInTemplateRegistry.all()
        val labels = templates.map { it.name }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("New Plugin")
            .setItems(labels) { _, which ->
                val tpl = templates[which]
                viewLifecycleOwner.lifecycleScope.launch {
                    val target = File(projectsRoot, tpl.manifest.id)
                    withContext(Dispatchers.IO) {
                        TemplateProjectWriter().write(tpl, target)
                    }
                    StudioSession.open(target, tpl.manifest)
                }
            }
            .show()
    }

    companion object {
        fun newInstance(): PluginStudioFragment = PluginStudioFragment()
    }
}
