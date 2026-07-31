package com.inscopelabs.abx.xtools.plugins.sdk.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.inscopelabs.abx.xtools.plugins.sdk.api.Permission
import com.inscopelabs.abx.xtools.plugins.sdk.api.Plugin
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The single `@JavascriptInterface` exposed to plugin JS. Methods are
 * individually routed through [PermissionGate] — a JS call to
 * `bridge.storage.read()` with no declared `STORAGE_READ` permission is
 * denied and logged to the [LogBridge].
 *
 * Every method returns a [BridgeResponse] which is marshaled to JSON and
 * passed back to the JS callback. JS never sees a thrown exception —
 * errors come back as a `failure` response with a structured code.
 *
 * Lifecycle: created once per [WebView], then attached via [attach]. The
 * bridge outlives individual plugin reloads; only [detach] is needed
 * when the WebView itself goes away.
 */
class PluginBridge(
    private val context: Context,
    private val gate: PermissionGate,
    private val logBridge: LogBridge,
) {

    private val _events = MutableSharedFlow<BridgeCallEvent>(extraBufferCapacity = 64)
    /** Emitted for every bridge call, allowed or denied. */
    val events: SharedFlow<BridgeCallEvent> = _events.asSharedFlow()

    private var attachedWebView: WebView? = null

    /**
     * Install the JS interface onto [webView]. Idempotent — calling
     * [attach] twice on the same WebView replaces the previous binding.
     */
    fun attach(webView: WebView) {
        detach()
        webView.addJavascriptInterface(this, JS_INTERFACE_NAME)
        attachedWebView = webView
    }

    fun detach() {
        attachedWebView?.removeJavascriptInterface(JS_INTERFACE_NAME)
        attachedWebView = null
    }

    // ── JS-callable surface ────────────────────────────────────────────
    // All methods follow the same shape:
    //   1) check permission
    //   2) record start time
    //   3) dispatch on a background dispatcher
    //   4) emit a BridgeCallEvent for the console
    //   5) invoke the JS callback with a serialized BridgeResponse

    @JavascriptInterface
    fun storageRead(path: String, callbackId: String) {
        handle(
            method = "storage.read",
            permission = Permission.STORAGE_READ,
            args = arrayOf(path),
            callbackId = callbackId,
        ) { ctx ->
            val bytes = ctx.readAsset(path) ?: ctx.hostStorage().read(path)
            if (bytes == null) BridgeResponse.failure("NOT_FOUND", "no such file: $path")
            else BridgeResponse.success(String(bytes))
        }
    }

    @JavascriptInterface
    fun storageWrite(path: String, contents: String, callbackId: String) {
        handle(
            method = "storage.write",
            permission = Permission.STORAGE_WRITE,
            args = arrayOf(path, "<${contents.length} chars>"),
            callbackId = callbackId,
        ) { ctx ->
            ctx.hostStorage().write(path, contents.toByteArray(Charsets.UTF_8))
            BridgeResponse.success("ok")
        }
    }

    @JavascriptInterface
    fun networkStatus(callbackId: String) {
        handle(
            method = "network.status",
            permission = Permission.NETWORK_STATUS,
            args = emptyArray(),
            callbackId = callbackId,
        ) { ctx ->
            BridgeResponse.success(ctx.networkStatus().asJson())
        }
    }

    @JavascriptInterface
    fun httpFetch(url: String, method: String, body: String?, callbackId: String) {
        handle(
            method = "network.http",
            permission = Permission.NETWORK_HTTP,
            args = arrayOf(method, url),
            callbackId = callbackId,
        ) { ctx ->
            ctx.httpClient().execute(method, url, body)
        }
    }

    @JavascriptInterface
    fun clipboardRead(callbackId: String) {
        handle(
            method = "clipboard.read",
            permission = Permission.CLIPBOARD,
            args = emptyArray(),
            callbackId = callbackId,
        ) { ctx ->
            BridgeResponse.success(ctx.clipboard().read())
        }
    }

    @JavascriptInterface
    fun clipboardWrite(text: String, callbackId: String) {
        handle(
            method = "clipboard.write",
            permission = Permission.CLIPBOARD,
            args = arrayOf("<${text.length} chars>"),
            callbackId = callbackId,
        ) { ctx ->
            ctx.clipboard().write(text)
            BridgeResponse.success("ok")
        }
    }

    @JavascriptInterface
    fun notify(title: String, body: String, callbackId: String) {
        handle(
            method = "notifications.show",
            permission = Permission.NOTIFICATIONS,
            args = arrayOf(title),
            callbackId = callbackId,
        ) { ctx ->
            ctx.notifier().show(title, body)
            BridgeResponse.success("ok")
        }
    }

    // ── internal ───────────────────────────────────────────────────────

    private fun handle(
        method: String,
        permission: Permission,
        args: Array<Any?>,
        callbackId: String,
        block: suspend (BridgeContext) -> BridgeResponse,
    ) {
        scope.launch {
            val start = System.nanoTime()
            val allowed = gate.isGranted(permission)
            val response: BridgeResponse = if (!allowed) {
                BridgeResponse.failure(
                    "PERMISSION_DENIED",
                    "bridge method '$method' requires '${permission.authority}'",
                )
            } else {
                runCatching { withContext(Dispatchers.IO) { block(BridgeContext.from(context)) } }
                    .getOrElse { t ->
                        BridgeResponse.failure(
                            "RUNTIME_ERROR",
                            t.message ?: t::class.java.simpleName,
                        )
                    }
            }
            val durationMs = (System.nanoTime() - start) / 1_000_000
            _events.tryEmit(
                BridgeCallEvent(
                    method = method,
                    allowed = allowed,
                    durationMs = durationMs,
                    args = args.map { it?.toString() ?: "null" },
                    resultPreview = response.preview(),
                )
            )
            deliverToJs(callbackId, response)
        }
    }

    private fun deliverToJs(callbackId: String, response: BridgeResponse) {
        val web = attachedWebView ?: return
        val js = "window.__xtools_bridge_cb('$callbackId', ${response.toJsLiteral()});"
        web.post { web.evaluateJavascript(js, null) }
    }

    companion object {
        /** Name the JS side calls back through. */
        const val JS_INTERFACE_NAME: String = "XToolsBridge"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
