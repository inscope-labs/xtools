package com.inscopelabs.abx.xtools.plugins.sdk.bridge.storage

/**
 * File-system storage scoped to a single plugin. Reads and writes happen
 * against `<filesDir>/plugins/<plugin-id>/`. The directory is created
 * lazily on first write.
 *
 * Path traversal is blocked: any `path` containing `..` or starting with
 * `/` is rejected before touching the filesystem.
 */
interface HostStorage {
    fun read(path: String): ByteArray?
    fun write(path: String, data: ByteArray)
    fun delete(path: String): Boolean
    fun list(prefix: String = ""): List<String>
}
