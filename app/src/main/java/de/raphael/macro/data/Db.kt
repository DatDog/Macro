package de.raphael.macro.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

/** Lebensmittel mit Werten je 100 g. [portion] = übliche Portion in Gramm (0 = keine). */
data class Food(
    val id: Long,
    val name: String,
    val kcal: Float,
    val p: Float,
    val c: Float,
    val f: Float,
    val portion: Float,
    val fav: Boolean,
    val lastUsed: Long,
    val barcode: String?,
)

/** Gegessene Menge. Die Werte werden beim Eintragen festgehalten, spätere Änderungen am Lebensmittel ändern den Verlauf nicht. */
data class Entry(
    val id: Long,
    val day: LocalDate,
    val ts: Long,
    val foodId: Long,
    val name: String,
    val grams: Float,
    val kcal: Float,
    val p: Float,
    val c: Float,
    val f: Float,
)

/**
 * Lokale Datenbank. Tabellen:
 *  food   Lebensmittel (Werte je 100 g, fav = Favorit, last_used = zuletzt benutzt, Zeit in ms)
 *  entry  Mahlzeiten-Einträge je Tag
 */
class Db private constructor(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "macro.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE food(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, kcal REAL NOT NULL, " +
                "p REAL NOT NULL, c REAL NOT NULL, f REAL NOT NULL, portion REAL NOT NULL DEFAULT 0, " +
                "fav INTEGER NOT NULL DEFAULT 0, last_used INTEGER NOT NULL DEFAULT 0, barcode TEXT)"
        )
        db.execSQL(
            "CREATE TABLE entry(id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, ts INTEGER NOT NULL, " +
                "food_id INTEGER NOT NULL, name TEXT NOT NULL, grams REAL NOT NULL, kcal REAL NOT NULL, " +
                "p REAL NOT NULL, c REAL NOT NULL, f REAL NOT NULL)"
        )
        db.execSQL("CREATE INDEX entry_day ON entry(day)")
        // Startwerte: Richtwerte aus Standardtabellen, bitte mit der eigenen Packung abgleichen
        val seed = listOf(
            arrayOf<Any>("Haferflocken", 370, 13.5, 59, 7, 50),
            arrayOf<Any>("Magerquark", 67, 12, 4, 0.3, 250),
            arrayOf<Any>("Skyr natur", 63, 11, 4, 0.2, 150),
            arrayOf<Any>("Hühnerbrust (roh)", 110, 23, 0, 1.5, 150),
            arrayOf<Any>("Reis (roh)", 350, 7, 78, 0.6, 75),
            arrayOf<Any>("Ei (Größe M)", 140, 12.5, 0.7, 9.5, 55),
            arrayOf<Any>("Banane", 90, 1.2, 20, 0.2, 120),
            arrayOf<Any>("Vollmilch 3,5 %", 64, 3.4, 4.8, 3.6, 200),
            arrayOf<Any>("Olivenöl", 884, 0, 0, 100, 10),
            arrayOf<Any>("Vollkornbrot", 230, 8, 42, 1.5, 50),
        )
        seed.forEach {
            db.execSQL("INSERT INTO food(name, kcal, p, c, f, portion) VALUES(?, ?, ?, ?, ?, ?)", it)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun foods(): List<Food> {
        val out = ArrayList<Food>()
        readableDatabase.rawQuery(
            "SELECT id, name, kcal, p, c, f, portion, fav, last_used, barcode FROM food ORDER BY name COLLATE NOCASE", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Food(
                        id = c.getLong(0), name = c.getString(1), kcal = c.getFloat(2), p = c.getFloat(3),
                        c = c.getFloat(4), f = c.getFloat(5), portion = c.getFloat(6), fav = c.getInt(7) == 1,
                        lastUsed = c.getLong(8), barcode = if (c.isNull(9)) null else c.getString(9),
                    )
                )
            }
        }
        return out
    }

    fun entries(since: LocalDate): List<Entry> {
        val out = ArrayList<Entry>()
        readableDatabase.rawQuery(
            "SELECT id, day, ts, food_id, name, grams, kcal, p, c, f FROM entry WHERE day >= ? ORDER BY day, ts",
            arrayOf(since.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                val d = runCatching { LocalDate.parse(c.getString(1)) }.getOrNull() ?: continue
                out.add(
                    Entry(
                        id = c.getLong(0), day = d, ts = c.getLong(2), foodId = c.getLong(3), name = c.getString(4),
                        grams = c.getFloat(5), kcal = c.getFloat(6), p = c.getFloat(7), c = c.getFloat(8), f = c.getFloat(9),
                    )
                )
            }
        }
        return out
    }

    private fun lastId(db: SQLiteDatabase): Long {
        var id = -1L
        db.rawQuery("SELECT last_insert_rowid()", null).use { if (it.moveToFirst()) id = it.getLong(0) }
        return id
    }

    fun addFood(name: String, kcal: Float, p: Float, c: Float, f: Float, portion: Float, barcode: String?): Long {
        val db = writableDatabase
        db.execSQL(
            "INSERT INTO food(name, kcal, p, c, f, portion, barcode) VALUES(?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(name, kcal, p, c, f, portion, barcode),
        )
        return lastId(db)
    }

    fun updateFood(id: Long, name: String, kcal: Float, p: Float, c: Float, f: Float, portion: Float) {
        writableDatabase.execSQL(
            "UPDATE food SET name = ?, kcal = ?, p = ?, c = ?, f = ?, portion = ? WHERE id = ?",
            arrayOf<Any>(name, kcal, p, c, f, portion, id),
        )
    }

    fun deleteFood(id: Long) {
        writableDatabase.execSQL("DELETE FROM food WHERE id = ?", arrayOf<Any>(id))
    }

    fun setFav(id: Long, fav: Boolean) {
        writableDatabase.execSQL("UPDATE food SET fav = ? WHERE id = ?", arrayOf<Any>(if (fav) 1 else 0, id))
    }

    fun touch(id: Long, ts: Long) {
        writableDatabase.execSQL("UPDATE food SET last_used = ? WHERE id = ?", arrayOf<Any>(ts, id))
    }

    fun addEntry(day: LocalDate, ts: Long, foodId: Long, name: String, grams: Float, kcal: Float, p: Float, c: Float, f: Float): Long {
        val db = writableDatabase
        db.execSQL(
            "INSERT INTO entry(day, ts, food_id, name, grams, kcal, p, c, f) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(day.toString(), ts, foodId, name, grams, kcal, p, c, f),
        )
        return lastId(db)
    }

    fun updateEntry(id: Long, grams: Float, kcal: Float, p: Float, c: Float, f: Float) {
        writableDatabase.execSQL(
            "UPDATE entry SET grams = ?, kcal = ?, p = ?, c = ?, f = ? WHERE id = ?",
            arrayOf<Any>(grams, kcal, p, c, f, id),
        )
    }

    fun deleteEntry(id: Long) {
        writableDatabase.execSQL("DELETE FROM entry WHERE id = ?", arrayOf<Any>(id))
    }

    companion object {
        @Volatile private var inst: Db? = null
        fun get(ctx: Context): Db = inst ?: synchronized(this) { inst ?: Db(ctx).also { inst = it } }
    }
}
