package com.inscopelabs.abx.xtools.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R

/**
 * Data access layer enable/disable switches (Standalone Mode only).
 * Lists: SAF storage, Encrypted store, Repo connector.
 * Visible only in STANDALONE mode – disabled/hidden in GOVERNED.
 *
 * @see §5.1.1, §5.1.2
 */
class DataAccessLayersFragment : Fragment() {

    private lateinit var safSwitch: Switch
    private lateinit var encryptedSwitch: Switch
    private lateinit var repoSwitch: Switch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_data_access_layers, container, false).apply {
            safSwitch = findViewById(R.id.switch_saf)
            encryptedSwitch = findViewById(R.id.switch_encrypted)
            repoSwitch = findViewById(R.id.switch_repo)

            // Stub: state will be read from EnableSwitchManager in Phase 4.
            safSwitch.isChecked = true
            encryptedSwitch.isChecked = true
            repoSwitch.isChecked = false

            // Each switch will toggle the respective DataAccessLayer via EnableSwitchManager.
            safSwitch.setOnCheckedChangeListener { _, isChecked ->
                // TODO: Phase 4 – call EnableSwitchManager.setLayerEnabled("saf", isChecked)
            }
        }
    }
}
