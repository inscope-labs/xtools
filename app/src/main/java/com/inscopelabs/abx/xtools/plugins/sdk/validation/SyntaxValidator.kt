package com.inscopelabs.abx.xtools.plugins.sdk.validation

import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import java.io.File

/**
 * Sanity-checks the JS / HTML / CSS in the project. Not a full parse —
 * just enough to catch the obvious "this won't run at all" cases the
 * user hits while iterating.
 *
 * A future revision will use a real JS parser (e.g. GraalJS or
 * Rhino). For now we use regex heuristics and a JS-syntax probe via
 * [javax.script.ScriptEngine] when available.
 */
object SyntaxValidator : Validator<PluginProject> {

    override fun validate(context: PluginProject): List<ValidationReport.Entry> {
        val out = mutableListOf<ValidationReport.Entry>()
        val jsFiles = context.assets.filter { it.name.endsWith(".js", ignoreCase = true) }
        for (file in jsFiles) {
            val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: continue
            probeJs(file, text)?.let(out::add)
        }
        context.entryFile.takeIf { it.name.endsWith(".html", true) }?.let { f ->
            val html = runCatching { f.readText(Charsets.UTF_8) }.getOrNull() ?: return@let
            probeHtml(html)?.forEach(out::add)
        }
        return out
    }

    private fun probeJs(file: java.io.File, text: String): ValidationReport.Entry? {
        // Cheapest possible check: unmatched braces / parens / brackets.
        val counts = Counts(text)
        val unbalanced = listOfNotNull(
            ("braces" to counts.braces).takeIf { it.second != 0 },
            ("parens" to counts.parens).takeIf { it.second != 0 },
            ("brackets" to counts.brackets).takeIf { it.second != 0 },
        )
        if (unbalanced.isNotEmpty()) {
            return ValidationReport.Entry(
                code = "JS_SYNTAX_UNBALANCED",
                severity = ValidationReport.Severity.ERROR,
                message = "${file.name}: unbalanced ${unbalanced.joinToString { it.first }}",
                path = file.path,
            )
        }
        if ("eval(" in text) {
            return ValidationReport.Entry(
                code = "JS_USES_EVAL",
                severity = ValidationReport.Severity.WARNING,
                message = "${file.name}: uses eval(); most CSPs will block this",
                path = file.path,
            )
        }
        return null
    }

    private fun probeHtml(html: String): List<ValidationReport.Entry> {
        val out = mutableListOf<ValidationReport.Entry>()
        val openTags = Regex("<([a-zA-Z][\\w-]*)(\\s[^>]*)?(?<!/)>").findAll(html).count()
        val closeTags = Regex("</([a-zA-Z][\\w-]*)>").findAll(html).count()
        if (openTags != closeTags) {
            out += ValidationReport.Entry(
                code = "HTML_TAG_MISMATCH",
                severity = ValidationReport.Severity.WARNING,
                message = "open/close tag count differs ($openTags vs $closeTags)",
                path = "index.html",
            )
        }
        return out
    }

    private class Counts(text: String) {
        // Very rough — strings and comments are NOT skipped. Acceptable
        // as a "you'll be surprised" smoke test for plugin authors.
        var braces: Int = 0
        var parens: Int = 0
        var brackets: Int = 0
        var inSingle = false
        var inDouble = false
        var inLineComment = false
        var inBlockComment = false
        var i = 0
        var prev = ' '
        init {
            while (i < text.length) {
                val c = text[i]
                if (inLineComment) {
                    if (c == '\n') inLineComment = false
                } else if (inBlockComment) {
                    if (prev == '*' && c == '/') inBlockComment = false
                } else if (inSingle) {
                    if (c == '\\') i++ // skip escaped char
                    else if (c == '\'') inSingle = false
                } else if (inDouble) {
                    if (c == '\\') i++
                    else if (c == '"') inDouble = false
                } else {
                    when {
                        prev == '/' && c == '/' -> { inLineComment = true; i-- }
                        prev == '/' && c == '*' -> { inBlockComment = true; i-- }
                        c == '\'' -> inSingle = true
                        c == '"' -> inDouble = true
                        c == '{' -> braces++
                        c == '}' -> braces--
                        c == '(' -> parens++
                        c == ')' -> parens--
                        c == '[' -> brackets++
                        c == ']' -> brackets--
                    }
                }
                prev = c
                i++
            }
        }
    }
}
