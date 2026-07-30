package com.inscopelabs.abx.xtools.ui

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity

/**
 * Coordinates keyboard input, focus management, and text selection
 * between native UI and WebView plugin content.
 *
 * @see §3.3 Step 2.3.1
 */
object InputCoordinator {

    /**
     * Hides the soft keyboard from the currently focused view.
     */
    fun hideKeyboard(activity: FragmentActivity) {
        val currentFocus = activity.currentFocus
        if (currentFocus != null) {
            val imm = activity.getSystemService<InputMethodManager>()
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    /**
     * Shows the soft keyboard for a target view.
     */
    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = view.context.getSystemService<InputMethodManager>()
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Requests focus for a WebView, ensuring it receives keyboard events.
     */
    fun requestWebViewFocus(webView: WebView) {
        webView.requestFocus()
        webView.requestFocusFromTouch()
    }
}
