package com.inscopelabs.abx.xtools.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.ui.ThemeManager

/**
 * Appearance settings: light/dark theme, dynamic color toggle.
 * Wires to ThemeManager.
 *
 * @see §3.1.1 Step 2.1.2, §3.1.1 Step 2.1.1
 */
class AppearanceFragment : Fragment() {

    private lateinit var darkModeSwitch: Switch
    private lateinit var dynamicColorSwitch: Switch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_appearance, container, false).apply {
            darkModeSwitch = findViewById(R.id.switch_dark_mode)
            dynamicColorSwitch = findViewById(R.id.switch_dynamic_color)

            // Load persisted preferences before attaching listeners to avoid triggering listeners on initial state-set
            val context = requireContext()
            darkModeSwitch.isChecked = ThemeManager.isDarkModeEnabled(context)
            dynamicColorSwitch.isChecked = ThemeManager.isDynamicColorEnabled(context)

            darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                ThemeManager.setThemePreference(requireContext(), isChecked)
            }

            dynamicColorSwitch.setOnCheckedChangeListener { _, isChecked ->
                ThemeManager.setDynamicColorPreference(requireContext(), isChecked)
                ThemeManager.applyTheme(requireContext())
            }
        }
    }
}
