package com.inscopelabs.abx.xtools.ui.feature

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.inscopelabs.abx.xtools.R
import com.inscopelabs.abx.xtools.webview.SecureWebView

class FeatureFragment : Fragment() {

    private var featureId: String? = null
    private var featureTitle: String? = null
    private var secureWebView: SecureWebView? = null

    companion object {
        private const val ARG_FEATURE_ID = "arg_feature_id"
        private const val ARG_FEATURE_TITLE = "arg_feature_title"

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
        featureId = arguments?.getString(ARG_FEATURE_ID)
        featureTitle = arguments?.getString(ARG_FEATURE_TITLE)
    }

    fun getFeatureTitle(): String {
        return featureTitle ?: arguments?.getString(ARG_FEATURE_TITLE) ?: "Feature"
    }

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

        val assetPluginPath = when (id) {
            "sample" -> "plugins/sample/index.html"
            "database", "sqlite-crud" -> "plugins/database/index.html"
            "system-info" -> "plugins/system-info/index.html"
            else -> null
        }

        if (assetPluginPath != null) {
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

            try {
                val baseUrl = "file:///android_asset/${assetPluginPath.substringBeforeLast("/")}/"
                val inputStream = requireContext().assets.open(assetPluginPath)
                val htmlContent = inputStream.bufferedReader().use { it.readText() }
                webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "utf-8", null)
            } catch (e: Exception) {
                placeholderContainer.visibility = View.VISIBLE
                webViewContainer.visibility = View.GONE
                idText.text = "Failed to load plugin asset: ${e.message}"
            }
        }
    }

    override fun onDestroyView() {
        secureWebView?.destroy()
        secureWebView = null
        super.onDestroyView()
    }
}
