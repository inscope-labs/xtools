package com.inscopelabs.abx.xtools.plugins.sdk.templates

/**
 * Enumerates the templates available for new projects. The Studio pulls
 * this list at startup and shows it in the project-creation wizard.
 *
 * Adding a new template means adding an entry to [BuiltInTemplates] — the
 * UI picks it up automatically.
 */
interface TemplateRegistry {
    fun all(): List<Template>
    fun byId(id: String): Template?

    companion object {
        fun builtIn(): TemplateRegistry = BuiltInTemplateRegistry
    }
}

object BuiltInTemplateRegistry : TemplateRegistry {
    override fun all(): List<Template> = BuiltInTemplates.all
    override fun byId(id: String): Template? = BuiltInTemplates.all.firstOrNull { it.id == id }
}
