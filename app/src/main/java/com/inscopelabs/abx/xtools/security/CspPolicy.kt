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

        /**
         * Generates a CSP header string evaluated against granted permissions and requested domains.
         * Pure function to avoid coupling security package directly to kernel objects.
         */
        fun generateForPermissions(
            pluginId: String,
            grantedPermissions: Set<String>,
            requestedConnectDomains: List<String> = emptyList()
        ): String {
            val hasNetwork = grantedPermissions.contains("http") || grantedPermissions.contains("network")
            val connectSrc = when {
                !hasNetwork -> "'none'"
                requestedConnectDomains.isNotEmpty() -> {
                    val domainsStr = requestedConnectDomains.joinToString(" ") { d ->
                        if (d.startsWith("https://") || d.startsWith("http://")) d else "https://$d"
                    }
                    "'self' $domainsStr"
                }
                else -> "'self' https:"
            }

            val hasStorage = grantedPermissions.contains("storage")
            val imgSrc = if (hasStorage) "'self' data: blob:" else "'self' data:"

            return "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src $imgSrc; connect-src $connectSrc;"
        }
    }
}

