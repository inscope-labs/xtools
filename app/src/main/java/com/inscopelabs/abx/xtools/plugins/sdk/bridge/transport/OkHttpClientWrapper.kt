package com.inscopelabs.abx.xtools.plugins.sdk.bridge.transport

import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Default [HttpClient] backed by OkHttp. The single client instance is
 * shared across all plugins — connection pooling is the host's
 * responsibility, not the plugin's. Per-call timeouts are deliberately
 * tight to prevent a misbehaving plugin from starving other plugins.
 */
class OkHttpClientWrapper(
    private val client: OkHttpClient = defaultClient(),
) : HttpClient {

    override suspend fun execute(
        method: String,
        url: String,
        body: String?,
    ): BridgeResponse = withContext(Dispatchers.IO) {
        val safeMethod = method.uppercase()
        if (safeMethod !in ALLOWED_METHODS) {
            return@withContext BridgeResponse.failure(
                "BAD_METHOD",
                "method $method not allowed",
            )
        }
        val requestBody = body?.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val builder = Request.Builder().url(url)
        when (safeMethod) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete(requestBody)
            else -> builder.method(safeMethod, requestBody)
        }
        val response = try {
            client.newCall(builder.build()).execute()
        } catch (t: Throwable) {
            return@withContext BridgeResponse.failure(
                "NETWORK_ERROR",
                t.message ?: t::class.java.simpleName,
            )
        }
        response.use { resp ->
            val text = resp.body?.string().orEmpty()
            BridgeResponse.success(
                mapOf(
                    "status" to resp.code,
                    "body" to text.take(MAX_BODY_CHARS),
                ),
            )
        }
    }

    companion object {
        private const val MAX_BODY_CHARS = 64 * 1024

        private val ALLOWED_METHODS = setOf("GET", "POST", "PUT", "PATCH", "DELETE")

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
