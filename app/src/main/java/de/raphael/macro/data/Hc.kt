package de.raphael.macro.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId

/** Stand der Verbindung zu Health Connect */
enum class HcState { UNKNOWN, NOT_INSTALLED, NEEDS_UPDATE, NO_PERMISSION, OK, ERROR }

/**
 * Schreibt Mahlzeiten als NutritionRecord (Kalorien, Eiweiß, Kohlenhydrate, Fett) in Health Connect, nur auf dem Gerät.
 * Jeder Eintrag hat eine feste Kennung (macro-ID): Änderungen ersetzen den Datensatz, gelöschte Einträge werden dort gelöscht.
 * Rezept-Einträge gehen mit den Gesamtwerten der gegessenen Portionen hinein (der Name nennt das Rezept).
 * Gemerkt wird, was schon geschrieben ist (id und Prüfwert), damit nichts doppelt entsteht.
 * Nur Schreiben: Macro liest nichts aus Health Connect.
 */
object Hc {
    private const val PROVIDER = "com.google.android.apps.healthdata"
    private const val NAME = "macro"
    val PERMISSIONS: Set<String> = setOf(HealthPermission.getWritePermission(NutritionRecord::class))

    fun state(ctx: Context): HcState {
        return when (HealthConnectClient.getSdkStatus(ctx, PROVIDER)) {
            HealthConnectClient.SDK_UNAVAILABLE -> HcState.NOT_INSTALLED
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HcState.NEEDS_UPDATE
            else -> try {
                val c = HealthConnectClient.getOrCreate(ctx)
                val granted = runBlocking { c.permissionController.getGrantedPermissions() }
                if (granted.containsAll(PERMISSIONS)) HcState.OK else HcState.NO_PERMISSION
            } catch (e: Exception) {
                HcState.ERROR
            }
        }
    }

    private fun cid(id: Long) = "macro-$id"

    private fun hash(e: Entry): Int =
        listOf(e.day, e.ts, e.name, e.kcal, e.p, e.c, e.f).joinToString("|").hashCode()

    private fun record(e: Entry): NutritionRecord {
        val zone = ZoneId.systemDefault()
        val ts = Instant.ofEpochMilli(e.ts)
        // Nachgetragene Einträge (anderer Tag als der Zeitpunkt) bekommen 12 Uhr an ihrem Tag
        val end = if (ts.atZone(zone).toLocalDate() == e.day) ts else e.day.atTime(12, 0).atZone(zone).toInstant()
        val start = end.minusSeconds(60)
        val off = zone.rules.getOffset(end)
        return NutritionRecord(
            startTime = start,
            startZoneOffset = off,
            endTime = end,
            endZoneOffset = off,
            name = e.name,
            energy = Energy.kilocalories(e.kcal.toDouble()),
            protein = Mass.grams(e.p.toDouble()),
            totalCarbohydrate = Mass.grams(e.c.toDouble()),
            totalFat = Mass.grams(e.f.toDouble()),
            metadata = Metadata(
                clientRecordId = cid(e.id),
                clientRecordVersion = System.currentTimeMillis(),
                recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY,
            ),
        )
    }

    private fun readWritten(ctx: Context): MutableMap<Long, Int> {
        val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val m = HashMap<Long, Int>()
        sp.getStringSet("hc_written", emptySet())?.forEach {
            val p = it.split(':')
            val id = p.getOrNull(0)?.toLongOrNull()
            val h = p.getOrNull(1)?.toIntOrNull()
            if (id != null && h != null) m[id] = h
        }
        return m
    }

    private fun saveWritten(ctx: Context, m: Map<Long, Int>) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putStringSet("hc_written", m.map { "${it.key}:${it.value}" }.toSet()).apply()
    }

    /** Gelöschten Eintrag vormerken, damit der Datensatz beim nächsten Abgleich auch in Health Connect verschwindet. */
    fun markDeleted(ctx: Context, id: Long) {
        if (!readWritten(ctx).containsKey(id)) return
        val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val set = HashSet(sp.getStringSet("hc_del", emptySet()) ?: emptySet())
        set.add(id.toString())
        sp.edit().putStringSet("hc_del", set).apply()
    }

    /** Gleicht die Einträge ab: neue und geänderte schreiben, gelöschte entfernen. Gibt false bei einem Fehler zurück. */
    fun sync(ctx: Context, entries: List<Entry>): Boolean {
        return try {
            val c = HealthConnectClient.getOrCreate(ctx)
            val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            val written = readWritten(ctx)
            val pending = (sp.getStringSet("hc_del", emptySet()) ?: emptySet()).mapNotNull { it.toLongOrNull() }
            runBlocking {
                val del = pending.filter { written.containsKey(it) }
                if (del.isNotEmpty()) {
                    c.deleteRecords(NutritionRecord::class, emptyList(), del.map { cid(it) })
                    del.forEach { written.remove(it) }
                    saveWritten(ctx, written)
                }
                sp.edit().remove("hc_del").apply()
                val todo = entries.filter { it.kcal > 0f || it.p > 0f || it.c > 0f || it.f > 0f }.filter { written[it.id] != hash(it) }
                todo.chunked(500).forEach { chunk ->
                    c.insertRecords(chunk.map { record(it) })
                    chunk.forEach { written[it.id] = hash(it) }
                    saveWritten(ctx, written)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
