package com.example.kitchenrecipeappbaml

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.kitchenrecipeappbaml.ui.RecipePlannerScreen
import com.example.kitchenrecipeappbaml.ui.RecipePlannerViewModel
import com.example.kitchenrecipeappbaml.ui.theme.KitchenRecipeAppBAMLTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<RecipePlannerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KitchenRecipeAppBAMLTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RecipePlannerScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
