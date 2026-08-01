package com.inscopelabs.abx.xtools.plugins.sdk.installer

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginRegistry
import com.inscopelabs.abx.xtools.plugins.sdk.api.RegisteredPlugin
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.ManifestCodec
import com.inscopelabs.abx.xtools.plugins.sdk.signing.SignatureVerifier
import com.inscopelabs.abx.xtools.plugins.sdk.validation.CompositeValidator
import com.inscopelabs.abx.xtools.plugins.sdk.validation.PluginProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * The single pipeline that takes a signed, packaged plugin ZIP and
 * installs it. Used by [LocalInstaller] today and by the future registry
 * client tomorrow — so any change here ripples to both.
 *
 * Steps, in order:
 *   1) Verify the signature
 *   2) Unpack the bundle to a staging directory
 *   3) Validate (manifest + assets + csp + syntax + deps)
 *   4) Move to the install root
 *   5) Update the in-memory registry
 *   6) Fire immediate activation
 */
class InstallationPipeline(
    private val config: Config,
    private val registry: PluginRegistry,
    private val signatureVerifier: SignatureVerifier,
    private val validator: CompositeValidator,
    private val onActivated: (PluginId) -> Unit = {},
) {

    data class Config(
        val pluginsRoot: File,
    )

    sealed interface Result {
        data class Success(val id: PluginId, val installPath: File) : Result
        data class Failure(val reason: String, val code: String = "INSTALL_FAILED") : Result
    }

    suspend fun install(bundle: File, signatureDir: File): Result = withContext(Dispatchers.IO) {
        // 1) signature
        when (val v = signatureVerifier.verify(bundle, signatureDir)) {
            is SignatureVerifier.VerificationResult.Missing ->
                return@withContext Result.Failure("plugin is unsigned", "UNSIGNED")
            is SignatureVerifier.VerificationResult.Invalid ->
                return@withContext Result.Failure("signature invalid: ${v.reason}", "BAD_SIGNATURE")
            is SignatureVerifier.VerificationResult.Valid -> Unit
        }

        // 2) stage
        val staging = File(config.pluginsRoot, "_staging/${bundle.nameWithoutExtension}-${System.currentTimeMillis()}")
        staging.parentFile?.mkdirs()
        runCatching { unzip(bundle, staging) }
            .onFailure { return@withContext Result.Failure("unzip failed: ${it.message}", "UNZIP_FAILED") }

        // 3) load + validate
        val manifestFile = File(staging, "plugin-manifest.json")
        if (!manifestFile.isFile) return@withContext Result.Failure("missing manifest", "NO_MANIFEST")
        val manifest = runCatching { ManifestCodec.decode(manifestFile.readBytes()) }
            .getOrElse { return@withContext Result.Failure("bad manifest: ${it.message}", "BAD_MANIFEST") }
        val project = scanProject(staging, manifest)
        val report = validator.run(project)
        if (report.errors().isNotEmpty()) {
            staging.deleteRecursively()
            return@withContext Result.Failure(
                "validation failed (${report.errors().size} errors)",
                "VALIDATION_FAILED",
            )
        }

        // 4) move to install root
        val target = File(config.pluginsRoot, manifest.id)
        if (target.exists()) target.deleteRecursively()
        if (!staging.renameTo(target)) {
            copyRecursively(staging, target)
            staging.deleteRecursively()
        }

        // 5) registry
        val registered = RegisteredPlugin(
            id = PluginId.of(manifest.id),
            manifest = manifest,
            installPath = target.absolutePath,
            signatureValid = true,
            installedAtMs = System.currentTimeMillis(),
            version = manifest.version,
        )
        registry.install(registered)

        // 6) activate
        onActivated(PluginId.of(manifest.id))
        Result.Success(PluginId.of(manifest.id), target)
    }

    private fun unzip(zip: File, into: File) {
        into.mkdirs()
        ZipInputStream(zip.inputStream()).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                val out = File(into, entry.name)
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zin.copyTo(it) }
                }
                zin.closeEntry()
                entry = zin.nextEntry
            }
        }
    }

    private fun scanProject(root: File, manifest: com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest): PluginProject {
        val assets = root.walkTopDown().filter { it.isFile }.toList()
        val total = assets.sumOf { it.length() }
        return PluginProject(
            root = root,
            manifest = manifest,
            manifestFile = File(root, "plugin-manifest.json"),
            entryFile = File(root, manifest.entry),
            assets = assets,
            totalSizeBytes = total,
            declaredDependencies = manifest.dependencies,
        )
    }

    private fun copyRecursively(from: File, to: File) {
        if (from.isDirectory) {
            to.mkdirs()
            from.listFiles()?.forEach { copyRecursively(it, File(to, it.name)) }
        } else {
            from.copyTo(to, overwrite = true)
        }
    }
}
