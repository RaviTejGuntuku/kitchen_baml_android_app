package com.example.kitchenrecipeappbaml.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import baml_client.BamlFunctions
import baml_client.BamlRuntime
import baml_client.types.CookingConstraints
import baml_client.types.InventoryAnalysis
import baml_client.types.RecipePlan
import baml_client.types.ShoppingPlanDeck
import com.boundaryml.baml.BamlImage
import com.boundaryml.baml.BamlException
import com.boundaryml.baml.CallOptions
import com.example.kitchenrecipeappbaml.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

enum class ImageInputMode {
    Preset,
    Uploaded,
}

enum class AgentPhase {
    Idle,
    Running,
    Success,
    Error,
}

data class AgentCardState(
    val title: String,
    val phase: AgentPhase = AgentPhase.Idle,
    val summary: String = "Waiting for input",
    val durationMs: Long? = null,
)

data class RecipePlannerUiState(
    val goalText: String = sampleFridges.first().starterGoal,
    val pantryText: String = "",
    val selectedSampleId: String = sampleFridges.first().id,
    val uploadedImageUri: Uri? = null,
    val activeImageMode: ImageInputMode = ImageInputMode.Preset,
    val selectedRestrictions: Set<String> = sampleFridges.first().defaultRestrictions,
    val isRunning: Boolean = false,
    val errorMessage: String? = null,
    val inventory: InventoryAnalysis? = null,
    val constraints: CookingConstraints? = null,
    val recipePlan: RecipePlan? = null,
    val selectedRecipeIndex: Int? = null,
    val shoppingPlanDeck: ShoppingPlanDeck? = null,
    val ingredientAgent: AgentCardState = AgentCardState("Ingredient Agent"),
    val constraintAgent: AgentCardState = AgentCardState("Constraint Agent"),
    val recipeAgent: AgentCardState = AgentCardState("Recipe Agent"),
    val shoppingAgent: AgentCardState = AgentCardState("Shopping Agent"),
) {
    val selectedSample: FridgeSample
        get() = sampleFridgesById.getValue(selectedSampleId)

    val activeImageModel: Any
        get() = if (activeImageMode == ImageInputMode.Uploaded && uploadedImageUri != null) {
            uploadedImageUri
        } else {
            selectedSample.imageResId
        }
}

class RecipePlannerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "RecipePlanner"
    }

    private var analysisJob: Job? = null
    private val sampleImageCache = mutableMapOf<String, BamlImage>()
    private val uploadedImageCache = mutableMapOf<String, BamlImage>()

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(RecipePlannerUiState())
    val state: kotlinx.coroutines.flow.StateFlow<RecipePlannerUiState> = _state

    fun updateGoal(value: String) {
        _state.value = _state.value.copy(goalText = value)
    }

    fun updatePantry(value: String) {
        _state.value = _state.value.copy(pantryText = value, errorMessage = null)
    }

    fun selectSample(sampleId: String) {
        val sample = sampleFridgesById.getValue(sampleId)
        _state.value = _state.value.copy(
            selectedSampleId = sampleId,
            activeImageMode = ImageInputMode.Preset,
            goalText = sample.starterGoal,
            pantryText = "",
            selectedRestrictions = sample.defaultRestrictions,
            errorMessage = null,
        )
    }

    fun setUploadedImage(uri: Uri?) {
        _state.value = _state.value.copy(
            uploadedImageUri = uri,
            activeImageMode = if (uri != null) ImageInputMode.Uploaded else ImageInputMode.Preset,
            errorMessage = null,
        )
    }

    fun usePresetImage() {
        _state.value = _state.value.copy(
            activeImageMode = ImageInputMode.Preset,
            errorMessage = null,
        )
    }

    fun useUploadedImage() {
        if (_state.value.uploadedImageUri == null) return
        _state.value = _state.value.copy(
            activeImageMode = ImageInputMode.Uploaded,
            errorMessage = null,
        )
    }

    fun clearUploadedImage() {
        val currentUri = _state.value.uploadedImageUri?.toString()
        if (currentUri != null) {
            uploadedImageCache.remove(currentUri)
        }
        _state.value = _state.value.copy(
            uploadedImageUri = null,
            activeImageMode = ImageInputMode.Preset,
            errorMessage = null,
        )
    }

    fun toggleRestriction(tag: String) {
        val current = _state.value.selectedRestrictions
        _state.value = _state.value.copy(
            selectedRestrictions = if (tag in current) current - tag else current + tag,
        )
    }

    fun resetInputs() {
        val sample = _state.value.selectedSample
        analysisJob?.cancel()
        _state.value = _state.value.copy(
            goalText = sample.starterGoal,
            pantryText = "",
            selectedRestrictions = sample.defaultRestrictions,
            errorMessage = null,
            inventory = null,
            constraints = null,
            recipePlan = null,
            selectedRecipeIndex = null,
            shoppingPlanDeck = null,
            ingredientAgent = AgentCardState("Ingredient Agent"),
            constraintAgent = AgentCardState("Constraint Agent"),
            recipeAgent = AgentCardState("Recipe Agent"),
            shoppingAgent = AgentCardState("Shopping Agent"),
            isRunning = false,
        )
    }

    fun selectRecipe(index: Int) {
        val snapshot = _state.value
        val recipePlan = snapshot.recipePlan ?: return
        if (index !in recipePlan.recipes.indices) return

        _state.value = snapshot.copy(
            selectedRecipeIndex = index,
            errorMessage = null,
        )
    }

    fun analyze() {
        val snapshot = _state.value
        val apiKey = resolvedApiKey()
        if (apiKey.isBlank()) {
            _state.value = snapshot.copy(
                errorMessage = "No OpenRouter key is available at runtime. Current BuildConfig key length: ${BuildConfig.BAML_OPENROUTER_API_KEY.length}. Rebuild and reinstall after setting OPENROUTER_API_KEY."
            )
            return
        }

        analysisJob?.cancel()
        _state.value = snapshot.copy(
            isRunning = true,
            errorMessage = null,
            inventory = null,
            constraints = null,
            recipePlan = null,
            selectedRecipeIndex = null,
            shoppingPlanDeck = null,
            ingredientAgent = AgentCardState("Ingredient Agent", AgentPhase.Running, "Scanning the selected fridge"),
            constraintAgent = AgentCardState("Constraint Agent", AgentPhase.Running, "Normalizing selected restrictions"),
            recipeAgent = AgentCardState("Recipe Agent", AgentPhase.Idle, "Waiting for inventory + constraints"),
            shoppingAgent = AgentCardState("Shopping Agent", AgentPhase.Idle, "Waiting for best recipe"),
        )

        analysisJob = viewModelScope.launch {
            val currentState = _state.value
            try {
                withContext(Dispatchers.Default) {
                    BamlRuntime.init(
                        envVars = mapOf("OPENROUTER_API_KEY" to apiKey),
                    )
                }
                val options = CallOptions(
                    tags = mapOf("demo" to "kitchen_recipe_app"),
                )
                val fridgeImage = withContext(Dispatchers.IO) {
                    prepareActiveImage(currentState)
                }

                supervisorScope {
                    val inventoryDeferred = async(Dispatchers.IO) {
                        runAgent(
                            title = "Ingredient Agent",
                            update = { _state.value = _state.value.copy(ingredientAgent = it) },
                            block = {
                                BamlFunctions.AnalyzeFridgeInventory(
                                    fridgeImage = fridgeImage,
                                    pantryText = currentState.pantryText,
                                    userGoal = currentState.goalText,
                                    selectedRestrictions = currentState.selectedRestrictions.toList(),
                                    options = options,
                                )
                            },
                            summary = { "${it.ingredients.size} confident items" },
                        ) { result ->
                            _state.value = _state.value.copy(inventory = result)
                        }
                    }

                    val constraintsDeferred = async(Dispatchers.IO) {
                        runAgent(
                            title = "Constraint Agent",
                            update = { _state.value = _state.value.copy(constraintAgent = it) },
                            block = {
                                BamlFunctions.InferCookingConstraints(
                                    userGoal = currentState.goalText,
                                    pantryText = currentState.pantryText,
                                    selectedRestrictions = currentState.selectedRestrictions.toList(),
                                    options = options,
                                )
                            },
                            summary = { "${it.diet} • ${it.maxCookTimeMinutes} min" },
                        ) { result ->
                            _state.value = _state.value.copy(constraints = result)
                        }
                    }

                    val inventory = inventoryDeferred.await()
                    val constraints = constraintsDeferred.await()

                    _state.value = _state.value.copy(
                        recipeAgent = AgentCardState("Recipe Agent", AgentPhase.Running, "Drafting recipe candidates"),
                        shoppingAgent = AgentCardState("Shopping Agent", AgentPhase.Idle, "Waiting for recipe candidates"),
                    )

                    val recipePlan = async(Dispatchers.IO) {
                        runAgent(
                            title = "Recipe Agent",
                            update = { _state.value = _state.value.copy(recipeAgent = it) },
                            block = {
                                BamlFunctions.SuggestRecipePlan(
                                    inventory = inventory,
                                    constraints = constraints,
                                    options = options,
                                )
                            },
                            summary = { "${it.recipes.size} recipe ideas" },
                        ) { result ->
                            _state.value = _state.value.copy(
                                recipePlan = result,
                                selectedRecipeIndex = if (result.recipes.isNotEmpty()) 0 else null,
                            )
                        }
                    }.await()

                    _state.value = _state.value.copy(
                        shoppingAgent = AgentCardState("Shopping Agent", AgentPhase.Running, "Precomputing shopping for all recipes"),
                    )

                    runAgent<ShoppingPlanDeck>(
                        title = "Shopping Agent",
                        update = { _state.value = _state.value.copy(shoppingAgent = it) },
                        block = {
                            BamlFunctions.BuildShoppingPlans(
                                inventory = inventory,
                                constraints = constraints,
                                recipePlan = recipePlan,
                                options = options,
                            )
                        },
                        summary = { deck: ShoppingPlanDeck -> "${deck.plans.size} recipe shopping plans ready" },
                        onSuccess = { result: ShoppingPlanDeck ->
                            _state.value = _state.value.copy(shoppingPlanDeck = result)
                        },
                    )
                }
            } finally {
                _state.value = _state.value.copy(isRunning = false)
            }
        }
    }

    private suspend fun <T> runAgent(
        title: String,
        update: (AgentCardState) -> Unit,
        block: suspend () -> T,
        summary: (T) -> String,
        onSuccess: (T) -> Unit,
    ): T {
        val startedAt = System.currentTimeMillis()
        try {
            val result = block()
            val duration = System.currentTimeMillis() - startedAt
            onSuccess(result)
            update(AgentCardState(title, AgentPhase.Success, summary(result), duration))
            return result
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            update(AgentCardState(title, AgentPhase.Idle, "Superseded by a new request"))
            throw cancelled
        } catch (error: Throwable) {
            val duration = System.currentTimeMillis() - startedAt
            val detailedMessage = when (error) {
                is BamlException -> error.message ?: "BAML request failed"
                else -> error.localizedMessage ?: "Unexpected error"
            }
            Log.e(TAG, "Agent failed: $title after ${duration}ms: $detailedMessage", error)
            update(AgentCardState(title, AgentPhase.Error, "Check Logcat", duration))
            _state.value = _state.value.copy(
                errorMessage = "Request failed. Check Logcat for $title details.",
                isRunning = false,
            )
            throw error
        }
    }

    private fun prepareSampleImage(sample: FridgeSample): BamlImage {
        sampleImageCache[sample.id]?.let { return it }
        val rawBytes = getApplication<Application>().resources.openRawResource(sample.imageResId).use { it.readBytes() }
        val base64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
        return BamlImage.fromBase64(base64, "image/jpeg").also {
            sampleImageCache[sample.id] = it
        }
    }

    private fun prepareActiveImage(state: RecipePlannerUiState): BamlImage {
        val uploadedUri = state.uploadedImageUri
        return if (state.activeImageMode == ImageInputMode.Uploaded && uploadedUri != null) {
            prepareUploadedImage(uploadedUri)
        } else {
            prepareSampleImage(state.selectedSample)
        }
    }

    private fun prepareUploadedImage(uri: Uri): BamlImage {
        val cacheKey = uri.toString()
        uploadedImageCache[cacheKey]?.let { return it }

        val resolver = getApplication<Application>().contentResolver
        val mimeType = resolver.getType(uri)
        val imageBytes = normalizeUploadedImage(uri)
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        return BamlImage.fromBase64(base64, mimeType ?: "image/jpeg").also {
            uploadedImageCache[cacheKey] = it
        }
    }

    private fun normalizeUploadedImage(uri: Uri): ByteArray {
        val resolver = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Unable to open selected image" }
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 1600)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = resolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Unable to reopen selected image" }
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalArgumentException("Failed to decode selected image")

        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth > maxDimension || currentHeight > maxDimension) {
            currentWidth /= 2
            currentHeight /= 2
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }
}

private fun resolvedApiKey(): String =
    BuildConfig.BAML_OPENROUTER_API_KEY.trim()
