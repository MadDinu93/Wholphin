---
name: thorphin-feature
description: Use when implementing, extending, or refactoring a Thorphin feature, including Android TV UI, navigation, playback, Jellyfin integration, background tasks, or persistence.
---

# Thorphin feature workflow

Root `CLAUDE.md` holds the project rules and the doc routing table. This skill only adds the order of work.

1. **Clarify the behavior.** Restate what the user should see or what should happen. Ask only if the answer changes the design and cannot be inferred.
2. **Read narrowly.** Pick the matching row in the `CLAUDE.md` routing table and read only that doc under `docs/ai/`. Then open the actual source files it points to; the code wins over the docs if they disagree (note the mismatch for step 8).
3. **Reuse existing patterns.** Find a similar feature (screen, ViewModel, service, preference, Room entity) and follow it. Add no new abstraction or dependency unless the existing ones cannot do the job. For a new app setting follow [DEVELOPMENT.md](../../../DEVELOPMENT.md).
4. **Upstream or fork?** Check whether the files you will touch are fork-only or upstream Wholphin (`docs/ai/fork-customizations.md`, then `git log`/`git diff 0aab8398 HEAD -- <file>` if unclear). If an upstream file or a compatibility-sensitive area (proto, Room, application ID, OkHttp clients) is involved, also apply the `thorphin-upstream-safety` skill. Prefer a new file plus a small hook.
5. **Plan the smallest coherent change.** State the files you will touch before editing. Keep unrelated cleanup out.
6. **Apply only the relevant Android checks:**
   - UI/TV: D-pad focus order, initial focus, focus restore on back navigation, no focus traps; Navigation 3 routes.
   - Lifecycle: collect flows lifecycle-aware, release players/listeners, no work tied to a destroyed screen.
   - Coroutines: right dispatcher and scope (`viewModelScope`, injected scopes), cancellation, no blocking calls on main.
   - DI: Hilt module/qualifier fit; the OkHttp client rule from `CLAUDE.md`.
   - Errors: network and server failures degrade gracefully, user-facing strings go in `strings.xml` (and `values-ro`).
7. **Implement and validate.** Run the narrowest useful check from `docs/ai/build-and-development.md` (targeted unit tests, then `./gradlew assembleDefaultDebug` if the change compiles into UI/DI; `pre-commit run` for formatting). Add or adjust a unit test where logic is testable. If a check cannot run (no SDK, no device, no server), say so.
8. **Update docs.** If documented behavior, architecture or constraints changed, update the one owning doc under `docs/ai/` (and the table in `fork-customizations.md` for new fork features). Remove statements that became false.
9. **Report:** files changed, validation actually run and its result, unresolved issues or follow-ups. Do not commit unless asked.
