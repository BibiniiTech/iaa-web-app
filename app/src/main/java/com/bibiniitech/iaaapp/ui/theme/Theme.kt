package com.bibiniitech.iaaapp.ui.theme

import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Preview

private val DarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = NavyDarkBackground,
    primaryContainer = IAANavy,
    onPrimaryContainer = Color.White,
    secondary = IAAGold,
    onSecondary = Color.White,
    secondaryContainer = IAANavyLight,
    onSecondaryContainer = Color.White,
    tertiary = IAASlate,
    background = NavyDarkBackground,
    onBackground = Color.White,
    surface = IndigoSlateSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSlateDivider,
    onSurfaceVariant = Color(0xFFE2E8F0),
    outline = SlateBlueGrey,
    outlineVariant = DarkSlateDivider,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IAANavy,
    secondary = IAAGold,
    tertiary = IAASlate,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A202C),
    onSurface = Color(0xFF1A202C)
)

@Preview(
    name = "Light Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class ThemePreviews

@Suppress("ObsoleteSdkInt")
@Composable
fun IAAAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) -> {
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
