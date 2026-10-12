package de.raphael.macro.data

import android.content.Context
import kotlin.math.roundToInt

/**
 * Fokus der App. Er bestimmt die Voreinstellung für das Kalorienziel (Prozent von den Erhaltungskalorien)
 * und das Eiweißziel (Gramm je kg Körpergewicht). Beides ist unter Mehr je Fokus änderbar.
 * Orientierung, keine Ernährungsberatung.
 */
enum class Focus(val id: String, val label: String, val chip: String, val pct: Int, val prot: Float) {
    RECOMP("recomp", "Fettverlust + Muskelaufbau", "Fett + Muskel", -10, 2.0f),
    CUT("cut", "Fettverlust", "Fettverlust", -20, 2.2f),
    BULK("bulk", "Muskelaufbau", "Aufbau", 10, 1.8f),
    MAINTAIN("keep", "Gewicht halten", "Halten", 0, 1.6f),
}

/** Einstellungen (alles unter Mehr, nur der Fokus-Schalter ist auch auf Heute). */
data class Settings(
    val focus: Focus = Focus.RECOMP,
    /** Körpergewicht in kg, 0 = noch nicht eingetragen */
    val weightKg: Float = 0f,
    /** Erhaltungskalorien pro Tag, 0 = noch nicht eingetragen */
    val maintenance: Int = 0,
    /** Abweichung der Kalorien vom Erhalt in Prozent, je Fokus */
    val pct: Map<Focus, Int> = Focus.values().associateWith { it.pct },
    /** Eiweiß in g je kg, je Fokus */
    val prot: Map<Focus, Float> = Focus.values().associateWith { it.prot },
    val haptics: Boolean = true,
    /** Gewicht und Erhaltungskalorien aus Health Local übernehmen (wenn verfügbar) */
    val fromHL: Boolean = true,
    /** Mahlzeiten in Health Connect schreiben (Standard aus, braucht die Berechtigung) */
    val toHC: Boolean = false,
    /** Beim Barcode-Scan Produktdaten bei Open Food Facts suchen (einzige Internetnutzung, nur die Nummer wird gesendet) */
    val offOnline: Boolean = true,
) {
    fun pctOf(f: Focus = focus): Int = pct[f] ?: f.pct
    fun protOf(f: Focus = focus): Float = prot[f] ?: f.prot
}

data class Targets(val kcal: Int, val protein: Int, val carbs: Int, val fat: Int)

object Prefs {
    private const val NAME = "macro"

    /** Fett: 25 % der Kalorien, Kohlenhydrate: der Rest. Null, solange Gewicht oder Erhalt fehlen. */
    fun targets(s: Settings, f: Focus = s.focus): Targets? {
        if (s.weightKg <= 0f || s.maintenance <= 0) return null
        val kcal = (s.maintenance * (1f + s.pctOf(f) / 100f)).roundToInt()
        val protein = (s.weightKg * s.protOf(f)).roundToInt()
        val fat = (kcal * 0.25f / 9f).roundToInt()
        val carbs = ((kcal - protein * 4f - fat * 9f) / 4f).roundToInt().coerceAtLeast(0)
        return Targets(kcal, protein, carbs, fat)
    }

    fun load(ctx: Context): Settings {
        val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val d = Settings()
        val focus = Focus.values().firstOrNull { it.id == sp.getString("focus", d.focus.id) } ?: d.focus
        return Settings(
            focus = focus,
            weightKg = sp.getFloat("weightKg", d.weightKg),
            maintenance = sp.getInt("maintenance", d.maintenance),
            pct = Focus.values().associateWith { sp.getInt("pct_${it.id}", it.pct) },
            prot = Focus.values().associateWith { sp.getFloat("prot_${it.id}", it.prot) },
            haptics = sp.getBoolean("haptics", d.haptics),
            fromHL = sp.getBoolean("fromHL", d.fromHL),
            toHC = sp.getBoolean("toHC", d.toHC),
            offOnline = sp.getBoolean("offOnline", d.offOnline),
        )
    }

    /** Zuletzt von Health Local geholte Werte (damit die Ziele auch ohne Verbindung stehen). */
    fun loadHl(ctx: Context): HlValues? {
        val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        if (!sp.getBoolean("hl_has", false)) return null
        fun f(k: String): Float? = if (sp.contains(k)) sp.getFloat(k, 0f) else null
        return HlValues(f("hl_kg"), f("hl_fat"), f("hl_lean"), f("hl_bmr"), f("hl_tdee"), sp.getString("hl_date", null))
    }

    fun saveHl(ctx: Context, v: HlValues) {
        val e = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putBoolean("hl_has", true)
        fun put(k: String, x: Float?) { if (x != null) e.putFloat(k, x) else e.remove(k) }
        put("hl_kg", v.weightKg); put("hl_fat", v.fatPct); put("hl_lean", v.leanKg); put("hl_bmr", v.bmr); put("hl_tdee", v.tdee)
        if (v.date != null) e.putString("hl_date", v.date) else e.remove("hl_date")
        e.apply()
    }

    /** Zeitpunkt der letzten Sicherung (ms), 0 = noch nie */
    fun lastBackup(ctx: Context): Long = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getLong("lastBackup", 0L)

    fun saveLastBackup(ctx: Context, ts: Long) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putLong("lastBackup", ts).apply()
    }

    fun save(ctx: Context, s: Settings) {
        val e = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("focus", s.focus.id)
            .putFloat("weightKg", s.weightKg)
            .putInt("maintenance", s.maintenance)
            .putBoolean("haptics", s.haptics)
            .putBoolean("fromHL", s.fromHL)
            .putBoolean("toHC", s.toHC)
            .putBoolean("offOnline", s.offOnline)
        Focus.values().forEach {
            e.putInt("pct_${it.id}", s.pctOf(it))
            e.putFloat("prot_${it.id}", s.protOf(it))
        }
        e.apply()
    }
}
