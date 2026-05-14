# KitchenRecipeAppBAML

This repo is the Android demo app for the Kotlin BAML SDK.

The demo flow is:

1. clone this app
2. get the Kotlin-capable CLI from [`RaviTejGuntuku/tej_baml_kotlin`](https://github.com/RaviTejGuntuku/tej_baml_kotlin)
3. generate `baml_client`
4. build and install the app
5. run the app with preset images or an uploaded fridge photo

## What This Repo Contains

- handwritten Android UI and ViewModel code
- checked-in `baml_src`
- no checked-in generated `baml_client`
- no app-local JNI C/CMake glue

The app expects the runtime to come from the published Maven artifact:

- `io.github.ravitejguntuku:baml-kotlin:0.1.0`

## SDK Repo

The Kotlin-capable CLI and SDK source live here:

- [RaviTejGuntuku/tej_baml_kotlin](https://github.com/RaviTejGuntuku/tej_baml_kotlin)

The Kotlin SDK install guide now lives in that repo’s Kotlin SDK README.

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

Clone the public fork next to the app repo:

```bash
cd ..
git clone https://github.com/RaviTejGuntuku/tej_baml_kotlin.git
cd kitchen_baml_android_app
```

### 3. Generate Kotlin code from BAML

For one-off generation:

```bash
cd ../tej_baml_kotlin
cargo run --manifest-path ./engine/cli/Cargo.toml -- generate --from /absolute/path/to/kitchen_baml_android_app/baml_src
cd ../kitchen_baml_android_app
```

For repeat runs, build the CLI once and reuse the binary:

```bash
cd ../tej_baml_kotlin
cargo build --manifest-path ./engine/cli/Cargo.toml
./engine/target/debug/baml-cli generate --from /absolute/path/to/kitchen_baml_android_app/baml_src
cd ../kitchen_baml_android_app
```

### 4. Build and install the app

```bash
./gradlew clean :app:installDebug
```

### 5. Launch the app

Open `Recipe Planner` on the emulator.

## Regenerate From Scratch

If you want to delete and regenerate the client:

```bash
rm -rf ./app/src/main/java/com/example/kitchenrecipeappbaml/baml_client
cd ../tej_baml_kotlin
./engine/target/debug/baml-cli generate --from /absolute/path/to/kitchen_baml_android_app/baml_src
cd ../kitchen_baml_android_app
./gradlew clean :app:installDebug
```

If you have not built the CLI binary yet, replace the generate step with:

```bash
cargo run --manifest-path ./engine/cli/Cargo.toml -- generate --from /absolute/path/to/kitchen_baml_android_app/baml_src
```

## Demo Script

This is the cleanest live demo:

1. Show `baml_src`.
2. Delete `app/src/main/java/com/example/kitchenrecipeappbaml/baml_client`.
3. Run Kotlin codegen from the public fork.
4. Run `./gradlew clean :app:installDebug`.
5. Open the app on the emulator.
6. Run with a preset fridge image.
7. Run again with an uploaded fridge image.

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

This demo is intentionally based on the public fork for codegen.

That means:

- runtime linking should come from Maven Central
- Kotlin codegen currently comes from your public fork source

So the demo is reproducible on another laptop, but the CLI install story is still:

- clone public fork
- run or build the CLI locally

rather than:

```bash
brew install baml-cli
```

## Architecture

If you want the runtime/FFI/Gradle architecture details, see:

- [`/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/ARCHITECTURE.md`](/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/ARCHITECTURE.md)
