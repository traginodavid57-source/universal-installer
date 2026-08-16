package app.pwhs.universalinstaller.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CustomGradientTheme(
    val enabled: Boolean = false,
    val startColor: Color = Color(0xFFEA580C),
    val endColor: Color = Color(0xFF3B82F6),
) {
    val colors: List<Color>
        get() = listOf(startColor, endColor)
}

@Immutable
data class LiquidGlassStyle(
    val enabled: Boolean = false,
)

val LocalCustomGradientTheme = staticCompositionLocalOf { CustomGradientTheme() }
val LocalLiquidGlassStyle = staticCompositionLocalOf { LiquidGlassStyle() }

fun Color.toPreferenceHex(): String {
    val alpha = (alpha * 255).toInt().coerceIn(0, 255)
    val red = (red * 255).toInt().coerceIn(0, 255)
    val green = (green * 255).toInt().coerceIn(0, 255)
    val blue = (blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X%02X".format(alpha, red, green, blue)
}

fun parseThemeColor(value: String, fallback: Color): Color {
    val normalized = value.trim().removePrefix("#")
    val argb = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized
        else -> return fallback
    }
    return argb.toLongOrNull(16)?.let { Color(it) } ?: fallback
}
