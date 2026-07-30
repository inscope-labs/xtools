# Process Report: Correct Interference in MainActivity from Pre-Phase Stub Merge

**Timestamp:** 2026-07-30T16:28:30Z  
**Task Slug:** correct-mainactivity-prephase-stub-interference

## Assessed Probability Score & Version Action
- **Assessed Probability Score:** 85% (Task involves modifying navigation container back stack handling and UI activity behavior requiring a debug build verification).
- **Resulting Action:** Incremented `versionCode` (9 -> 10) and `debugCode` (0009 -> 0010) in `version.properties`.

---

## STEP 1 — Mandatory Drift-Check Findings
1. **`MainActivity.kt`**:
   - Class KDoc comment contained outdated reference to hosting bottom navigation ("Hosts the bottom navigation and coordinates fragment transactions").
   - `onCreate()` invoked `setupBottomNavigation()`, which was an empty stub method.
   - `navigateToPluginDetail(pluginId)` and `navigateToSettings()` both called `supportFragmentManager.beginTransaction().replace(R.id.pluginsContainer, fragment)...`, destroying the persistent `PluginsFragment` host and its child back stack.
2. **`PluginsFragment.kt` & `ConsoleFragment.kt`**:
   - Both host an internal `R.id.childContainer` frame inside `childFragmentManager` and push feature fragments onto their own child back stacks via `childFragmentManager.beginTransaction().replace(R.id.childContainer, fragment).addToBackStack(null).commit()`.
   - `MainActivity` listens to `childFragmentManager` back stack changes on both container tags (`TAG_PLUGINS` and `TAG_CONSOLE`) and handles back presses by calling `popActiveBackStack()`.

---

## STEP 2 — Corrections Implemented (`MainActivity.kt`)

### 1. Fixed Navigation Methods (`navigateToPluginDetail` and `navigateToSettings`)
Changed `navigateToPluginDetail(pluginId)` and `navigateToSettings()` to retrieve the persistent `PluginsFragment` via `supportFragmentManager.findFragmentByTag(TAG_PLUGINS)` and push target fragments onto its `childFragmentManager` targeting `R.id.childContainer`, preserving the `add()` dual-container invariant and back stack mechanics. Also updated `updateToolbarForCurrentContainer()` to resolve titles for `PluginDetailFragment` ("Plugin Details") and `AppearanceFragment` ("Settings").

### 2. Removed Dead `setupBottomNavigation()` Stub
Removed `setupBottomNavigation()` and its call site from `onCreate()`. Corrected class KDoc comment to accurately describe the dual-container layout (Plugins and Console) managed via `PillModeSwitch`.

---

## Exact Diff (`MainActivity.kt`)

```diff
 /**
  * Main activity container for the xtools host UI.
- * Hosts the bottom navigation and coordinates fragment transactions.
+ * Manages the dual-container layout (Plugins and Console) toggled via a pill mode switch
+ * and coordinates child fragment transactions within each container.
  * Jetpack Compose is NOT used – everything is XML/Fragment-based.
  *
  * @see §3.1.1, §3.1.1 Step 2.1.1
  */
 class MainActivity : AppCompatActivity() {

@@ -71,7 +71,6 @@
                 .commitNow()
         }
 
-        setupBottomNavigation()
         applyTheme()

@@ -121,7 +121,9 @@
             val topFragment = childFm.findFragmentById(R.id.childContainer)
             val title = (topFragment as? FeatureFragment)?.getFeatureTitle()
                 ?: topFragment?.arguments?.getString("arg_feature_title")
-                ?: "Feature"
+                ?: if (topFragment is com.inscopelabs.abx.xtools.ui.plugindetail.PluginDetailFragment) "Plugin Details"
+                else if (topFragment is com.inscopelabs.abx.xtools.ui.settings.AppearanceFragment) "Settings"
+                else "Feature"
             toolbarViewModel.setToolbarState(ToolbarState.Feature(title))
         } else {
             toolbarViewModel.setToolbarState(ToolbarState.Branded)

@@ -184,11 +184,6 @@
         }
     }
 
-    private fun setupBottomNavigation() {
-        // Real implementation will use setOnItemSelectedListener to switch fragments.
-        // Stub: bottom navigation is defined in res/menu/bottom_nav_menu.xml.
-    }
-
     private fun applyTheme() {
         // Delegates to ThemeManager for dynamic Material You colors.
         ThemeManager.applyTheme(this)

     fun navigateToPluginDetail(pluginId: String) {
+        val pluginsFrag = supportFragmentManager.findFragmentByTag(TAG_PLUGINS)
         val fragment = com.inscopelabs.abx.xtools.ui.plugindetail.PluginDetailFragment.newInstance(pluginId)
-        supportFragmentManager.beginTransaction()
-            .replace(R.id.pluginsContainer, fragment, "plugin_detail")
-            .addToBackStack(null)
-            .commit()
+        pluginsFrag?.childFragmentManager?.beginTransaction()
+            ?.replace(R.id.childContainer, fragment)
+            ?.addToBackStack(null)
+            ?.commit()
     }

     fun navigateToSettings() {
+        val pluginsFrag = supportFragmentManager.findFragmentByTag(TAG_PLUGINS)
         val fragment = com.inscopelabs.abx.xtools.ui.settings.AppearanceFragment()
-        supportFragmentManager.beginTransaction()
-            .replace(R.id.pluginsContainer, fragment)
-            .addToBackStack(null)
-            .commit()
+        pluginsFrag?.childFragmentManager?.beginTransaction()
+            ?.replace(R.id.childContainer, fragment)
+            ?.addToBackStack(null)
+            ?.commit()
     }
```

---

## Orphaned Resource Files Reference Audit
- `res/menu/menu_bottom_navigation.xml`: Confirmed NOT referenced anywhere in source files under `app/src/main/`. Preserved in repository as instructed.
- `res/color/bottom_nav_item_color.xml`: Confirmed NOT referenced anywhere in source files under `app/src/main/`. Preserved in repository as instructed.

---

## Verification & Build Status
- `compile_applet`: Compilation succeeded with zero errors.
