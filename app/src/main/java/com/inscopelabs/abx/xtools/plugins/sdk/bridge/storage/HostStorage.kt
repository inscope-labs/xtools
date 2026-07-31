package com.inscopelabs.abx.xtools.plugins.sdk.bridge.storage

interface HostStorage {
    fun read(path: String): ByteArray?
    fun write(path: String, contents: ByteArray)
}
