package de.raphael.macro.data

import android.content.Context
import android.net.Uri

/** Rezept aus dem Kochbuch. Nährwerte pro Portion (geschätzt), [cookedAt] = letztes "Gekocht" in ms, 0 = nie. */
data class KbRecipe(
    val id: String,
    val title: String,
    val category: String,
    val servings: Int,
    val kcal: Float,
    val p: Float,
    val c: Float,
    val f: Float,
    val cookedAt: Long,
    /** Portionen, die beim letzten Kochen gewählt waren */
    val cookedServings: Int,
)

enum class KbState { UNKNOWN, OK, EMPTY, NO_APP, DENIED }

class KbResult(val state: KbState, val recipes: List<KbRecipe> = emptyList())

/**
 * Holt die Rezepte mit Nährwerten aus dem Kochbuch, direkt auf dem Gerät (ContentProvider, Signatur-Berechtigung).
 * Kein Internet. Das Kochbuch trägt nichts selbst in Macro ein.
 */
object KochbuchBridge {
    private val URI: Uri = Uri.parse("content://de.raphael.kochbuchzuhause.bridge/recipes")

    fun read(ctx: Context): KbResult {
        return try {
            val c = ctx.contentResolver.query(URI, null, null, null, null) ?: return KbResult(KbState.NO_APP)
            c.use {
                val out = ArrayList<KbRecipe>()
                val iId = it.getColumnIndex("id"); val iTitle = it.getColumnIndex("title")
                val iCat = it.getColumnIndex("category"); val iSrv = it.getColumnIndex("servings")
                val iK = it.getColumnIndex("kcal"); val iP = it.getColumnIndex("protein")
                val iC = it.getColumnIndex("carbs"); val iF = it.getColumnIndex("fat")
                val iCo = it.getColumnIndex("cooked_at"); val iCs = it.getColumnIndex("cooked_servings")
                if (iId < 0 || iTitle < 0 || iK < 0) return KbResult(KbState.NO_APP)
                while (it.moveToNext()) {
                    out.add(
                        KbRecipe(
                            id = it.getString(iId), title = it.getString(iTitle),
                            category = if (iCat >= 0) it.getString(iCat) ?: "" else "",
                            servings = if (iSrv >= 0) it.getInt(iSrv) else 1,
                            kcal = it.getFloat(iK), p = if (iP >= 0) it.getFloat(iP) else 0f,
                            c = if (iC >= 0) it.getFloat(iC) else 0f, f = if (iF >= 0) it.getFloat(iF) else 0f,
                            cookedAt = if (iCo >= 0) it.getLong(iCo) else 0L,
                            cookedServings = if (iCs >= 0) it.getInt(iCs) else 0,
                        )
                    )
                }
                KbResult(if (out.isEmpty()) KbState.EMPTY else KbState.OK, out)
            }
        } catch (e: SecurityException) {
            KbResult(KbState.DENIED)
        } catch (e: Exception) {
            KbResult(KbState.NO_APP)
        }
    }
}
