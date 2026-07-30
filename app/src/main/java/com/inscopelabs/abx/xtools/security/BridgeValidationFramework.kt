package com.inscopelabs.abx.xtools.security

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Validates incoming bridge requests against JSON schemas, action allowlists,
 * and payload field types. Rejects malformed requests before they reach handler implementations.
 *
 * @see §2.4 Step 1.5.2
 */
class BridgeValidationFramework {

    companion object {
        private val ALLOWED_TOP_LEVEL_KEYS = setOf(
            "id", "action", "payload", "args", "pluginId", "type", "streamMarker"
        )

        private val KNOWN_ACTIONS = setOf(
            "storage.read", "storage.write", "storage.list", "storage.createDirectory",
            "storage.deleteFile", "storage.deleteDirectory",
            "context.addEntry", "context.getEntries", "context.exportContext",
            "context.clearContext", "context.estimateSize",
            "system.getDeviceInfo", "system.showNotification", "system.requestPermission",
            "system.getPreference",
            "db.query", "db.exec", "ui.toast", "http.request"
        )

        // Action -> map of (fieldName to expectedType) where expectedType is "string", "boolean", "number", "object", "array"
        private val REQUIRED_PAYLOAD_FIELDS = mapOf(
            "storage.read" to mapOf("path" to "string"),
            "storage.write" to mapOf("path" to "string", "content" to "string"),
            "storage.list" to mapOf("path" to "string"),
            "storage.createDirectory" to mapOf("path" to "string"),
            "storage.deleteFile" to mapOf("path" to "string"),
            "storage.deleteDirectory" to mapOf("path" to "string"),
            "context.addEntry" to mapOf("key" to "string", "value" to "string"),
            "system.showNotification" to mapOf("title" to "string", "message" to "string"),
            "system.requestPermission" to mapOf("capability" to "string"),
            "system.getPreference" to mapOf("key" to "string")
        )
    }

    fun validate(requestJson: String): ValidationResult {
        return try {
            val element = JsonParser.parseString(requestJson)
            if (!element.isJsonObject) {
                return ValidationResult.Invalid("Request must be a JSON object")
            }
            val json = element.asJsonObject

            // 1. Check top-level keys for unexpected fields
            for (key in json.keySet()) {
                if (key !in ALLOWED_TOP_LEVEL_KEYS) {
                    return ValidationResult.Invalid("Unexpected top-level field '$key' in bridge request")
                }
            }

            // 2. Presence of mandatory fields
            if (!json.has("pluginId") || json.get("pluginId").asString.isBlank()) {
                return ValidationResult.Invalid("Missing or empty 'pluginId' field")
            }
            if (!json.has("action") || json.get("action").asString.isBlank()) {
                return ValidationResult.Invalid("Missing or empty 'action' field")
            }

            val action = json.get("action").asString

            // 3. Action allowlist check
            if (action !in KNOWN_ACTIONS) {
                return ValidationResult.Invalid("Action '$action' is not in the allowed bridge action list")
            }

            // 4. Payload validation
            val payloadObj = when {
                json.has("payload") && json.get("payload").isJsonObject -> json.getAsJsonObject("payload")
                json.has("args") && json.get("args").isJsonObject -> json.getAsJsonObject("args")
                else -> JsonObject()
            }

            val payloadValResult = validatePayloadObject(action, payloadObj)
            if (payloadValResult is ValidationResult.Invalid) {
                return payloadValResult
            }

            ValidationResult.Valid(json)
        } catch (e: Exception) {
            ValidationResult.Invalid("Malformed JSON: ${e.message}")
        }
    }

    fun validatePayload(action: String, payloadJsonStr: String): ValidationResult {
        if (action !in KNOWN_ACTIONS) {
            return ValidationResult.Invalid("Action '$action' is not in the allowed bridge action list")
        }
        return try {
            val element = JsonParser.parseString(payloadJsonStr)
            val payloadObj = if (element.isJsonObject) element.asJsonObject else JsonObject()
            validatePayloadObject(action, payloadObj)
        } catch (e: Exception) {
            ValidationResult.Invalid("Malformed payload JSON: ${e.message}")
        }
    }

    private fun validatePayloadObject(action: String, payload: JsonObject): ValidationResult {
        val requiredFields = REQUIRED_PAYLOAD_FIELDS[action] ?: return ValidationResult.Valid(payload)

        for ((field, expectedType) in requiredFields) {
            if (!payload.has(field)) {
                return ValidationResult.Invalid("Missing required payload field '$field' for action '$action'")
            }
            val element = payload.get(field)
            if (!isTypeMatching(element, expectedType)) {
                return ValidationResult.Invalid("Invalid type for field '$field' in action '$action'. Expected $expectedType")
            }
        }
        return ValidationResult.Valid(payload)
    }

    private fun isTypeMatching(element: JsonElement, expectedType: String): Boolean {
        return when (expectedType) {
            "string" -> element.isJsonPrimitive && element.asJsonPrimitive.isString
            "number" -> element.isJsonPrimitive && element.asJsonPrimitive.isNumber
            "boolean" -> element.isJsonPrimitive && element.asJsonPrimitive.isBoolean
            "object" -> element.isJsonObject
            "array" -> element.isJsonArray
            else -> true
        }
    }

    sealed class ValidationResult {
        data class Valid(val payload: JsonObject) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}

