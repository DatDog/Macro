package de.raphael.macro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import de.raphael.macro.data.Entry
import de.raphael.macro.data.Focus
import de.raphael.macro.data.isPortions
import de.raphael.macro.data.Model
import de.raphael.macro.data.Sum
import de.raphael.macro.data.Targets
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun TodayScreen(model: Model, onAdd: () -> Unit, onSetup: () -> Unit) {
    val view = LocalView.current
    val s = model.settings
    val today = model.today
    val total = model.totalOf(today)
    val list = model.entriesOf(today).sortedByDescending { it.ts }
    val t = model.targets
    var openId by remember { mutableStateOf(-1L) }

    val top: @Composable () -> Unit = {
        Spacer(Modifier.height(20.dp))
        Enter(0) { Header(today) }
        Spacer(Modifier.height(14.dp))
        Enter(1) {
            // Fokus-Schalter
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Focus.values().forEach { f ->
                    Chip(f.chip, selected = s.focus == f) {
                        if (s.focus != f) {
                            if (s.haptics) view.tickOff()
                            model.update { it.copy(focus = f) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Enter(2) { Hero(total, t, s.focus, onSetup) }
        Spacer(Modifier.height(14.dp))
        Enter(3) { BigButton("Mahlzeit hinzufügen", onClick = onAdd) }
    }
    val eaten: @Composable () -> Unit = {
        Enter(4) { SectionTitle("Heute gegessen") }
        if (list.isEmpty()) {
            T("Noch nichts eingetragen.", Ty.Body, C.Muted, Modifier.padding(start = 4.dp))
        }
        list.forEach { e ->
            Box(Modifier.padding(bottom = 10.dp)) {
                EntryRow(model, e, open = openId == e.id) { openId = if (openId == e.id) -1L else e.id }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 640.dp) {
            // Aufgeklappt: links Ziele und Hinzufügen, rechts die Liste, jede Spalte scrollt für sich
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    top()
                    Spacer(Modifier.height(24.dp))
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(20.dp))
                    eaten()
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                top()
                Spacer(Modifier.height(26.dp))
                eaten()
            }
        }
    }
}

@Composable
private fun Header(today: LocalDate) {
    Column {
        T("Heute", Ty.Title)
        T(today.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)), Ty.Body, C.Muted)
    }
}

@Composable
private fun Hero(total: Sum, t: Targets?, focus: Focus, onSetup: () -> Unit) {
    Panel {
        if (t == null) {
            T("Ziele einrichten", Ty.Heading)
            Spacer(Modifier.height(8.dp))
            T(
                "Schalte unter „Mehr“ die Übernahme aus Health Local ein oder trage Gewicht und Erhaltungskalorien von Hand ein. Dann zeigt Macro die Ziele für „${focus.label}“.",
                Ty.Small, C.Muted,
            )
            Spacer(Modifier.height(14.dp))
            BigButton("Zu den Einstellungen", onClick = onSetup)
            Spacer(Modifier.height(18.dp))
            MacroRow("Kalorien", total.kcal, null, C.Accent, unit = "kcal")
            Spacer(Modifier.height(12.dp))
            MacroRow("Eiweiß", total.p, null, C.Prot, strong = true)
            Spacer(Modifier.height(12.dp))
            MacroRow("Kohlenhydrate", total.c, null, C.Carb)
            Spacer(Modifier.height(12.dp))
            MacroRow("Fett", total.f, null, C.Fat)
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { KcalRing(total.kcal, t.kcal) }
            Spacer(Modifier.height(18.dp))
            MacroRow("Eiweiß", total.p, t.protein, C.Prot, strong = true)
            Spacer(Modifier.height(12.dp))
            MacroRow("Kohlenhydrate", total.c, t.carbs, C.Carb)
            Spacer(Modifier.height(12.dp))
            MacroRow("Fett", total.f, t.fat, C.Fat)
        }
    }
}

@Composable
private fun KcalRing(eaten: Float, target: Int) {
    val frac = if (target > 0) eaten / target else 0f
    val p by animateFloatAsState(frac.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "ring")
    val left = target - eaten
    val over = left < 0f
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 14.dp.toPx()
            val topLeft = Offset(sw / 2f, sw / 2f)
            val sz = Size(size.width - sw, size.height - sw)
            drawArc(
                color = C.CardHi, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = topLeft, size = sz, style = Stroke(sw, cap = StrokeCap.Round),
            )
            if (p > 0f) {
                drawArc(
                    color = if (over) C.Warn else C.Accent, startAngle = 135f, sweepAngle = 270f * p, useCenter = false,
                    topLeft = topLeft, size = sz, style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(fmt(abs(left)), Ty.Title, if (over) C.Warn else C.Text)
            T(if (over) "kcal drüber" else "kcal übrig", Ty.Small, C.Muted)
            Spacer(Modifier.height(4.dp))
            T("${fmt(eaten)} von $target", Ty.Label, C.Muted)
        }
    }
}

@Composable
private fun MacroRow(name: String, got: Float, target: Int?, color: Color, strong: Boolean = false, unit: String = "g") {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T(name, if (strong) Ty.Heading else Ty.Body)
            T(if (target != null) "${fmt(got)} / $target $unit" else "${fmt(got)} $unit", Ty.Body, C.Muted)
        }
        Spacer(Modifier.height(6.dp))
        Bar(if (target != null && target > 0) got / target else 0f, color, if (strong) 10.dp else 8.dp)
    }
}

@Composable
private fun EntryRow(model: Model, e: Entry, open: Boolean, onToggle: () -> Unit) {
    val view = LocalView.current
    val time = java.time.Instant.ofEpochMilli(e.ts).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    Panel(onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T(e.name, Ty.Heading, maxLines = 1)
                T("$time · ${amountText(e)} · ${fmt(e.p)} g Eiweiß", Ty.Small, C.Muted)
            }
            Spacer(Modifier.width(10.dp))
            T("${fmt(e.kcal)} kcal", Ty.Body, C.Muted)
        }
        AnimatedVisibility(open) {
            var g by remember(e.id) { mutableStateOf(fmtG(e.grams)) }
            Column(Modifier.padding(top = 14.dp)) {
                T(
                    "Kohlenhydrate ${fmt(e.c)} g · Fett ${fmt(e.f)} g",
                    Ty.Small, C.Muted, Modifier.padding(bottom = 12.dp),
                )
                Field(if (e.isPortions) "Portionen" else "Menge", g, { g = it }, Modifier.fillMaxWidth(), suffix = if (e.isPortions) "Portionen" else "g")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val v = parseNum(g)
                    Box(Modifier.weight(1f)) {
                        BigButton("Speichern", enabled = v != null && v > 0f) {
                            if (v != null && v > 0f) {
                                if (model.settings.haptics) view.tickOn()
                                model.changeGrams(e, v)
                                onToggle()
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        BigButton("Löschen", color = C.Warn) {
                            if (model.settings.haptics) view.tickOff()
                            model.deleteEntry(e.id)
                        }
                    }
                }
            }
        }
    }
}

private fun amountText(e: Entry): String =
    if (e.isPortions) "${fmtG(e.grams)} ${if (e.grams == 1f) "Portion" else "Portionen"}" else "${fmtG(e.grams)} g"
