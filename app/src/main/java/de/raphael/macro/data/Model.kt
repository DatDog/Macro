package de.raphael.macro.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/** Summe von Kalorien und Makros */
data class Sum(val kcal: Float = 0f, val p: Float = 0f, val c: Float = 0f, val f: Float = 0f) {
    operator fun plus(o: Sum) = Sum(kcal + o.kcal, p + o.p, c + o.c, f + o.f)
}

fun Entry.sum() = Sum(kcal, p, c, f)

/** Einträge aus dem Kochbuch haben keine Lebensmittel-ID, ihre Menge zählt in Portionen statt Gramm. */
const val RECIPE_FOOD_ID = -1L
val Entry.isPortions: Boolean get() = foodId == RECIPE_FOOD_ID

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
    /** Zuletzt von Health Local geholte Werte und Stand der Verbindung */
    var hl by mutableStateOf(Prefs.loadHl(ctx))
        private set
    var hlState by mutableStateOf(HlState.UNKNOWN)
        private set
    /** Rezepte aus dem Kochbuch (Nährwerte pro Portion) und Stand der Verbindung */
    var recipes by mutableStateOf<List<KbRecipe>>(emptyList())
        private set
    var kbState by mutableStateOf(KbState.UNKNOWN)
        private set
    /** Health Connect (nur Schreiben): Stand der Verbindung */
    var hcState by mutableStateOf(HcState.UNKNOWN)
        private set
    /** Zeitpunkt der letzten Sicherung (ms, 0 = noch nie) und Meldung zur letzten Sicherung/Wiederherstellung */
    var lastBackup by mutableStateOf(Prefs.lastBackup(ctx))
        private set
    var backupMsg by mutableStateOf<String?>(null)
        private set

    fun refreshToday() {
        val n = LocalDate.now()
        if (n != today) today = n
    }

    /** Holt Gewicht und Tagesverbrauch von Health Local (im Hintergrund), wenn die Übernahme an ist. */
    fun refreshHealthLocal() {
        if (!settings.fromHL) return
        io.execute {
            val r = Bridge.read(ctx)
            hlState = r.state
            val v = r.values
            if (v != null && r.state == HlState.OK) {
                hl = v
                Prefs.saveHl(ctx, v)
            }
        }
    }

    /** Prüft Health Connect (Installation, Berechtigung) und gleicht danach ab, wenn das Schreiben an ist. */
    fun refreshHc() {
        io.execute {
            val st = Hc.state(ctx)
            hcState = st
            if (st == HcState.OK && settings.toHC && !Hc.sync(ctx, entries)) hcState = HcState.ERROR
        }
    }

    /** Nach jeder Änderung an Einträgen: Health Connect nachziehen (im Hintergrund, nur wenn an und erlaubt). */
    private fun hcSync() {
        if (!settings.toHC || hcState != HcState.OK) return
        val snap = entries
        io.execute { if (!Hc.sync(ctx, snap)) hcState = HcState.ERROR }
    }

    fun setToHc(on: Boolean) {
        update { it.copy(toHC = on) }
        if (on) refreshHc()
    }

    /** Alles neu aus der Datenbank laden (nach dem Einlesen einer Sicherung). */
    private fun reload() {
        foods = db.foods()
        entries = db.entries(LocalDate.now().minusDays(120))
        settings = Prefs.load(ctx)
    }

    /** Sicherung in die vom Nutzer gewählte Datei schreiben (im Hintergrund). */
    fun backupTo(uri: Uri) {
        backupMsg = "Sicherung läuft …"
        io.execute {
            backupMsg = try {
                Backup.write(ctx, uri)
                lastBackup = Prefs.lastBackup(ctx)
                "Sicherung gespeichert."
            } catch (e: Exception) {
                "Sicherung fehlgeschlagen: " + (e.message ?: "unbekannter Fehler")
            }
        }
    }

    /** Sicherung einlesen (ergänzt nur, Vorhandenes bleibt). */
    fun restoreFrom(uri: Uri) {
        backupMsg = "Einlesen läuft …"
        io.execute {
            backupMsg = try {
                val r = Backup.read(ctx, uri)
                reload()
                hcSync()
                "Eingelesen: ${r.foods} neue Lebensmittel, ${r.entries} neue Einträge" +
                    (if (r.settings) ", Einstellungen übernommen." else ".")
            } catch (e: Exception) {
                "Einlesen fehlgeschlagen: " + (e.message ?: "unbekannter Fehler")
            }
        }
    }

    /** Seit dem letzten Kochen von diesem Rezept gegessene Portionen (Summe der Einträge). */
    fun eatenOf(r: KbRecipe): Float =
        if (r.cookedAt <= 0L) 0f
        else entries.filter { it.isPortions && it.name == r.title && it.ts >= r.cookedAt }.sumOf { it.grams.toDouble() }.toFloat()

    /**
     * Nährwerte pro Portion. In den 3 Tagen nach dem Kochen gelten die Werte des letzten Kochens
     * (bei eigener Portionszahl umgerechnet, z. B. größere Wraps), sonst die Standardwerte des Rezepts.
     */
    fun perPortion(r: KbRecipe): Sum {
        val recent = r.cookedAt > 0L && System.currentTimeMillis() - r.cookedAt < 3 * 24 * 3600_000L
        return (if (recent) r.cook else null) ?: Sum(r.kcal, r.p, r.c, r.f)
    }

    /** Gekochte Portionen (beim Kochen gewählt, sonst die des Rezepts). */
    fun cookedOf(r: KbRecipe): Int = if (r.cookedServings > 0) r.cookedServings else r.servings

    /** Noch übrige Portionen vom letzten Kochen, nie unter 0. */
    fun leftOf(r: KbRecipe): Float = (cookedOf(r) - eatenOf(r)).coerceAtLeast(0f)

    /** Holt die Rezepte aus dem Kochbuch (im Hintergrund). */
    fun refreshKochbuch() {
        io.execute {
            val r = KochbuchBridge.read(ctx)
            kbState = r.state
            if (r.state == KbState.OK) recipes = r.recipes
        }
    }

    /**
     * Rezept essen: [portions] Portionen. Der Eintrag bekommt food_id [RECIPE_FOOD_ID], die Menge steht in Portionen.
     * Werte werden wie bei Lebensmitteln im Moment des Eintrags festgehalten.
     */
    fun addRecipe(r: KbRecipe, portions: Float, day: LocalDate = today) {
        val ts = System.currentTimeMillis()
        val pp = perPortion(r)
        val s = Sum(pp.kcal * portions, pp.p * portions, pp.c * portions, pp.f * portions)
        val id = db.addEntry(day, ts, RECIPE_FOOD_ID, r.title, portions, s.kcal, s.p, s.c, s.f)
        if (id > 0) entries = entries + Entry(id, day, ts, RECIPE_FOOD_ID, r.title, portions, s.kcal, s.p, s.c, s.f)
        hcSync()
    }

    /** Einstellungen mit den Werten aus Health Local (wenn an und vorhanden), sonst die von Hand eingetragenen. */
    val effective: Settings
        get() {
            val h = hl
            if (!settings.fromHL || h == null) return settings
            return settings.copy(
                weightKg = h.weightKg ?: settings.weightKg,
                maintenance = h.tdee?.let { (it / 10f).roundToInt() * 10 } ?: settings.maintenance,
            )
        }

    fun update(f: (Settings) -> Settings) {
        val s = f(settings)
        settings = s
        io.execute { Prefs.save(ctx, s) }
    }

    val targets: Targets? get() = Prefs.targets(effective)

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
        hcSync()
    }

    fun changeGrams(entry: Entry, grams: Float) {
        val food = foodById(entry.foodId)
        // Ohne zugehöriges Lebensmittel (gelöscht): Werte anteilig umrechnen
        val k = if (entry.grams > 0f) grams / entry.grams else 1f
        val s = food?.sumFor(grams) ?: Sum(entry.kcal * k, entry.p * k, entry.c * k, entry.f * k)
        entries = entries.map { if (it.id == entry.id) it.copy(grams = grams, kcal = s.kcal, p = s.p, c = s.c, f = s.f) else it }
        io.execute { db.updateEntry(entry.id, grams, s.kcal, s.p, s.c, s.f) }
        hcSync()
    }

    fun deleteEntry(id: Long) {
        entries = entries.filter { it.id != id }
        io.execute { db.deleteEntry(id); Hc.markDeleted(ctx, id) }
        hcSync()
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
