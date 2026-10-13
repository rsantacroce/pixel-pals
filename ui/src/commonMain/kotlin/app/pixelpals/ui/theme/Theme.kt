package app.pixelpals.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pixelpals.io.ThemeMode

private val Light =
    lightColorScheme(
        primary = Color(0xFF5B3CF5),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE3DCFF),
        onPrimaryContainer = Color(0xFF1F0B72),
        secondary = Color(0xFFFF7A1A),
        onSecondary = Color(0xFF2B1300),
        secondaryContainer = Color(0xFFFFE1C7),
        onSecondaryContainer = Color(0xFF3D1C00),
        tertiary = Color(0xFF14A85A),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFC9F5DA),
        onTertiaryContainer = Color(0xFF00391B),
        background = Color(0xFFFFF8EC),
        onBackground = Color(0xFF221B33),
        surface = Color(0xFFFFFBF4),
        onSurface = Color(0xFF221B33),
        surfaceVariant = Color(0xFFF1E9F9),
        onSurfaceVariant = Color(0xFF4A4060),
        surfaceContainer = Color(0xFFF7EEFF),
        surfaceContainerHigh = Color(0xFFEFE4FB),
        outline = Color(0xFF7B7090),
        error = Color(0xFFD7263D),
    )

private val Dark =
    darkColorScheme(
        primary = Color(0xFFB9A8FF),
        onPrimary = Color(0xFF250E86),
        primaryContainer = Color(0xFF4127C9),
        onPrimaryContainer = Color(0xFFE6DFFF),
        secondary = Color(0xFFFFB37A),
        onSecondary = Color(0xFF4A2100),
        secondaryContainer = Color(0xFF7A3A00),
        onSecondaryContainer = Color(0xFFFFE1C7),
        tertiary = Color(0xFF6FE3A0),
        onTertiary = Color(0xFF003919),
        tertiaryContainer = Color(0xFF00602F),
        onTertiaryContainer = Color(0xFFC9F5DA),
        background = Color(0xFF17131F),
        onBackground = Color(0xFFEDE6F7),
        surface = Color(0xFF1D1827),
        onSurface = Color(0xFFEDE6F7),
        surfaceVariant = Color(0xFF2E2740),
        onSurfaceVariant = Color(0xFFCFC4E3),
        surfaceContainer = Color(0xFF241E31),
        surfaceContainerHigh = Color(0xFF2E2740),
        outline = Color(0xFF978CAD),
        error = Color(0xFFFF6B7D),
    )

private val Rounded =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )

private fun Typography.chunky(): Typography =
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    )

/** Colors used outside the Material scheme (checkerboard, grid). */
data class CanvasColors(val checkerA: Color, val checkerB: Color, val grid: Color, val axis: Color, val backdrop: Color)

val LocalCanvasColors = staticCompositionLocalOf { CanvasColors(Color.White, Color.LightGray, Color.Gray, Color.Magenta, Color.White) }

/** True when the dark theme is active. */
val LocalIsDark = staticCompositionLocalOf { false }

@Composable
fun PixelPalsTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark =
        when (mode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val canvas =
        if (dark) {
            CanvasColors(Color(0xFF3A3448), Color(0xFF2C2738), Color(0x66FFFFFF), Color(0xFFFF6BC1), Color(0xFF120F18))
        } else {
            CanvasColors(Color(0xFFFFFFFF), Color(0xFFE4E0EC), Color(0x55302845), Color(0xFFE0329B), Color(0xFFEDE5F7))
        }
    androidx.compose.runtime.CompositionLocalProvider(LocalCanvasColors provides canvas, LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            shapes = Rounded,
            typography = Typography().chunky(),
            content = content,
        )
    }
}

/** Default text style for small labels under icons. */
val LabelStyle: TextStyle @Composable get() = MaterialTheme.typography.labelMedium
