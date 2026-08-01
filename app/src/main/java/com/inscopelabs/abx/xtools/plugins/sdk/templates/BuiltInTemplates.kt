package com.inscopelabs.abx.xtools.plugins.sdk.templates

import com.inscopelabs.abx.xtools.plugins.sdk.api.Capability
import com.inscopelabs.abx.xtools.plugins.sdk.api.McpConfig
import com.inscopelabs.abx.xtools.plugins.sdk.api.Permission
import com.inscopelabs.abx.xtools.plugins.sdk.api.PluginManifest
import com.inscopelabs.abx.xtools.plugins.sdk.api.Theme

/**
 * The catalog of templates the Studio offers. Each entry is a complete
 * starting point — manifest, code, assets, and (where relevant) MCP
 * wiring. Add a new template by appending to [all].
 *
 * Keep this file declarative; runtime code lives in [HelloWorldTemplate],
 * [McpClientTemplate], etc.
 */
object BuiltInTemplates {

    val all: List<Template> by lazy {
        listOf(
            HelloWorldTemplate,
            FileExplorerTemplate,
            TextEditorTemplate,
            MarkdownViewerTemplate,
            JsonFormatterTemplate,
            ImageViewerTemplate,
            RestClientTemplate,
            McpClientTemplate,
            McpServerTemplate,
            McpHybridTemplate,
            TerminalWidgetTemplate,
            AiToolTemplate,
            SettingsPanelTemplate,
        )
    }
}

private fun manifest(
    id: String,
    name: String,
    summary: String,
    permissions: List<Permission> = emptyList(),
    capabilities: List<Capability> = emptyList(),
    mcp: McpConfig? = null,
): PluginManifest = PluginManifest(
    id = id,
    name = name,
    description = summary,
    version = "0.1.0",
    permissions = permissions.map { it.authority },
    capabilities = capabilities.map { it.tag },
    mcp = mcp,
    theme = Theme.SYSTEM,
)

private fun txt(path: String, body: String) = TemplateFile(path, body)
private fun folder(name: String) = name

// ── 1. Hello World ───────────────────────────────────────────────────
val HelloWorldTemplate: Template = Template(
    id = "hello-world",
    name = "Hello World",
    summary = "Minimal plugin that shows a greeting and a button.",
    tags = listOf("starter", "ui"),
    manifest = manifest(
        id = "com.example.hello-world",
        name = "Hello World",
        summary = "Minimal plugin that shows a greeting and a button.",
        capabilities = listOf(Capability.LAUNCHER_ENTRY),
    ),
    folders = listOf("assets", "docs", "tests"),
    files = listOf(
        txt("plugin-manifest.json", """
            {
              "id": "com.example.hello-world",
              "version": "0.1.0",
              "name": "Hello World",
              "description": "Minimal plugin that shows a greeting and a button.",
              "permissions": [],
              "capabilities": ["launcher.entry"]
            }
        """.trimIndent()),
        txt("index.html", """
            <!doctype html>
            <html><head><meta charset="utf-8"><link rel="stylesheet" href="styles.css"></head>
            <body>
              <h1 id="greeting">Hello, XTools!</h1>
              <button id="btn">Click me</button>
              <script src="main.js"></script>
            </body></html>
        """.trimIndent()),
        txt("main.js", """
            document.getElementById('btn').addEventListener('click', () => {
              document.getElementById('greeting').textContent = 'Clicked at ' + new Date().toISOString();
            });
        """.trimIndent()),
        txt("styles.css", "body { font-family: system-ui; padding: 1.5rem; } button { padding: .5rem 1rem; }"),
        txt("docs/README.md", "# Hello World\n\nClick the button. That's the whole plugin."),
        txt("tests/hello.test.js", "// run from the Studio's preview pane\nassert(document.getElementById('greeting') !== null);"),
    ),
)

// ── 2. File Explorer ─────────────────────────────────────────────────
val FileExplorerTemplate: Template = Template(
    id = "file-explorer",
    name = "File Explorer",
    summary = "Browse and read files in the plugin's own storage scope.",
    tags = listOf("files"),
    manifest = manifest(
        id = "com.example.file-explorer",
        name = "File Explorer",
        summary = "Browse and read files in the plugin's own storage scope.",
        permissions = listOf(Permission.STORAGE_READ),
        capabilities = listOf(Capability.LAUNCHER_ENTRY, Capability.SIDEBAR_PANEL),
    ),
    folders = listOf(folder("assets")),
    files = listOf(
        txt("index.html", "<div id='tree'></div><script src='main.js'></script>"),
        txt("main.js", """
            async function refresh() {
              const r = await window.XToolsBridge.storageList('');
              document.getElementById('tree').textContent = JSON.stringify(r, null, 2);
            }
            refresh();
        """.trimIndent()),
    ),
)

// ── 3. Text Editor ───────────────────────────────────────────────────
val TextEditorTemplate: Template = Template(
    id = "text-editor",
    name = "Text Editor",
    summary = "Open, edit, and save plain text files in plugin storage.",
    tags = listOf("editor", "files"),
    manifest = manifest(
        id = "com.example.text-editor",
        name = "Text Editor",
        summary = "Open, edit, and save plain text files in plugin storage.",
        permissions = listOf(Permission.STORAGE_READ, Permission.STORAGE_WRITE),
        capabilities = listOf(Capability.LAUNCHER_ENTRY),
    ),
    files = listOf(
        txt("index.html", "<textarea id='ed' style='width:100%;height:100%'></textarea>"),
        txt("main.js", """
            const ed = document.getElementById('ed');
            ed.addEventListener('change', () => {
              window.XToolsBridge.storageWrite('note.txt', ed.value, 'cb');
            });
        """.trimIndent()),
    ),
)

// ── 4. Markdown Viewer ───────────────────────────────────────────────
val MarkdownViewerTemplate: Template = Template(
    id = "markdown-viewer",
    name = "Markdown Viewer",
    summary = "Renders a markdown file from plugin storage to HTML.",
    tags = listOf("docs"),
    manifest = manifest(
        id = "com.example.markdown-viewer",
        name = "Markdown Viewer",
        summary = "Renders a markdown file from plugin storage to HTML.",
        permissions = listOf(Permission.STORAGE_READ),
        capabilities = listOf(Capability.LAUNCHER_ENTRY),
    ),
    files = listOf(
        txt("index.html", "<article id='md'></article><script src='main.js'></script>"),
        txt("main.js", """
            // Use a markdown lib bundled in the plugin, e.g. marked.min.js
            // For brevity, this stub leaves the article empty.
        """.trimIndent()),
    ),
)

// ── 5. JSON Formatter ────────────────────────────────────────────────
val JsonFormatterTemplate: Template = Template(
    id = "json-formatter",
    name = "JSON Formatter",
    summary = "Pretty-print and validate JSON pasted by the user.",
    tags = listOf("tools", "dev"),
    manifest = manifest(
        id = "com.example.json-formatter",
        name = "JSON Formatter",
        summary = "Pretty-print and validate JSON pasted by the user.",
        permissions = listOf(Permission.CLIPBOARD),
        capabilities = listOf(Capability.LAUNCHER_ENTRY),
    ),
    files = listOf(
        txt("index.html", "<textarea id='in' placeholder='paste JSON'></textarea><pre id='out'></pre>"),
        txt("main.js", """
            document.getElementById('in').addEventListener('input', (e) => {
              try {
                document.getElementById('out').textContent =
                  JSON.stringify(JSON.parse(e.target.value), null, 2);
              } catch (err) {
                document.getElementById('out').textContent = 'Invalid: ' + err.message;
              }
            });
        """.trimIndent()),
    ),
)

// ── 6. Image Viewer ──────────────────────────────────────────────────
val ImageViewerTemplate: Template = Template(
    id = "image-viewer",
    name = "Image Viewer",
    summary = "Drop a file, preview it. Uses the system file picker.",
    tags = listOf("media"),
    manifest = manifest(
        id = "com.example.image-viewer",
        name = "Image Viewer",
        summary = "Drop a file, preview it. Uses the system file picker.",
        permissions = listOf(Permission.FILESYSTEM_PICK),
        capabilities = listOf(Capability.LAUNCHER_ENTRY),
    ),
    files = listOf(
        txt("index.html", "<div id='picker'></div><img id='preview' />"),
        txt("main.js", """
            // The bridge exposes a file picker; bind it to a button here.
            document.getElementById('picker').addEventListener('click', () => {
              window.XToolsBridge.filesystemPick('image/*', 'cb');
            });
        """.trimIndent()),
    ),
)

// ── 7. REST Client ───────────────────────────────────────────────────
val RestClientTemplate: Template = Template(
    id = "rest-client",
    name = "REST Client",
    summary = "Send HTTP requests and inspect responses.",
    tags = listOf("network", "dev"),
    manifest = manifest(
        id = "com.example.rest-client",
        name = "REST Client",
        summary = "Send HTTP requests and inspect responses.",
        permissions = listOf(Permission.NETWORK_HTTP),
        capabilities = listOf(Capability.LAUNCHER_ENTRY),
    ),
    files = listOf(
        txt("index.html", "<input id='url'/><button id='go'>Send</button><pre id='out'></pre>"),
        txt("main.js", """
            document.getElementById('go').addEventListener('click', () => {
              window.XToolsBridge.httpFetch('GET', document.getElementById('url').value, null, 'cb');
            });
        """.trimIndent()),
    ),
)

// ── 8. MCP Client ────────────────────────────────────────────────────
val McpClientTemplate: Template = Template(
    id = "mcp-client",
    name = "MCP Client",
    summary = "Consume MCP endpoints exposed by other plugins.",
    tags = listOf("mcp"),
    mcP = true,
    manifest = manifest(
        id = "com.example.mcp-client",
        name = "MCP Client",
        summary = "Consume MCP endpoints exposed by other plugins.",
        permissions = listOf(Permission.MCP_CLIENT),
        capabilities = listOf(Capability.MCP_CLIENT_CONSUMER, Capability.LAUNCHER_ENTRY),
        mcp = McpConfig(role = McpConfig.Role.CLIENT),
    ),
    files = listOf(
        txt("index.html", "<ul id='tools'></ul>"),
        txt("main.js", """
            // window.xtoolsMcp.listTools() returns the union of tools
            // exposed by every plugin that registered an MCP server.
            window.xtoolsMcp.listTools().then(tools => {
              document.getElementById('tools').innerHTML =
                tools.map(t => '<li>' + t.name + ' — ' + t.description + '</li>').join('');
            });
        """.trimIndent()),
    ),
)

// ── 9. MCP Server ────────────────────────────────────────────────────
val McpServerTemplate: Template = Template(
    id = "mcp-server",
    name = "MCP Server",
    summary = "Expose this plugin's functionality as MCP tools.",
    tags = listOf("mcp"),
    mcP = true,
    manifest = manifest(
        id = "com.example.mcp-server",
        name = "MCP Server",
        summary = "Expose this plugin's functionality as MCP tools.",
        permissions = listOf(Permission.MCP_SERVER),
        capabilities = listOf(Capability.MCP_SERVER_HOST, Capability.MCP_TOOLS),
        mcp = McpConfig(
            role = McpConfig.Role.SERVER,
            tools = listOf(
                McpConfig.McpTool(
                    name = "echo",
                    description = "Returns the input text unchanged.",
                ),
            ),
        ),
    ),
    files = listOf(
        txt("main.js", """
            // Register a tool with the host's MCP bridge.
            window.xtoolsMcp.registerTool({
              name: 'echo',
              description: 'Returns the input text unchanged.',
              handler: async ({ text }) => text,
            });
        """.trimIndent()),
    ),
)

// ── 10. Hybrid MCP ───────────────────────────────────────────────────
val McpHybridTemplate: Template = Template(
    id = "mcp-hybrid",
    name = "Hybrid MCP Plugin",
    summary = "Expose tools to other plugins while also consuming MCP endpoints.",
    tags = listOf("mcp"),
    mcP = true,
    manifest = manifest(
        id = "com.example.mcp-hybrid",
        name = "Hybrid MCP Plugin",
        summary = "Expose tools to other plugins while also consuming MCP endpoints.",
        permissions = listOf(Permission.MCP_SERVER, Permission.MCP_CLIENT),
        capabilities = listOf(Capability.MCP_SERVER_HOST, Capability.MCP_CLIENT_CONSUMER),
        mcp = McpConfig(role = McpConfig.Role.HYBRID),
    ),
    files = listOf(
        txt("main.js", "// Register a tool AND consume another plugin's tools here."),
    ),
)

// ── 11. Terminal Widget ──────────────────────────────────────────────
val TerminalWidgetTemplate: Template = Template(
    id = "terminal-widget",
    name = "Terminal Widget",
    summary = "A small REPL-style widget that talks to a host-side executor.",
    tags = listOf("terminal", "dev"),
    manifest = manifest(
        id = "com.example.terminal-widget",
        name = "Terminal Widget",
        summary = "A small REPL-style widget that talks to a host-side executor.",
        capabilities = listOf(Capability.TERMINAL_WIDGET, Capability.LAUNCHER_ENTRY),
    ),
    files = listOf(
        txt("index.html", "<div id='term'></div>"),
        txt("main.js", "// Wire your favourite xterm.js build into #term here."),
    ),
)

// ── 12. AI Tool ──────────────────────────────────────────────────────
val AiToolTemplate: Template = Template(
    id = "ai-tool",
    name = "AI Tool",
    summary = "A surface that registers an AI callable tool with the host.",
    tags = listOf("ai"),
    manifest = manifest(
        id = "com.example.ai-tool",
        name = "AI Tool",
        summary = "A surface that registers an AI callable tool with the host.",
        capabilities = listOf(Capability.AI_TOOL, Capability.LAUNCHER_ENTRY),
    ),
    files = listOf(
        txt("main.js", """
            // window.xtoolsAi.registerTool({...}) — see AI bridge docs.
        """.trimIndent()),
    ),
)

// ── 13. Settings Panel ───────────────────────────────────────────────
val SettingsPanelTemplate: Template = Template(
    id = "settings-panel",
    name = "Settings Panel",
    summary = "Adds a screen to the host's Settings app.",
    tags = listOf("settings"),
    manifest = manifest(
        id = "com.example.settings-panel",
        name = "Settings Panel",
        summary = "Adds a screen to the host's Settings app.",
        capabilities = listOf(Capability.SETTINGS_PANEL),
    ),
    files = listOf(
        txt("index.html", "<form id='f'></form>"),
        txt("main.js", "// The host will render this form inside the settings screen."),
    ),
)
