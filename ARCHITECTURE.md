# Architecture and integration boundaries

Frankenstein Lab is intended to be an Android product assembled from independently sourced modules. This initial repository state contains one imported library component only; it is not yet a complete Android application.

## Current module

- `:modules:manga-reader-core` — Kototoro's platform-independent reader core. It models page/scene geometry, viewport and progress state, transitions, and a resource-window plan. It has no Android or Compose dependency in its production source set.

The Android page loader, image renderer, gestures/UI, persistence adapter, and app navigation are deliberately not represented as implemented yet. They must connect to the reader core through explicit adapters and be sourced/implemented only after the corresponding upstream code, APIs, and licenses have been inspected.

## Integration constraints

- Keep each upstream license and attribution with its actual files.
- Record exact source URL, branch/commit, copied paths, changes, and dependency licenses.
- Avoid importing whole applications when a smaller module is sufficient.
- Keep source/plugin systems separate until each native runtime is integrated and tested.
- Do not expose unsupported player controls, add-on types, or service options in the UI.
- Arabic and RTL requirements apply to every user-facing screen once the Android UI is introduced.
