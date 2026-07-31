package com.inscopelabs.abx.xtools.plugins.sdk.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.storage.HostStorage
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.storage.PluginScopedStorage
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.transport.HttpClient
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.transport.OkHttpClientWrapper
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.notify.Notifier
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.notify.SystemTrayNotifier
import java.io.File

/**
 * Per-call carrier holding the collaborators the bridge methods need.
 * Constructed on demand by [BridgeContext.from] — these collaborators are
 * intentionally swappable so the Studio can stub them out in tests.
 */
class BridgeContext private constructor(
    val storage: HostStorage,
    val httpClient: HttpClient,
    val notifier: Notifier,
    val clipboard: ClipboardAccess,
    val networkStatus: NetworkStatus,
    val assets: AssetReader,
) {
    fun hostStorage(): HostStorage = storage
    fun httpClient(): HttpClient = httpClient
    fun notifier(): Notifier = notifier
    fun clipboard(): ClipboardAccess = clipboard
    fun networkStatus(): NetworkStatus = networkStatus
    fun readAsset(path: String): ByteArray? = assets.read(path)

    companion object {
        fun from(context: Context): BridgeContext = from(context, scope = null, installDir = null)

        /**
         * Full constructor. `installDir` enables per-plugin asset access;
         * pass `null` when the bridge runs outside an installed plugin
         * (e.g. the Studio's own live preview).
         */
        fun from(
            context: Context,
            scope: String?,
            installDir: File?,
        ): BridgeContext {
            val appContext = context.applicationContext
            val storage = PluginScopedStorage(
                root = File(appContext.filesDir, "plugins/${scope ?: "_scratch_"}"),
            )
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return BridgeContext(
                storage = storage,
                httpClient = OkHttpClientWrapper(),
                notifier = SystemTrayNotifier(appContext),
                clipboard = AndroidClipboard(appContext),
                networkStatus = AndroidNetworkStatus(cm),
                assets = installDir?.let { FileAssetReader(it) } ?: EmptyAssetReader,
            )
        }
    }
}

/** Read-only access to the plugin's bundled assets. */
interface AssetReader {
    fun read(path: String): ByteArray?
}

private class FileAssetReader(private val root: File) : AssetReader {
    override fun read(path: String): ByteArray? {
        val f = File(root, path)
        if (!f.exists() || !f.canRead()) return null
        return f.readBytes()
    }
}

private object EmptyAssetReader : AssetReader {
    override fun read(path: String): ByteArray? = null
}

/** Clipboard facade — small enough to live in this file. */
interface ClipboardAccess {
    fun read(): String
    fun write(text: String)
}

private class AndroidClipboard(private val context: Context) : ClipboardAccess {
    private val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    override fun read(): String {
        val desc = cm.primaryClipDescription
        if (desc == null) return ""
        val clip = cm.primaryClip ?: return ""
        if (clip.itemCount == 0) return ""
        return clip.getItemAt(0).text?.toString() ?: ""
    }

    override fun write(text: String) {
        cm.setPrimaryClip(ClipData.newPlainText("xtools", text))
    }
}

interface NetworkStatus {
    fun asJson(): String
}

private class AndroidNetworkStatus(private val cm: ConnectivityManager) : NetworkStatus {
    override fun asJson(): String {
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        return """{"online":$online,"wifi":$wifi,"cellular":$cellular}"""
    }
}
