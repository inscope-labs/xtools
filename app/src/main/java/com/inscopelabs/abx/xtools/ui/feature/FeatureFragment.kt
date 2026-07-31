package com.inscopelabs.abx.xtools.ui.feature

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.XToolsApplication
import com.inscopelabs.abx.xtools.bridge.JsBridge
import com.inscopelabs.abx.xtools.kernel.session.PluginSession
import com.inscopelabs.abx.xtools.kernel.session.SessionManager
import com.inscopelabs.abx.xtools.plugin.manager.PluginLoadResult
import com.inscopelabs.abx.xtools.plugin.manager.UnifiedPluginLoader
import com.inscopelabs.abx.xtools.webview.SecureWebView
import kotlinx.coroutines.launch

class FeatureFragment : Fragment() {

    private var featureId: String? = null
    private var featureTitle: String? = null
    private var secureWebView: SecureWebView? = null
    private var jsBridge: JsBridge? = null
    private var currentSession: PluginSession? = null

    private val app: XToolsApplication
        get() = requireActivity().application as XToolsApplication

    private val sessionManager: SessionManager
        get() = app.sessionManager

    companion object {
        private const val ARG_FEATURE_ID = "arg_feature_id"
        private const val ARG_FEATURE_TITLE = "arg_feature_title"
        private const val KEY_WEBVIEW_STATE = "key_webview_state"

        fun newInstance(id: String, title: String): FeatureFragment {
            return FeatureFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FEATURE_ID, id)
                    putString(ARG_FEATURE_TITLE, title)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        featureId = savedInstanceState?.getString(ARG_FEATURE_ID) ?: arguments?.getString(ARG_FEATURE_ID)
        featureTitle = savedInstanceState?.getString(ARG_FEATURE_TITLE) ?: arguments?.getString(ARG_FEATURE_TITLE)
    }

    fun getFeatureTitle(): String {
        return featureTitle ?: arguments?.getString(ARG_FEATURE_TITLE) ?: "Feature"
    }

    fun webViewCanGoBack(): Boolean = secureWebView?.canGoBack() ?: false
    fun webViewGoBack() { secureWebView?.goBack() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feature, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val placeholderContainer = view.findViewById<LinearLayout>(R.id.placeholderContainer)
        val titleText = view.findViewById<TextView>(R.id.featureTitleText)
        val idText = view.findViewById<TextView>(R.id.featureIdText)
        val webViewContainer = view.findViewById<FrameLayout>(R.id.webViewContainer)

        val title = getFeatureTitle()
        val id = featureId ?: ""

        titleText.text = "Feature: $title"
        idText.text = "ID: $id"

        val targetPluginDir = when (id) {
            "sample", "com.inscopelabs.xtools.sample.fileviewer" -> "sample"
            "database", "sqlite-crud", "com.inscopelabs.xtools.plugin.contextbuilder" -> "database"
            "system-info", "com.inscopelabs.xtools.plugin.sysinfo" -> "system-info"
            else -> null
        }

        if (targetPluginDir != null) {
            placeholderContainer.visibility = View.GONE
            webViewContainer.visibility = View.VISIBLE

            val webView = SecureWebView(requireContext())
            secureWebView = webView
            webViewContainer.addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            val pluginId = when (targetPluginDir) {
                "sample" -> "com.inscopelabs.xtools.sample.fileviewer"
                "database" -> "com.inscopelabs.xtools.plugin.contextbuilder"
                "system-info" -> "com.inscopelabs.xtools.plugin.sysinfo"
                else -> targetPluginDir
            }

            // Register declared & granted capabilities on PermissionManager for sample plugins
            val permManager = app.permissionManager
            when (targetPluginDir) {
                "system-info" -> {
                    permManager.registerPluginDeclaredPermissions(pluginId, listOf("system", "ui"))
                    permManager.grantPermission(pluginId, "system")
                    permManager.grantPermission(pluginId, "ui")
                }
                "sample" -> {
                    permManager.registerPluginDeclaredPermissions(pluginId, listOf("system", "storage", "ui"))
                    permManager.grantPermission(pluginId, "system")
                    permManager.grantPermission(pluginId, "storage")
                    permManager.grantPermission(pluginId, "ui")
                }
                "database" -> {
                    permManager.registerPluginDeclaredPermissions(pluginId, listOf("storage", "context", "ui"))
                    permManager.grantPermission(pluginId, "storage")
                    permManager.grantPermission(pluginId, "context")
                    permManager.grantPermission(pluginId, "ui")
                }
            }

            // Start plugin session in shared SessionManager
            currentSession = sessionManager.createSession(pluginId)

            // Construct & attach live JsBridge backed by shared BridgeApiFacade
            val bridge = JsBridge(
                handler = null,
                scope = viewLifecycleOwner.lifecycleScope,
                facade = app.bridgeApiFacade
            )
            bridge.attachWebView(webView, pluginId)
            webView.addJavascriptInterface(bridge, "XToolsNativeBridge")
            jsBridge = bridge

            val webViewState = savedInstanceState?.getBundle(KEY_WEBVIEW_STATE)
            if (webViewState != null) {
                webView.restoreState(webViewState)
            } else {
                lifecycleScope.launch {
                    val loader = UnifiedPluginLoader(requireContext(), isDevelopmentMode = true)
                    when (val result = loader.loadPlugin(targetPluginDir)) {
                        is PluginLoadResult.Success -> {
                            webView.loadDataWithBaseURL(
                                result.baseUrl,
                                result.contentHtml,
                                "text/html",
                                "utf-8",
                                null
                            )
                        }
                        is PluginLoadResult.Error -> {
                            placeholderContainer.visibility = View.VISIBLE
                            webViewContainer.visibility = View.GONE
                            idText.text = "Failed to load plugin: ${result.reason}"
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_FEATURE_ID, featureId)
        outState.putString(ARG_FEATURE_TITLE, featureTitle)
        secureWebView?.let { webView ->
            val webViewState = Bundle()
            webView.saveState(webViewState)
            outState.putBundle(KEY_WEBVIEW_STATE, webViewState)
        }
    }

    override fun onResume() {
        super.onResume()
        secureWebView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        secureWebView?.onPause()
    }

    override fun onDestroyView() {
        jsBridge?.detachWebView()
        jsBridge = null

        currentSession?.let { session ->
            sessionManager.closeSession(session.id)
            currentSession = null
        }
        secureWebView?.destroy()
        secureWebView = null
        super.onDestroyView()
    }
}

