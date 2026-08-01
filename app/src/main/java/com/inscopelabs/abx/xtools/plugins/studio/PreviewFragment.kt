package com.inscopelabs.abx.xtools.plugins.studio

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.inscopelabs.abx.xtools.plugins.debug.LogBridge
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.CspEnforcer
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.LogBridge as SdkLogBridge
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.PluginBridge
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.PermissionGate
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders the open project's entry file in a sandboxed WebView. Live
 * reload is file-watch based: any change to the project tree triggers a
 * re-load. Errors are forwarded to the [LogBridge] so the
 * [com.inscopelabs.abx.xtools.plugins.debug.ConsoleView] can render them.
 */
class PreviewFragment : Fragment() {

    private var webView: WebView? = null
    private var bridge: PluginBridge? = null
    private var lastLoadSignature: String? = null
    private var fileWatcherJob: kotlinx.coroutines.Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val id = resources.getIdentifier("fragment_preview", "layout", requireContext().packageName)
        return inflater.inflate(id, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(resources.getIdentifier("preview_webview", "id", requireContext().packageName))
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                msg ?: return false
                SdkLogBridge.shared.publish(
                    PluginEvent.Log(
                        pluginId = pluginId(),
                        timestampMs = System.currentTimeMillis(),
                        level = when (msg.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR -> PluginEvent.Log.Level.ERROR
                            ConsoleMessage.MessageLevel.WARNING -> PluginEvent.Log.Level.WARN
                            else -> PluginEvent.Log.Level.INFO
                        },
                        message = "[${msg.messageLevel()}] ${msg.message()}",
                    )
                )
                return true
            }
        }
        webView?.webViewClient = object : android.webkit.WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                request: android.webkit.WebResourceRequest,
                error: android.webkit.WebResourceError,
            ) {
                SdkLogBridge.shared.publish(
                    PluginEvent.Error(
                        pluginId = pluginId(),
                        timestampMs = System.currentTimeMillis(),
                        message = "${error.description} on ${request.url}",
                    )
                )
            }
        }
        observeSession()
    }

    private fun pluginId(): PluginId = PluginId.of(
        StudioSession.state.value.manifest?.id ?: "com.xtools.preview"
    )

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                StudioSession.state.collectLatest { state ->
                    val manifest = state.manifest ?: return@collectLatest
                    val root = state.projectRoot ?: return@collectLatest
                    wireBridge(root, manifest)
                    reload()
                    watchForChanges(root)
                }
            }
        }
    }

    private fun wireBridge(
        root: File,
        manifest: com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest,
    ) {
        val gate = PermissionGate.strict(manifest)
        val log = SdkLogBridge.shared
        bridge?.detach()
        bridge = PluginBridge(requireContext().applicationContext, gate, log).also {
            it.attach(webView!!)
        }
        CspEnforcer.apply(webView!!, manifest.csp)
    }

    private fun watchForChanges(root: File) {
        fileWatcherJob?.cancel()
        fileWatcherJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var lastSnapshot = snapshot(root)
            while (true) {
                kotlinx.coroutines.delay(750)
                val now = snapshot(root)
                if (now != lastSnapshot) {
                    lastSnapshot = now
                    view?.post { reload() }
                }
            }
        }
    }

    private fun snapshot(root: File): String {
        val sb = StringBuilder()
        for (f in root.walkTopDown().filter { it.isFile }) {
            sb.append(f.relativeTo(root).invariantSeparatorsPath)
            sb.append(':')
            sb.append(f.length())
            sb.append(';')
        }
        return sb.toString()
    }

    private fun reload() {
        val state = StudioSession.state.value
        val root = state.projectRoot ?: return
        val manifest = state.manifest ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val entryText = withContext(Dispatchers.IO) {
                runCatching { File(root, manifest.entry).readText(Charsets.UTF_8) }.getOrNull()
            } ?: return@launch
            val withCsp = if ("Content-Security-Policy" in entryText) entryText
            else entryText.replaceFirst("<head>", "<head>${CspEnforcer.metaTag(manifest.csp)}")
            val sig = withCsp.hashCode().toString()
            if (sig == lastLoadSignature) return@launch
            lastLoadSignature = sig
            webView?.loadDataWithBaseURL(
                "https://xtools.local/",
                withCsp,
                "text/html",
                "utf-8",
                null,
            )
        }
    }

    override fun onDestroyView() {
        fileWatcherJob?.cancel()
        fileWatcherJob = null
        bridge?.detach()
        bridge = null
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): PreviewFragment = PreviewFragment()
    }
}
