package com.inscopelabs.abx.xtools.plugins.sdk.validation

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginSdk
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.ManifestCodec

/**
 * Validates the manifest's structural and semantic content. Idempotent and
 * side-effect free.
 */
object ManifestValidator : Validator<PluginProject> {

    override fun validate(context: PluginProject): List<ValidationReport.Entry> {
        val m = context.manifest
        val out = mutableListOf<ValidationReport.Entry>()

        if (!PluginId.isValid(m.id)) {
            out += ValidationReport.Entry(
                code = "MANIFEST_BAD_ID",
                severity = ValidationReport.Severity.ERROR,
                message = "manifest.id '${m.id}' is not a valid reverse-DNS identifier",
                path = "plugin-manifest.json#id",
            )
        }
        ManifestCodec.schemaCheck(m)?.let { msg ->
            out += ValidationReport.Entry(
                code = "MANIFEST_BAD_SCHEMA",
                severity = ValidationReport.Severity.ERROR,
                message = msg,
                path = "plugin-manifest.json#schema",
            )
        }
        if (!SEMVER.matches(m.version)) {
            out += ValidationReport.Entry(
                code = "MANIFEST_BAD_VERSION",
                severity = ValidationReport.Severity.WARNING,
                message = "version '${m.version}' is not strict semver",
                path = "plugin-manifest.json#version",
            )
        }
        if (m.minSdk > PluginSdk.VERSION && m.minSdk != "0.0.0") {
            out += ValidationReport.Entry(
                code = "MANIFEST_MIN_SDK_HIGH",
                severity = ValidationReport.Severity.WARNING,
                message = "manifest.minSdk ${m.minSdk} is higher than running SDK ${PluginSdk.VERSION}",
                path = "plugin-manifest.json#minSdk",
            )
        }
        if (m.name.isBlank()) {
            out += ValidationReport.Entry(
                code = "MANIFEST_NAME_EMPTY",
                severity = ValidationReport.Severity.WARNING,
                message = "name is blank; UI will fall back to id",
                path = "plugin-manifest.json#name",
            )
        }
        m.permissions
            .filter { com.inscopelabs.abx.xtools.plugins.sdk.api.Permission.fromAuthority(it) == null }
            .forEach { unknown ->
                out += ValidationReport.Entry(
                    code = "MANIFEST_UNKNOWN_PERMISSION",
                    severity = ValidationReport.Severity.WARNING,
                    message = "permission '$unknown' is not in the SDK catalog",
                    path = "plugin-manifest.json#permissions",
                )
            }
        return out
    }

    private val SEMVER = Regex("""^\d+\.\d+\.\d+(-[\w.]+)?(\+[\w.]+)?$""")
}
