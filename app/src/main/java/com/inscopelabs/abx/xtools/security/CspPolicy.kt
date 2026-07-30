package com.inscopelabs.abx.xtools.security

/**
 * Defines the Content Security Policy for WebViews.
 * Restricts script sources, network access, and resource loading.
 *
 * @see §2.4 Step 1.5.1
 */
class CspPolicy {
    companion object {
        val DEFAULT_POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'none';"

        /**
         * Generates a CSP header string, optionally relaxed based on plugin manifest.
         */
        fun generate(pluginId: String, requiresNetwork: Boolean = false): String {
            return if (requiresNetwork) {
                "default-src 'self'; script-src 'self'; connect-src 'self' https:;"
            } else {
                DEFAULT_POLICY
            }
        }
    }
}
