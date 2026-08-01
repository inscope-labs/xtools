package com.inscopelabs.abx.xtools.plugins.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.ManifestCodec
import com.inscopelabs.abx.xtools.plugins.sdk.packaging.ManifestGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The visual manifest designer. Each field in the spec is bound to an
 * EditText (resolved at runtime by id). On save the form is round-tripped
 * through a [PluginManifest] and written to disk.
 *
 * The "rich" fields (permissions, capabilities, MCP, menu entries) are
 * delegated to the [PermissionEditorFragment] — the user navigates there
 * and we come back with the updated list in the session state.
 */
class ManifestEditorFragment : Fragment() {

    private val fieldIds = listOf(
        "manifest_id" to "id",
        "manifest_name" to "name",
        "manifest_version" to "version",
        "manifest_description" to "description",
        "manifest_author" to "author",
        "manifest_min_sdk" to "minSdk",
        "manifest_entry" to "entry",
        "manifest_icon" to "icon",
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val id = resources.getIdentifier("fragment_manifest_editor", "layout", requireContext().packageName)
        return inflater.inflate(id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSession(view)
        view.findViewById<Button>(resources.getIdentifier("manifest_save", "id", requireContext().packageName))
            ?.setOnClickListener { onSave() }
        view.findViewById<Button>(resources.getIdentifier("manifest_edit_perms", "id", requireContext().packageName))
            ?.setOnClickListener {
                studioNavController().navigate(StudioRoutes.PERMISSION_EDITOR)
            }
    }

    private fun observeSession(root: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                StudioSession.state.collectLatest { state ->
                    val m = state.manifest ?: return@collectLatest
                    for ((resName, field) in fieldIds) {
                        val id = resources.getIdentifier(resName, "id", requireContext().packageName)
                        val et = root.findViewById<EditText>(id) ?: continue
                        when (field) {
                            "id" -> et.setText(m.id)
                            "name" -> et.setText(m.name)
                            "version" -> et.setText(m.version)
                            "description" -> et.setText(m.description)
                            "author" -> et.setText(m.author)
                            "minSdk" -> et.setText(m.minSdk)
                            "entry" -> et.setText(m.entry)
                            "icon" -> et.setText(m.icon)
                        }
                    }
                }
            }
        }
    }

    private fun onSave() {
        val state = StudioSession.state.value
        val root = state.projectRoot ?: return
        val current = state.manifest ?: return
        val updated = current.copy(
            id = readField("manifest_id") ?: current.id,
            name = readField("manifest_name") ?: current.name,
            version = readField("manifest_version") ?: current.version,
            description = readField("manifest_description") ?: current.description,
            author = readField("manifest_author") ?: current.author,
            minSdk = readField("manifest_min_sdk") ?: current.minSdk,
            entry = readField("manifest_entry") ?: current.entry,
            icon = readField("manifest_icon") ?: current.icon,
        )
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ManifestGenerator().write(updated, root)
            }
            StudioSession.open(root, updated)
        }
    }

    private fun readField(resName: String): String? {
        val id = resources.getIdentifier(resName, "id", requireContext().packageName)
        return view?.findViewById<EditText>(id)?.text?.toString()?.takeIf { it.isNotBlank() }
    }

    /** Exposed so the [PermissionEditorFragment] can write back. */
    fun writeManifest(updated: PluginManifest) {
        val root = StudioSession.state.value.projectRoot ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { ManifestGenerator().write(updated, root) }
            StudioSession.open(root, updated)
        }
    }

    fun loadManifestText(): String? = StudioSession.state.value.manifest
        ?.let { ManifestCodec.encode(it) }

    companion object {
        fun newInstance(): ManifestEditorFragment = ManifestEditorFragment()
    }
}
