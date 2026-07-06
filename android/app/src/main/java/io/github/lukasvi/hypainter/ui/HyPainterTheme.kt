package io.github.lukasvi.hypainter.ui

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

internal fun supportsWallpaperColors(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
internal fun HyPainterTheme(
    useWallpaperColors: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        useWallpaperColors && supportsWallpaperColors() && darkTheme -> dynamicDarkColorScheme(context)
        useWallpaperColors && supportsWallpaperColors() -> dynamicLightColorScheme(context)
        darkTheme -> HyPainterDarkColors
        else -> HyPainterLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

private val HyPainterDarkColors = darkColorScheme(
    primary = Color(0xFF7CB7FF),
    secondary = Color(0xFF67D6BE),
    background = Color(0xFF15171A),
    surface = Color(0xFF202329),
    surfaceVariant = Color(0xFF2A303B),
    onBackground = Color(0xFFE8EDF6),
    onSurface = Color(0xFFE8EDF6),
)

private val HyPainterLightColors = lightColorScheme(
    primary = Color(0xFF245FB8),
    secondary = Color(0xFF087765),
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE1E7F0),
    onBackground = Color(0xFF15171A),
    onSurface = Color(0xFF15171A),
)
