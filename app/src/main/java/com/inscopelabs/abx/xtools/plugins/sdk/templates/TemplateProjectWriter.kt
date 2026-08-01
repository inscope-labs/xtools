package com.inscopelabs.abx.xtools.plugins.sdk.templates

import java.io.File
import java.util.Base64

/**
 * Materializes a [Template] into a directory on disk. Existing files are
 * not overwritten unless [overwrite] is true.
 */
class TemplateProjectWriter(
    private val overwrite: Boolean = false,
) {
    fun write(template: Template, into: File) {
        require(into.exists() || into.mkdirs()) { "could not create $into" }
        template.folders.forEach { rel ->
            File(into, rel).mkdirs()
        }
        for (file in template.files) {
            val target = File(into, file.path)
            target.parentFile?.mkdirs()
            if (target.exists() && !overwrite) continue
            val payload = file.base64?.let { Base64.getDecoder().decode(it) }
            if (payload != null) {
                target.writeBytes(payload)
            } else {
                target.writeText(file.content, Charsets.UTF_8)
            }
        }
    }
}
