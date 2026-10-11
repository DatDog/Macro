package de.raphael.macro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import de.raphael.macro.data.Model
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(model: Model) {
    val today = model.today
    val t = model.targets
    val days = (0 until 14).map { today.minusDays((13 - it).toLong()) }
    val totals = days.map { model.totalOf(it) }
    // Nur Tage mit Einträgen zählen für Durchschnitte (ein leerer Tag wäre meist vergessen, nicht gefastet)
    val week = (0 until 7).map { today.minusDays(it.toLong()) }.filter { model.entriesOf(it).isNotEmpty() }
    val avgKcal = if (week.isEmpty()) null else week.map { model.totalOf(it).kcal }.average().toFloat()
    val avgProt = if (week.isEmpty()) null else week.map { model.totalOf(it).p }.average().toFloat()
    val protHits = if (t == null) null else week.count { model.totalOf(it).p >= t.protein * 0.95f }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Enter(0) { T("Verlauf", Ty.Title) }
        Spacer(Modifier.height(16.dp))
        Enter(1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    Panel(Modifier.fillMaxWidth()) {
                        T("Ø KALORIEN", Ty.Section, C.Muted)
                        Spacer(Modifier.height(6.dp))
                        T(if (avgKcal != null) fmt(avgKcal) else "–", Ty.Title)
                        T("letzte 7 Tage", Ty.Small, C.Muted)
                    }
                }
                Box(Modifier.weight(1f)) {
                    Panel(Modifier.fillMaxWidth()) {
                        T("Ø EIWEISS", Ty.Section, C.Muted)
                        Spacer(Modifier.height(6.dp))
                        T(if (avgProt != null) "${fmt(avgProt)} g" else "–", Ty.Title)
                        T(
                            if (protHits != null) "Ziel an $protHits von ${week.size} Tagen" else "letzte 7 Tage",
                            Ty.Small, C.Muted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Enter(2) {
            Panel(Modifier.fillMaxWidth()) {
                T("KALORIEN, 14 TAGE", Ty.Section, C.Muted)
                Spacer(Modifier.height(12.dp))
                KcalBars(totals.map { it.kcal }, t?.kcal)
                Spacer(Modifier.height(8.dp))
                if (t != null) T("Linie = Ziel für den aktuellen Fokus", Ty.Label, C.Muted)
            }
        }
        Spacer(Modifier.height(24.dp))
        Enter(3) { SectionTitle("Tage") }
        days.reversed().forEach { d ->
            val sum = model.totalOf(d)
            if (model.entriesOf(d).isNotEmpty()) {
                Box(Modifier.padding(bottom = 10.dp)) {
                    Panel(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            T(d.format(DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN)), Ty.Heading)
                            T("${fmt(sum.kcal)} kcal", Ty.Heading)
                        }
                        Spacer(Modifier.height(4.dp))
                        T(
                            "Eiweiß ${fmt(sum.p)} g · KH ${fmt(sum.c)} g · Fett ${fmt(sum.f)} g",
                            Ty.Small, C.Muted,
                        )
                    }
                }
            }
        }
        if (days.none { model.entriesOf(it).isNotEmpty() }) {
            T("Noch keine Einträge. Sobald du Mahlzeiten einträgst, erscheinen hier deine Tage.", Ty.Body, C.Muted)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun KcalBars(values: List<Float>, target: Int?) {
    val max = maxOf(values.maxOrNull() ?: 0f, (target ?: 0) * 1.15f, 1f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val n = values.size
        val gap = 6.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        values.forEachIndexed { i, v ->
            val h = (v / max) * size.height
            if (h > 0f) {
                val over = target != null && v > target
                drawRoundRect(
                    color = if (over) C.Warn else C.Accent,
                    topLeft = Offset(i * (bw + gap), size.height - h),
                    size = Size(bw, h),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
            } else {
                drawRoundRect(
                    color = C.CardHi,
                    topLeft = Offset(i * (bw + gap), size.height - 3.dp.toPx()),
                    size = Size(bw, 3.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
        if (target != null) {
            val y = size.height - (target / max) * size.height
            drawLine(color = C.Mint, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.5.dp.toPx())
        }
    }
}
