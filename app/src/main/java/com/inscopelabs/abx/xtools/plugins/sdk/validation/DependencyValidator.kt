package com.inscopelabs.abx.xtools.plugins.sdk.validation

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginId
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginRegistry

/**
 * Confirms every declared dependency is installed and (recursively) that
 * their dependencies are present. Detects cycles.
 */
class DependencyValidator(
    private val registry: PluginRegistry,
) : Validator<PluginProject> {

    override fun validate(context: PluginProject): List<ValidationReport.Entry> {
        val out = mutableListOf<ValidationReport.Entry>()
        val seen = mutableSetOf<PluginId>()
        val stack = mutableListOf<PluginId>()

        fun visit(raw: String) {
            if (!PluginId.isValid(raw)) {
                out += ValidationReport.Entry(
                    code = "DEP_BAD_ID",
                    severity = ValidationReport.Severity.ERROR,
                    message = "dependency '$raw' is not a valid plugin id",
                    path = "plugin-manifest.json#dependencies",
                )
                return
            }
            val id = PluginId.of(raw)
            if (id in stack) {
                out += ValidationReport.Entry(
                    code = "DEP_CYCLE",
                    severity = ValidationReport.Severity.ERROR,
                    message = "dependency cycle: ${stack.joinToString(" -> ")} -> $id",
                    path = "plugin-manifest.json#dependencies",
                )
                return
            }
            if (!seen.add(id)) return
            stack.add(id)
            val dep = registry.get(id)
            if (dep == null) {
                out += ValidationReport.Entry(
                    code = "DEP_MISSING",
                    severity = ValidationReport.Severity.ERROR,
                    message = "dependency '$id' is not installed",
                    path = "plugin-manifest.json#dependencies",
                )
            } else {
                dep.manifest.dependencies.forEach(::visit)
            }
            stack.removeLast()
        }
        context.declaredDependencies.forEach(::visit)
        return out
    }
}
