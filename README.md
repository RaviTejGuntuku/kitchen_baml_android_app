# KitchenRecipeAppBAML

This repo is set up to demonstrate the end-to-end developer experience for the BAML Kotlin SDK on Android.

The current state is intentionally **pre-codegen**:

- the handwritten Android frontend is present
- the BAML source files are present in `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src`
- the generated `baml_client` code is **not** checked in
- the app-local JNI / `dylib` integration is **not** checked in

That means the app will not build until you generate the Kotlin client from the BAML source.

## Project Structure

- BAML source: `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src`
- Android app: `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app`
- Generated Kotlin client target:
  `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app/src/main/java/com/example/kitchenrecipeappbaml`

The generator configuration lives in:

- `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src/generators.baml`

It currently writes generated Kotlin into the app source tree under the app package.

## Prerequisites

You need:

1. A working `baml-cli`
2. The `baml-kotlin` SDK available to the Android app
3. An `OPENROUTER_API_KEY` in `local.properties` or your shell environment

Example `local.properties` entry:

```properties
OPENROUTER_API_KEY=your_key_here
```

## Generate The Kotlin Client

If you want plain `baml-cli generate` to work with Kotlin on this machine, first install the local engine CLI:

```bash
cd /Users/tejguntuku/TEJ/tej_baml_kotlin
cargo install --path engine/cli --bin baml-cli --force
```

Then from the project root:

```bash
cd /Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML
baml-cli generate --from ./baml_src
```

If you are developing against the local `tej_baml_kotlin` fork directly, use the newer engine CLI entrypoint:

```bash
cd /Users/tejguntuku/TEJ/tej_baml_kotlin
cargo run --manifest-path engine/cli/Cargo.toml -- generate --from /Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src
```

This command is verified to generate the Kotlin client for this app.

Note: the older CLI under `/Users/tejguntuku/TEJ/tej_baml_kotlin/baml_language` currently has `generate` disabled, so do not use that workspace as the codegen entrypoint.

After generation, you should see a regenerated `baml_client` package under:

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

## Expected Developer Workflow

The intended workflow for this repo is:

1. Edit BAML files in `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src`
2. Run BAML codegen
3. Build and run the Android app

The handwritten frontend already references the generated API surface, so after generation it should be able to call:

- `BamlRuntime.init(...)`
- `BamlFunctions.AnalyzeFridgeInventory(...)`
- `BamlFunctions.InferCookingConstraints(...)`
- `BamlFunctions.SuggestRecipePlan(...)`
- `BamlFunctions.BuildShoppingPlans(...)`

## Notes

- This repo no longer carries app-local generated code or native bridge artifacts in version control.
- The Kotlin SDK itself is consumed as a dependency; generation recreates only the app-specific `baml_client` layer.
- If `baml-cli generate` changes the schema or function signatures, the handwritten frontend may need to be updated to match the new generated client.
