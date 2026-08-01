# Standing Instructions for AI Studio Build Agent — xtools 

## 1. Mandatory Process Report on Every Task

This environment provides no way to copy, save, or download your
responses. You MUST record a report for every task you complete, saved
as an actual file in the repository (not just a chat response),
committed and pushed:

Path: agent-reports/<UTC-ISO-timestamp>-<short-task-slug>.md

The report must include:
- What was asked.
- What you actually changed (files touched, with a diff or summary).
- Any commands you ran and their results.
- Any assumptions you made.
- Any errors, partial failures, or things you were unable to verify.

Do not overwrite previous reports — each task gets its own timestamped
file. This folder must NOT be gitignored; it must be pushed to GitHub
so it can be read outside this environment.

## 2. Version Increment Rule

version.properties is exclusively controlled by the AI Studio agent. GitHub
Actions CI workflows must never write to this file under any circumstance —
this rule exists specifically so that guarantee holds; do not weaken it in
any future task.

Current format:
versionCode=1
versionName=0.0.1
debugCode=0001

Before starting any task (after reading the task but before making any file
change), assess the probability that the task will require a debug build —
score it 0-100. If that assessed score is greater than 75:

- Increment versionCode by 1 (plain integer increment).
- Increment debugCode by 1, preserving its existing zero-padded width (e.g.
  0001 -> 0002, 0099 -> 0100, 0999 -> 1000 if it grows a digit).
- Do this exactly once per task, regardless of how many files the task
  touches or how many internal steps it involves. Do not increment more
  than once even if the task is large or multi-part.
- Do not increment versionName — that stays under manual control.
- If the assessed score is 75 or below, make no change to version.properties
  at all for that task.

State the assessed probability score and the resulting action (incremented /
not incremented) explicitly in the task's mandatory agent report, per section
1 above — this is a required addition to every future agent report, not just
this one.

## 3. Mandatory Logging Standard

Every new Activity, Fragment, feature, or discrete piece of functionality
must implement adequate logging of its own process flow — entry points,
key decision branches, and completion/failure outcomes — sufficient for
someone to reconstruct what happened after the fact from the log file
alone, without needing to reproduce the issue live. Use the existing
Logger facade (com.inscopelabs.abx.xtools.diagnostics.Logger — d/i/w/e)
exactly as it's already used throughout the codebase. Logger is safe to
call from any file regardless of build variant — it resolves to a real
implementation in debug builds and a true no-op in release builds
automatically, so new code never needs to guard calls to it or worry about
whether it's "allowed" to log; just call it the same way existing code
already does.

If a task requires reading, reviewing, or writing to an EXISTING file that
does not already implement adequate logging per the standard above, flag
that file explicitly and prominently in the task's mandatory agent report
(per section 1) — a dedicated, clearly-labeled line or subsection such as
"LOGGING GAP FLAGGED: <file path> — <one-line reason>", not buried in
general notes. This applies whether or not the file was otherwise in scope
for the task's actual changes — flagging a logging gap does not require
fixing it in the same task unless the task's own scope already covers that
file's logic.

