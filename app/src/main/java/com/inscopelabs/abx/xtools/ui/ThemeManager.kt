package com.inscopelabs.abx.xtools.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

/**
 * Manages Material You / Material 3 theme synchronisation.
 * Applies dynamic color if available, otherwise falls back to a branded palette.
 *
 * @see §3.1.1 Step 2.1.2
 */
object ThemeManager {

    private const val PREFS_NAME = "xtools_theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Applies the theme to the given Context/Activity.
     * Reads persisted dark-mode and dynamic-color preferences.
     */
    fun applyTheme(context: Context) {
        val prefs = getPrefs(context)

        // 1. Apply night mode setting
        if (!prefs.contains(KEY_DARK_MODE)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else {
            val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // 2. Apply dynamic colors if enabled
        val dynamicColorEnabled = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        if (dynamicColorEnabled) {
            val app = context.applicationContext as? Application
            if (app != null) {
                DynamicColors.applyToActivitiesIfAvailable(app)
            }
        }
    }

    /**
     * Switches between light and dark themes (user preference) and persists choice.
     * Called from AppearanceFragment.
     */
    fun setThemePreference(context: Context, isDark: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, isDark).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /**
     * Enables or disables dynamic color (Material You) preference and applies theme.
     */
    fun setDynamicColorPreference(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        applyTheme(context)
    }

    /**
     * Returns true if dark mode is explicitly enabled, or if unset, if the system is in dark mode.
     */
    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_DARK_MODE)) {
            prefs.getBoolean(KEY_DARK_MODE, false)
        } else {
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * Returns whether dynamic color is enabled in user preferences (default false).
     */
    fun isDynamicColorEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DYNAMIC_COLOR, false)
    }
}

