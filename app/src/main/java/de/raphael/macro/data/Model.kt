package de.raphael.macro.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.util.concurrent.Executors

/** Summe von Kalorien und Makros */
data class Sum(val kcal: Float = 0f, val p: Float = 0f, val c: Float = 0f, val f: Float = 0f) {
    operator fun plus(o: Sum) = Sum(kcal + o.kcal, p + o.p, c + o.c, f + o.f)
}

fun Entry.sum() = Sum(kcal, p, c, f)

/** Werte für eine Menge in Gramm */
fun Food.sumFor(grams: Float): Sum {
    val k = grams / 100f
    return Sum(kcal * k, p * k, c * k, f * k)
}

/**
 * Hält alle Daten im Speicher (klein) und schreibt Änderungen im Hintergrund in die Datenbank.
 * Die Oberfläche reagiert sofort, ohne auf die Datenbank zu warten → flüssig.
 */
class Model(context: Context) {
    private val ctx = context.applicationContext
    private val db = Db.get(ctx)
    private val io = Executors.newSingleThreadExecutor()

    var foods by mutableStateOf(db.foods())
        private set
    /** Einträge der letzten 120 Tage */
    var entries by mutableStateOf(db.entries(LocalDate.now().minusDays(120)))
        private set
    var settings by mutableStateOf(Prefs.load(ctx))
        private set
    var today by mutableStateOf(LocalDate.now())
        private set

    fun refreshToday() {
        val n = LocalDate.now()
        if (n != today) today = n
    }

    fun update(f: (Settings) -> Settings) {
        val s = f(settings)
        settings = s
        io.execute { Prefs.save(ctx, s) }
    }

    val targets: Targets? get() = Prefs.targets(settings)

    fun entriesOf(day: LocalDate): List<Entry> = entries.filter { it.day == day }

    fun totalOf(day: LocalDate): Sum = entriesOf(day).fold(Sum()) { a, e -> a + e.sum() }

    /** Favoriten zuerst nach Name */
    val favorites: List<Food> get() = foods.filter { it.fav }

    /** Zuletzt benutzte Lebensmittel, neueste zuerst */
    fun recent(limit: Int = 8): List<Food> = foods.filter { it.lastUsed > 0L }.sortedByDescending { it.lastUsed }.take(limit)

    fun foodById(id: Long): Food? = foods.firstOrNull { it.id == id }

    fun addEntry(food: Food, grams: Float, day: LocalDate = today) {
        val s = food.sumFor(grams)
        val ts = System.currentTimeMillis()
        val id = db.addEntry(day, ts, food.id, food.name, grams, s.kcal, s.p, s.c, s.f)
        if (id > 0) entries = entries + Entry(id, day, ts, food.id, food.name, grams, s.kcal, s.p, s.c, s.f)
        foods = foods.map { if (it.id == food.id) it.copy(lastUsed = ts) else it }
        io.execute { db.touch(food.id, ts) }
    }

    fun changeGrams(entry: Entry, grams: Float) {
        val food = foodById(entry.foodId)
        // Ohne zugehöriges Lebensmittel (gelöscht): Werte anteilig umrechnen
        val k = if (entry.grams > 0f) grams / entry.grams else 1f
        val s = food?.sumFor(grams) ?: Sum(entry.kcal * k, entry.p * k, entry.c * k, entry.f * k)
        entries = entries.map { if (it.id == entry.id) it.copy(grams = grams, kcal = s.kcal, p = s.p, c = s.c, f = s.f) else it }
        io.execute { db.updateEntry(entry.id, grams, s.kcal, s.p, s.c, s.f) }
    }

    fun deleteEntry(id: Long) {
        entries = entries.filter { it.id != id }
        io.execute { db.deleteEntry(id) }
    }

    fun addFood(name: String, kcal: Float, p: Float, c: Float, f: Float, portion: Float): Food? {
        val id = db.addFood(name, kcal, p, c, f, portion, null)
        if (id <= 0) return null
        val food = Food(id, name, kcal, p, c, f, portion, false, 0L, null)
        foods = (foods + food).sortedBy { it.name.lowercase() }
        return food
    }

    fun editFood(id: Long, name: String, kcal: Float, p: Float, c: Float, f: Float, portion: Float) {
        foods = foods.map { if (it.id == id) it.copy(name = name, kcal = kcal, p = p, c = c, f = f, portion = portion) else it }
            .sortedBy { it.name.lowercase() }
        io.execute { db.updateFood(id, name, kcal, p, c, f, portion) }
    }

    fun deleteFood(id: Long) {
        foods = foods.filter { it.id != id }
        io.execute { db.deleteFood(id) }
    }

    fun toggleFav(food: Food) {
        val v = !food.fav
        foods = foods.map { if (it.id == food.id) it.copy(fav = v) else it }
        io.execute { db.setFav(food.id, v) }
    }
}
