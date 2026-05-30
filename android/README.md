# Weigh to Go! — Android (preserved)

The original *Weigh to Go!* native Android application (Java), built for CS 360 at
Southern New Hampshire University. It is preserved in this monorepo in
**maintenance-only** status — still buildable, with its full unit-test suite
passing, and receiving no new features. Active development targets the web
rebuild under [`../web/`](../web/).

## Stack

- Java 21 · Android SDK 34 (Android 14) · min SDK 26 (Android 8.0)
- Gradle 8.2+ · Material Components · SQLite

## Build & test

Open the **`android/`** directory (not the repository root) in Android Studio
(Ladybug 2024.2.1+), with JDK 21 and Android SDK 34 installed.

```bash
./gradlew build   # build
./gradlew test    # unit tests
./gradlew lint    # lint
```

The debug APK is written to
`weightogo/build/outputs/apk/debug/weightogo-debug.apk`. Android Studio generates
`local.properties` (the SDK location) automatically; for command-line builds, set
`ANDROID_HOME` or create the file manually.

## Documentation

- Database schema, ER diagrams, and DAO patterns:
  [`../docs/architecture/WeighToGo_Database_Architecture.md`](../docs/architecture/WeighToGo_Database_Architecture.md)
- UI/UX design specifications: [`../docs/design/`](../docs/design/)
- Android-era decisions: [ADR index](../docs/adr/README.md) (0001–0006) and
  [DDR index](../docs/ddr/README.md) (0001–0002)
- Manual test plans and setup guides: [`../docs/testing/`](../docs/testing/)
- Project overview: [root README](../README.md)
