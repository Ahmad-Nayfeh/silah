package io.github.ahmadnayfeh.silah.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.ahmadnayfeh.silah.R
import io.github.ahmadnayfeh.silah.data.ThemeMode
import io.github.ahmadnayfeh.silah.domain.Tag

val PlexArabic = FontFamily(
    Font(R.font.ibm_plex_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_arabic_bold, FontWeight.Bold),
)

// Calm palette: deep green-grey, sage and warm sand. No red anywhere.
private val Dark = darkColorScheme(
    primary = Color(0xFF7CCBA9),
    onPrimary = Color(0xFF0B2A1F),
    primaryContainer = Color(0xFF1F4A3B),
    onPrimaryContainer = Color(0xFFBFEBD6),
    secondary = Color(0xFFD9B48F),
    onSecondary = Color(0xFF3A2814),
    secondaryContainer = Color(0xFF4A3923),
    onSecondaryContainer = Color(0xFFF6DDC0),
    tertiary = Color(0xFF9DB7E0),
    onTertiary = Color(0xFF15253F),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFE4ECE9),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFE4ECE9),
    surfaceVariant = Color(0xFF1C2926),
    onSurfaceVariant = Color(0xFFA7B6B1),
    surfaceContainerLowest = Color(0xFF0A100F),
    surfaceContainerLow = Color(0xFF131C1A),
    surfaceContainer = Color(0xFF172220),
    surfaceContainerHigh = Color(0xFF1C2926),
    surfaceContainerHighest = Color(0xFF23312E),
    outline = Color(0xFF52625E),
    outlineVariant = Color(0xFF2D3B38),
    error = Color(0xFFE0B48F),
    onError = Color(0xFF3A2814),
)

private val Light = lightColorScheme(
    primary = Color(0xFF2F7D62),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBDD),
    onPrimaryContainer = Color(0xFF0B2A1F),
    secondary = Color(0xFF8D6337),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E1CB),
    onSecondaryContainer = Color(0xFF3A2814),
    tertiary = Color(0xFF45679B),
    background = Color(0xFFF6F4EF),
    onBackground = Color(0xFF1B2421),
    surface = Color(0xFFF6F4EF),
    onSurface = Color(0xFF1B2421),
    surfaceVariant = Color(0xFFE6EAE6),
    onSurfaceVariant = Color(0xFF55625E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFAF7),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0EEE8),
    surfaceContainerHighest = Color(0xFFE9E6DF),
    outline = Color(0xFF8C9894),
    outlineVariant = Color(0xFFD9DDD9),
    error = Color(0xFF8D6337),
)

private fun TextStyle.plex() = copy(fontFamily = PlexArabic)

private val base = Typography()
private val SilahTypography = Typography(
    displaySmall = base.displaySmall.plex().copy(fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.plex().copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.plex().copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.plex().copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.plex().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.plex().copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.plex().copy(fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.plex().copy(lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.plex().copy(lineHeight = 22.sp),
    bodySmall = base.bodySmall.plex(),
    labelLarge = base.labelLarge.plex().copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = base.labelMedium.plex(),
    labelSmall = base.labelSmall.plex(),
)

private val SilahShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Soft identifying colour for each tag (used for avatars only; never as a warning). */
fun tagColor(tag: Tag, dark: Boolean): Color = when (tag) {
    Tag.FAMILY -> if (dark) Color(0xFFD9B48F) else Color(0xFFB98A5A)
    Tag.FRIENDS -> if (dark) Color(0xFF9DB7E0) else Color(0xFF5F82B8)
    Tag.OTHER -> if (dark) Color(0xFFC3B1E1) else Color(0xFF8A74B3)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

/** App theme. The whole UI is right-to-left, whatever the phone's language. */
@Composable
fun SilahTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark(mode)) Dark else Light,
        typography = SilahTypography,
        shapes = SilahShapes,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
    }
}
