package com.inscopelabs.abx.xtools.dispatcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatCache {
    private val cache = mutableMapOf<String, String>()
    private val _size = MutableStateFlow(0)
    val size: StateFlow<Int> = _size

    fun get(key: String): String? = cache[key]

    fun put(key: String, value: String) {
        cache[key] = value
        _size.value = cache.size
    }

    fun clear() {
        cache.clear()
        _size.value = 0
    }
}