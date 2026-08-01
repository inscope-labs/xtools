package com.inscopelabs.abx.xtools.plugins.studio

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.LogBridge as SdkLogBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hosts the in-app code editor. Stage 7.6 says Monaco is preferred; this
 * class loads a local HTML wrapper that pulls in `monaco-editor`'s
 * loader.js from a bundled asset.
 *
 * The editor runs inside a WebView with a tight CSP and no remote
 * network access. Save is exposed via JS bridge so it can be triggered
 * from the editor's own keyboard shortcuts.
 *
 * The class is also used by the asset manager and the live preview to
 * display non-code text files — they just set a different "mode".
 */
class CodeEditorFragment : Fragment() {

    private var webView: WebView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val id = resources.getIdentifier("fragment_code_editor", "layout", requireContext().packageName)
        return inflater.inflate(id, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(resources.getIdentifier("code_webview", "id", requireContext().packageName))
        webView?.settings?.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            allowFileAccess = false
        }
        webView?.addJavascriptInterface(
            EditorBridge(this),
            "XToolsEditor",
        )
        observeSession()
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                StudioSession.state.collectLatest { state ->
                    val file = state.currentFile ?: return@collectLatest
                    val root = state.projectRoot ?: return@collectLatest
                    val text = withContext(Dispatchers.IO) {
                        runCatching { File(root, file).readText(Charsets.UTF_8) }.getOrDefault("")
                    }
                    webView?.post {
                        webView?.loadUrl("file:///android_asset/xtools/editor.html")
                        webView?.evaluateJavascript(
                            "window.__xtools_openEditor(${jsStr(file)}, ${jsStr(text)});",
                            null,
                        )
                    }
                }
            }
        }
    }

    /** Called from the editor JS when the user hits save. */
    internal fun onSaveFromEditor(path: String, text: String) {
        val root = StudioSession.state.value.projectRoot ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                File(root, path).writeText(text, Charsets.UTF_8)
            }
            StudioSession.markDirty()
            SdkLogBridge.shared.publish(
                com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent.Log(
                    pluginId = com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId.of(
                        StudioSession.state.value.manifest?.id ?: "com.xtools.studio"
                    ),
                    timestampMs = System.currentTimeMillis(),
                    level = com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent.Log.Level.INFO,
                    message = "Saved $path",
                )
            )
        }
    }

    private fun jsStr(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    /** Bridge exposed to the editor WebView. Method names map to JS calls. */
    @Suppress("unused")
    private class EditorBridge(private val fragment: CodeEditorFragment) {
        @android.webkit.JavascriptInterface
        fun save(path: String, text: String) = fragment.onSaveFromEditor(path, text)
    }

    override fun onDestroyView() {
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(path: String): CodeEditorFragment = CodeEditorFragment().apply {
            arguments = Bundle().apply { putString("path", path) }
        }
    }
}
