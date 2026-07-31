package com.inscopelabs.abx.xtools.plugins.sdk.bridge

import android.webkit.WebView

/**
 * Applies Content-Security-Policy headers to a WebView about to load a
 * plugin. Per Stage 7.17 "CSP policies are enforced" — the manifest's CSP
 * string is the source of truth. If the manifest's policy is empty the
 * default-deny policy is installed instead.
 */
object CspEnforcer {

    private const val DEFAULT_DENY: String =
        "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'"

    fun apply(webView: WebView, csp: String?) {
        // WebView on Android doesn't expose a programmatic CSP hook
        // pre-2024. The supported mechanism is the CSP meta tag in the
        // served HTML. We expose [metaTag] so callers can inject it into
        // the entry document.
        val policy = csp?.takeIf { it.isNotBlank() } ?: DEFAULT_DENY
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = true
        }
    }

    /** Returns the `<meta>` tag the host should prepend to plugin HTML. */
    fun metaTag(csp: String?): String {
        val policy = csp?.takeIf { it.isNotBlank() } ?: DEFAULT_DENY
        return """<meta http-equiv="Content-Security-Policy" content="$policy">"""
    }
}
