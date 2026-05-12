# KitchenRecipeAppBAML

This repo is set up to demonstrate the Kotlin SDK developer flow for BAML on Android.

The repo is intentionally in a mostly pre-codegen state:

- the handwritten Android frontend is checked in
- the BAML source files are checked in
- the generated `baml_client` code is not checked in
- the app-local JNI shim is checked in
- the Rust `libbridge_cffi.so` binaries are not checked in

That means the closest working flow today is:

1. `baml-cli generate`
2. build app
3. run app

There is still one Android-specific caveat: the app builds a tiny JNI shim (`libbaml_jni.so`) locally, because the SDK does not yet ship a fully Android-ready AAR containing both native pieces.

## Project Structure

- BAML source:
  `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src`
- Android app:
  `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app`
- Generated Kotlin client target:
  `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app/src/main/java/com/example/kitchenrecipeappbaml/baml_client`

The generator configuration lives in:

- `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src/generators.baml`

## Prerequisites

You need:

1. A Kotlin-capable `baml-cli`
2. The `baml-kotlin` SDK published to `mavenLocal()`
3. An `OPENROUTER_API_KEY` in `local.properties` or your shell environment
4. Android SDK / NDK installed so Gradle can compile the JNI shim

Example `local.properties` entry:

```properties
OPENROUTER_API_KEY=your_key_here
```

## One-Time Local SDK Setup

Install the local Kotlin-capable CLI:

```bash
cd /Users/tejguntuku/TEJ/tej_baml_kotlin
cargo install --path engine/cli --bin baml-cli --force
```

Publish the Kotlin SDK to local Maven:

```bash
cd /Users/tejguntuku/TEJ/tej_baml_kotlin/engine/language_client_kotlin
./gradlew publishToMavenLocal
```

Notes:

- Do not use the older CLI under `/Users/tejguntuku/TEJ/tej_baml_kotlin/baml_language`; its `generate` subcommand is currently disabled.
- The Android app depends on `io.github.ravitejguntuku:baml-kotlin:0.1.0`, so `publishToMavenLocal` must happen before the app build.

## Generate The Kotlin Client

From the app repo:

```bash
cd /Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML
baml-cli generate --from ./baml_src
```

If you want to run directly from the local fork instead of the installed binary:

```bash
cd /Users/tejguntuku/TEJ/tej_baml_kotlin
cargo run --manifest-path engine/cli/Cargo.toml -- generate --from /Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src
```

After generation, you should see:

```text
/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app/src/main/java/com/example/kitchenrecipeappbaml/baml_client
```

## Build And Run The App

Once codegen is complete:

```bash
cd /Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Then launch the app from Android Studio or the emulator.

## What Gradle Does For You

The app is now closer to `generate -> build -> run` than before:

- Gradle extracts `libbridge_cffi.so` from the `baml-kotlin` SDK jar automatically
- the extracted files are placed under:
  `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app/build/generated/baml-jniLibs`
- CMake compiles the tiny app-local JNI shim (`baml_jni.c`) into `libbaml_jni.so`
- the APK packages both:
  - `libbridge_cffi.so`
  - `libbaml_jni.so`

So you no longer need to manually copy Rust `.so` files into `app/src/main/jniLibs`.

## Expected Developer Workflow

The intended workflow for this repo is:

1. Edit BAML files in `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src`
2. Run `baml-cli generate --from ./baml_src`
3. Run `./gradlew :app:installDebug`
4. Launch the app

The handwritten frontend already references the generated API surface, so after generation it should be able to call:

- `BamlRuntime.init(...)`
- `BamlFunctions.AnalyzeFridgeInventory(...)`
- `BamlFunctions.InferCookingConstraints(...)`
- `BamlFunctions.SuggestRecipePlan(...)`
- `BamlFunctions.BuildShoppingPlans(...)`

## Current Limitation

The last remaining non-ideal part of the Android experience is that this repo still carries:

- `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app/src/main/cpp/CMakeLists.txt`
- `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app/src/main/cpp/baml_jni.c`

That is because the current `baml-kotlin` publication is a JVM jar with bundled `libbridge_cffi.so` resources, not a full Android AAR that already exposes JNI entrypoints.

So the current reality is:

- `baml-cli generate`
- build app
- run app

with no manual `.so` copying, but not yet with zero Android-native build glue.
