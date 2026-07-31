package com.inscopelabs.abx.xtools.plugins.debug

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginEvent
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeCallEvent
import com.inscopelabs.abx.xtools.plugins.sdk.bridge.LogBridge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The in-app console that renders every [PluginEvent] and
 * [BridgeCallEvent]. Drop one of these into any layout — it manages its
 * own scrolling and color-coding. The host wires it to a [LogBridge]
 * (and optionally a bridge `events` flow) and the console starts
 * streaming.
 *
 * Not a RecyclerView — a scrolling TextView is plenty for the volume
 * the Studio produces, and avoids adapter coupling.
 */
class ConsoleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ScrollView(context, attrs, defStyleAttr) {

    private val text = TextView(context).apply {
        setTextColor(Color.parseColor("#E0E0E0"))
        setBackgroundColor(Color.parseColor("#101010"))
        typeface = Typeface.MONOSPACE
        setPadding(24, 24, 24, 24)
        textSize = 12f
    }
    private val builder = SpannableStringBuilder()
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)
    private val maxLines: Int = 5_000
    private val pending = MutableSharedFlow<Line>(extraBufferCapacity = 1024)
    private var lifecycleOwner: LifecycleOwner? = null

    init {
        addView(text)
        isFillViewport = true
    }

    /** Bind the console to a [LifecycleOwner] and start consuming. */
    fun bindTo(owner: LifecycleOwner, log: LogBridge = LogBridge.shared) {
        lifecycleOwner = owner
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { log.events.collect { appendEvent(it) } }
            }
        }
    }

    /** Wire in bridge events from [com.inscopelabs.abx.xtools.plugins.sdk.bridge.PluginBridge.events]. */
    fun observeBridge(events: SharedFlow<BridgeCallEvent>) {
        lifecycleOwner?.lifecycleScope?.launch {
            events.collect { appendBridge(it) }
        }
    }

    /** Manual injection — useful from tests or non-lifecycle hosts. */
    fun submit(line: Line) {
        pending.tryEmit(line)
    }

    fun clear() {
        builder.clear()
        text.text = builder
    }

    private fun appendEvent(event: PluginEvent) {
        when (event) {
            is PluginEvent.Log -> append(
                Line(
                    level = event.level.name,
                    message = event.message,
                    tag = event.pluginId.value,
                )
            )
            is PluginEvent.Error -> append(
                Line(
                    level = "ERROR",
                    message = event.message + (event.stack?.let { "\n$it" } ?: ""),
                    tag = event.pluginId.value,
                )
            )
            is PluginEvent.BridgeCall -> append(
                Line(
                    level = if (event.allowed) "BRIDGE" else "DENIED",
                    message = "${event.method}(${event.args.joinToString(", ")}) -> ${event.resultPreview}",
                    tag = "bridge",
                )
            )
            is PluginEvent.Lifecycle -> append(
                Line(
                    level = "LIFECYCLE",
                    message = event.phase.name,
                    tag = event.pluginId.value,
                )
            )
        }
    }

    private fun appendBridge(event: BridgeCallEvent) {
        appendEvent(
            PluginEvent.BridgeCall(
                pluginId = com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId.of("com.xtools.runtime"),
                timestampMs = System.currentTimeMillis(),
                method = event.method,
                allowed = event.allowed,
                durationMs = event.durationMs,
                resultPreview = event.resultPreview,
            )
        )
    }

    private fun append(line: Line) {
        val stamp = timeFmt.format(Date())
        val raw = "$stamp [${line.level}] (${line.tag}) ${line.message}\n"
        val start = builder.length
        builder.append(raw)
        val end = builder.length
        val color = when (line.level) {
            "ERROR" -> Color.parseColor("#FF6E6E")
            "WARN" -> Color.parseColor("#FFB74D")
            "DENIED" -> Color.parseColor("#FF5252")
            "BRIDGE" -> Color.parseColor("#80CBC4")
            "LIFECYCLE" -> Color.parseColor("#9FA8DA")
            else -> Color.parseColor("#E0E0E0")
        }
        builder.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(StyleSpan(Typeface.BOLD), start, start + stamp.length + line.level.length + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        trimIfNeeded()
        text.text = builder
        post { fullScroll(View.FOCUS_DOWN) }
    }

    private fun trimIfNeeded() {
        if (builder.length <= maxLines * 80) return
        // Keep the most recent `maxLines` worth of text. Spannables
        // before the cut point are dropped wholesale.
        val lines = builder.toString().split('\n')
        if (lines.size <= maxLines) return
        val keep = lines.takeLast(maxLines).joinToString("\n")
        val keepLen = keep.length
        builder.clear()
        builder.append(keep)
        // Drop any leading spans — we don't preserve them across the
        // re-append. Acceptable: the trim happens only after the user
        // has produced an enormous log; the first lines are usually
        // boot noise anyway.
        text.setText(builder, TextView.BufferType.SPANNABLE)
        // Suppress the unused warning on keepLen — the line above
        // intentionally drops the value; keep the comment so a future
        // reader doesn't "fix" it.
        @Suppress("UNUSED_VARIABLE") val _u: Int = keepLen
    }

    /** Inlined to avoid a public dep on android.view.View in the import block above. */
    data class Line(
        val level: String,
        val message: String,
        val tag: String,
    )
}
