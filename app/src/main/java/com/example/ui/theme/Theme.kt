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
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFBBF7D0),
    secondary = Color(0xFF86EFAC),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF1F3A27),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF121413),
    surface = Color(0xFF1B1E1C),
    surfaceVariant = Color(0xFF262C28),
    onSurface = Color(0xFFF1F5F2),
    onSurfaceVariant = Color(0xFFA3ACA5),
    outline = Color(0xFF707A72),
    outlineVariant = Color(0xFF38403A),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    secondaryContainer = GreenSecondaryContainer,
    tertiary = GreenTertiary,
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF2EE),
    onSurface = Color(0xFF191C1A),
    onBackground = Color(0xFF191C1A),
    onSurfaceVariant = Color(0xFF5A635C),
    outline = Color(0xFF717972),
    outlineVariant = Color(0xFFD6DDD7),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Light Mode como padrão (Regra 4)
    dynamicColor: Boolean = false, // Cores consistentes e limpas
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
