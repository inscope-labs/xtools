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
