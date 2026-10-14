# THIRD-PARTY NOTICES

## Kototoro — manga reader core

- Upstream repository: https://github.com/Kototoro-app/Kototoro
- Source module: `reader-core/`
- Upstream branch: `devel`
- Upstream commit used: `6410cdd1e1a92031f429c46cec39b5d2ea041fa2`
- License: Apache License 2.0; the upstream license text is preserved in [`LICENSE`](LICENSE).
- Imported paths: `reader-core/src/commonMain/`, `reader-core/src/jvmTest/`, and the module `build.gradle` (stored under `modules/manga-reader-core/`). The original package namespace and upstream attribution in source files are retained.
- Changes made to imported source: none in this initial integration.
- Scope: pure Kotlin reader geometry, scene/viewport/progress models, transition semantics, and resource-window planning. It is a library component, not a complete interactive manga-reader screen.

Apache-2.0 redistribution conditions apply. Any future changes to copied files must be marked clearly, and the applicable copyright, license, attribution, and NOTICE materials must be retained. This project is independent and is not affiliated with or endorsed by Kototoro. No upstream marks or logos are included.

## Dependency status

No third-party binaries or Android/media assets have been bundled in this initial integration. The module's Gradle test configuration declares Kotest and JUnit dependencies; their licenses and exact resolved versions must be recorded after dependency resolution, which has not been run.
