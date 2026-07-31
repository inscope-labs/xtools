# Process Report: xtools Phase 2, Step 2.1.1 — Toolbar Menu Scaffold

**Timestamp:** 2026-07-31T00:31:00Z  
**Task Slug:** toolbar-menu-scaffold

## Assessed Probability Score & Version Action
- **Assessed Probability Score:** 80% (Task updates app resources, toolbar menu definitions, and adds vector drawables affecting the app's top bar layout).
- **Resulting Action:** Incremented `versionCode` (11 -> 12) and `debugCode` (0011 -> 0012) in `version.properties`.

---

## What Was Asked
Implement Phase 2 Step 2.1.1 (Toolbar Menu Scaffold) by replacing the search item in `menu_main.xml` with three unwired menu items (Chat, Settings, Privacy) using Material-style vector drawables:
- `action_chat`: `app:showAsAction="always"`, `@drawable/ic_chat`, `Chat`
- `action_settings`: `app:showAsAction="always"`, `@drawable/ic_settings`, `Settings`
- `action_privacy`: `app:showAsAction="never"`, `@drawable/ic_privacy`, `Privacy`

---

## Files Touched & Summary of Changes

1. **`version.properties`**:
   - Incremented `versionCode` to 12 and `debugCode` to `0012` per AGENTS.md rule.

2. **`app/src/main/res/drawable/ic_chat.xml`** (New File):
   - Created 24dp x 24dp vector drawable with a standard chat bubble outline glyph (`fillColor="#FF000000"`).

3. **`app/src/main/res/drawable/ic_settings.xml`** (New File):
   - Created 24dp x 24dp vector drawable with a standard gear/cog glyph (`fillColor="#FF000000"`).

4. **`app/src/main/res/drawable/ic_privacy.xml`** (New File):
   - Created 24dp x 24dp vector drawable with a standard shield outline glyph (`fillColor="#FF000000"`).

5. **`app/src/main/res/menu/menu_main.xml`**:
   - Removed `action_search` item.
   - Added `action_chat` (`showAsAction="always"`), `action_settings` (`showAsAction="always"`), and `action_privacy` (`showAsAction="never"`).

---

## Confirmations & Out-Of-Scope Verification
- **`MainActivity.kt`**: Confirmed NOT modified in any way. No menu click handling or navigation wiring was added.
- **`ic_search.xml`**: Left in place untouched.
- **Compose Files**: No Jetpack Compose files were created or modified.

---

## Commands Executed & Results
- `compile_applet`: Compilation succeeded cleanly with zero errors.
