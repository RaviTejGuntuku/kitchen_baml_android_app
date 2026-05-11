package com.example.kitchenrecipeappbaml.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Butter,
    secondary = SoftSage,
    tertiary = TomatoRed,
    background = DeepPan,
    surface = Charcoal,
    onPrimary = DeepPan,
    onSecondary = DeepPan,
    onTertiary = CounterWhite,
    onBackground = WarmCream,
    onSurface = WarmCream
)

private val LightColorScheme = lightColorScheme(
    primary = TomatoRed,
    secondary = OliveGreen,
    tertiary = Butter,
    background = WarmCream,
    surface = CounterWhite,
    onPrimary = CounterWhite,
    onSecondary = CounterWhite,
    onTertiary = Charcoal,
    onBackground = Charcoal,
    onSurface = Charcoal
)

@Composable
fun KitchenRecipeAppBAMLTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
