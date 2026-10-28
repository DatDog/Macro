package de.raphael.macro.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import de.raphael.macro.data.Backup
import de.raphael.macro.data.Focus
import de.raphael.macro.data.Hc
import de.raphael.macro.data.HcState
import de.raphael.macro.data.HlState
import de.raphael.macro.data.Model
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun MoreScreen(model: Model) {
    val view = LocalView.current
    val s = model.settings
    val t = model.targets
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) model.backupTo(uri)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.restoreFrom(uri)
    }
    val hcPermission = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        model.refreshHc()
    }

    val sections: Map<Int, @Composable () -> Unit> = mapOf(
        1 to {
            Enter(1) {
                Column {
                    SectionTitle("Fokus")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Focus.values().forEach { f ->
                            Chip(f.label, selected = s.focus == f) {
                                if (s.focus != f) {
                                    if (s.haptics) view.tickOff()
                                    model.update { it.copy(focus = f) }
                                }
                            }
                        }
                    }
                }
            }
        },
        2 to {
            Enter(2) {
                Column {
                    SectionTitle("Persönliche Werte")
                    Panel(Modifier.fillMaxWidth()) {
                        val h = model.hl
                        val hlWeight = if (s.fromHL) h?.weightKg else null
                        val hlTdee = if (s.fromHL) h?.tdee else null
                        var w by remember { mutableStateOf(if (s.weightKg > 0f) fmtG(s.weightKg) else "") }
                        var m by remember { mutableStateOf(if (s.maintenance > 0) s.maintenance.toString() else "") }
    
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                T("Aus Health Local übernehmen", Ty.Body)
                                T("Gewicht und geschätzter Tagesverbrauch, nur auf dem Gerät", Ty.Small, C.Muted)
                            }
                            Switch(
                                checked = s.fromHL,
                                onCheckedChange = { v ->
                                    model.update { it.copy(fromHL = v) }
                                    if (v) model.refreshHealthLocal()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = C.Accent,
                                    uncheckedThumbColor = C.Muted,
                                    uncheckedTrackColor = C.CardHi,
                                    uncheckedBorderColor = C.Line,
                                ),
                            )
                        }
                        if (s.fromHL) {
                            Spacer(Modifier.height(6.dp))
                            T(
                                when (model.hlState) {
                                    HlState.OK -> "Verbunden" + (h?.date?.let { ", Gewicht vom $it" } ?: "") + "."
                                    HlState.EMPTY -> "Health Local hat noch kein Gewicht. Bitte dort zuerst synchronisieren."
                                    HlState.NO_APP -> "Health Local nicht gefunden oder zu alt (ab Version 0.22 nötig)."
                                    HlState.DENIED -> "Keine Berechtigung. Beide Apps müssen mit demselben Schlüssel signiert sein."
                                    HlState.UNKNOWN -> if (h != null) "Zuletzt geholte Werte." else "Noch nicht abgefragt."
                                },
                                Ty.Small, if (model.hlState == HlState.OK) C.Mint else C.Fat,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
    
                        if (hlWeight != null) {
                            ValueRow("Körpergewicht", "${fmt(hlWeight, 1)} kg", "aus Health Local")
                        } else {
                            Field(
                                "Körpergewicht", w,
                                { w = it; parseNum(it)?.let { v -> if (v in 30f..300f) model.update { st -> st.copy(weightKg = v) } } },
                                Modifier.fillMaxWidth(), suffix = "kg",
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        if (hlTdee != null) {
                            ValueRow("Erhaltungskalorien", "${model.effective.maintenance} kcal", "aus Health Local, geschätzt (±10 %)")
                        } else {
                            Field(
                                "Erhaltungskalorien pro Tag", m,
                                { m = it; parseNum(it)?.let { v -> if (v in 1000f..6000f) model.update { st -> st.copy(maintenance = v.roundToInt()) } } },
                                Modifier.fillMaxWidth(), suffix = "kcal",
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        T(
                            if (s.fromHL && model.hlState == HlState.OK && hlTdee == null)
                                "Health Local kann den Tagesverbrauch noch nicht berechnen (braucht Grundumsatz und mindestens 4 Tage Aktivitätsdaten). Bitte trage die Erhaltungskalorien vorerst von Hand ein."
                            else
                                "Erhaltungskalorien = Kalorien, bei denen dein Gewicht stabil bleibt. Die Schätzung aus Health Local kann um etwa 10 % danebenliegen. Prüfe sie nach 2 bis 3 Wochen am Gewichtstrend.",
                            Ty.Small, C.Muted,
                        )
                    }
                }
            }
        },
        3 to {
            Enter(3) {
                Column {
                    SectionTitle("Ziele für „${s.focus.label}“")
                    Panel(Modifier.fillMaxWidth()) {
                        Stepper(
                            label = "Kalorien zum Erhalt",
                            value = (if (s.pctOf() > 0) "+" else "") + s.pctOf() + " %",
                            onMinus = { model.update { it.copy(pct = it.pct + (it.focus to (it.pctOf() - 5).coerceAtLeast(-30))) } },
                            onPlus = { model.update { it.copy(pct = it.pct + (it.focus to (it.pctOf() + 5).coerceAtMost(25))) } },
                        )
                        Spacer(Modifier.height(14.dp))
                        Stepper(
                            label = "Eiweiß pro kg Körpergewicht",
                            value = fmt(s.protOf(), 1) + " g",
                            onMinus = { model.update { it.copy(prot = it.prot + (it.focus to (((it.protOf() * 10f).roundToInt() - 1) / 10f).coerceAtLeast(1.2f))) } },
                            onPlus = { model.update { it.copy(prot = it.prot + (it.focus to (((it.protOf() * 10f).roundToInt() + 1) / 10f).coerceAtMost(3.0f))) } },
                        )
                        Spacer(Modifier.height(16.dp))
                        if (t != null) {
                            T("RESULTIERENDE TAGESZIELE", Ty.Section, C.Muted)
                            Spacer(Modifier.height(8.dp))
                            T("${t.kcal} kcal", Ty.Title)
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                T("Eiweiß ${t.protein} g", Ty.Body, C.Prot)
                                T("KH ${t.carbs} g", Ty.Body, C.Carb)
                                T("Fett ${t.fat} g", Ty.Body, C.Fat)
                            }
                        } else {
                            T("Trage oben Gewicht und Erhaltungskalorien ein, dann erscheinen hier deine Tagesziele.", Ty.Small, C.Muted)
                        }
                    }
                }
            }
        },
        4 to {
            Enter(4) {
                Column {
                    SectionTitle("Sicherung")
                    Panel(Modifier.fillMaxWidth()) {
                        val last = model.lastBackup
                        T(
                            if (last <= 0L) "Noch keine Sicherung. Sichere deine Mahlzeiten und Lebensmittel regelmäßig, z. B. in dein Drive."
                            else "Letzte Sicherung: " + LocalDateTime.ofInstant(Instant.ofEpochMilli(last), ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                            Ty.Small, if (last <= 0L) C.Fat else C.Muted,
                        )
                        Spacer(Modifier.height(14.dp))
                        BigButton("Sicherung speichern") {
                            if (s.haptics) view.tickOn()
                            createBackup.launch(Backup.fileName())
                        }
                        Spacer(Modifier.height(10.dp))
                        BigButton("Sicherung einlesen", color = C.CardHi) { openBackup.launch(arrayOf("*/*")) }
                        model.backupMsg?.let {
                            Spacer(Modifier.height(10.dp))
                            T(it, Ty.Small, if (it.contains("fehlgeschlagen")) C.Warn else C.Mint)
                        }
                        Spacer(Modifier.height(10.dp))
                        T(
                            "Die Datei enthält alle Lebensmittel, Einträge und Einstellungen und ist nicht verschlüsselt. Einlesen ergänzt nur, Vorhandenes bleibt. Einstellungen kommen nur zurück, wenn hier noch nichts eingetragen ist.",
                            Ty.Label, C.Muted,
                        )
                    }
                }
            }
        },
        5 to {
            Enter(5) {
                Column {
                    SectionTitle("Health Connect")
                    Panel(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                T("Mahlzeiten schreiben", Ty.Body)
                                T("Kalorien und Makros, nur auf dem Gerät", Ty.Small, C.Muted)
                            }
                            Switch(
                                checked = s.toHC,
                                onCheckedChange = { v ->
                                    model.setToHc(v)
                                    if (v && model.hcState == HcState.NO_PERMISSION) hcPermission.launch(Hc.PERMISSIONS)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = C.Accent,
                                    uncheckedThumbColor = C.Muted,
                                    uncheckedTrackColor = C.CardHi,
                                    uncheckedBorderColor = C.Line,
                                ),
                            )
                        }
                        if (s.toHC) {
                            Spacer(Modifier.height(8.dp))
                            T(
                                when (model.hcState) {
                                    HcState.OK -> "Verbunden. Neue und geänderte Mahlzeiten werden geschrieben, gelöschte dort ebenfalls gelöscht."
                                    HcState.NO_PERMISSION -> "Die Berechtigung zum Schreiben von Ernährungsdaten fehlt."
                                    HcState.NOT_INSTALLED -> "Health Connect ist auf diesem Handy nicht verfügbar."
                                    HcState.NEEDS_UPDATE -> "Health Connect muss aktualisiert werden."
                                    HcState.ERROR -> "Health Connect hat nicht geantwortet. Später noch einmal versuchen."
                                    HcState.UNKNOWN -> "Noch nicht abgefragt."
                                },
                                Ty.Small, if (model.hcState == HcState.OK) C.Mint else C.Fat,
                            )
                            if (model.hcState == HcState.NO_PERMISSION) {
                                Spacer(Modifier.height(10.dp))
                                BigButton("Berechtigung erteilen") { hcPermission.launch(Hc.PERMISSIONS) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        T(
                            "Macro schreibt nur und liest nichts aus Health Connect. Rezepte gehen mit den Werten der gegessenen Portionen hinein. Beim Ausschalten bleibt, was schon geschrieben ist, in Health Connect.",
                            Ty.Label, C.Muted,
                        )
                    }
                }
            }
        },
        6 to {
            Enter(6) {
                Column {
                    SectionTitle("Allgemein")
                    Panel(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                T("Barcode online suchen", Ty.Body)
                                T("Open Food Facts, sendet nur die Barcode-Nummer", Ty.Small, C.Muted)
                            }
                            Switch(
                                checked = s.offOnline,
                                onCheckedChange = { v -> model.update { it.copy(offOnline = v) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = C.Accent,
                                    uncheckedThumbColor = C.Muted,
                                    uncheckedTrackColor = C.CardHi,
                                    uncheckedBorderColor = C.Line,
                                ),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            T("Haptik", Ty.Body, modifier = Modifier.weight(1f))
                            Switch(
                                checked = s.haptics,
                                onCheckedChange = { v -> model.update { it.copy(haptics = v) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = C.Accent,
                                    uncheckedThumbColor = C.Muted,
                                    uncheckedTrackColor = C.CardHi,
                                    uncheckedBorderColor = C.Line,
                                ),
                            )
                        }
                    }
                }
            }
        },
        7 to {
            Enter(7) {
                Column {
                    SectionTitle("Hinweise")
                    T(
                        "Macro ist eine Orientierung und keine Ernährungsberatung. Die Startwerte der Lebensmittel sind Richtwerte aus Standardtabellen, bitte gleiche sie mit deiner Packung ab. Alles bleibt auf deinem Handy. Das Internet nutzt Macro nur für die Barcode-Suche bei Open Food Facts (nur die Nummer wird gesendet), sie ist oben abschaltbar. Die Daten dort stammen von Nutzern und können falsch sein.",
                        Ty.Small, C.Muted, Modifier.padding(horizontal = 4.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    T(
                        "Version 0.6 (Stufe 3). Neu: Barcode-Scanner mit Open Food Facts. Zuvor 0.5: Sicherung und Health Connect.",
                        Ty.Small, C.Muted, Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        },
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 640.dp
        if (!wide) {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(20.dp))
                Enter(0) { T("Mehr", Ty.Title) }
                Spacer(Modifier.height(20.dp))
                for (k in 1..7) {
                    sections.getValue(k)()
                    Spacer(Modifier.height(if (k == 7) 32.dp else 24.dp))
                }
            }
        } else {
            Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))
                Enter(0) { T("Mehr", Ty.Title) }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        for (k in listOf(1, 2, 3)) {
                            sections.getValue(k)()
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        for (k in listOf(4, 5, 6, 7)) {
                            sections.getValue(k)()
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val view = LocalView.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T(label, Ty.Small, C.Muted)
            T(value, Ty.Heading)
        }
        StepButton("−") { view.tickOff(); onMinus() }
        Spacer(Modifier.size(8.dp))
        StepButton("+") { view.tickOn(); onPlus() }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .pressable(onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(C.CardHi),
        contentAlignment = Alignment.Center,
    ) { T(label, Ty.Heading) }
}

@Composable
private fun ValueRow(label: String, value: String, note: String) {
    Column(Modifier.fillMaxWidth()) {
        T(label, Ty.Small, C.Muted)
        T(value, Ty.Title.copy(fontSize = 26.sp))
        T(note, Ty.Label, C.Muted)
    }
}
