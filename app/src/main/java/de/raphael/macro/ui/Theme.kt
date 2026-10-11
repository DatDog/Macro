package de.raphael.macro.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import de.raphael.macro.R

object C {
    val Bg = Color(0xFF0B0B10)
    val Card = Color(0xFF15151D)
    val CardHi = Color(0xFF1E1E2A)
    val Line = Color(0xFF2A2A38)
    val Text = Color(0xFFF3F3F7)
    val Muted = Color(0xFF8E8EA0)
    val Accent = Color(0xFF8E7CFF)
    val Mint = Color(0xFF4DE3C1)
    val Warn = Color(0xFFFF5A6E)
    val OnMint = Color(0xFF07130F)
    val Fat = Color(0xFFFFB86B)
    val Prot = Color(0xFF4DE3C1)
    val Carb = Color(0xFF8E7CFF)
}

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun st(size: Int, w: FontWeight, ls: Double = 0.0) =
    TextStyle(fontFamily = Inter, fontSize = size.sp, fontWeight = w, letterSpacing = ls.sp)

/** Textstile der App */
object Ty {
    val Display = st(56, FontWeight.Bold, -1.5)
    val Title = st(34, FontWeight.Bold, -0.8)
    val Heading = st(17, FontWeight.SemiBold)
    val Body = st(15, FontWeight.Normal)
    val Small = st(13, FontWeight.Normal)
    val Label = st(12, FontWeight.Medium, 0.2)
    val Section = st(11, FontWeight.SemiBold, 1.2)
}

private val typography = Typography(
    headlineLarge = Ty.Title,
    titleMedium = Ty.Heading,
    bodyLarge = Ty.Body,
    bodyMedium = Ty.Body,
    bodySmall = Ty.Small,
    labelLarge = Ty.Label,
    labelMedium = Ty.Label,
    labelSmall = Ty.Label,
)

@Composable
fun MacroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = C.Bg, surface = C.Bg, surfaceContainer = C.Card, surfaceContainerHigh = C.CardHi,
            primary = C.Accent, onPrimary = Color.White,
            onBackground = C.Text, onSurface = C.Text, onSurfaceVariant = C.Muted,
            secondaryContainer = C.CardHi, onSecondaryContainer = C.Text,
            outline = C.Line,
        ),
        typography = typography,
        content = content,
    )
}

/** Text mit festen Stilen und Farbe (Color.Unspecified übernimmt die Farbe des Elternteils). */
@Composable
fun T(
    text: String,
    style: TextStyle,
    color: Color = C.Text,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(text = text, modifier = modifier, color = color, style = style, maxLines = maxLines, textAlign = textAlign)
}

/** Haptik: Ein, Aus, leichter Tick */
fun View.tickOn() {
    performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP)
}

fun View.tickOff() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}
