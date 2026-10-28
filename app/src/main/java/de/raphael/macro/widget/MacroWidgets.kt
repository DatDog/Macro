package de.raphael.macro.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.raphael.macro.MainActivity
import de.raphael.macro.R
import de.raphael.macro.data.Db
import de.raphael.macro.data.Prefs
import de.raphael.macro.data.Sum
import de.raphael.macro.data.Targets
import de.raphael.macro.data.kcalLeftText
import de.raphael.macro.data.proteinGapText
import de.raphael.macro.data.shareOf
import de.raphael.macro.data.sum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Farben wie in der App (dunkel, Mint für Kalorien und Eiweiß, Violett für Kohlenhydrate, Orange für Fett). */
private object MC {
    val ink = ColorProvider(day = Color(0xFFF3F3F7), night = Color(0xFFF3F3F7))
    val soft = ColorProvider(day = Color(0xFF9A9AB0), night = Color(0xFF9A9AB0))
    const val MINT = 0xFF4DE3C1.toInt()
    const val WARN = 0xFFFF5A6E.toInt()
    const val CARB = 0xFF8E7CFF.toInt()
    const val FAT = 0xFFFFB86B.toInt()
    const val TRACK = 0xFF2A2A38.toInt()
}

private val VER = longPreferencesKey("ver")

@Composable
private fun <T> rememberFresh(load: (Context) -> T): T? {
    val ctx = LocalContext.current
    val ver = currentState<Preferences>()[VER]
    return remember(ver) { runCatching { load(ctx) }.getOrNull() }
}

private suspend fun GlanceAppWidget.reload(context: Context) {
    GlanceAppWidgetManager(context).getGlanceIds(this.javaClass).forEach { id ->
        updateAppWidgetState(context, id) { it[VER] = System.nanoTime() }
        update(context, id)
    }
}

/** Widgets von außen auffrischen (nach neuen oder geänderten Einträgen und geänderten Zielen). */
object MacroWidgets {
    fun refresh(ctx: Context) {
        val app = ctx.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            listOf(TagWidget(), KcalWidget()).forEach { runCatching { it.reload(app) } }
        }
    }
}

private data class DayData(val eaten: Sum, val t: Targets?)

/** Tagessumme und Ziele aus der Datenbank und den gespeicherten Werten (Health-Local-Werte stehen dort schon drin). */
private fun loadDay(ctx: Context): DayData {
    val s = Prefs.load(ctx)
    val hl = Prefs.loadHl(ctx)
    val eff = if (s.fromHL && hl != null) {
        s.copy(
            weightKg = hl.weightKg ?: s.weightKg,
            maintenance = hl.tdee?.let { (it / 10f).roundToInt() * 10 } ?: s.maintenance,
        )
    } else s
    val today = LocalDate.now()
    val eaten = Db.get(ctx).entries(today).filter { it.day == today }.fold(Sum()) { a, e -> a + e.sum() }
    return DayData(eaten, Prefs.targets(eff))
}

private fun openApp(ctx: Context) = actionStartActivity(
    Intent(ctx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
)

/** Ring für den Anteil [share] (0 bis 1,5); über dem Ziel färbt er sich rot. */
private fun ringBitmap(share: Float, color: Int, size: Int = 320): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = size * 0.11f; strokeCap = Paint.Cap.ROUND }
    val pad = p.strokeWidth / 2f + 2f
    val r = RectF(pad, pad, size - pad, size - pad)
    p.color = MC.TRACK
    c.drawArc(r, 0f, 360f, false, p)
    if (share > 0.005f) {
        p.color = if (share > 1.05f) MC.WARN else color
        c.drawArc(r, -90f, 360f * share.coerceAtMost(1f), false, p)
    }
    return bmp
}

/** Balken für den Anteil [share], links gerundet. */
private fun barBitmap(share: Float, color: Int, w: Int = 400, h: Int = 24): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = MC.TRACK
    c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), h / 2f, h / 2f, p)
    val s = share.coerceIn(0f, 1f)
    if (s > 0.01f) {
        p.color = color
        c.drawRoundRect(RectF(0f, 0f, maxOf(h.toFloat(), w * s), h.toFloat()), h / 2f, h / 2f, p)
    }
    return bmp
}

@Composable
private fun Card(modifier: GlanceModifier = GlanceModifier, content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg_night)).cornerRadius(24.dp)
    ) { content() }
}

// ───────────────────────────── Tag: Ring und Makros ─────────────────────────────

class TagWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { TagContent(rememberFresh(::loadDay)) }
    }
}

class TagWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TagWidget()
}

@Composable
private fun MacroRow(label: String, value: Float, target: Int, color: Int) {
    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = MC.soft, fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Text(
                if (target > 0) "${value.roundToInt()} / $target g" else "${value.roundToInt()} g",
                style = TextStyle(color = MC.ink, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        Image(ImageProvider(barBitmap(shareOf(value, target), color)), null, GlanceModifier.fillMaxWidth().height(6.dp), contentScale = ContentScale.FillBounds)
    }
}

@Composable
private fun TagContent(d: DayData?) {
    val ctx = LocalContext.current
    val data = d ?: DayData(Sum(), null)
    val t = data.t
    val share = shareOf(data.eaten.kcal, t?.kcal ?: 0)
    Card(GlanceModifier.clickable(openApp(ctx))) {
        Row(GlanceModifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.size(88.dp), contentAlignment = Alignment.Center) {
                Image(ImageProvider(ringBitmap(share, MC.MINT)), null, GlanceModifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        data.eaten.kcal.roundToInt().toString(),
                        style = TextStyle(color = MC.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(if (t != null) "von ${t.kcal}" else "kcal", style = TextStyle(color = MC.soft, fontSize = 10.sp))
                }
            }
            Spacer(GlanceModifier.width(14.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(kcalLeftText(data.eaten.kcal, t?.kcal), maxLines = 1, style = TextStyle(color = MC.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(6.dp))
                MacroRow("Eiweiß", data.eaten.p, t?.protein ?: 0, MC.MINT)
                Spacer(GlanceModifier.height(5.dp))
                MacroRow("Kohlenhydrate", data.eaten.c, t?.carbs ?: 0, MC.CARB)
                Spacer(GlanceModifier.height(5.dp))
                MacroRow("Fett", data.eaten.f, t?.fat ?: 0, MC.FAT)
            }
        }
    }
}

// ───────────────────────────── Klein: Kalorien und Eiweiß ─────────────────────────────

class KcalWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { KcalContent(rememberFresh(::loadDay)) }
    }
}

class KcalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KcalWidget()
}

@Composable
private fun KcalContent(d: DayData?) {
    val ctx = LocalContext.current
    val data = d ?: DayData(Sum(), null)
    val t = data.t
    val share = shareOf(data.eaten.kcal, t?.kcal ?: 0)
    val left = if (t != null) (t.kcal - data.eaten.kcal.roundToInt()) else null
    Card(GlanceModifier.clickable(openApp(ctx))) {
        Column(GlanceModifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.size(84.dp), contentAlignment = Alignment.Center) {
                Image(ImageProvider(ringBitmap(share, MC.MINT)), null, GlanceModifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (left != null) (if (left >= 0) left.toString() else "+${-left}") else data.eaten.kcal.roundToInt().toString(),
                        style = TextStyle(color = MC.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(if (left != null) (if (left >= 0) "übrig" else "drüber") else "kcal", style = TextStyle(color = MC.soft, fontSize = 10.sp))
                }
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(
                proteinGapText(data.eaten, t) ?: if (t != null) "Eiweiß ✓" else "Macro",
                maxLines = 1,
                style = TextStyle(color = MC.soft, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}
