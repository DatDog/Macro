package de.raphael.macro.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Zahl für die Anzeige: ganze Zahl ohne Komma, sonst eine Nachkommastelle, deutsches Komma. */
fun fmt(v: Float, decimals: Int = 0): String =
    String.format(Locale.GERMAN, "%.${decimals}f", v)

fun fmtG(v: Float): String = if (v >= 10f || v == Math.floor(v.toDouble()).toFloat()) fmt(v) else fmt(v, 1)

/** Eingabe mit Komma oder Punkt lesen */
fun parseNum(s: String): Float? = s.trim().replace(',', '.').toFloatOrNull()

/** Zahlenfeld im dunklen Stil. Liefert den Text bei jeder Änderung. */
@Composable
fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    number: Boolean = true,
    suffix: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier,
        label = { T(label, Ty.Label, C.Muted) },
        suffix = if (suffix != null) ({ T(suffix, Ty.Small, C.Muted) }) else null,
        singleLine = singleLine,
        textStyle = Ty.Body.copy(color = C.Text),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.Accent,
            unfocusedBorderColor = C.Line,
            focusedContainerColor = C.Card,
            unfocusedContainerColor = C.Card,
            cursorColor = C.Accent,
            focusedTextColor = C.Text,
            unfocusedTextColor = C.Text,
        ),
    )
}

/** Große Schaltfläche */
@Composable
fun BigButton(label: String, enabled: Boolean = true, color: Color = C.Accent, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .let { if (enabled) it.pressable(onClick) else it }
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) color else C.CardHi)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) { T(label, Ty.Heading, if (enabled) Color.White else C.Muted) }
}

/** Fortschrittsbalken, füllt sich weich. [value] 0 bis 1 (darüber gekappt). */
@Composable
fun Bar(value: Float, color: Color, height: androidx.compose.ui.unit.Dp = 8.dp) {
    val v by animateFloatAsState(value.coerceIn(0f, 1f), tween(520, easing = FastOutSlowInEasing), label = "bar")
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(C.CardHi)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(v)
                .clip(RoundedCornerShape(height))
                .background(color)
        )
    }
}
