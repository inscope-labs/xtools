# xtools Android Application

A modular Android application with Kotlin-JavaScript bridge for webview-based plugin execution.

## Application ID
`com.inscopelabs.abx.xtools`

## Build

```bash
./gradlew assembleDebug
```

## Architecture

### Phase 1: Foundation & Bridge Contract
- Secure WebView configuration
- Kotlin-JavaScript bridge communication
- Basic plugin loader

### Phase 2: Native Host UI
- Material 3 UI shell
- Edge-to-edge support
- Navigation handling

### Phase 3-6: Plugin System & Production
- Plugin download & installation
- Security sandbox
- Native APIs
- Production readiness

## Project Structure

```
app/src/main/java/com/inscopelabs/abx/xtools/
├── bridge/          # Kotlin-JavaScript bridge contracts
├── plugin/          # Plugin management
│   └── manager/     # Plugin lifecycle management
├── ui/              # UI components
└── webview/         # Secure WebView implementation

app/src/main/assets/plugins/
├── sample/          # Sample plugin
└── xtools-bridge.js # JavaScript bridge SDK
```

## Security Features
- Content Security Policy enforcement
- SHA-256 checksum verification
- Encrypted preferences storage
- Sandbox execution environment
- Permission-based access control

## Design System
- Material 3 design language
- Corporate/Modern aesthetic
- Inter + JetBrains Mono typography
- 8px baseline grid
