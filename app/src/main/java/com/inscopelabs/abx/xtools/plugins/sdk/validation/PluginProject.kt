package com.inscopelabs.abx.xtools.plugins.sdk.validation

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import java.io.File

data class PluginProject(
    val root: File,
    val manifest: PluginManifest,
    val manifestFile: File,
    val entryFile: File,
    val assets: List<File>,
    val totalSizeBytes: Long,
    val declaredDependencies: List<String> = emptyList(),
)
