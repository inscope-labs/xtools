package com.inscopelabs.abx.xtools.plugin.lifecycle

/**
 * Represents an atomic operation that either completes fully or rolls back.
 * Used by InstallationPipeline to ensure no partially installed plugins.
 *
 * @see §4.5 Step 3.5.4
 */
interface AtomicOperation<T> {
    suspend fun execute(): T
    suspend fun rollback()
}

class AtomicInstallation(
    private val installFn: suspend () -> InstallResult,
    private val rollbackFn: suspend () -> Unit
) : AtomicOperation<InstallResult> {
    private var completed = false

    override suspend fun execute(): InstallResult {
        return try {
            installFn().also { completed = true }
        } catch (e: Exception) {
            rollback()
            throw e
        }
    }

    override suspend fun rollback() {
        if (!completed) rollbackFn()
    }
}
