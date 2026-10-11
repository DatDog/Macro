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
import de.raphael.macro.data.KbRecipe
import de.raphael.macro.data.KbState
import de.raphael.macro.data.Model
import de.raphael.macro.data.isPortions
import de.raphael.macro.data.sumFor
import kotlin.math.abs

/** Schritte im Hinzufügen-Bildschirm */
private sealed interface Step {
    data object Pick : Step
    data class Amount(val foodId: Long) : Step
    data class Portions(val recipeId: String) : Step
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
                is Step.Pick -> PickStep(
                    model, onClose,
                    onPick = { step = Step.Amount(it.id) },
                    onNew = { step = Step.Form(null) },
                    onRecipe = { step = Step.Portions(it.id) },
                )
                is Step.Portions -> {
                    val r = model.recipes.firstOrNull { it.id == st.recipeId }
                    if (r != null) {
                        PortionsStep(model, r, onBack = { step = Step.Pick }, onDone = onClose)
                    } else {
                        LaunchedEffect(Unit) { step = Step.Pick }
                    }
                }
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
private fun PickStep(
    model: Model, onClose: () -> Unit, onPick: (Food) -> Unit, onNew: () -> Unit, onRecipe: (KbRecipe) -> Unit,
) {
    val view = LocalView.current
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    val q = query.trim().lowercase()
    // Frisch gekocht (letzte 18 Stunden) und heute noch nicht eingetragen → Vorschlag, nie automatisch
    val now = System.currentTimeMillis()
    val fresh = model.recipes
        .filter { it.cookedAt > 0L && now - it.cookedAt < 18 * 3600 * 1000L }
        .filter { r -> model.entries.none { it.isPortions && it.name == r.title && it.ts >= r.cookedAt } }
        .maxByOrNull { it.cookedAt }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        TopRow("Hinzufügen", "Schließen", onClose)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Lebensmittel", selected = tab == 0) { tab = 0 }
            Chip("Kochbuch", selected = tab == 1) { tab = 1 }
        }
        Spacer(Modifier.height(12.dp))
        if (fresh != null) {
            Panel(Modifier.fillMaxWidth(), onClick = { onRecipe(fresh) }) {
                T("GERADE GEKOCHT", Ty.Section, C.Mint)
                Spacer(Modifier.height(4.dp))
                T(fresh.title, Ty.Heading, maxLines = 1)
                T("${fmt(fresh.kcal)} kcal pro Portion · Tippen zum Eintragen", Ty.Small, C.Muted)
            }
            Spacer(Modifier.height(12.dp))
        }
        Field("Suchen", query, { query = it }, Modifier.fillMaxWidth(), number = false)
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (tab == 1) {
                RecipeList(model, q, onRecipe)
            } else if (q.isNotEmpty()) {
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
        if (tab == 0) {
            Spacer(Modifier.height(10.dp))
            BigButton("Neues Lebensmittel", color = C.CardHi, onClick = onNew)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RecipeList(model: Model, q: String, onRecipe: (KbRecipe) -> Unit) {
    when (model.kbState) {
        KbState.NO_APP -> T("Kochbuch nicht gefunden oder zu alt. Installiere die neueste Version des Kochbuchs.", Ty.Body, C.Muted, Modifier.padding(start = 4.dp))
        KbState.DENIED -> T("Keine Berechtigung. Beide Apps müssen mit demselben Schlüssel signiert sein.", Ty.Body, C.Fat, Modifier.padding(start = 4.dp))
        KbState.EMPTY -> T("Das Kochbuch hat noch keine Rezepte mit Nährwerten.", Ty.Body, C.Muted, Modifier.padding(start = 4.dp))
        KbState.UNKNOWN -> T("Lade Rezepte …", Ty.Body, C.Muted, Modifier.padding(start = 4.dp))
        KbState.OK -> {
            if (q.isNotEmpty()) {
                val hits = model.recipes.filter { it.title.lowercase().contains(q) || it.category.lowercase().contains(q) }
                if (hits.isEmpty()) T("Kein Rezept gefunden.", Ty.Body, C.Muted, Modifier.padding(start = 4.dp))
                hits.forEach { RecipeRow(it, onRecipe) }
            } else {
                val cooked = model.recipes.filter { it.cookedAt > 0L }.sortedByDescending { it.cookedAt }.take(8)
                if (cooked.isNotEmpty()) {
                    SectionTitle("Zuletzt gekocht")
                    cooked.forEach { RecipeRow(it, onRecipe) }
                    Spacer(Modifier.height(14.dp))
                }
                SectionTitle("Alle Rezepte")
                model.recipes.forEach { RecipeRow(it, onRecipe) }
            }
        }
    }
}

@Composable
private fun RecipeRow(r: KbRecipe, onRecipe: (KbRecipe) -> Unit) {
    Box(Modifier.padding(bottom = 8.dp)) {
        Panel(onClick = { onRecipe(r) }) {
            T(r.title, Ty.Heading, maxLines = 1)
            T("${fmt(r.kcal)} kcal · ${fmtG(r.p)} g Eiweiß pro Portion", Ty.Small, C.Muted, maxLines = 1)
        }
    }
}

@Composable
private fun PortionsStep(model: Model, r: KbRecipe, onBack: () -> Unit, onDone: () -> Unit) {
    val view = LocalView.current
    var n by remember(r.id) { mutableStateOf("1") }
    val portions = parseNum(n)
    val ok = portions != null && portions > 0f && portions <= 20f
    val k = if (ok) portions!! else 0f

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        TopRow(r.title, "Zurück", onBack)
        T("Werte pro Portion, vom Kochbuch geschätzt. Das Rezept ergibt ${r.servings} Portionen.", Ty.Small, C.Muted)
        Spacer(Modifier.height(18.dp))
        Field("Portionen", n, { n = it }, Modifier.fillMaxWidth(), suffix = "Portionen")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("0,5", "1", "1,5", "2", "3").forEach { v -> Chip(v, selected = false) { n = v } }
        }
        Spacer(Modifier.height(20.dp))
        Panel {
            T(if (ok) "${fmt(r.kcal * k)} kcal" else "–", Ty.Title)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                T("Eiweiß ${if (ok) fmt(r.p * k, 1) else "–"} g", Ty.Body, C.Prot)
                T("KH ${if (ok) fmt(r.c * k, 1) else "–"} g", Ty.Body, C.Carb)
                T("Fett ${if (ok) fmt(r.f * k, 1) else "–"} g", Ty.Body, C.Fat)
            }
        }
        Spacer(Modifier.height(20.dp))
        BigButton("Hinzufügen", enabled = ok) {
            if (ok) {
                if (model.settings.haptics) view.tickOn()
                model.addRecipe(r, k)
                onDone()
            }
        }
        Spacer(Modifier.height(24.dp))
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
