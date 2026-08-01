package com.inscopelabs.abx.xtools.plugins.sdk.validation

/**
 * A single validation pass. Validators are pure functions: they take
 * context in, return a list of [ValidationReport.Entry]s out. The
 * [CompositeValidator] runs them in order and folds the results.
 */
fun interface Validator<T> {
    fun validate(context: T): List<ValidationReport.Entry>
}
