package com.inscopelabs.abx.xtools.plugins.sdk.validation

data class ValidationError(val message: String)

data class ValidationReport(
    val errors: List<ValidationError> = emptyList(),
) {
    fun errors(): List<ValidationError> = errors
}

interface Validator {
    fun validate(project: PluginProject): List<ValidationError>
}

class CompositeValidator(
    private val validators: List<Validator> = emptyList(),
) {
    fun run(project: PluginProject): ValidationReport {
        val allErrors = validators.flatMap { it.validate(project) }
        return ValidationReport(allErrors)
    }
}
