package com.inscopelabs.abx.xtools.dispatcher

import kotlinx.coroutines.flow.Flow

interface ChatProvider {
    suspend fun sendMessage(prompt: String, settings: ChatSettings): Flow<String>
    fun supportsCapability(capability: String): Boolean
}
