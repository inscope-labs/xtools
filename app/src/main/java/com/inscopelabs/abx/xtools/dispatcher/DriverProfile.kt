package com.inscopelabs.abx.xtools.dispatcher

data class DriverProfile(
    val driverId: String,
    val enabled: Boolean = false,
    val settings: ChatSettings = ChatSettings()
)
