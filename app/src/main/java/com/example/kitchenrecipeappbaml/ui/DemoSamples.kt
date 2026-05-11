package com.example.kitchenrecipeappbaml.ui

import androidx.annotation.DrawableRes
import com.example.kitchenrecipeappbaml.R

data class FridgeSample(
    val id: String,
    val title: String,
    val subtitle: String,
    @param:DrawableRes val imageResId: Int,
    val pantryText: String,
    val starterGoal: String,
    val defaultRestrictions: Set<String>,
)

val restrictionOptions = listOf(
    "Vegetarian",
    "High Protein",
    "Gluten Free",
    "Dairy Free",
    "Low Carb",
    "Under 30 Min",
    "Meal Prep",
    "Budget Friendly",
)

val sampleFridges = listOf(
    FridgeSample(
        id = "weekday",
        title = "Weeknight Fridge",
        subtitle = "Protein, greens, eggs, leftovers",
        imageResId = R.drawable.fridge_weekday,
        pantryText = "Chicken breasts, spinach, eggs, cherry tomatoes, Greek yogurt, shredded cheese, garlic, leftover rice, lemon, olive oil, salt, pepper.",
        starterGoal = "High protein dinner under 30 minutes",
        defaultRestrictions = setOf("High Protein", "Under 30 Min"),
    ),
    FridgeSample(
        id = "veggie",
        title = "Veg-Forward Shelf",
        subtitle = "Produce-heavy, flexible pantry meal",
        imageResId = R.drawable.fridge_veggie,
        pantryText = "Tofu, mushrooms, broccoli, bell peppers, scallions, spinach, rice noodles, soy sauce, sesame oil, garlic, ginger, peanuts.",
        starterGoal = "Use what is here for a colorful vegetarian dinner",
        defaultRestrictions = setOf("Vegetarian"),
    ),
    FridgeSample(
        id = "family",
        title = "Family Restock",
        subtitle = "Crowded fridge, practical meal plan",
        imageResId = R.drawable.fridge_family,
        pantryText = "Ground turkey, milk, eggs, carrots, celery, onions, cheddar, tortillas, pasta, canned tomatoes, lettuce, sandwich bread, butter.",
        starterGoal = "Plan a simple family meal and tell me what I still need",
        defaultRestrictions = setOf("Budget Friendly"),
    ),
)

val sampleFridgesById: Map<String, FridgeSample> = sampleFridges.associateBy { it.id }
