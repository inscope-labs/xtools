package com.inscopelabs.abx.xtools.plugins.sdk.bridge.storage

import java.io.File

class PluginScopedStorage(private val root: File) : HostStorage {

    init {
        // Don't create eagerly — many plugins never write.
    }

    override fun read(path: String): ByteArray? {
        val resolved = resolve(path) ?: return null
        if (!resolved.isFile) return null
        return resolved.readBytes()
    }

    override fun write(path: String, data: ByteArray) {
        val resolved = resolve(path) ?: throw SecurityException("path escape: $path")
        resolved.parentFile?.mkdirs()
        resolved.writeBytes(data)
    }

    override fun delete(path: String): Boolean =
        resolve(path)?.let { if (it.exists()) it.delete() else false } ?: false

    override fun list(prefix: String): List<String> {
        val base = resolve(prefix) ?: return emptyList()
        if (!base.isDirectory) return emptyList()
        return base.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(base).invariantSeparatorsPath }
            .toList()
    }

    private fun resolve(path: String): File? {
        if (path.isBlank() || path.contains("..") || path.startsWith("/")) return null
        val candidate = File(root, path).canonicalFile
        val rootCanonical = root.canonicalFile
        // Path-containment check.
        if (!candidate.absolutePath.startsWith(rootCanonical.absolutePath + File.separator)
            && candidate != rootCanonical
        ) return null
        return candidate
    }
}
