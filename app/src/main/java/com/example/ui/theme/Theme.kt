package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    secondary = SecondaryViolet,
    onSecondary = Color.White,
    tertiary = SuccessEmerald,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextMain,
    onSurface = DarkTextMain,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorder,
    outlineVariant = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    secondary = SecondaryViolet,
    onSecondary = Color.White,
    tertiary = SuccessEmerald,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightTextMain,
    onSurface = LightTextMain,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = LightTextMuted,
    outline = LightBorder,
    outlineVariant = LightBorderVariant
)

@Composable
fun KasiGratisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use brand primary colors consistently
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun appTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    focusedBorderColor = PrimaryIndigo,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = PrimaryIndigo,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPrefixColor = PrimaryIndigo,
    unfocusedPrefixColor = PrimaryIndigo
)


