package de.raphael.macro.data

import kotlin.math.roundToInt

/** Anteil von [value] am Ziel [target] (0 ohne Ziel), bis 150 % für die Anzeige gedeckelt. */
fun shareOf(value: Float, target: Int): Float = if (target <= 0) 0f else (value / target).coerceIn(0f, 1.5f)

/** Kurzer Satz zur Kalorienbilanz des Tages für die Widgets. */
fun kcalLeftText(eaten: Float, target: Int?): String {
    if (target == null || target <= 0) return "${eaten.roundToInt()} kcal gegessen"
    val diff = target - eaten.roundToInt()
    return when {
        diff > 0 -> "noch $diff kcal"
        diff >= -50 -> "Ziel erreicht"
        else -> "${-diff} kcal drüber"
    }
}

/** Hinweis auf das Eiweiß, das noch fehlt (erst ab 10 g, sonst nichts). */
fun proteinGapText(eaten: Sum, t: Targets?): String? {
    if (t == null) return null
    val gap = t.protein - eaten.p
    return if (gap >= 10f) "Eiweiß: noch ${gap.roundToInt()} g" else null
}
