package com.inscopelabs.abx.xtools.plugins.sdk.validation

import java.io.File

/**
 * Confirms every asset the manifest references is actually present and
 * that the entry-point file exists. Also flags duplicate paths and total
 * bundle size.
 */
object AssetsValidator : Validator<PluginProject> {

    private const val MAX_BUNDLE_BYTES: Long = 50L * 1024 * 1024 // 50 MB

    override fun validate(context: PluginProject): List<ValidationReport.Entry> {
        val out = mutableListOf<ValidationReport.Entry>()
        val m = context.manifest

        if (!context.entryFile.isFile) {
            out += ValidationReport.Entry(
                code = "ENTRY_MISSING",
                severity = ValidationReport.Severity.ERROR,
                message = "entry file '${m.entry}' not found at project root",
                path = m.entry,
            )
        }
        if (!context.hasAsset(m.icon)) {
            out += ValidationReport.Entry(
                code = "ICON_MISSING",
                severity = ValidationReport.Severity.ERROR,
                message = "declared icon '${m.icon}' does not exist in bundle",
                path = m.icon,
            )
        }
        // Duplicate detection — same relative path appearing twice in
        // the assets list (case-sensitive on Android, case-insensitive
        // on HFS+ etc; we treat them as duplicates either way).
        val seen = HashSet<String>()
        val lowerSeen = HashSet<String>()
        for (asset in context.assets) {
            val rel = asset.relativeTo(context.root).invariantSeparatorsPath
            if (!seen.add(rel)) {
                out += ValidationReport.Entry(
                    code = "ASSET_DUPLICATE",
                    severity = ValidationReport.Severity.WARNING,
                    message = "duplicate asset path '$rel'",
                    path = rel,
                )
            }
            if (!lowerSeen.add(rel.lowercase())) {
                out += ValidationReport.Entry(
                    code = "ASSET_DUPLICATE_CASE",
                    severity = ValidationReport.Severity.WARNING,
                    message = "asset '$rel' collides case-insensitively with another asset",
                    path = rel,
                )
            }
        }
        if (context.totalSizeBytes > MAX_BUNDLE_BYTES) {
            out += ValidationReport.Entry(
                code = "BUNDLE_TOO_LARGE",
                severity = ValidationReport.Severity.ERROR,
                message = "bundle is ${context.totalSizeBytes / (1024 * 1024)} MB; limit is ${MAX_BUNDLE_BYTES / (1024 * 1024)} MB",
            )
        }
        return out
    }
}
