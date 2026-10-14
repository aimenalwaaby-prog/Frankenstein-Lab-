# THIRD-PARTY NOTICES

## Kototoro — manga reader core

- Upstream repository: https://github.com/Kototoro-app/Kototoro
- Source module: `reader-core/`
- Upstream branch: `devel`; commit: `6410cdd1e1a92031f429c46cec39b5d2ea041fa2`
- License: Apache License 2.0; the upstream license text is preserved in [`LICENSE`](LICENSE).
- Imported paths: `reader-core/src/commonMain/`, `reader-core/src/jvmTest/`, and the module `build.gradle` (stored under `modules/manga-reader-core/`). The original package namespace and upstream attribution in source files are retained.
- Changes made to imported source: none in this integration.
- Scope: pure Kotlin reader geometry, scene/viewport/progress models, transition semantics, and resource-window planning. It is a library component, not a complete interactive manga-reader screen.

## Grimoire — EPUB parser

- Upstream repository: https://github.com/Operation-Grimoire/grimoire
- Upstream branch: `master`; commit: `8016041da36d8db3a6935268aa144d798cf204c0`
- License: Apache License 2.0; the upstream license text is preserved in [`licenses/Grimoire-LICENSE`](licenses/Grimoire-LICENSE).
- Imported path: `app/src/main/java/io/grimoire/app/data/epub/EpubParser.kt`, retained at `modules/novel-epub/src/jvmMain/kotlin/io/grimoire/app/data/epub/EpubParser.kt` with its original namespace.
- Changes to imported parser code: none. Separate local parser tests were added under the module's `jvmTest` source set.
- The standalone parser's direct dependency on Jsoup is pinned to `1.17.2`, the version declared by the Grimoire extensions API repository at `https://github.com/Operation-Grimoire/grimoire-extensions-api` (commit `dd6145178e8d421e2f7c76991d3c66580c858a72`).

Apache-2.0 redistribution conditions apply to both source components. Keep the applicable license and attribution materials, mark any future changed files, and do not imply affiliation or endorsement by Kototoro or Grimoire. No upstream marks, APKs, books, images, video, or source catalog payloads are included.

## Dependency status

No third-party binaries have been bundled. Gradle declares Jsoup, Kotest, and JUnit for the source modules' build/tests. Dependency resolution has not been run, so the exact transitive dependency graph and notices for resolved artifacts remain to be recorded before binary distribution.
