package com.inscopelabs.abx.xtools.plugins.sdk.templates

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest

/**
 * A data-driven plugin template. Templates are pure values — they can be
 * serialized, embedded, or downloaded from a future registry. Rendering a
 * template produces a project tree.
 *
 * The [TemplateRegistry] enumerates the templates the Studio offers in
 * its "New Project" wizard.
 */
data class Template(
    val id: String,
    val name: String,
    val summary: String,
    val manifest: PluginManifest,
    val files: List<TemplateFile>,
    val folders: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val mcP: Boolean = false,
) {
    /** Convenience: list of declared permission authorities. */
    val permissions: List<String> get() = manifest.permissions
}

data class TemplateFile(
    /** Path relative to the project root, using `/` separators. */
    val path: String,
    /** UTF-8 contents. */
    val content: String,
    /** Optional marker for binary files (e.g. icons) — base64 payload. */
    val base64: String? = null,
)
