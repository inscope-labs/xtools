package com.inscopelabs.abx.xtools.plugins.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginRegistry
import com.inscopelabs.abx.xtools.plugins.sdk.installer.InstallationPipeline
import com.inscopelabs.abx.xtools.plugins.sdk.installer.LocalInstaller
import com.inscopelabs.abx.xtools.plugins.sdk.installer.RollbackSupport
import com.inscopelabs.abx.xtools.plugins.sdk.packaging.BuildDirectoryManager
import com.inscopelabs.abx.xtools.plugins.sdk.packaging.BundlePackager
import com.inscopelabs.abx.xtools.plugins.sdk.signing.DevKeyStore
import com.inscopelabs.abx.xtools.plugins.sdk.signing.PluginSigner
import com.inscopelabs.abx.xtools.plugins.sdk.signing.SignatureVerifier
import com.inscopelabs.abx.xtools.plugins.sdk.validation.CompositeValidator
import com.inscopelabs.abx.xtools.plugins.sdk.validation.PluginProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The Build / Validate / Sign / Install pipeline UI. The full pipeline
 * is built here; the host only needs to provide a [DevKeyStore] and a
 * [PluginRegistry]. The actual [DevKeyStore] comes from the host — this
 * fragment receives it via the [BuildFragment.wire] helper, which the
 * host calls from its `onCreate`.
 */
class BuildFragment : Fragment() {

    /**
     * The host-provided collaborators. The fragment tolerates these
     * being `null` — it just disables the buttons until they're
     * injected.
     */
    var devKeyStore: DevKeyStore? = null
    var registry: PluginRegistry? = null
    var pluginsRoot: File? = null

    private var buildButton: Button? = null
    private var installButton: Button? = null
    private var output: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val id = resources.getIdentifier("fragment_build", "layout", requireContext().packageName)
        return inflater.inflate(id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildButton = view.findViewById(resources.getIdentifier("build_button", "id", requireContext().packageName))
        installButton = view.findViewById(resources.getIdentifier("install_button", "id", requireContext().packageName))
        output = view.findViewById(resources.getIdentifier("build_output", "id", requireContext().packageName))
        buildButton?.setOnClickListener { runBuild() }
        installButton?.setOnClickListener { runInstall() }
        observeSession()
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                StudioSession.state.collectLatest { state ->
                    val ready = state.projectRoot != null && devKeyStore != null && registry != null
                    buildButton?.isEnabled = ready
                    installButton?.isEnabled = ready
                }
            }
        }
    }

    private fun runBuild() {
        val state = StudioSession.state.value
        val root = state.projectRoot ?: return
        val manifest = state.manifest ?: return
        val ks = devKeyStore ?: return
        val reg = registry ?: return
        val out = output ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            out.append("\n— Build started —\n")
            val project = withContext(Dispatchers.IO) { scan(root, manifest) }
            val validator = CompositeValidator.defaults(reg)
            val report = validator.run(project)
            if (report.errors().isNotEmpty()) {
                out.append("Build Failed\n")
                out.append("${report.errors().size} Errors\n")
                out.append("${report.warnings().size} Warnings\n")
                report.entries.forEach { e ->
                    out.append("  [${e.severity}] ${e.code}: ${e.message}\n")
                }
                return@launch
            }
            val bdm = BuildDirectoryManager(root)
            bdm.clean(); bdm.ensure()
            val bundle = BundlePackager().packageProject(project, bdm.outputPath(manifest.id, manifest.version))
            val signer = PluginSigner(ks)
            val signature = withContext(Dispatchers.IO) {
                signer.sign(bundle, File(bdm.artifactsDir, "${bundle.nameWithoutExtension}.sig"))
            }
            out.append("Build Successful\n")
            out.append("  bundle: ${bundle.absolutePath}\n")
            out.append("  hash: ${signature.hashFile.readText()}\n")
            out.append("  sig: ${signature.signatureFile.name}\n")
        }
    }

    private fun runInstall() {
        val state = StudioSession.state.value
        val root = state.projectRoot ?: return
        val manifest = state.manifest ?: return
        val reg = registry ?: return
        val ks = devKeyStore ?: return
        val pluginsBase = pluginsRoot ?: return
        val out = output ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val bdm = BuildDirectoryManager(root)
            val bundle = bdm.outputPath(manifest.id, manifest.version)
            if (!bundle.exists()) {
                out.append("Run Build first.\n"); return@launch
            }
            val sigDir = File(bdm.artifactsDir, "${bundle.nameWithoutExtension}.sig")
            val pipeline = InstallationPipeline(
                config = InstallationPipeline.Config(pluginsRoot = pluginsBase),
                registry = reg,
                signatureVerifier = SignatureVerifier(),
                validator = CompositeValidator.defaults(reg),
            )
            val localInstaller = LocalInstaller(pipeline)
            when (val r = localInstaller.install(bundle, sigDir)) {
                is InstallationPipeline.Result.Success -> {
                    out.append("Installed ${r.id} -> ${r.installPath}\n")
                    try {
                        val canonicalManifest = StudioManifestBridge.toCanonical(manifest)
                        com.inscopelabs.abx.xtools.XToolsApplication.instance.pluginRegistry.register(
                            manifest = canonicalManifest,
                            installationPath = r.installPath.path,
                            category = "studio",
                            trustTier = com.inscopelabs.abx.xtools.kernel.registry.PluginTrustTier.PIPELINE_SIGNED
                        )
                        out.append("Registered in app registry (PIPELINE_SIGNED)\n")
                    } catch (e: Exception) {
                        out.append("Local build succeeded but registration into real app failed: ${e.message}\n")
                    }
                }
                is InstallationPipeline.Result.Failure ->
                    out.append("Install failed: ${r.reason} (${r.code})\n")
            }
        }
    }

    private fun scan(root: File, manifest: com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest): PluginProject {
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

    companion object {
        fun newInstance(): BuildFragment = BuildFragment()
    }
}

/**
 * Public host-side wiring helper. Call from your Activity:
 *
 *     val devKeyStore = DevKeyStore(File(filesDir, "xtools/keystore.p12"), password)
 *     val pluginRepository = PluginRepository(File(filesDir, "xtools/registry.json"))
 *     val memoryRegistry = PluginRegistry.inMemory()
 *     val facade = RegistryFacade(memoryRegistry, pluginRepository)
 *     lifecycleScope.launch { facade.boot() }
 *     BuildFragment.wire(frag, devKeyStore, facade, File(filesDir, "plugins"))
 */
object BuildFragmentWiring {
    fun wire(
        fragment: BuildFragment,
        devKeyStore: DevKeyStore,
        registry: PluginRegistry,
        pluginsRoot: File,
    ) {
        fragment.devKeyStore = devKeyStore
        fragment.registry = registry
        fragment.pluginsRoot = pluginsRoot
    }
}
