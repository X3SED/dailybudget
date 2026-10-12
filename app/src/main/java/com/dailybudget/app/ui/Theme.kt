package com.dailybudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HeroStart = Color(0xFF0F766E)
val HeroEnd = Color(0xFF134E4A)
val Positive = Color(0xFF16A34A)
val Negative = Color(0xFFDC2626)

private val Light = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFFB45309),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    background = Color(0xFFF6F7F5),
    surface = Color(0xFFF6F7F5),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    error = Negative,
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFFBBF24),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0E1412),
    surface = Color(0xFF0E1412),
    surfaceContainer = Color(0xFF18201E),
    surfaceContainerHigh = Color(0xFF1F2927),
    error = Color(0xFFF87171),
)

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
