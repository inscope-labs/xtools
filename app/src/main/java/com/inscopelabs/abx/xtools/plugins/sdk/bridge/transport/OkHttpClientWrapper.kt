package com.inscopelabs.abx.xtools.plugins.sdk.bridge.transport

import com.inscopelabs.abx.xtools.plugins.sdk.bridge.BridgeResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpClientWrapper(
    private val client: OkHttpClient = OkHttpClient()
) : HttpClient {
    override fun execute(method: String, url: String, body: String?): BridgeResponse {
        return runCatching {
            val requestBody = body?.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .method(method.uppercase(), if (method.equals("GET", ignoreCase = true) || method.equals("HEAD", ignoreCase = true)) null else requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    BridgeResponse.success(responseBody)
                } else {
                    BridgeResponse.failure("HTTP_${response.code}", responseBody)
                }
            }
        }.getOrElse { e ->
            BridgeResponse.failure("NETWORK_ERROR", e.message ?: "Unknown network error")
        }
    }
}
