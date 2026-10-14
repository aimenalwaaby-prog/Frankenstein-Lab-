# Testing

The imported Kototoro module contains its upstream `jvmTest` source set (32 test/fixture files). This repository has not executed those tests yet. No Android SDK is installed in the current sandbox and no Gradle dependency resolution was performed.

When a network-enabled JDK 21 environment is available:

```bash
./gradlew :modules:manga-reader-core:jvmTest
./gradlew :modules:manga-reader-core:compileKotlinJvm
```

A passing result must be captured from the actual command before marking the module as verified here. Android instrumentation tests and APK builds are outside the current requested scope.
