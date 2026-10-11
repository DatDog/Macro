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
