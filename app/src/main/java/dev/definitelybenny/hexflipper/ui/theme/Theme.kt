package dev.definitelybenny.hexflipper.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HexFlipperColorScheme = darkColorScheme(
    primary = Color(0xFF4FACFE),
    secondary = Color(0xFF43E97B),
    tertiary = Color(0xFFF093FB),
    background = Color(0xFF0F0C29),
    surface = Color(0xFF1E1B3A),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun HexFlipperTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(LocalContext.current)
        }
        else -> HexFlipperColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
