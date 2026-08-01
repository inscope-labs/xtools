package com.inscopelabs.abx.xtools.plugins.sdk.validation

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a fixed list of [Validator]s and folds the results into a
 * [ValidationReport]. Pure orchestration — no validation logic lives here.
 *
 * Async-friendly: all validators are run in sequence on `Dispatchers.IO`,
 * so a slow CSS-parse validator doesn't block the UI thread.
 */
class CompositeValidator(
    private val validators: List<Validator<PluginProject>>,
) {
    suspend fun run(project: PluginProject): ValidationReport = withContext(Dispatchers.Default) {
        val entries = validators.flatMap { it.validate(project) }
        val hasErrors = entries.any { it.severity == ValidationReport.Severity.ERROR }
        if (hasErrors) ValidationReport.Failure(entries = entries)
        else ValidationReport.Success(entries = entries)
    }

    companion object {
        /**
         * Default validator chain — every public Validator the SDK ships
         * out of the box, in the order they should run.
         */
        fun defaults(registry: PluginRegistry): CompositeValidator = CompositeValidator(
            listOf(
                ManifestValidator,
                AssetsValidator,
                CspValidator,
                SyntaxValidator,
                DependencyValidator(registry),
            )
        )
    }
}
