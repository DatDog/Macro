package de.raphael.macro.data

import android.content.Context
import android.net.Uri

/** Werte, die Health Local lokal bereitstellt (leer = nicht bekannt). */
data class HlValues(
    val weightKg: Float?,
    val fatPct: Float?,
    val leanKg: Float?,
    val bmr: Float?,
    /** geschätzter Tagesverbrauch, entspricht den Erhaltungskalorien */
    val tdee: Float?,
    /** Datum der Gewichtsmessung, JJJJ-MM-TT */
    val date: String?,
)

enum class HlState { UNKNOWN, OK, EMPTY, NO_APP, DENIED }

class HlResult(val state: HlState, val values: HlValues? = null)

/**
 * Holt Gewicht und Tagesverbrauch von Health Local, direkt auf dem Gerät (ContentProvider, Signatur-Berechtigung).
 * Kein Internet. Health Local ab Version 0.22.
 */
object Bridge {
    private val URI: Uri = Uri.parse("content://com.example.healthlocal.bridge/body")

    fun read(ctx: Context): HlResult {
        return try {
            val c = ctx.contentResolver.query(URI, null, null, null, null) ?: return HlResult(HlState.NO_APP)
            c.use {
                if (!it.moveToFirst()) return HlResult(HlState.EMPTY)
                fun f(name: String): Float? {
                    val i = it.getColumnIndex(name)
                    return if (i < 0 || it.isNull(i)) null else it.getFloat(i)
                }
                val di = it.getColumnIndex("date")
                val date = if (di < 0 || it.isNull(di)) null else it.getString(di)
                val v = HlValues(f("weight_kg"), f("fat_pct"), f("lean_kg"), f("bmr_kcal"), f("tdee_kcal"), date)
                HlResult(if (v.weightKg == null && v.tdee == null) HlState.EMPTY else HlState.OK, v)
            }
        } catch (e: SecurityException) {
            HlResult(HlState.DENIED)
        } catch (e: Exception) {
            HlResult(HlState.NO_APP)
        }
    }
}
