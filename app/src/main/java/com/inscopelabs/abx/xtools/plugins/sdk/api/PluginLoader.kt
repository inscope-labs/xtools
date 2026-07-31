package com.inscopelabs.abx.xtools.plugins.sdk.api

import android.content.Context
import java.io.File

/**
 * Loads a plugin from its installed location. Implementations are
 * responsible for verifying the signature (see
 * [com.inscopelabs.abx.xtools.plugins.sdk.signing.SignatureVerifier])
 * before invoking user code.
 *
 * The default loader is file-system based: it expects a plugin to be
 * extracted into a directory of the form
 * `<pluginsRoot>/<plugin-id-as-path>/`.
 */
fun interface PluginLoader {
    fun load(context: Context, pluginId: PluginId): Plugin

    companion object {
        /**
         * Default loader — reads from `pluginsRoot/<id>/` and wires the
         * manifest + asset path into a [FilesystemPlugin]. Real bridge
         * setup happens in [com.inscopelabs.abx.xtools.plugins.sdk.bridge.PluginBridge]
         * when the host attaches a WebView.
         */
        fun filesystem(pluginsRoot: File): PluginLoader = PluginLoader { _, id ->
            val dir = File(pluginsRoot, id.asPath())
            require(dir.isDirectory) { "Plugin directory not found: ${dir.absolutePath}" }
            val manifestFile = File(dir, "plugin-manifest.json")
            require(manifestFile.isFile) { "Missing plugin-manifest.json in ${dir.absolutePath}" }
            val manifest = com.inscopelabs.abx.xtools.plugins.sdk.bridge.ManifestCodec
                .decode(manifestFile.readBytes())
            FilesystemPlugin(
                id = id,
                installDir = dir,
                manifest = manifest,
            )
        }
    }
}

/**
 * Minimal [Plugin] implementation backed by a directory on disk. The Studio
 * uses a richer subclass that tracks dirty state for live preview.
 */
open class FilesystemPlugin(
    override val id: PluginId,
    protected val installDir: File,
    override val manifest: PluginManifest,
) : Plugin {

    override val displayName: String get() = manifest.name

    override fun onLoad(context: Context, host: PluginHost) {
        // Subclasses wire the bridge; the filesystem flavor is intentionally
        // a no-op so the loader can be swapped without coupling to the
        // bridge package at API-evaluation time.
    }

    override fun onResume() = Unit
    override fun onPause() = Unit
    override fun onDestroy() = Unit

    /** The on-disk location of the plugin bundle. */
    val root: File get() = installDir
}
