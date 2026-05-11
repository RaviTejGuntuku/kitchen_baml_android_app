class IngredientDetection {
  name string
  quantity string?
  confidence float
  note string?
}

class InventoryAnalysis {
  title string
  summary string
  ingredients IngredientDetection[]
  possibleIngredients string[]
}

class CookingConstraints {
  goalSummary string
  diet string
  maxCookTimeMinutes int
  proteinPriority string
  preferredCuisines string[]
  avoidIngredients string[]
}

class RecipeSuggestion {
  name string
  description string
  cookTimeMinutes int
  proteinEstimateGrams int
  difficulty string
  usesIngredients string[]
  missingIngredients string[]
  steps string[]
}

class RecipePlan {
  title string
  whyItFits string
  recipes RecipeSuggestion[]
}

class ShoppingItem {
  name string
  quantity string
  reason string
  estimatedCostUsd float
}

class ShoppingPlan {
  title string
  items ShoppingItem[]
  pantryWins string[]
  notes string[]
  estimatedTotalCostUsd float
}

function AnalyzeFridgeInventory(fridgeImage: image, pantryText: string?, userGoal: string?, selectedRestrictions: string[]) -> InventoryAnalysis {
  client PlannerClient
  prompt #"
    You are the Ingredient Agent in a live meal-planning demo.

    Study the fridge image first, then use the pantry notes as supporting context.
    Extract ingredients that are clearly present in the image or explicitly mentioned in the notes.
    Estimate rough quantities only when there is visual or textual evidence.
    Keep confidence realistic. If something is uncertain, put it in possibleIngredients instead of the main ingredient list.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    Fridge image:
    {{ fridgeImage }}

    User goal: {{ userGoal or "No extra request provided" }}
    Selected restrictions: {{ selectedRestrictions }}
    Pantry notes: {{ pantryText or "No additional pantry notes provided." }}
  "#
}

function InferCookingConstraints(userGoal: string?, pantryText: string?, selectedRestrictions: string[]) -> CookingConstraints {
  client PlannerClient
  prompt #"
    You are the Constraint Agent in a live meal-planning demo.

    Normalize the user's meal constraints from the chosen restrictions and the freeform request.
    If the user did not specify a time limit, choose a practical default that fits a weeknight meal.
    Keep the answer concrete and concise.
    Respect the selected restrictions exactly; do not infer a conflicting diet.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    User goal: {{ userGoal or "No extra request provided" }}
    Selected restrictions: {{ selectedRestrictions }}
    Pantry notes: {{ pantryText or "No additional pantry notes provided." }}
  "#
}

function SuggestRecipePlan(inventory: InventoryAnalysis, constraints: CookingConstraints) -> RecipePlan {
  client PlannerClient
  prompt #"
    You are the Recipe Agent in a live meal-planning demo.

    Suggest 3 realistic recipes that fit the structured inventory and normalized constraints.
    Prefer recipes that mostly use ingredients marked as clearly present.
    Missing ingredients should be short and practical.
    Steps should be brief and actionable.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    Inventory title: {{ inventory.title }}
    Inventory summary: {{ inventory.summary }}
    Confirmed ingredients:
    {% for ingredient in inventory.ingredients %}
    - {{ ingredient.name }}{% if ingredient.quantity %} ({{ ingredient.quantity }}){% endif %} [confidence={{ ingredient.confidence }}]
    {% endfor %}
    Possible ingredients: {{ inventory.possibleIngredients }}

    Constraints:
    - Goal summary: {{ constraints.goalSummary }}
    - Diet: {{ constraints.diet }}
    - Max cook time: {{ constraints.maxCookTimeMinutes }}
    - Protein priority: {{ constraints.proteinPriority }}
    - Preferred cuisines: {{ constraints.preferredCuisines }}
    - Avoid ingredients: {{ constraints.avoidIngredients }}
  "#
}

function BuildShoppingPlan(inventory: InventoryAnalysis, constraints: CookingConstraints, recipePlan: RecipePlan) -> ShoppingPlan {
  client PlannerClient
  prompt #"
    You are the Shopping Agent in a live meal-planning demo.

    Look at the structured inventory, constraints, and recipe plan.
    Choose the single best recipe from the plan and produce a short, legible shopping list.
    Include only the missing items that materially improve the meal.
    Each item must include a practical quantity and a rough US dollar estimate.
    estimatedTotalCostUsd should be the approximate total cost of the missing items.
    pantryWins should mention ingredients that are already available and useful.
    notes should stay short.

    {{ ctx.output_format }}

    {{ _.role("user") }}
    Inventory summary: {{ inventory.summary }}
    Available ingredients:
    {% for ingredient in inventory.ingredients %}
    - {{ ingredient.name }}{% if ingredient.quantity %} ({{ ingredient.quantity }}){% endif %}
    {% endfor %}

    Constraints:
    - Goal summary: {{ constraints.goalSummary }}
    - Diet: {{ constraints.diet }}
    - Max cook time: {{ constraints.maxCookTimeMinutes }}

    Recipe candidates:
    {% for recipe in recipePlan.recipes %}
    - {{ recipe.name }} | {{ recipe.cookTimeMinutes }} min | {{ recipe.proteinEstimateGrams }}g protein
      Uses: {{ recipe.usesIngredients }}
      Missing: {{ recipe.missingIngredients }}
    {% endfor %}
  "#
}
