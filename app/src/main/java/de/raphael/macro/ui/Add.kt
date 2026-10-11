package de.raphael.macro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raphael.macro.data.Food
import de.raphael.macro.data.Model
import de.raphael.macro.data.sumFor
import kotlin.math.abs

/** Schritte im Hinzufügen-Bildschirm */
private sealed interface Step {
    data object Pick : Step
    data class Amount(val foodId: Long) : Step
    data class Form(val foodId: Long?) : Step
}

@Composable
fun AddScreen(model: Model, onClose: () -> Unit) {
    var step by remember { mutableStateOf<Step>(Step.Pick) }
    // Eine Ebene zurück, solange man nicht in der Auswahl ist (hat Vorrang vor dem Schließen)
    BackHandler(enabled = step !is Step.Pick) { step = Step.Pick }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .imePadding()
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120))) },
            label = "addStep",
        ) { st ->
            when (st) {
                is Step.Pick -> PickStep(model, onClose, onPick = { step = Step.Amount(it.id) }, onNew = { step = Step.Form(null) })
                is Step.Amount -> {
                    val food = model.foodById(st.foodId)
                    if (food != null) {
                        AmountStep(
                            model, food, onBack = { step = Step.Pick }, onDone = onClose,
                            onEdit = { step = Step.Form(food.id) },
                        )
                    } else {
                        LaunchedEffect(Unit) { step = Step.Pick }
                    }
                }
                is Step.Form -> FormStep(
                    model, st.foodId?.let { model.foodById(it) },
                    onBack = { step = if (st.foodId != null) Step.Amount(st.foodId) else Step.Pick },
                    onSaved = { id -> step = Step.Amount(id) },
                    onDeleted = { step = Step.Pick },
                )
            }
        }
    }
}

@Composable
private fun TopRow(title: String, backLabel: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(title, Ty.Title.copy(fontSize = 28.sp), modifier = Modifier.weight(1f), maxLines = 1)
        Box(Modifier.pressable(onBack).padding(horizontal = 10.dp, vertical = 8.dp)) {
            T(backLabel, Ty.Body, C.Accent)
        }
    }
}

@Composable
private fun PickStep(model: Model, onClose: () -> Unit, onPick: (Food) -> Unit, onNew: () -> Unit) {
    val view = LocalView.current
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        TopRow("Hinzufügen", "Schließen", onClose)
        Field("Suchen", query, { query = it }, Modifier.fillMaxWidth(), number = false)
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (q.isNotEmpty()) {
                val hits = model.foods.filter { it.name.lowercase().contains(q) }
                if (hits.isEmpty()) T("Nichts gefunden. Lege das Lebensmittel neu an.", Ty.Body, C.Muted, Modifier.padding(start = 4.dp))
                hits.forEach { FoodRow(model, it, view, onPick) }
            } else {
                val fav = model.favorites
                val recent = model.recent()
                if (fav.isNotEmpty()) {
                    SectionTitle("Favoriten")
                    fav.forEach { FoodRow(model, it, view, onPick) }
                    Spacer(Modifier.height(14.dp))
                }
                if (recent.isNotEmpty()) {
                    SectionTitle("Zuletzt gegessen")
                    recent.forEach { FoodRow(model, it, view, onPick) }
                    Spacer(Modifier.height(14.dp))
                }
                SectionTitle("Alle Lebensmittel")
                model.foods.forEach { FoodRow(model, it, view, onPick) }
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(10.dp))
        BigButton("Neues Lebensmittel", color = C.CardHi, onClick = onNew)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun FoodRow(model: Model, food: Food, view: android.view.View, onPick: (Food) -> Unit) {
    Box(Modifier.padding(bottom = 8.dp)) {
        Panel(onClick = { onPick(food) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T(food.name, Ty.Heading, maxLines = 1)
                    T(
                        "${fmt(food.kcal)} kcal · ${fmtG(food.p)} g Eiweiß je 100 g",
                        Ty.Small, C.Muted, maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .pressable {
                            if (model.settings.haptics) { if (food.fav) view.tickOff() else view.tickOn() }
                            model.toggleFav(food)
                        }
                        .padding(8.dp)
                ) { T(if (food.fav) "★" else "☆", Ty.Heading, if (food.fav) C.Fat else C.Muted) }
            }
        }
    }
}

@Composable
private fun AmountStep(model: Model, food: Food, onBack: () -> Unit, onDone: () -> Unit, onEdit: () -> Unit) {
    val view = LocalView.current
    var g by remember(food.id) { mutableStateOf(fmtG(if (food.portion > 0f) food.portion else 100f)) }
    val grams = parseNum(g)
    val sum = if (grams != null && grams > 0f) food.sumFor(grams) else null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        TopRow(food.name, "Zurück", onBack)
        T(
            "Je 100 g: ${fmt(food.kcal)} kcal · ${fmtG(food.p)} g Eiweiß · ${fmtG(food.c)} g KH · ${fmtG(food.f)} g Fett",
            Ty.Small, C.Muted,
        )
        Spacer(Modifier.height(18.dp))
        Field("Menge", g, { g = it }, Modifier.fillMaxWidth(), suffix = "g")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (food.portion > 0f) {
                Chip("Portion (${fmtG(food.portion)} g)", selected = false) { g = fmtG(food.portion) }
            }
            listOf(50f, 100f, 150f, 200f, 250f).forEach { v ->
                Chip("${fmt(v)} g", selected = false) { g = fmt(v) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Panel {
            val s = sum
            T(if (s != null) "${fmt(s.kcal)} kcal" else "–", Ty.Title)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                T("Eiweiß ${if (s != null) fmt(s.p, 1) else "–"} g", Ty.Body, C.Prot)
                T("KH ${if (s != null) fmt(s.c, 1) else "–"} g", Ty.Body, C.Carb)
                T("Fett ${if (s != null) fmt(s.f, 1) else "–"} g", Ty.Body, C.Fat)
            }
        }
        Spacer(Modifier.height(20.dp))
        BigButton("Hinzufügen", enabled = sum != null) {
            if (grams != null && grams > 0f) {
                if (model.settings.haptics) view.tickOn()
                model.addEntry(food, grams)
                onDone()
            }
        }
        Spacer(Modifier.height(10.dp))
        BigButton("Lebensmittel bearbeiten", color = C.CardHi, onClick = onEdit)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FormStep(model: Model, food: Food?, onBack: () -> Unit, onSaved: (Long) -> Unit, onDeleted: () -> Unit) {
    val view = LocalView.current
    var name by remember(food?.id) { mutableStateOf(food?.name ?: "") }
    var kcal by remember(food?.id) { mutableStateOf(food?.let { fmtG(it.kcal) } ?: "") }
    var p by remember(food?.id) { mutableStateOf(food?.let { fmtG(it.p) } ?: "") }
    var c by remember(food?.id) { mutableStateOf(food?.let { fmtG(it.c) } ?: "") }
    var f by remember(food?.id) { mutableStateOf(food?.let { fmtG(it.f) } ?: "") }
    var portion by remember(food?.id) { mutableStateOf(food?.let { if (it.portion > 0f) fmtG(it.portion) else "" } ?: "") }
    var confirmDelete by remember(food?.id) { mutableStateOf(false) }

    val vk = parseNum(kcal)
    val vp = parseNum(p) ?: 0f
    val vc = parseNum(c) ?: 0f
    val vf = parseNum(f) ?: 0f
    val vPortion = if (portion.isBlank()) 0f else parseNum(portion)
    val ok = name.isNotBlank() && vk != null && vk >= 0f && vPortion != null
    // Kalorien sollten ungefähr zu den Makros passen (4/4/9 kcal je g)
    val calc = vp * 4f + vc * 4f + vf * 9f
    val mismatch = vk != null && vk > 0f && abs(vk - calc) > 0.2f * vk + 15f

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        TopRow(if (food == null) "Neues Lebensmittel" else "Bearbeiten", "Zurück", onBack)
        T("Alle Werte je 100 g, wie auf der Packung.", Ty.Small, C.Muted)
        Spacer(Modifier.height(14.dp))
        Field("Name", name, { name = it }, Modifier.fillMaxWidth(), number = false)
        Spacer(Modifier.height(10.dp))
        Field("Kalorien", kcal, { kcal = it }, Modifier.fillMaxWidth(), suffix = "kcal")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Eiweiß", p, { p = it }, Modifier.weight(1f), suffix = "g")
            Field("Kohlenhydrate", c, { c = it }, Modifier.weight(1f), suffix = "g")
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Fett", f, { f = it }, Modifier.weight(1f), suffix = "g")
            Field("Portion (optional)", portion, { portion = it }, Modifier.weight(1f), suffix = "g")
        }
        if (mismatch) {
            Spacer(Modifier.height(10.dp))
            T(
                "Die Kalorien passen nicht ganz zu den Makros (die ergeben ca. ${fmt(calc)} kcal). Bitte kurz prüfen.",
                Ty.Small, C.Fat,
            )
        }
        Spacer(Modifier.height(20.dp))
        BigButton("Speichern", enabled = ok) {
            if (ok && vk != null && vPortion != null) {
                if (model.settings.haptics) view.tickOn()
                if (food == null) {
                    val nf = model.addFood(name.trim(), vk, vp, vc, vf, vPortion)
                    if (nf != null) onSaved(nf.id)
                } else {
                    model.editFood(food.id, name.trim(), vk, vp, vc, vf, vPortion)
                    onSaved(food.id)
                }
            }
        }
        if (food != null) {
            Spacer(Modifier.height(10.dp))
            BigButton(if (confirmDelete) "Wirklich löschen? Einträge bleiben erhalten" else "Lebensmittel löschen", color = C.Warn) {
                if (confirmDelete) {
                    if (model.settings.haptics) view.tickOff()
                    model.deleteFood(food.id)
                    onDeleted()
                } else confirmDelete = true
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
