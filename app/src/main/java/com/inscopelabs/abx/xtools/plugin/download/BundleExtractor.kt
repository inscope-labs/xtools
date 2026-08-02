package com.inscopelabs.abx.xtools.plugin.download

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Extracts a plugin bundle while preventing zip-slip (path traversal) attacks.
 *
 * @see §4.2 Step 3.2.3
 */
object BundleExtractor {

    @Throws(SecurityException::class)
    fun extract(zipFile: File, destinationDir: File): List<File> {
        if (!destinationDir.exists()) destinationDir.mkdirs()

        val extractedFiles = mutableListOf<File>()
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val targetFile = File(destinationDir, entry.name)

                // SECURITY: Prevent zip-slip.
                val canonicalDest = destinationDir.canonicalPath
                val canonicalTarget = targetFile.canonicalPath
                if (!canonicalTarget.startsWith(canonicalDest + File.separator) && canonicalTarget != canonicalDest) {
                    throw SecurityException("Zip-slip attempt: ${entry.name} escapes destination directory")
                }

                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    extractedFiles.add(targetFile)
                }
            }
        }
        return extractedFiles
    }
}
