package de.raphael.macro.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** Vorschlag für ein neues Lebensmittel aus einem Barcode (Werte je 100 g bzw. 100 ml). Fehlende Werte sind null. */
data class Draft(
    val name: String,
    val kcal: Float?,
    val p: Float?,
    val c: Float?,
    val f: Float?,
    val portion: Float,
)

sealed interface OffResult {
    data class Found(val draft: Draft) : OffResult
    data object NotFound : OffResult
    data class Error(val msg: String) : OffResult
}

/**
 * Produktsuche über Open Food Facts (openfoodfacts.org). Das ist die einzige Internetnutzung von Macro: Es wird
 * nur die Barcode-Nummer gesendet, keine Konto- oder Gerätedaten. Ergebnis wird nie automatisch eingetragen,
 * der Nutzer prüft die Werte im Formular und speichert sie lokal.
 */
object Off {
    fun lookup(code: String): OffResult {
        if (!code.all { it.isDigit() } || code.length !in 6..14) return OffResult.Error("Ungültiger Barcode")
        return try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=product_name,brands,nutriments,serving_quantity,product_quantity")
            val c = url.openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.setRequestProperty("User-Agent", "Macro/0.6 (Android, private Nutzung)")
            c.setRequestProperty("Accept", "application/json")
            val status = c.responseCode
            if (status == 404) return OffResult.NotFound
            if (status !in 200..299) return OffResult.Error("Server antwortet mit Fehler $status")
            val text = c.inputStream.use { String(it.readBytes(), Charsets.UTF_8) }
            val root = JSONObject(text)
            if (root.optInt("status", 0) != 1) return OffResult.NotFound
            val pr = root.optJSONObject("product") ?: return OffResult.NotFound
            val n = pr.optJSONObject("nutriments") ?: JSONObject()
            fun num(o: JSONObject, k: String): Float? {
                if (!o.has(k) || o.isNull(k)) return null
                val v = o.optString(k).replace(',', '.').toFloatOrNull() ?: return null
                return if (v.isNaN() || v < 0f) null else v
            }
            val kcal = num(n, "energy-kcal_100g") ?: num(n, "energy_100g")?.let { it / 4.184f }
            val name = listOf(pr.optString("brands").split(',').firstOrNull()?.trim().orEmpty(), pr.optString("product_name").trim())
                .filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Produkt $code" }
            val serving = num(pr, "serving_quantity") ?: 0f
            val pack = num(pr, "product_quantity") ?: 0f
            // Portion: angegebene Portion, sonst die Packung, wenn sie klein ist (z. B. Dose, Becher)
            val portion = if (serving > 0f) serving else if (pack in 1f..500f) pack else 0f
            fun r1(v: Float?) = v?.let { (it * 10f).roundToInt() / 10f }
            OffResult.Found(
                Draft(name, kcal?.roundToInt()?.toFloat(), r1(num(n, "proteins_100g")), r1(num(n, "carbohydrates_100g")), r1(num(n, "fat_100g")), portion)
            )
        } catch (e: Exception) {
            OffResult.Error(
                if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException) "Kein Internet oder Server nicht erreichbar"
                else (e.message ?: "Unbekannter Fehler")
            )
        }
    }
}
