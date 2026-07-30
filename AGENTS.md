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
