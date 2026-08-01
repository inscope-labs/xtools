package com.inscopelabs.abx.xtools.plugins.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.inscopelabs.abx.xtools.plugins.sdk.api.Capability
import com.inscopelabs.abx.xtools.plugins.sdk.api.Permission
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Lets the user toggle [Permission]s and [Capability]s with a checkable
 * list. The save button round-trips through the parent
 * [ManifestEditorFragment] so the manifest on disk stays the source of
 * truth.
 */
class PermissionEditorFragment : Fragment() {

    private var permsList: ListView? = null
    private var capsList: ListView? = null
    private val checkedPerms = linkedSetOf<String>()
    private val checkedCaps = linkedSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val id = resources.getIdentifier("fragment_permission_editor", "layout", requireContext().packageName)
        return inflater.inflate(id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permsList = view.findViewById(resources.getIdentifier("perms_list", "id", requireContext().packageName))
        capsList = view.findViewById(resources.getIdentifier("caps_list", "id", requireContext().packageName))
        view.findViewById<Button>(resources.getIdentifier("perms_save", "id", requireContext().packageName))
            ?.setOnClickListener { onSave() }
        observeSession()
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                StudioSession.state.collectLatest { state ->
                    val m = state.manifest ?: return@collectLatest
                    checkedPerms.clear(); checkedPerms.addAll(m.permissions)
                    checkedCaps.clear(); checkedCaps.addAll(m.capabilities)
                    renderPerms()
                    renderCaps()
                }
            }
        }
    }

    private fun renderPerms() {
        val perms = Permission.ALL_AUTHORITIES
        permsList?.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        permsList?.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_list_item_multiple_choice, perms,
        )
        perms.forEachIndexed { idx, p ->
            permsList?.setItemChecked(idx, p in checkedPerms)
        }
        permsList?.setOnItemClickListener { _, _, position, _ ->
            val p = perms[position]
            if (p in checkedPerms) checkedPerms.remove(p) else checkedPerms.add(p)
        }
    }

    private fun renderCaps() {
        val caps = Capability.ALL_TAGS
        capsList?.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        capsList?.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_list_item_multiple_choice, caps,
        )
        caps.forEachIndexed { idx, c ->
            capsList?.setItemChecked(idx, c in checkedCaps)
        }
        capsList?.setOnItemClickListener { _, _, position, _ ->
            val c = caps[position]
            if (c in checkedCaps) checkedCaps.remove(c) else checkedCaps.add(c)
        }
    }

    private fun onSave() {
        val state = StudioSession.state.value
        val m = state.manifest ?: return
        val updated = m.copy(
            permissions = checkedPerms.toList(),
            capabilities = checkedCaps.toList(),
        )
        (parentFragment as? ManifestEditorFragment)?.writeManifest(updated)
            ?: (activity as? android.app.Activity)?.let { activity ->
                // Fallback: write directly via the session.
                StudioSession.open(state.projectRoot!!, updated)
            }
        studioNavController().popBackStack()
    }

    companion object {
        fun newInstance(): PermissionEditorFragment = PermissionEditorFragment()
    }
}
