package com.inscopelabs.abx.xtools.plugins.sdk.validation

import com.inscopelabs.abx.xtools.plugins.sdk.api.Permission
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest

/**
 * Cross-checks the manifest's CSP against the declared permissions. If a
 * plugin needs [Permission.NETWORK_HTTP] but its CSP forbids `connect-src`,
 * the user is going to be confused — surface that here.
 */
object CspValidator : Validator<PluginProject> {

    override fun validate(context: PluginProject): List<ValidationReport.Entry> {
        val out = mutableListOf<ValidationReport.Entry>()
        val m = context.manifest
        if (m.permissions.contains(Permission.NETWORK_HTTP.authority)) {
            if (blocksConnectSrc(m.csp)) {
                out += ValidationReport.Entry(
                    code = "CSP_CONFLICTS_PERMISSION",
                    severity = ValidationReport.Severity.ERROR,
                    message = "plugin declares network.http permission but CSP denies connect-src",
                    path = "plugin-manifest.json#csp",
                )
            }
        }
        if (m.permissions.contains(Permission.STORAGE_READ.authority) ||
            m.permissions.contains(Permission.STORAGE_WRITE.authority)
        ) {
            // No CSP check needed; storage goes through the bridge, not the network.
        }
        if (m.csp.isBlank()) {
            out += ValidationReport.Entry(
                code = "CSP_EMPTY",
                severity = ValidationReport.Severity.WARNING,
                message = "CSP is empty; default-deny will be applied",
                path = "plugin-manifest.json#csp",
            )
        }
        return out
    }

    private fun blocksConnectSrc(csp: String): Boolean {
        val directive = csp.split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("connect-src", ignoreCase = true) }
            ?: return true // absent -> default-deny per spec
        val sources = directive.substringAfter(' ').split(' ').map { it.trim() }
        return "'none'" in sources || sources.isEmpty()
    }
}
