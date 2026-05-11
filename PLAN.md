# Fix Kotlin BAML “Generate And Run” Workflow

## Summary
Make the Kotlin SDK and generator self-sufficient so an app can:
1. define `.baml` files,
2. run `baml-cli generate`,
3. build and run without any app-specific library patches.

The current failure has two root causes:
- the generated Kotlin runtime is incomplete and still requires an app-local shim (`BamlCompat.kt`) to inject env vars and register types;
- the lightweight runtime path parses structured outputs too strictly, so valid model answers that are not raw JSON fail with `PrimitiveClient.parse`.

## Implementation Changes

### 1. Make generated Kotlin runtime complete on its own
Update the Kotlin generator so generated code no longer depends on `app/src/main/java/baml_client/BamlCompat.kt`.

Required behavior:
- generated `BamlRuntime` must create and use a `BamlTypeMap` and call `registerBamlTypes(...)`;
- generated `BamlRuntime.init(envVars)` must actually pass `envVars` into `com.boundaryml.baml.BamlRuntime.create(...)`;
- generated function wrappers must resolve the client through generated runtime code only, not through an app-local override.

Concrete targets:
- fix the runtime template in `~/TEJ/tej_baml_kotlin/engine/generators/languages/kotlin/src/_templates/runtime.kt.j2`;
- keep generated output shape compatible with existing `BamlFunctions.kt`, `BamlTypeMap.kt`, and `BamlRuntimeInit.kt`;
- remove the need for the app shim currently living at `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app/src/main/java/baml_client/BamlCompat.kt`.

Result:
- end developers only use generated files plus the SDK;
- the app becomes a normal consumer of generated code, not a patched integration sample.

### 2. Replace strict structured parsing in the lightweight path
Fix the lightweight runtime so structured return types use the same tolerant parsing strategy as the main runtime.

Decision:
- do not rely on `serde_json::from_str` for non-primitive outputs in `sys_llm`;
- instead, route structured parsing through shared `jsonish`-style coercion against the expected return type.

Required behavior:
- if the model returns fenced JSON, JSON with small formatting issues, or text plus recoverable structured content, parsing should still succeed when the response is semantically valid;
- primitive returns can stay on the current fast path;
- class, enum, list, map, optional, and nested outputs must use schema-guided coercion.

Concrete targets:
- replace the current non-primitive parse branch in `~/TEJ/tej_baml_kotlin/baml_language/crates/sys_llm/src/lib.rs`;
- reuse existing shared parsing machinery from `~/TEJ/tej_baml_kotlin/engine/baml-lib/jsonish` instead of maintaining a second bespoke parser;
- keep provider-response extraction in `sys_llm/src/parse_response/*` as-is; only change post-extraction typed parsing.

Result:
- the screenshot error goes away for realistic model outputs;
- Kotlin lightweight execution becomes behaviorally closer to the main BAML runtime.

### 3. Keep request rendering aligned with the full runtime
Stabilize the lightweight prompt/render path so generated BAML prompts with `{{ ctx.output_format }}` behave the same way they do elsewhere.

Required behavior:
- render with the actual function return type, not a default string type;
- include class and enum schema definitions needed by `ctx.output_format`;
- preserve multimodal prompt parts and preamble text when user messages are present;
- keep OpenRouter/OpenAI-compatible image serialization valid.

Concrete targets:
- preserve the current direction in `~/TEJ/tej_baml_kotlin/baml_language/crates/sys_llm/src/lib.rs`, `sys_types/src/lib.rs`, `bex_engine/src/lib.rs`, and `sys_llm/src/build_request/openai.rs`;
- validate that these are necessary library fixes, not app workarounds;
- if any of these are still app-driven instead of generator/runtime-driven, move them into the library.

Result:
- multimodal structured prompts remain portable across generated Kotlin apps;
- no sample-specific prompt hacks are needed.

### 4. Restore the sample app to a clean consumer
After the generator/runtime fixes exist, regenerate the app client and remove app-only integration code.

Required app cleanup:
- regenerate from `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/baml_src`;
- delete `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML/app/src/main/java/baml_client/BamlCompat.kt`;
- update the app to initialize generated `baml_client.BamlRuntime` directly with env vars and use generated wrappers only;
- keep app behavior the same: bundled fridge images, optional URL image, concurrent agent calls, BuildConfig-provided OpenRouter key.

Result:
- the sample proves the intended developer workflow instead of masking SDK gaps.

## Public API / Generated Interface Changes
These are the intended outward-facing changes:

- Generated Kotlin runtime:
  - `BamlRuntime.init(envVars: Map<String, String> = emptyMap())` becomes functional, not a no-op parameter.
- Generated Kotlin code:
  - no app-local `client` shim required.
- SDK behavior:
  - structured outputs from generated function calls become tolerant of realistic LLM formatting instead of requiring perfect raw JSON.

No app developer should need to modify the library or write custom runtime glue after generation.

## Test Plan

### Library tests
Add or update tests for:
- Kotlin generator output:
  - generated runtime includes `typeMap` registration;
  - generated runtime passes `envVars` to `BamlRuntime.create(...)`;
  - generated functions compile against generated runtime without external shim.
- Lightweight structured parsing:
  - plain JSON object response;
  - fenced JSON response;
  - prose + fenced JSON;
  - slightly malformed but recoverable JSON that `jsonish` should coerce;
  - nested class/list outputs matching the recipe planner schema.
- Multimodal request building:
  - prompt text before `_.role("user")` is preserved;
  - image payload serializes correctly for OpenAI-compatible chat completions.

### Integration checks
Run:
- Kotlin generator tests in `~/TEJ/tej_baml_kotlin/engine/generators/languages/kotlin`;
- targeted `sys_llm` and parsing tests in `~/TEJ/tej_baml_kotlin/baml_language/crates/sys_llm`;
- regenerate the app client from `baml_src`;
- Android build/install for `/Users/tejguntuku/AndroidStudioProjects/KitchenRecipeAppBAML`;
- one real fridge-photo run confirming all four agents return typed objects instead of prose parse failures.

## Assumptions
- The correct product direction is to keep the lightweight runtime and make it robust, not replace Kotlin with the full runtime stack.
- OpenRouter support should continue through `openai-generic`.
- The sample app remains a validation target, but the fixes belong in the generator/SDK unless a change is clearly demo-only.
- Streaming stays out of scope for this fix; success is non-streaming multimodal + typed outputs + concurrent calls working through generated Kotlin code alone.
