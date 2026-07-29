# Task Report: Wire feature card taps to open dedicated fragments with toolbar swap

**Timestamp:** 2026-07-29T12:30:00Z  
**Task Slug:** feature-open-toolbar-swap-navigation

## 1. What was asked
Wire feature card taps in `PluginsFragment` and `ConsoleFragment` to open dedicated `FeatureFragment` views with toolbar state swap:
- Create `FeatureFragment` (`FeatureFragment.kt` and `fragment_feature.xml`) to display feature details or load sample webview assets when matching.
- Update `PluginsFragment.kt` and `ConsoleFragment.kt` to push `FeatureFragment` onto their own `childFragmentManager` when feature cards are tapped.
- Update `MainActivity.kt` to listen to child backstack changes in both containers, dynamically updating `ToolbarStateViewModel` between `Branded` and `Feature(title)`.
- Ensure switching pill modes re-evaluates and displays the active container's toolbar state without affecting backstacks.
- Handle back navigation so pressing back pops the active container's child backstack before exiting the app.

## 2. What was changed

### Created Files:
- `app/src/main/java/com/inscopelabs/abx/xtools/ui/feature/FeatureFragment.kt`:
  - Receives `featureId` and `featureTitle` via arguments.
  - Renders placeholder text or embeds `SecureWebView` if matching HTML assets exist (`sample`, `database`, `system-info`).
- `app/src/main/res/layout/fragment_feature.xml`:
  - Contains placeholder title/id layout and `webViewContainer` for embedding `SecureWebView`.

### Modified Files:
- `app/src/main/java/com/inscopelabs/abx/xtools/ui/category/CategoryFragment.kt`:
  - Added `var onFeatureClickListener: ((FeatureItem) -> Unit)?` property and invoked it from `SectionedFeatureAdapter`.
- `app/src/main/java/com/inscopelabs/abx/xtools/ui/PluginsFragment.kt`:
  - Listens to feature clicks from `CategoryFragment` and replaces `childContainer` with `FeatureFragment`, adding transaction to `childFragmentManager`'s backstack.
- `app/src/main/java/com/inscopelabs/abx/xtools/ui/ConsoleFragment.kt`:
  - Listens to feature clicks from `CategoryFragment` and replaces `childContainer` with `FeatureFragment`, adding transaction to `childFragmentManager`'s backstack.
- `app/src/main/java/com/inscopelabs/abx/xtools/ui/MainActivity.kt`:
  - Attached `addOnBackStackChangedListener` to `PluginsFragment` and `ConsoleFragment` child fragment managers.
  - Implemented `updateToolbarForCurrentContainer()` to check active container depth and set `ToolbarState.Feature(title)` or `ToolbarState.Branded`.
  - Re-evaluates toolbar state on pill mode toggle.
  - Overrode `OnBackPressedCallback` to call `popActiveBackStack()`.

## 3. Commands Executed & Results
- `find app/src/main -type f`: Found existing webview, bridge, and asset structures.
- `compile_applet`: Compilation succeeded without any build errors.

## 4. Assumptions Made
- `FeatureFragment` matches sample assets by checking `id` (`sample`, `database`, `sqlite-crud`, `system-info`).

## 5. Errors or Unverified Behavior
- None. Build verified clean via `compile_applet`.
