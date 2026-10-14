# Architecture and integration boundaries

Frankenstein Lab is intended to be an Android product assembled from independently sourced modules. The repository currently contains two imported library components and is not yet a complete Android application.

## Current modules

- `:modules:manga-reader-core` — Kototoro's platform-independent reader core. It models page/scene geometry, viewport and progress state, transitions, and a resource-window plan. Its production source has no Android or Compose dependency.
- `:modules:novel-epub` — Grimoire's EPUB parser, extracted as a JVM module with an explicit Jsoup dependency. It parses book metadata, navigation, chapter text, and embedded image bytes. The surrounding Grimoire importer, database, UI, and TTS have not been integrated.

The Android page loader, image renderer, gestures/UI, persistence adapters, and app navigation are not represented as implemented. Future Android components must connect to these libraries through explicit adapters after the corresponding upstream code, APIs, dependencies, and licenses have been inspected.

## Integration constraints

Keep each upstream license and attribution with its actual files. Record exact source URL, branch/commit, copied paths, changes, and dependency licenses. Avoid importing whole applications when a smaller module is sufficient. Keep source/plugin systems separate until each native runtime is integrated and tested. Do not expose unsupported player controls, add-on types, or service options in the UI. Arabic and RTL requirements apply to every user-facing screen once the Android UI is introduced.
