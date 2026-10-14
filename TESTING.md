# Testing

The repository contains 32 upstream JVM test/fixture files for `manga-reader-core`, plus local tests for the Grimoire EPUB parser. No Gradle build or test command has been executed yet. The current sandbox has JDK 21 but no Android SDK; dependency resolution has intentionally not been run.

When a network-enabled JDK 21 environment is available:

```bash
./gradlew :modules:manga-reader-core:jvmTest
./gradlew :modules:novel-epub:jvmTest
```

The EPUB parser tests create a small temporary EPUB archive in memory-backed test setup; no external book file is required. A passing result must be captured from actual command output before marking either module as verified. Android instrumentation tests and APK builds are outside the current requested scope.
