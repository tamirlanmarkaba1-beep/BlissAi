package com.bliss.blisschat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

private fun hsl(h: Float, s: Float, l: Float): Color =
    Color(ColorUtils.HSLToColor(floatArrayOf(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))

/** Builds a full M3 colour scheme from ONE hue, so the user can pick any colour of the spectrum. */
fun schemeFor(hue: Float, dark: Boolean): ColorScheme {
    // hue < 0 is the "black" swatch: a colourless (grey / white) palette
    val k = if (hue < 0f) 0f else 1f
    fun x(h: Float, s: Float, l: Float): Color = hsl(h, s * k, l)
    val h2 = hue + 10f   // calm grey-green companion (header pills, input bar, tab bar) as in the mockups
    val h3 = hue + 110f
    return if (dark) darkColorScheme(
        primary = x(hue, 0.62f, 0.70f), onPrimary = x(hue, 0.85f, 0.10f),
        primaryContainer = x(hue, 0.90f, 0.17f), onPrimaryContainer = x(hue, 0.80f, 0.88f),
        inversePrimary = x(hue, 0.65f, 0.38f),
        secondary = x(h2, 0.25f, 0.74f), onSecondary = x(h2, 0.45f, 0.13f),
        secondaryContainer = x(h2, 0.12f, 0.27f), onSecondaryContainer = x(h2, 0.18f, 0.90f),
        tertiary = x(h3, 0.48f, 0.76f), onTertiary = x(h3, 0.60f, 0.14f),
        tertiaryContainer = x(h3, 0.36f, 0.27f), onTertiaryContainer = x(h3, 0.60f, 0.90f),
        background = x(hue, 0.20f, 0.065f), onBackground = x(hue, 0.10f, 0.91f),
        surface = x(hue, 0.20f, 0.065f), onSurface = x(hue, 0.10f, 0.91f),
        surfaceVariant = x(hue, 0.12f, 0.21f), onSurfaceVariant = x(hue, 0.12f, 0.78f),
        surfaceTint = x(hue, 0.62f, 0.70f),
        inverseSurface = x(hue, 0.08f, 0.90f), inverseOnSurface = x(hue, 0.14f, 0.14f),
        outline = x(hue, 0.08f, 0.58f), outlineVariant = x(hue, 0.10f, 0.27f),
        surfaceDim = x(hue, 0.18f, 0.05f), surfaceBright = x(hue, 0.12f, 0.22f),
        surfaceContainerLowest = x(hue, 0.18f, 0.045f), surfaceContainerLow = x(hue, 0.15f, 0.09f),
        surfaceContainer = x(hue, 0.14f, 0.125f), surfaceContainerHigh = x(hue, 0.12f, 0.18f),
        surfaceContainerHighest = x(hue, 0.10f, 0.22f)
    ) else lightColorScheme(
        primary = x(hue, 0.70f, 0.30f), onPrimary = Color.White,
        primaryContainer = x(hue, 0.75f, 0.84f), onPrimaryContainer = x(hue, 0.85f, 0.10f),
        inversePrimary = x(hue, 0.62f, 0.70f),
        secondary = x(h2, 0.30f, 0.34f), onSecondary = Color.White,
        secondaryContainer = x(h2, 0.22f, 0.86f), onSecondaryContainer = x(h2, 0.50f, 0.11f),
        tertiary = x(h3, 0.35f, 0.34f), onTertiary = Color.White,
        tertiaryContainer = x(h3, 0.55f, 0.88f), onTertiaryContainer = x(h3, 0.60f, 0.12f),
        background = x(hue, 0.30f, 0.975f), onBackground = x(hue, 0.15f, 0.10f),
        surface = x(hue, 0.30f, 0.975f), onSurface = x(hue, 0.15f, 0.10f),
        surfaceVariant = x(hue, 0.25f, 0.89f), onSurfaceVariant = x(hue, 0.15f, 0.28f),
        surfaceTint = x(hue, 0.70f, 0.30f),
        inverseSurface = x(hue, 0.10f, 0.19f), inverseOnSurface = x(hue, 0.20f, 0.94f),
        outline = x(hue, 0.08f, 0.45f), outlineVariant = x(hue, 0.15f, 0.78f),
        surfaceDim = x(hue, 0.20f, 0.86f), surfaceBright = x(hue, 0.30f, 0.975f),
        surfaceContainerLowest = Color.White, surfaceContainerLow = x(hue, 0.30f, 0.95f),
        surfaceContainer = x(hue, 0.28f, 0.93f), surfaceContainerHigh = x(hue, 0.26f, 0.91f),
        surfaceContainerHighest = x(hue, 0.24f, 0.89f)
    )
}

@Composable
private fun anim(target: Color): Color = animateColorAsState(target, tween(250), label = "scheme").value

/** Smoothly morphs every colour when the accent hue or light/dark mode changes. */
@Composable
private fun animated(t: ColorScheme): ColorScheme = t.copy(
    primary = anim(t.primary), onPrimary = anim(t.onPrimary),
    primaryContainer = anim(t.primaryContainer), onPrimaryContainer = anim(t.onPrimaryContainer),
    inversePrimary = anim(t.inversePrimary),
    secondary = anim(t.secondary), onSecondary = anim(t.onSecondary),
    secondaryContainer = anim(t.secondaryContainer), onSecondaryContainer = anim(t.onSecondaryContainer),
    tertiary = anim(t.tertiary), onTertiary = anim(t.onTertiary),
    tertiaryContainer = anim(t.tertiaryContainer), onTertiaryContainer = anim(t.onTertiaryContainer),
    background = anim(t.background), onBackground = anim(t.onBackground),
    surface = anim(t.surface), onSurface = anim(t.onSurface),
    surfaceVariant = anim(t.surfaceVariant), onSurfaceVariant = anim(t.onSurfaceVariant),
    surfaceTint = anim(t.surfaceTint),
    inverseSurface = anim(t.inverseSurface), inverseOnSurface = anim(t.inverseOnSurface),
    error = anim(t.error), onError = anim(t.onError),
    errorContainer = anim(t.errorContainer), onErrorContainer = anim(t.onErrorContainer),
    outline = anim(t.outline), outlineVariant = anim(t.outlineVariant),
    surfaceDim = anim(t.surfaceDim), surfaceBright = anim(t.surfaceBright),
    surfaceContainerLowest = anim(t.surfaceContainerLowest), surfaceContainerLow = anim(t.surfaceContainerLow),
    surfaceContainer = anim(t.surfaceContainer), surfaceContainerHigh = anim(t.surfaceContainerHigh),
    surfaceContainerHighest = anim(t.surfaceContainerHighest)
)

@Composable
fun BlissTheme(darkTheme: Boolean, hue: Float, dynamic: Boolean = false, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val c = WindowCompat.getInsetsController(window, view)
            c.isAppearanceLightStatusBars = !darkTheme
            c.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    val typography = remember { blissTypography(NunitoFamily.load(context)) }
    MaterialTheme(
        colorScheme = animated(
            if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else schemeFor(hue, darkTheme)
        ),
        typography = typography,
        shapes = BlissShapes,
        content = content
    )
}
