package com.inscopelabs.abx.xtools.plugins.sdk.bridge.storage

import java.io.File

class PluginScopedStorage(private val root: File) : HostStorage {
    init {
        root.mkdirs()
    }

    override fun read(path: String): ByteArray? {
        val file = File(root, path)
        if (!file.exists() || !file.isFile || !file.canRead()) return null
        return file.readBytes()
    }

    override fun write(path: String, contents: ByteArray) {
        val file = File(root, path)
        file.parentFile?.mkdirs()
        file.writeBytes(contents)
    }
}
