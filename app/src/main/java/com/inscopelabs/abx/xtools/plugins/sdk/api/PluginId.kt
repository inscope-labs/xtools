package com.inscopelabs.abx.xtools.plugins.sdk.api

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Strongly-typed plugin identifier. Wraps a reverse-DNS string such as
 * `com.example.hello-world`. Use the factory [PluginId.of] rather than the
 * constructor directly — it normalizes case and validates shape.
 */
@JvmInline
@Serializable
value class PluginId(val value: String) {

    init {
        require(isValid(value)) {
            "Invalid PluginId: '$value'. Must match $PATTERN"
        }
    }

    /** Lowercased string form, suitable for filesystem paths. */
    fun asPath(): String = value.lowercase(Locale.ROOT)

    override fun toString(): String = value

    companion object {
        // 3+ dot-separated segments, each [a-z0-9-], leading segment must
        // contain a dot. Allows hyphens, which reverse-DNS often uses for
        // multi-word names.
        private val PATTERN = Regex("^[a-z0-9]+(\\.[a-z0-9-]+){2,}$")

        fun isValid(raw: String): Boolean =
            raw.isNotEmpty() && PATTERN.matches(raw.lowercase(Locale.ROOT))

        fun of(raw: String): PluginId {
            val normalized = raw.trim().lowercase(Locale.ROOT)
            require(isValid(normalized)) {
                "PluginId must be reverse-DNS, got '$raw'"
            }
            return PluginId(normalized)
        }
    }
}
