package de.raphael.macro.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Sicherung und Wiederherstellung als JSON-Datei über den Android-Dateidialog (z. B. nach Drive).
 * Enthält alle Lebensmittel, alle Einträge und die Einstellungen. Die Datei ist unverschlüsselt.
 * Einlesen ergänzt nur: Vorhandenes bleibt. Einstellungen kommen nur zurück, wenn hier noch nichts eingetragen ist.
 */
object Backup {
    private const val FORMAT = 1

    data class Result(val foods: Int, val entries: Int, val settings: Boolean)

    fun fileName(): String = "macro-sicherung-${LocalDate.now()}.json"

    fun write(ctx: Context, uri: Uri) {
        val db = Db.get(ctx)
        val root = JSONObject()
            .put("app", "macro")
            .put("format", FORMAT)
            .put("exportedAt", System.currentTimeMillis())
        val s = Prefs.load(ctx)
        val set = JSONObject()
            .put("focus", s.focus.id)
            .put("weightKg", s.weightKg.toDouble())
            .put("maintenance", s.maintenance)
            .put("haptics", s.haptics)
            .put("fromHL", s.fromHL)
            .put("toHC", s.toHC)
        val pct = JSONObject()
        val prot = JSONObject()
        Focus.values().forEach {
            pct.put(it.id, s.pctOf(it))
            prot.put(it.id, s.protOf(it).toDouble())
        }
        root.put("settings", set.put("pct", pct).put("prot", prot))
        val foods = JSONArray()
        db.foods().forEach {
            foods.put(
                JSONObject().put("id", it.id).put("name", it.name).put("kcal", it.kcal.toDouble()).put("p", it.p.toDouble())
                    .put("c", it.c.toDouble()).put("f", it.f.toDouble()).put("portion", it.portion.toDouble())
                    .put("fav", it.fav).put("lastUsed", it.lastUsed).put("barcode", it.barcode ?: JSONObject.NULL)
            )
        }
        root.put("foods", foods)
        val entries = JSONArray()
        db.allEntries().forEach {
            entries.put(
                JSONObject().put("day", it.day.toString()).put("ts", it.ts).put("foodId", it.foodId).put("name", it.name)
                    .put("grams", it.grams.toDouble()).put("kcal", it.kcal.toDouble()).put("p", it.p.toDouble())
                    .put("c", it.c.toDouble()).put("f", it.f.toDouble())
            )
        }
        root.put("entries", entries)
        val out = ctx.contentResolver.openOutputStream(uri, "wt") ?: error("Datei nicht beschreibbar")
        out.use { it.write(root.toString(1).toByteArray(Charsets.UTF_8)) }
        Prefs.saveLastBackup(ctx, System.currentTimeMillis())
    }

    fun read(ctx: Context, uri: Uri): Result {
        val text = (ctx.contentResolver.openInputStream(uri) ?: error("Datei nicht lesbar")).use { String(it.readBytes(), Charsets.UTF_8) }
        val root = JSONObject(text)
        if (root.optString("app") != "macro") error("Das ist keine Macro-Sicherung")
        if (root.optInt("format", 0) > FORMAT) error("Die Sicherung ist von einer neueren Version")

        val foods = ArrayList<Food>()
        val fa = root.optJSONArray("foods") ?: JSONArray()
        for (i in 0 until fa.length()) {
            val o = fa.getJSONObject(i)
            foods.add(
                Food(
                    id = o.getLong("id"), name = o.getString("name"), kcal = o.getDouble("kcal").toFloat(),
                    p = o.getDouble("p").toFloat(), c = o.getDouble("c").toFloat(), f = o.getDouble("f").toFloat(),
                    portion = o.optDouble("portion", 0.0).toFloat(), fav = o.optBoolean("fav", false),
                    lastUsed = o.optLong("lastUsed", 0L), barcode = if (o.isNull("barcode")) null else o.optString("barcode"),
                )
            )
        }
        val entries = ArrayList<Entry>()
        val ea = root.optJSONArray("entries") ?: JSONArray()
        for (i in 0 until ea.length()) {
            val o = ea.getJSONObject(i)
            val day = runCatching { LocalDate.parse(o.getString("day")) }.getOrNull() ?: continue
            entries.add(
                Entry(
                    id = 0L, day = day, ts = o.getLong("ts"), foodId = o.getLong("foodId"), name = o.getString("name"),
                    grams = o.getDouble("grams").toFloat(), kcal = o.getDouble("kcal").toFloat(), p = o.getDouble("p").toFloat(),
                    c = o.getDouble("c").toFloat(), f = o.getDouble("f").toFloat(),
                )
            )
        }
        val (nf, ne) = Db.get(ctx).importBackup(foods, entries)

        // Einstellungen nur auf einem frischen Gerät zurückholen (Gewicht und Erhalt noch leer)
        var restored = false
        val so = root.optJSONObject("settings")
        val cur = Prefs.load(ctx)
        if (so != null && cur.weightKg <= 0f && cur.maintenance <= 0) {
            val pct = so.optJSONObject("pct")
            val prot = so.optJSONObject("prot")
            val d = Settings()
            val s = Settings(
                focus = Focus.values().firstOrNull { it.id == so.optString("focus") } ?: d.focus,
                weightKg = so.optDouble("weightKg", 0.0).toFloat(),
                maintenance = so.optInt("maintenance", 0),
                pct = Focus.values().associateWith { pct?.optInt(it.id, it.pct) ?: it.pct },
                prot = Focus.values().associateWith { prot?.optDouble(it.id, it.prot.toDouble())?.toFloat() ?: it.prot },
                haptics = so.optBoolean("haptics", d.haptics),
                fromHL = so.optBoolean("fromHL", d.fromHL),
                // Health Connect braucht auf dem Gerät eine eigene Zustimmung: nie aus der Sicherung einschalten
                toHC = cur.toHC,
            )
            Prefs.save(ctx, s)
            restored = true
        }
        return Result(nf, ne, restored)
    }
}
