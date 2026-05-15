package ar.gov.entrerios.cge.concursos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ar.gov.entrerios.cge.concursos.core.model.DarkMode

private val LightColors = lightColorScheme(
    primary = CgeBlue,
    onPrimary = Color.White,
    primaryContainer = CgeBlueLight,
    onPrimaryContainer = Color.White,
    secondary = CgeBlueDark,
    background = Surface,
    onBackground = OnSurface,
    surface = Color.White,
    onSurface = OnSurface,
    outline = OutlineLight
)

private val DarkColors = darkColorScheme(
    primary = CgeBlueLight,
    onPrimary = Color.Black,
    primaryContainer = CgeBlueDark,
    onPrimaryContainer = Color.White,
    secondary = CgeBlue,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = Color(0xFF1B1D1F),
    onSurface = DarkOnSurface
)

@Composable
fun ConcursosTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = when (darkMode) {
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
        DarkMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (isDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        isDark -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
