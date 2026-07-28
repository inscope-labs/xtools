package com.inscopelabs.abx.xtools.bridge

import java.util.UUID

data class ConsoleLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val level: String,
    val message: String,
    val pluginId: String = "system",
    val timestamp: Long = System.currentTimeMillis()
)
