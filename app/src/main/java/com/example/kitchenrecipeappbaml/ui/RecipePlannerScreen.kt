package com.example.kitchenrecipeappbaml.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import baml_client.types.CookingConstraints
import baml_client.types.InventoryAnalysis
import baml_client.types.RecipePlan
import baml_client.types.RecipeSuggestion
import baml_client.types.ShoppingPlanDeck
import com.example.kitchenrecipeappbaml.ui.theme.Butter
import com.example.kitchenrecipeappbaml.ui.theme.Charcoal
import com.example.kitchenrecipeappbaml.ui.theme.CounterWhite
import com.example.kitchenrecipeappbaml.ui.theme.OliveGreen
import com.example.kitchenrecipeappbaml.ui.theme.SoftSage
import com.example.kitchenrecipeappbaml.ui.theme.Stone
import com.example.kitchenrecipeappbaml.ui.theme.TomatoRed
import com.example.kitchenrecipeappbaml.ui.theme.WarmCream
import coil.compose.AsyncImage

@Composable
fun RecipePlannerScreen(
    viewModel: RecipePlannerViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        viewModel.setUploadedImage(uri)
    }

    LazyColumn(
        modifier = modifier
            .background(WarmCream)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroSection()
        }
        item {
            SampleSelector(
                state = state,
                onSelect = viewModel::selectSample,
                onUploadClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onUsePreset = viewModel::usePresetImage,
                onUseUploaded = viewModel::useUploadedImage,
                onClearUploaded = viewModel::clearUploadedImage,
            )
        }
        item {
            PromptSection(
                goalText = state.goalText,
                pantryText = state.pantryText,
                selectedRestrictions = state.selectedRestrictions,
                onGoalChange = viewModel::updateGoal,
                onPantryChange = viewModel::updatePantry,
                onAnalyze = viewModel::analyze,
                onToggleRestriction = viewModel::toggleRestriction,
                onReset = viewModel::resetInputs,
                isRunning = state.isRunning,
            )
        }
        if (state.errorMessage != null) {
            item {
                BannerCard(
                    title = "Run issue",
                    body = state.errorMessage,
                    accent = TomatoRed,
                )
            }
        }
        item {
            AgentGrid(
                cards = listOf(
                    state.ingredientAgent,
                    state.constraintAgent,
                    state.recipeAgent,
                    state.shoppingAgent,
                ),
            )
        }
        if (state.inventory != null || state.constraints != null || state.recipePlan != null || state.shoppingPlanDeck != null) {
            item {
                ResultSection(
                    inventory = state.inventory,
                    constraints = state.constraints,
                    recipePlan = state.recipePlan,
                    selectedRecipeIndex = state.selectedRecipeIndex,
                    onRecipeSelect = viewModel::selectRecipe,
                    shoppingPlanDeck = state.shoppingPlanDeck,
                )
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Card(
        colors = CardDefaults.cardColors(containerColor = CounterWhite),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Recipe Planner", style = MaterialTheme.typography.displaySmall)
            Text(
                "Scan a fridge, normalize restrictions, and let typed BAML agents turn messy multimodal input into a meal plan.",
                style = MaterialTheme.typography.bodyLarge,
                color = Stone,
            )
            Surface(
                color = Butter,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = "Baseline demo mode",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Charcoal,
                )
            }
        }
    }
}

@Composable
private fun SampleSelector(
    state: RecipePlannerUiState,
    onSelect: (String) -> Unit,
    onUploadClick: () -> Unit,
    onUsePreset: () -> Unit,
    onUseUploaded: () -> Unit,
    onClearUploaded: () -> Unit,
) {
    SectionCard(title = "1. Pick a fridge") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val selectedSample = state.selectedSample
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CounterWhite),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AsyncImage(
                        model = state.activeImageModel,
                        contentDescription = selectedSample.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 210.dp),
                        contentScale = ContentScale.Crop,
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val isUploaded = state.activeImageMode == ImageInputMode.Uploaded && state.uploadedImageUri != null
                        Text(
                            if (isUploaded) "Uploaded Fridge Image" else selectedSample.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (isUploaded) "Using a photo selected from the device. Tap a preset to switch back instantly."
                            else selectedSample.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Stone,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionButton(
                    onClick = onUploadClick,
                    enabled = true,
                    primary = true,
                ) {
                    Text("Upload Image")
                }
                if (state.uploadedImageUri != null && state.activeImageMode == ImageInputMode.Preset) {
                    ActionButton(
                        onClick = onUseUploaded,
                        enabled = true,
                        primary = false,
                    ) {
                        Text("Use Uploaded")
                    }
                }
                if (state.uploadedImageUri != null) {
                    ActionButton(
                        onClick = onClearUploaded,
                        enabled = true,
                        primary = false,
                    ) {
                        Text("Clear Upload")
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sampleFridges) { sample ->
                    Card(
                        modifier = Modifier
                            .width(158.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = if (sample.id == state.selectedSampleId && state.activeImageMode == ImageInputMode.Preset) 2.dp else 0.dp,
                                color = if (sample.id == state.selectedSampleId && state.activeImageMode == ImageInputMode.Preset) OliveGreen else Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                            ),
                        colors = CardDefaults.cardColors(containerColor = CounterWhite),
                        onClick = { onSelect(sample.id) },
                    ) {
                        Column {
                            AsyncImage(
                                model = sample.imageResId,
                                contentDescription = sample.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp),
                                contentScale = ContentScale.Crop,
                            )
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(sample.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    sample.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Stone,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            if (state.uploadedImageUri != null) {
                OutlinedButton(onClick = onUsePreset, modifier = Modifier.align(Alignment.Start)) {
                    Text("Use Preset Gallery")
                }
            }
        }
    }
}

@Composable
private fun PromptSection(
    goalText: String,
    pantryText: String,
    selectedRestrictions: Set<String>,
    onGoalChange: (String) -> Unit,
    onPantryChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onToggleRestriction: (String) -> Unit,
    onReset: () -> Unit,
    isRunning: Boolean,
) {
    SectionCard(title = "2. Simulate the ask") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = pantryText,
                onValueChange = onPantryChange,
                label = { Text("Pantry notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                supportingText = {
                    Text("Optional notes supplement the fridge image. Keep these short and factual.")
                },
            )
            OutlinedTextField(
                value = goalText,
                onValueChange = onGoalChange,
                label = { Text("Meal goal") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                restrictionOptions.chunked(4).forEach { rowChips ->
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rowChips) { chip ->
                            FilterChip(
                                selected = chip in selectedRestrictions,
                                onClick = { onToggleRestriction(chip) },
                                label = { Text(chip) },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionButton(
                    onClick = onAnalyze,
                    enabled = !isRunning,
                    primary = true,
                ) {
                    Text(if (isRunning) "Agents Running…" else "Run Planner")
                }
                ActionButton(
                    onClick = onReset,
                    enabled = !isRunning,
                    primary = false,
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun AgentGrid(cards: List<AgentCardState>) {
    SectionCard(title = "3. Concurrent agents") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.forEach { card ->
                val accent = when (card.phase) {
                    AgentPhase.Idle -> Stone
                    AgentPhase.Running -> OliveGreen
                    AgentPhase.Success -> OliveGreen
                    AgentPhase.Error -> TomatoRed
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CounterWhite)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(card.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            card.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Stone,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (card.durationMs != null) {
                        Text(
                            text = "${card.durationMs / 1000.0}s",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSection(
    inventory: InventoryAnalysis?,
    constraints: CookingConstraints?,
    recipePlan: RecipePlan?,
    selectedRecipeIndex: Int?,
    onRecipeSelect: (Int) -> Unit,
    shoppingPlanDeck: ShoppingPlanDeck?,
) {
    val selectedRecipeName = recipePlan
        ?.recipes
        ?.getOrNull(selectedRecipeIndex ?: 0)
        ?.name
    val shoppingPlan = shoppingPlanDeck
        ?.plans
        ?.firstOrNull { option -> option.recipeName == selectedRecipeName }
        ?.plan

    SectionCard(title = "4. Typed results") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            inventory?.let {
                ResultBlock("Inventory", it.title, it.summary) {
                    it.ingredients.forEach { ingredient ->
                        Text(
                            "• ${ingredient.name} ${ingredient.quantity?.let { qty -> "(${qty}) " } ?: ""}· ${(ingredient.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (it.possibleIngredients.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Maybe detected: ${it.possibleIngredients.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Stone,
                        )
                    }
                }
            }
            constraints?.let {
                ResultBlock("Constraints", it.goalSummary, "${it.diet} • ${it.maxCookTimeMinutes} min target") {
                    Text(
                        "Preferred cuisines: ${it.preferredCuisines.joinToString().ifBlank { "Flexible" }}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Avoid: ${it.avoidIngredients.joinToString().ifBlank { "None flagged" }}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            recipePlan?.let {
                ResultBlock("Recipes", it.title, it.whyItFits) {
                    Text(
                        "Tap a dish to see what you need to buy for that exact recipe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Stone,
                    )
                    it.recipes.forEachIndexed { index, recipe ->
                        RecipeOptionCard(
                            recipe = recipe,
                            selected = index == selectedRecipeIndex,
                            onClick = { onRecipeSelect(index) },
                        )
                    }
                }
            }
            shoppingPlan?.let {
                ResultBlock("Shopping", it.title, selectedRecipeName?.let { recipe -> "For $recipe" }) {
                    Text(
                        "Estimated total: $${"%.2f".format(it.estimatedTotalCostUsd)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = OliveGreen,
                    )
                    it.items.forEach { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarmCream),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text(item.reason, style = MaterialTheme.typography.bodyMedium, color = Stone)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.quantity, style = MaterialTheme.typography.labelLarge, color = Charcoal)
                                    Text("$${"%.2f".format(item.estimatedCostUsd)}", style = MaterialTheme.typography.labelLarge, color = OliveGreen)
                                }
                            }
                        }
                    }
                    Text(
                        "Pantry wins: ${it.pantryWins.joinToString().ifBlank { "No clear pantry wins" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OliveGreen,
                    )
                    if (it.notes.isNotEmpty()) {
                        Text(
                            "Notes: ${it.notes.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Stone,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeOptionCard(
    recipe: RecipeSuggestion,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) SoftSage else WarmCream),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) OliveGreen else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (selected) {
                    Text("Selected", style = MaterialTheme.typography.labelLarge, color = OliveGreen)
                }
            }
            Text(
                "${recipe.cookTimeMinutes} min • ${recipe.proteinEstimateGrams}g protein • ${recipe.difficulty}",
                style = MaterialTheme.typography.bodyMedium,
                color = OliveGreen,
            )
            Text(recipe.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Missing: ${recipe.missingIngredients.joinToString().ifBlank { "Nothing essential" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone,
            )
        }
    }
}

@Composable
private fun RowScope.ActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean,
    content: @Composable () -> Unit,
) {
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            content()
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            content()
        }
    }
}

@Composable
private fun BannerCard(title: String, body: String?, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ResultBlock(
    label: String,
    title: String,
    subtitle: String?,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TomatoRed)
        Text(title, style = MaterialTheme.typography.titleLarge, color = Charcoal)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Stone)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = CounterWhite),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = body,
            )
        }
    }
}
