package com.inscopelabs.abx.xtools.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

/**
 * Manages Material You / Material 3 theme synchronisation.
 * Applies dynamic color if available, otherwise falls back to a branded palette.
 *
 * @see §3.1.1 Step 2.1.2
 */
object ThemeManager {

    /**
     * Applies the theme to the given Activity.
     * If dynamic color is enabled and available, applies it via DynamicColors.
     * Otherwise, uses the base theme from styles.xml.
     */
    fun applyTheme(context: Context) {
        // DynamicColors.applyToActivitiesIfAvailable(application) – production code.
        // For now, stub ensures MaterialComponents theme is used.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * Switches between light and dark themes (user preference).
     * Called from AppearanceFragment.
     */
    fun setThemePreference(context: Context, isDark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
