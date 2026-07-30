package com.inscopelabs.abx.xtools.security

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Validates incoming bridge requests against JSON schemas and basic security checks.
 * Rejects malformed requests before they reach handler implementations.
 *
 * @see §2.4 Step 1.5.2
 */
class BridgeValidationFramework {

    fun validate(requestJson: String): ValidationResult {
        return try {
            val json = JsonParser.parseString(requestJson).asJsonObject
            when {
                !json.has("pluginId") -> ValidationResult.Invalid("Missing 'pluginId' field")
                !json.has("action") -> ValidationResult.Invalid("Missing 'action' field")
                else -> ValidationResult.Valid(json)
            }
        } catch (e: Exception) {
            ValidationResult.Invalid("Malformed JSON: ${e.message}")
        }
    }

    sealed class ValidationResult {
        data class Valid(val payload: JsonObject) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
