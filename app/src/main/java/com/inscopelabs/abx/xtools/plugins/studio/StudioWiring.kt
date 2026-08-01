package com.inscopelabs.abx.xtools.plugins.studio

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginRegistry
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeContext
import com.inscopelabs.abx.xtools.plugins.sdk.installer.InstallationPipeline
import com.inscopelabs.abx.xtools.plugins.sdk.installer.RollbackSupport
import com.inscopelabs.abx.xtools.plugins.sdk.registry.RegistryFacade
import com.inscopelabs.abx.xtools.plugins.sdk.signing.DevKeyStore
import com.inscopelabs.abx.xtools.plugins.sdk.validation.CompositeValidator
import java.io.File

/**
 * One-shot wiring for the Studio. The host creates the collaborators,
 * calls [wire], and the Studio's fragments are ready to go. Keeps the
 * "where do these things come from" question out of every Fragment.
 *
 * Typical host usage (in the host Activity / NavHost setup):
 *
 *     val wiring = StudioWiring(
 *         devKeyStore = DevKeyStore(File(filesDir, "xtools/keystore.p12"), password),
 *         registry = registryFacade,
 *         pluginsRoot = File(filesDir, "plugins"),
 *     )
 *     wiring.wireToFragment(myBuildFragment)
 */
class StudioWiring(
    val devKeyStore: DevKeyStore,
    val registry: PluginRegistry,
    val registryFacade: RegistryFacade,
    val pluginsRoot: File,
) {

    val rollback: RollbackSupport = RollbackSupport(pluginsRoot)

    fun newInstallationPipeline(): InstallationPipeline = InstallationPipeline(
        config = InstallationPipeline.Config(pluginsRoot = pluginsRoot),
        registry = registry,
        signatureVerifier = com.inscopelabs.abx.xtools.plugins.sdk.signing.SignatureVerifier(),
        validator = CompositeValidator.defaults(registry),
    )

    fun newBridgeContext(
        context: android.content.Context,
        scope: String? = null,
        installDir: File? = null,
    ): BridgeContext = BridgeContext.from(
        context = context,
        scope = scope,
        installDir = installDir,
    )
}
