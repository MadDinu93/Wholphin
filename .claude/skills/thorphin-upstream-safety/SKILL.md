---
name: thorphin-upstream-safety
description: Use when modifying code that may overlap with Wholphin upstream, preparing upstream merges, resolving conflicts, or reviewing fork-specific changes for compatibility risks.
---

# Thorphin upstream safety

Thorphin is a fork of Wholphin. The goal is to keep future upstream merges cheap. Project rules are in root `CLAUDE.md`; merge hazards are in `docs/ai/known-issues.md`.

1. **Read first:** `docs/ai/fork-customizations.md` (baseline commit, fork areas) and `docs/ai/known-issues.md` (merge hazards). Add `architecture.md` or `build-and-development.md` when the change touches DI, persistence or build files.
2. **Inspect real Git state** before editing: `git status`, `git remote -v` (`upstream` = damontecres/Wholphin, `thorphin` = the fork repo), `git branch -vv`, and the diffs of the files involved.
3. **Classify the code.** Use `git diff <baseline> HEAD -- <file>`, `git log --follow <file>` and `git blame`; also `git diff HEAD upstream/main -- <file>` if the `upstream` ref exists locally. Do not assume code is unchanged upstream because it looks familiar; the baseline in the doc may be stale. Report how stale the local `upstream/main` is (e.g. `git log -1 --format=%cd upstream/main`, and how far it is ahead of the baseline). Ask for permission before running `git fetch`; it only updates remote-tracking refs, not the working tree, but it contacts the network.
4. **Minimize overlap.** Keep upstream files close to upstream; put larger fork features in new files with a small hook in the upstream file. Mark non-obvious fork edits in upstream files with `// Thorphin:` and a reason.
5. **Hard constraints (do not change as part of routine work):**
   - Kotlin package/namespace `com.github.damontecres.wholphin`; no package renames.
   - `applicationId` `io.github.maddinu93.thorphin` and the existing debug variant config (`.debug` suffix, "Thorphin (Debug)").
   - Proto DataStore enum values and field numbers; Room schema/version numbering and migrations. In routine work, never change the meaning of a number that existing installs already use (no reuse, no renumbering), and check upstream's current numbers before adding one. Collisions caused by upstream changes are a separate case: resolve them as `docs/ai/known-issues.md` describes (move the fork's side to a new number, or to a later Room version with a migration), never upstream's, and get the user's approval for the plan first.
   - Jellyfin tokens never go to third-party hosts: `@StandardOkHttpClient` for non-server URLs, `@AuthOkHttpClient` only for the Jellyfin server.
6. **Git safety.** Never run merge, rebase, reset, `checkout`/`restore` that discards changes, `clean`, force-push, stash drop or branch deletion without explicit user authorization for that action. For a merge preview use read-only commands (`git merge-tree`, `git diff`, `git log A..B`). Do not touch unrelated untracked files.
7. **Report before concluding:** compatibility risks found (files, why), anything that will likely conflict on the next upstream merge, and the validation needed (build, unit tests, a Room/proto migration check, manual test on a device or server) and what was actually run.
