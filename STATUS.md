# Macro: Status

## AKTUELL 2026-10-24: Aufraeumen (Zweig review, am 2026-10-24 nach main gemerged und per Release veroeffentlicht)
- Zweig review (von main): eingebautes Kotlin (AGP 9; Plugin org.jetbrains.kotlin.android, kotlinOptions und die Opt-outs android.builtInKotlin/newDsl=false entfernt), Workflow mit paths-ignore fuer Doku, concurrency pro Zweig, retention-days 3 und Testlauf (assembleRelease test).
- Unit-Tests (JUnit 4, reine JVM): 12 Tests: Prefs.targets (alle Foki, eigene Prozente/Eiweiss, Kohlenhydrate nie negativ), Sum.plus, Food.sumFor. testImplementation junit:junit:4.13.2. Compiler-Warnung in Add.kt (immer wahre Bedingung) behoben.
- CI auf review gruen (Bau 2026-10-24, mit Tests). Nutzer hat die Veroeffentlichung am 2026-10-24 freigegeben (go im Chat). Am Geraet noch nicht getestet.
- Lokal bauen (PC mit Android SDK, JDK 21): C:\Users\rapha\apps\gb.ps1 <App> assembleDebug test. Der Windows-PATH darf keine Leerzeichen enthalten (Tailscale), das Skript setzt einen sauberen PATH.


Stand 2026-10-11 (nach Release v1). Eigene App fuer Makro-Tracking (Kalorien, Eiweiss, Kohlenhydrate, Fett), Paket de.raphael.macro, Design wie Cadence (dunkel, minimalistisch, fluessig). Nur lokal, kein Internet, keine KI.

## Beschluesse (Nutzer)
- Fokus umschaltbar, aktuell Fettverlust + Muskelaufbau (Rekomposition); weitere Profile: Fettverlust, Aufbau, Halten.
- Erfassung: Favoriten/Zuletzt (1), Kochbuch-Rezepte (2), Barcode (3), Auswertung (5). Foto-Erkennung zurueckgestellt (erstmal ohne KI).
- Internet fuer Barcode (Open Food Facts) vom Nutzer erlaubt (2026-10-11), erst in Stufe 3 relevant.

## Stand Stufe 1 (Version 0.1, Release v1 veroeffentlicht 2026-10-11, noch nie am Geraet getestet)
- Tabs Heute / Verlauf / Mehr. Fokus-Schalter auf Heute und unter Mehr.
- Heute: Kalorienring, Makro-Balken (Eiweiss hervorgehoben), Mahlzeit hinzufuegen, Eintraege antippen = Menge aendern oder loeschen.
- Hinzufuegen: Suche, Favoriten (Stern), Zuletzt gegessen, Alle; Menge in Gramm mit Schnellwahl und Portion; Lebensmittel neu/bearbeiten/loeschen (Werte je 100 g, Plausibilitaetshinweis).
- Verlauf: Durchschnitt 7 Tage, Eiweissziel-Tage, 14-Tage-Balken, Tagesliste.
- Mehr: Fokus, Koerpergewicht, Erhaltungskalorien, je Fokus Kalorien-Prozent und Eiweiss g/kg, Tagesziele (Fett 25 % der Kalorien, Rest Kohlenhydrate), Haptik.
- Startwerte: 10 Lebensmittel als Richtwerte (Standardtabellen), Nutzer soll mit Packung abgleichen.
- Technik: Kotlin/Compose, SQLite (food, entry), SharedPreferences, Daten im Speicher mit Hintergrund-Schreiben.

## Offen / naechste Schritte
- Nutzer installiert per Obtainium (https://github.com/DatDog/Macro), traegt unter Mehr Gewicht und Erhaltungskalorien ein, testet. Standardzweig im Repo auf main stellen (nur Nutzer).
- Stufe 1b: Sicherung/Wiederherstellung (Backup.kt-Verfahren aus Health Local), Schreiben der Mahlzeiten als NutritionRecord in Health Connect (Manifest-Berechtigung WRITE_NUTRITION, Rationale-Activity).
- Stufe 2: Kochbuch-Rezepte als Mahlzeit (vorher pruefen, ob Rezepte Naehrwerte pro Portion haben).
- Stufe 3: Barcode-Scan (ML Kit, offline), Produktdaten Open Food Facts (Treffer lokal speichern).
- Health Local: Erhaltungskalorien/Magermasse spaeter automatisch uebernehmen (Schnittstelle wie bei Cadence).
- Release (Push auf main) nur nach ausdruecklicher Freigabe des Nutzers im Chat. Secret KEYSTORE_B64 muss im Repo gesetzt sein (Nutzer).

## Stand 2026-10-11 (Nacht): Macro 0.3 (Release v5) - Stufe 2 Kochbuch-Rezepte
- Hinzufuegen hat Reiter Lebensmittel | Kochbuch (ui/Add.kt: PickStep, RecipeList, PortionsStep). Kochbuch-Reiter: Zuletzt gekocht, alle Rezepte, Suche; Portionen waehlen, Werte pro Portion (geschaetzt) aus dem Kochbuch.
- Vorschlag "Gerade gekocht": Rezept mit cooked_at < 18 h und ohne Eintrag seit dem Kochen; nur Vorschlag, nie automatisch (Nutzerentscheidung).
- data/KochbuchBridge.kt liest content://de.raphael.kochbuchzuhause.bridge/recipes (Spalten id, title, category, servings, kcal, protein, carbs, fat, cooked_at); Berechtigung de.raphael.kochbuchzuhause.permission.BRIDGE (Signatur). Kochbuch ab 0.75 noetig.
- Rezept-Eintraege: food_id = RECIPE_FOOD_ID (-1), grams = Portionen (Entry.isPortions), Werte eingefroren. Keine DB-Migration.
- NICHT am Geraet getestet. Zuerst Kochbuch 0.75, dann Macro 0.3 installieren.
- Offen: Stufe 1b (Sicherung, Health-Connect-Schreiben), Stufe 3 Barcode, Planer-Anzeige im Kochbuch (Nutzerwunsch, braucht Macro-Ziele per Schnittstelle).

## Stand 2026-10-11 (spaet): Macro 0.4 (Release v8) - Portionen gekocht/gegessen/uebrig, eigene Portionen
- Zeile "N Portionen gekocht (vor X) · Y gegessen · Z uebrig" im Vorschlag, bei Zuletzt gekocht und in der Portionsauswahl (ui/Add.kt: portionLine). gegessen = Summe der Portionen-Eintraege gleichen Namens seit cooked_at (Model.eatenOf), gekocht = cooked_servings (eigene Zahl) sonst Rezept-Portionen (Model.cookedOf), uebrig = gekocht - gegessen (leftOf).
- Vorschlag "Gerade gekocht" bleibt bis 3 Tage nach dem Kochen, solange Portionen uebrig sind (vorher 18 h). Danach nur "Zuletzt vor X Tagen gekocht".
- Eigene Portionen (Wraps, Burritos): Kochbuch 0.76 fragt nach "Gekocht" nach der echten Anzahl; der Provider liefert cook_kcal/cook_protein/cook_carbs/cook_fat = Gesamtmenge (Rezept-Portionen x Naehrwerte) / eigene Zahl. Model.perPortion(r) nimmt diese Werte in den 3 Tagen nach dem Kochen, sonst die Standardwerte. addRecipe, Vorschlag und Portionsauswahl nutzen perPortion. Bereits eingetragene Mahlzeiten bleiben unveraendert.
- Neue Provider-Spalten: cooked_servings, cook_kcal, cook_protein, cook_carbs, cook_fat (Kochbuch ab 0.76). Mit Kochbuch 0.75 faellt Macro auf Standardwerte zurueck (Spalten fehlen).
- NICHT am Geraet getestet. Installationsreihenfolge: Health Local 0.22, Kochbuch 0.76, Macro 0.4.

## Stand 2026-10-12: Macro 0.5 (Release v15, veroeffentlicht, Zweig main) - Stufe 1b Sicherung und Health Connect
- Basis war main (0.4 plus Tablet-Layout: Seitenleiste ab 600 dp). Gebaut auf stufe-1b, CI gruen, Freigabe des Nutzers im Chat 2026-10-12, Push stufe-1b nach main. Der erste Push (Commit mit [skip ci]) loeste keinen Release aus; ein leerer Commit "Release 0.5" (e175912) startete den Bau und Release v15. Merke: Der letzte Commit auf main darf kein [skip ci] tragen, sonst gibt es keinen Release.
- Sicherung (data/Backup.kt, Mehr > Sicherung): JSON ueber den Android-Dateidialog (CreateDocument/OpenDocument), enthaelt alle Lebensmittel, alle Eintraege, Einstellungen; unverschluesselt. Einlesen ergaenzt nur (Db.importBackup: gleiche Lebensmittel nach Name und Werten, gleiche Eintraege nach Tag, Zeit, Name, Menge werden uebersprungen; Rezept-Eintraege behalten food_id -1, Eintraege geloeschter Lebensmittel bekommen food_id 0). Einstellungen kommen nur zurueck, wenn Gewicht und Erhalt hier noch leer sind; toHC wird nie aus der Sicherung eingeschaltet. Letzte Sicherung wird in den Prefs gemerkt und unter Mehr angezeigt.
- Health Connect (data/Hc.kt, Mehr > Health Connect): Schalter "Mahlzeiten schreiben" (Standard aus, Settings.toHC). Nur Schreiben, WRITE_NUTRITION. NutritionRecord je Eintrag (energy, protein, totalCarbohydrate, totalFat, name), Zeit: Ende = Zeitpunkt des Eintrags (bei Nachtrag 12 Uhr am Tag), Start = 60 s davor, RECORDING_METHOD_MANUAL_ENTRY, clientRecordId macro-<entry id>, clientRecordVersion = Zeit. Gemerkt wird je Eintrag ein Pruefwert (Prefs hc_written, id:hash), nur neue/geaenderte werden geschrieben; geloeschte Eintraege werden ueber hc_del vorgemerkt und per clientRecordId in Health Connect geloescht. Abgleich im Hintergrund nach jeder Aenderung und in onResume (Model.refreshHc, Model.hcSync). Rezept-Eintraege gehen mit den Gesamtwerten der gegessenen Portionen hinein (Name = Rezept). Abhaengigkeit androidx.health.connect:connect-client:1.1.0-alpha10. Manifest: Berechtigung, queries auf com.google.android.apps.healthdata, RationaleActivity (+ activity-alias ViewPermissionUsageActivity fuer Android 14).
- Beim Ausschalten bleiben schon geschriebene Daten in Health Connect. Eintraege, die aus dem 120-Tage-Fenster fallen, werden dort nicht geloescht.
- NICHT am Geraet getestet (auch 0.1 bis 0.4 nicht). Pruefen: Mehr > Sicherung speichern, dann einlesen; Schalter Health Connect > Berechtigungsdialog > Mahlzeit eintragen > in Health Connect (Ernaehrung) sichtbar; Eintrag aendern und loeschen.
- Offen: Health Local braucht READ_NUTRITION fuer die Ernaehrungs-Ideen (20_APPS_IDEEN); Kochbuch-Planer gegen Macro-Ziel (Provider in Macro); Stufe 3 Barcode.

## Stand 2026-10-12 (Abend): Macro 0.6 (Release v18, veroeffentlicht, Zweig main) - Stufe 3 Barcode
- Hinzufuegen: Knopf "Barcode scannen" (neben "Neu"). Scanner = Google Code Scanner (play-services-code-scanner 16.1.0, eigene Kamera-Oberflaeche der Play-Dienste, keine Kamera-Berechtigung noetig), Formate EAN-13/EAN-8/UPC-A/UPC-E.
- Ablauf (ui/Add.kt ScanStep): Barcode schon in food.barcode gespeichert -> direkt zur Menge. Sonst Open Food Facts (data/Off.kt, https://world.openfoodfacts.org/api/v2/product/CODE.json, nur die Nummer wird gesendet, Timeout 8 s) -> Formular (FormStep) vorbefuellt mit Name (Marke + Produkt), kcal, Eiweiss, KH, Fett je 100 g/ml, Portion (serving_quantity, sonst Packung bis 500 g/ml). Nichts wird automatisch eingetragen: Nutzer prueft und speichert, dann wird der Barcode mit dem Lebensmittel gespeichert (naechster Scan findet es sofort). Nicht gefunden/Fehler: Meldung + "Von Hand anlegen" (Barcode wird trotzdem gespeichert).
- Getraenke: Werte je 100 ml werden wie je 100 g behandelt, Menge in ml = g eingeben (1 ml ~ 1 g, bei Softdrinks ca. 4 % Abweichung). Kein eigenes Getraenke-Budget: Tagesziel kommt aus Erhaltung, Fokus, Eiweiss pro kg; Getraenke zaehlen nur, wenn eingetragen.
- Mehr > Allgemein: Schalter "Barcode online suchen" (Settings.offOnline, Standard an, auch in der Sicherung). Manifest: INTERNET-Berechtigung (einzige Internetnutzung der App, vom Nutzer erlaubt).
- CI gruen (Lauf 38269824123). NICHT am Geraet getestet. Freigabe des Nutzers im Chat 2026-10-12, leerer Commit "Release 0.6" (2e8b27a) nach main, Release v18. Fuer den Fast-Forward-Push vorher git fetch --depth=100 origin main barcode (flache Klone).
- Offen: ml als eigene Einheit fuer Getraenke (Flag am Lebensmittel), Doku in 20_APPS_IDEEN/43_STATUS_Macro erst nach Release nachziehen.
