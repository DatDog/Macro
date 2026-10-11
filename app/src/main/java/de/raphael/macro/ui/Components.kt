package de.raphael.macro.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

/** Weicher Druck-Effekt: Element federt beim Antippen leicht ein. */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Karte mit weichem Rand; mit onClick federt sie beim Antippen. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val base = if (onClick != null) modifier.pressable(onClick) else modifier
    Column(
        base
            .clip(shape)
            .background(C.Card)
            .border(1.dp, C.Line, shape)
            .padding(18.dp),
        content = content,
    )
}

/** Einblenden von unten mit kurzer Verzögerung je Position → gestaffelter Aufbau der Liste. */
@Composable
fun Enter(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(45L * index)
        shown = true
    }
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "enter",
    )
    Box(
        Modifier.graphicsLayer {
            alpha = a
            translationY = (1f - a) * 26.dp.toPx()
        }
    ) { content() }
}

/** Runder Haken: Kreis füllt sich mit leichtem Überschwingen, der Haken zeichnet sich nach. */
@Composable
fun CheckBubble(done: Boolean, size: Dp = 46.dp) {
    val p by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "check",
    )
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val stroke = 2.dp.toPx()
        drawCircle(color = C.Line, radius = r - stroke / 2f, style = Stroke(stroke))
        val fillR = (r - stroke / 2f) * p.coerceAtLeast(0f)
        if (fillR > 0.5f) drawCircle(color = C.Mint, radius = fillR)

        val t = p.coerceIn(0f, 1f)
        if (t > 0f) {
            val w = this.size.width
            val h = this.size.height
            val a = Offset(w * 0.29f, h * 0.53f)
            val b = Offset(w * 0.44f, h * 0.68f)
            val c = Offset(w * 0.72f, h * 0.36f)
            val first = (t * 2f).coerceIn(0f, 1f)
            val second = ((t - 0.5f) * 2f).coerceIn(0f, 1f)
            val sw = 3.2.dp.toPx()
            drawLine(color = C.OnMint, start = a, end = lerp(a, b, first), strokeWidth = sw, cap = StrokeCap.Round)
            if (second > 0f) drawLine(color = C.OnMint, start = b, end = lerp(b, c, second), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

/** Auswahl-Chip, Farbe wechselt weich. */
@Composable
fun Chip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    hPad: Dp = 14.dp,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (selected) C.Accent else C.CardHi, tween(220), label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else C.Muted, tween(220), label = "chipFg")
    Box(
        modifier
            .pressable(onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = hPad, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) { T(label, Ty.Label, fg, maxLines = 1) }
}

@Composable
fun SectionTitle(text: String) {
    T(text.uppercase(Locale.GERMAN), Ty.Section, C.Muted, Modifier.padding(start = 4.dp, bottom = 10.dp))
}

fun dayLetter(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.GERMAN)
