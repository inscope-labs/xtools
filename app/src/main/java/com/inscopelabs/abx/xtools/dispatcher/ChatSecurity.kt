package com.inscopelabs.abx.xtools.dispatcher

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ChatSecurity(context: Context, storeName: String = "abx_secure_chat_prefs") {
    private val sharedPreferences: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            storeName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun storeApiKey(provider: String, apiKey: String) {
        sharedPreferences.edit().putString("api_key_$provider", apiKey).apply()
    }

    fun getApiKey(provider: String): String? {
        return sharedPreferences.getString("api_key_$provider", null)
    }
}