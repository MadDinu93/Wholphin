---
name: thorphin-review
description: Use after implementing a feature, before committing changes, or when asked to review Thorphin code for bugs, regressions, test gaps, and upstream compatibility risks.
---

# Thorphin review

Read-only by default. Do not edit source during a review unless the user explicitly asks to fix the findings.

1. **Get the diff.** `git status`, then `git diff` (and `git diff --staged`, or `git diff <base>...HEAD` if the user names a branch/range). Read the surrounding code of each hunk, not only the changed lines. Read untracked files only when they are relevant to the requested change (e.g. new source files or docs the change added); do not inspect unrelated untracked directories or archives.
2. **Hunt for real problems, in this order:** crashes and incorrect behavior, regressions in neighboring features, security (token leakage, see the OkHttp rule in `CLAUDE.md`), lifecycle and coroutine problems (leaks, wrong scope or dispatcher, main-thread blocking, unreleased players), Android TV focus regressions, data-compat breaks, missing tests for new logic.
3. **Check Thorphin constraints that apply:** the "Essential constraints" in `CLAUDE.md`; proto/Room numbering and package/applicationId (details in `docs/ai/known-issues.md` and `fork-customizations.md`); new strings in `strings.xml` and `values-ro`; `// Thorphin:` comments on non-obvious edits in upstream files. Read only the `docs/ai/` file for the touched area. If the diff touches upstream-sensitive files, Proto DataStore definitions, or Room schema/version, also consult the `thorphin-upstream-safety` skill and apply its checks.
4. **Scope check.** Flag unnecessary dependencies, broad refactors, reformatting noise, and unrelated changes. Flag edits to upstream files that could have been a new file.
5. **Docs check.** If behavior, architecture or constraints described in `docs/ai/` changed, the owning doc must change in the same diff; flag it if not.
6. **Validate when practical.** Run the narrow check from `docs/ai/build-and-development.md` (targeted unit tests, compile, `pre-commit run` on changed files). State exactly what ran and the result; state what was skipped and why. Never imply a check ran if it did not.
7. **Report findings ordered by severity** (high, medium, low), each with `file:line`, what is wrong, and the concrete scenario that triggers it. Separate **Confirmed defects** (you traced the code path or reproduced it) from **Potential risks** (plausible but unverified, say what would confirm it). Drop speculative or style-only nitpicks; ktlint covers formatting. If nothing is wrong, say so plainly, with what you checked.
