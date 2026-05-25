# KitchenRecipeAppBAML

This repo is the Android demo app for the Kotlin BAML SDK.

The demo flow is:

1. clone this app
2. clone `RaviTejGuntuku/tej_baml_kotlin`
3. build that fork’s CLI and put it on `PATH`
4. generate `baml_client`
5. build and install the app
6. run the app with preset images or an uploaded fridge photo

## What This Repo Contains

- handwritten Android UI and ViewModel code
- checked-in `baml_src`
- no checked-in generated `baml_client`
- no app-local JNI C/CMake glue

The app expects the runtime to come from the published Maven artifact:

- `io.github.ravitejguntuku:baml-kotlin:0.1.0`

## How The Rust Runtime Gets Into The App

The Android app does not manually vendor the Rust runtime.

Instead:

1. the app depends on the published Maven artifact:
   - `io.github.ravitejguntuku:baml-kotlin:0.1.0`
2. that artifact contains the Android native runtime libraries:
   - `native/android-arm64/libbridge_cffi.so`
   - `native/android-x86_64/libbridge_cffi.so`
3. during the Gradle build, the app extracts those `.so` files from the SDK artifact
4. Gradle packages the correct ABI-specific `.so` into the APK
5. at runtime, the Kotlin SDK loads the native library with:

```kotlin
System.loadLibrary("bridge_cffi")
```

So from the app consumer’s point of view, the Rust runtime comes from Maven Central.

## SDK Repo

The Kotlin implementation used by this demo lives in this fork:

- [RaviTejGuntuku/tej_baml_kotlin](https://github.com/RaviTejGuntuku/tej_baml_kotlin)

The important locations inside that fork are:

- codegen CLI:
  - `engine/cli`
- Kotlin SDK:
  - `engine/language_client_kotlin`
- native Rust runtime that Android loads:
  - `baml_language/crates/bridge_cffi`

So for this app:

- `engine/cli` is the part that generates `baml_client`
- `engine/language_client_kotlin` is the SDK the app depends on from Maven
- `baml_language/crates/bridge_cffi` is the Rust runtime that becomes `libbridge_cffi.so`

The Kotlin SDK install guide lives in:

- `engine/language_client_kotlin/README.md`

## Demo Prerequisites

You need:

1. Android Studio and an Android emulator
2. Java 21
3. Rust and Cargo
4. an OpenRouter API key

Put this in `local.properties` at the root of this app repo:

```properties
OPENROUTER_API_KEY=your_key_here
```

## Fresh Demo Setup

### 1. Clone the app

```bash
git clone https://github.com/RaviTejGuntuku/kitchen_baml_android_app.git
cd kitchen_baml_android_app
```

### 2. Clone the Kotlin SDK fork

Clone the fork next to the app repo:

```bash
cd ..
git clone https://github.com/RaviTejGuntuku/tej_baml_kotlin.git
cd kitchen_baml_android_app
```

### 3. Build the fork’s CLI and set `baml-cli` to that binary

```bash
cd ../tej_baml_kotlin
cargo build --manifest-path ./engine/cli/Cargo.toml
export PATH="$(pwd)/engine/target/debug:$PATH"
which baml-cli
baml-cli --version
cd ../kitchen_baml_android_app
```

This ensures `baml-cli` resolves to the CLI built from your fork’s Kotlin codegen implementation in `engine/cli`.

### 4. Generate Kotlin code from BAML

Generate code:

```bash
baml-cli generate --from ./baml_src
```

### 5. Build and install the app

```bash
./gradlew clean :app:installDebug
```

### 6. Launch the app

Open `Recipe Planner` on the emulator.

## Regenerate From Scratch

If you want to delete and regenerate the client:

```bash
rm -rf ./app/src/main/java/com/example/kitchenrecipeappbaml/baml_client
baml-cli generate --from ./baml_src
./gradlew clean :app:installDebug
```

If you have not built the CLI binary yet, build the fork and set `PATH` first:

```bash
cd ../tej_baml_kotlin
cargo build --manifest-path ./engine/cli/Cargo.toml
export PATH="$(pwd)/engine/target/debug:$PATH"
cd ../kitchen_baml_android_app
baml-cli generate --from ./baml_src
```

## Demo Script

This is the cleanest live demo:

1. Show `baml_src`.
2. Delete `app/src/main/java/com/example/kitchenrecipeappbaml/baml_client`.
3. Set `PATH` so `baml-cli` points at the fork binary.
4. Run Kotlin codegen.
5. Run `./gradlew clean :app:installDebug`.
6. Open the app on the emulator.
7. Run with a preset fridge image.
8. Run again with an uploaded fridge image.

## Uploading Images To The Emulator

The app supports both:

- preset fridge gallery images
- uploaded images from the emulator’s storage

### Fastest option

Drag a `.jpg` or `.png` directly into the emulator window.

### `adb` option

```bash
adb push /absolute/path/to/fridge.jpg /sdcard/Download/
```

Then in the app:

1. tap `Upload Image`
2. pick the image from `Downloads` or the system photo picker

## Notes On The Current State

This demo is intentionally based on your fork for codegen.

That means:

- runtime linking should come from Maven Central
- Kotlin codegen should come from the `baml-cli` binary built from `RaviTejGuntuku/tej_baml_kotlin`
- the native runtime the app loads comes from the same fork’s `baml_language/crates/bridge_cffi`, packaged into the published SDK artifact

So the intended setup is:

- clone the fork
- build the CLI once
- export that binary onto `PATH`
- run `baml-cli generate`

rather than:

```bash
brew install baml-cli
```

## Architecture

If you want the runtime/FFI/Gradle architecture details, see:

- [`/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/ARCHITECTURE.md`](/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/ARCHITECTURE.md)
