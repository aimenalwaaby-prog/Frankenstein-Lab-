# THIRD-PARTY NOTICES

## Kototoro — manga reader core

- Upstream repository: https://github.com/Kototoro-app/Kototoro
- Source module: `reader-core/`
- Upstream branch: `devel`; commit: `6410cdd1e1a92031f429c46cec39b5d2ea041fa2`
- License: Apache License 2.0; upstream license text is preserved in [`LICENSE`](LICENSE).
- Imported paths: `reader-core/src/commonMain/`, `reader-core/src/jvmTest/`, and module build file in `modules/manga-reader-core/`; upstream namespace and notices are retained.
- Changes to imported production source: none. The Android UI/adapter is separate project code.
- Scope: reader geometry, scene/progress and resource-window models; this is not the complete upstream reader UI.

## Grimoire — EPUB parser

- Upstream repository: https://github.com/Operation-Grimoire/grimoire
- Upstream branch: `master`; commit: `8016041da36d8db3a6935268aa144d798cf204c0`
- License: Apache License 2.0; upstream text is preserved in [`licenses/Grimoire-LICENSE`](licenses/Grimoire-LICENSE).
- Imported file: `app/src/main/java/io/grimoire/app/data/epub/EpubParser.kt`, stored at `modules/novel-epub/src/jvmMain/kotlin/io/grimoire/app/data/epub/EpubParser.kt` with original namespace.
- Changes to imported parser: none. Local parser tests are separate.
- Direct Jsoup dependency: `1.17.2`, pinned from the Grimoire extensions API repository version; Jsoup is distributed under MIT License. Verify the matching license notice when resolving distribution artifacts.

## Direct application dependencies

The application declares AndroidX Compose/Activity/Core/DocumentFile/Lifecycle, AndroidX Media3 ExoPlayer/UI `1.9.3`, and Kotlin Coroutines Android. These projects publish under Apache License 2.0; retain their license/attribution files in any binary distribution. Test dependencies include JUnit 5 and Kotest. This list is based on declared direct dependencies; Gradle dependency resolution has not been run, so it is not a complete transitive SBOM or binary notice bundle.

## Candidate repositories audited but not copied as application code

- Kotori: https://github.com/tailolicon/kotori — Apache-2.0. No source module copied.
- Nekoyomi: https://github.com/Seguaz/Nekoyomi — Apache-2.0. No source module copied.
- Mihon: https://github.com/mihonapp/mihon — Apache-2.0. No source module copied.
- Aniyomi: https://github.com/aniyomiorg/aniyomi — Apache-2.0. No source module copied.
- CloudStream, OpenStream, Material Files, and mpv-android were reviewed as candidates; no source code from these repositories was copied into this version. Use each upstream's current LICENSE and dependency notices before any later integration.
- Zangetsu was explicitly excluded per user instruction; Club Stream was not guessed because its repository is unspecified.

Apache-2.0 redistribution conditions apply to the imported source modules. Preserve licenses and attribution, mark future changes to imported files, and do not imply upstream endorsement. No upstream marks, APKs, books, images, video, or source catalog payloads are included.

Before binary distribution, resolve all dependencies, collect their exact LICENSE/NOTICE texts and versions, generate an SBOM, and check compatibility for the selected distribution model.
