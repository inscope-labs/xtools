# Agent Task Report: Include Debug Plugin Files

- **Timestamp (UTC)**: 2026-07-31T20:25:00Z
- **Task Slug**: include-debug-plugin-files

## 1. Task Request
The user requested to include debug plugin files in the codebase (`ConsoleView.kt`, `DebugToolkit.kt`, `Inspector.kt`, `LogBridge.kt`, and `PerformanceMonitor.kt`).

## 2. Version Increment Assessment
- **Assessed Score**: 100 / 100 (high probability that task requires a debug build as Kotlin source code under `app/src/main/java` was added/modified).
- **Resulting Action**: Incremented `versionCode` (20 -> 21) and `debugCode` (0020 -> 0021) in `version.properties`.

## 3. Files Created / Modified
- `version.properties`: Incremented `versionCode` to 21 and `debugCode` to 0021.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/debug/ConsoleView.kt`: Added `ConsoleView` UI component for streaming plugin and bridge logs.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/debug/DebugToolkit.kt`: Added unified debug manager attaching console, inspector, performance monitor, and log bridge.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/debug/Inspector.kt`: Added DOM, storage, and state inspector snapshot utility.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/debug/LogBridge.kt`: Added log bridge event router.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/debug/PerformanceMonitor.kt`: Added performance metric recorder.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/sdk/api/PluginId.kt`: Added SDK `PluginId` value class.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/sdk/api/PluginEvent.kt`: Added SDK `PluginEvent` sealed hierarchy.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/sdk/bridge/BridgeCallEvent.kt`: Added SDK `BridgeCallEvent` model.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/sdk/bridge/LogBridge.kt`: Added SDK `LogBridge` shared event bus.
- `app/src/main/java/com/inscopelabs/abx/xtools/plugins/sdk/bridge/PluginBridge.kt`: Added SDK `PluginBridge` event publisher.

## 4. Commands Executed & Results
- `compile_applet`: Initial build failed with `Unresolved reference 'invoke'` in `Inspector.kt`. Resolved by replacing `storageAccessor?.invoke()` with `storageAccessor?.list()`. Subsequent `compile_applet` build succeeded.

## 5. Assumptions & Notes
- `StorageAccessor` SAM interface defines `list()`, so `captureStorage()` calls `storageAccessor?.list()`.
- Added missing SDK contract types under `com.inscopelabs.abx.xtools.plugins.sdk` so that all debug components resolve cleanly.
