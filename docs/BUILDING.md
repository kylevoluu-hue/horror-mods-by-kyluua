# Building Verity

## Prerequisites
- **JDK 21** (Temurin/Adoptium recommended). Verify with `java -version`.
- **Internet access** the first time you build — Gradle downloads Forge, Minecraft
  and GeckoLib.
- ~3 GB free disk for the Gradle/Forge caches.

## 1. Get the Gradle wrapper
This repo ships `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.8) but not the
binary `gradle-wrapper.jar`. Generate the wrapper once:

```bash
gradle wrapper --gradle-version 8.8
```
(If you don't have a system Gradle, install it via your package manager or
SDKMAN: `sdk install gradle 8.8`.)

After this you'll have `gradlew` / `gradlew.bat` and can use them for everything.

## 2. Build the jar
```bash
./gradlew build
```
Output: `build/libs/verity-forge-1.21.1-1.0.0.jar`.

The first run is slow (it decompiles and patches Minecraft). Subsequent builds are
fast.

## 3. Run in a dev environment
```bash
./gradlew runClient   # client with Verity + GeckoLib already loaded
./gradlew runServer   # dedicated server (accept the EULA in run/eula.txt)
```

To test multiplayer locally: start `runServer`, then `runClient`, and connect to
`localhost`.

## Common issues
- **`Unsupported class file major version` / toolchain errors** → you're not on
  JDK 21. Point Gradle at a 21 JDK (`org.gradle.java.home` or `JAVA_HOME`).
- **GeckoLib not found** → confirm the Cloudsmith repo line in `build.gradle` and
  that `geckolib_version` in `gradle.properties` exists for 1.21.1.
- **Networking / API symbol not found** → make sure you're on **Forge 52.1.x**
  for **1.21.1**; the payload API (`RegisterPayloadHandlersEvent`,
  `PayloadRegistrar`) is the 1.21 version.
- **Missing sound warnings at startup** → expected. The `.ogg` files are
  placeholders; add real audio (see `ASSETS.md`) to silence them.

## Where to change versions
All versions live in `gradle.properties` (`minecraft_version`, `forge_version`,
`geckolib_version`, ranges). `build.gradle` and `mods.toml` read from there.
