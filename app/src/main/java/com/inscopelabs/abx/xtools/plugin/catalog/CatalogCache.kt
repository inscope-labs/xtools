package com.inscopelabs.abx.xtools.plugin.catalog

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Caches catalog results locally to reduce network usage and enable offline browsing.
 * Uses SharedPreferences for lightweight storage (or a database for larger caches).
 *
 * @see §4.1.1 Step 3.1.2
 */
class CatalogCache(private val context: Context) {
    private val prefs = context.getSharedPreferences("catalog_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _cachedSearch = MutableStateFlow<CatalogSearchResult?>(null)
    val cachedSearch: StateFlow<CatalogSearchResult?> = _cachedSearch.asStateFlow()

    fun cacheSearchResult(result: CatalogSearchResult) {
        _cachedSearch.value = result
        prefs.edit { putString("last_search", gson.toJson(result)) }
    }

    fun loadCachedSearch(): CatalogSearchResult? {
        val json = prefs.getString("last_search", null) ?: return null
        return try {
            gson.fromJson(json, CatalogSearchResult::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        _cachedSearch.value = null
        prefs.edit { clear() }
    }
}
