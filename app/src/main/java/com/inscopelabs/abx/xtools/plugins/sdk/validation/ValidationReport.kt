package com.inscopelabs.abx.xtools.plugins.sdk.validation

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId

/**
 * Result of running a validator. Sealed so the [BuildFragment] can render
 * failure vs. success differently without re-parsing the report.
 */
sealed interface ValidationReport {
    val pluginId: PluginId?
    val entries: List<Entry>

    data class Success(
        override val pluginId: PluginId? = null,
        override val entries: List<Entry> = emptyList(),
    ) : ValidationReport {
        val isClean: Boolean get() = entries.none { it.severity == Severity.ERROR }
    }

    data class Failure(
        override val pluginId: PluginId? = null,
        override val entries: List<Entry>,
    ) : ValidationReport {
        init {
            require(entries.any { it.severity == Severity.ERROR }) {
                "ValidationReport.Failure must contain at least one error"
            }
        }
    }

    enum class Severity { ERROR, WARNING, INFO }

    data class Entry(
        val code: String,
        val severity: Severity,
        val message: String,
        val path: String? = null,
    )

    fun errors(): List<Entry> = entries.filter { it.severity == Severity.ERROR }
    fun warnings(): List<Entry> = entries.filter { it.severity == Severity.WARNING }
}
