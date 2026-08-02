# Agent Report: Scaffold Release Build Workflows

**Date/Timestamp (UTC):** 2026-08-01T17:46:00Z  
**Task Slug:** `scaffold-release-build-workflows`

---

## 1. Task Request Overview
This task created manual release workflows for building signed APKs (`.github/workflows/release-apk.yml`) and AABs (`.github/workflows/release-aab.yml`) using Supabase atomic `versionCode` generation. It also created `release-state.json` at the repository root and appended Section 2a ("Debug Code Reset After a Release") to `AGENTS.md`.

---

## 2. Version Increment Assessment (Rule 2 & Task Scope)
- **Assessed Probability Score:** 0% (Task scope explicitly excludes `version.properties`; workflow and documentation setup only).
- **Resulting Action:** Not incremented. `version.properties` was NOT read, written, or modified.

---

## 3. Files Touched and Changes Made

### New Files Created
1. `.github/workflows/release-apk.yml`
   - Manual `workflow_dispatch` workflow to build, sign, verify, and upload a release APK. Fetch atomic `versionCode` via Supabase RPC (`increment_version_code`). Updates `release-state.json` upon completion.
2. `.github/workflows/release-aab.yml`
   - Manual `workflow_dispatch` workflow to build, sign, verify, and upload a release AAB. Fetch atomic `versionCode` via Supabase RPC (`increment_version_code`). Updates `release-state.json` upon completion.
3. `release-state.json`
   - Repository root JSON state file tracking `pending_debug_reset`, `last_release_version_code`, `last_release_version_name`, `released_at`, and `released_by`. Initialized with `pending_debug_reset: false`.

### Files Modified
4. `AGENTS.md`
   - Appended `## 2a. Debug Code Reset After a Release` section between Section 2 and Section 3. Sections 1 and 2 remain byte-identical.

---

## 4. Required Explicit Confirmations
- **(a) `version.properties` Isolation:** `version.properties` was NOT read, written, or referenced anywhere in the new workflow files or created files.
- **(b) Untouched Existing Build Logic:** `build.gradle.kts` (app and root) and `.github/workflows/build-apk-debug.yml` were NOT touched.
- **(c) `AGENTS.md` Section Integrity:** Sections 1 and 2 of `AGENTS.md` are byte-identical to before this task; only Section 2a was added.

---

## 5. Prerequisites Noted
- **GitHub Repository Secrets:** Secrets `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` must be configured in GitHub repository secrets under environment `ENV_ABX_XTOOLS`.
- **Supabase RPC Function:** The Supabase `increment_version_code` RPC must exist and have (or create) a counter row for `p_app_id = "com.inscopelabs.abx.xtools"`.

---

## 6. Commands Executed & Build Results
- Executed `compile_applet` tool to verify project integrity.
- **Result:** Build succeeded - the applet is compiled.

---

## 7. Assumptions & Unverified Items
- **Assumptions:** GitHub Actions runner environment `ENV_ABX_XTOOLS` will be configured with required environment secrets.
- **Failures/Errors:** None.
