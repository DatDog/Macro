# Macro: Status

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
