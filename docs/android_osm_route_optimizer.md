# Technisches Konzept: Android Routen-Optimierer

## 1. Projektziel

Entwicklung einer nativen Android-App, die eine optimierte Route von A nach C berechnet, wobei ein Zwischenstopp B aus einer Kategorie (z. B. "Supermarkt", "Lidl", "McFit") ausgewählt wird. Die App soll automatisch den besten Standort der Kategorie B finden, der die Gesamtstrecke oder Gesamtfahrzeit minimiert.

## 2. Technologie-Stack (Vorschlag)

- **App:** Android Native (Kotlin mit Jetpack Compose für moderne UI).
- **Karten- & Ortsdaten:** OpenStreetMap (OSM).
- **Geocoding & Suche (Autocomplete):**
  - API: Nominatim (für Adresssuche und Autocomplete) oder Photon (spezialisiert auf OSM-Suche).
  - Zweck: Umwandlung von Adressen (Feld 1 & 3) in Koordinaten und Bereitstellung von Vorschlägen.
- **Kategorie-Suche (Feld 2):**
  - API: Overpass API
  - Zweck: Finden aller Instanzen einer Kategorie (z. B. alle `shop=supermarket` oder `name~"Lidl"`) in einem bestimmten geografischen Bereich.
- **Routing-API (Routenberechnung):**
  - API: OSRM (Open Source Routing Machine) oder GraphHopper.
  - Zweck: Berechnung der schnellsten/kürzesten Route für die gewählten Transportmittel (Auto, Fahrrad, zu Fuß).

## 3. Benutzeroberfläche (UI) – Jetpack Compose

Die App besteht aus einem Hauptbildschirm:

- **Startpunkt (Feld 1):**
  - TextField (Texteingabefeld) für "Start".
  - Dropdown-Liste mit Autocomplete-Vorschlägen (von Nominatim/Photon).
  - Button für "Aktuellen Standort verwenden" (erfordert GPS-Berechtigung).
- **Zwischenstopp-Kategorie (Feld 2):**
  - TextField für "Suche Kategorie..." (z. B. "Supermarkt", "Lidl", "U-Bahn Hof", "McFit").
  - Autocomplete-Vorschläge basierend auf OSM-Tags (z. B. amenity, shop, name).
- **Endpunkt (Feld 3):**
  - TextField für "Ziel".
  - Dropdown-Liste mit Autocomplete-Vorschlägen (von Nominatim/Photon).
- **Transportmittel:**
  - ToggleButtons oder `Row` mit RadioButtons für:
    - Auto (driving)
    - Fahrrad (bicycle)
    - Zu Fuß (walking)
    - ÖPNV (transit) – siehe Hinweis unten.
- **Aktions-Button:**
  - Button ("Optimale Route finden").
- **Ergebnis-Anzeige:**
  - Eine Kartenansicht (z. B. Osmdroid) zur Anzeige der finalen Route.
  - Text-Zusammenfassung (z. B. "Gesamtdauer: 45 Min", "Zwischenstopp bei: Lidl Musterstraße 123").

## 4. Kernlogik: Der Optimierungs-Flow

Dies ist der komplexeste Teil und erfordert serverseitige Logik oder viele API-Aufrufe von der App aus.

1. **Ereignis:** Klick auf "Optimale Route finden".
2. **Validierung:** Sind alle drei Felder ausgefüllt?
3. **Geocoding (A & C):**
   - Wandle die Eingaben aus Feld 1 (Start A) und Feld 3 (Ziel C) in GPS-Koordinaten um (falls nicht schon geschehen).
   - `A_coord = (lat, lon)`, `C_coord = (lat, lon)`.
4. **Kategorie-Suche (B-Kategorie):**
   - Bounding Box definieren: Berechne einen sinnvollen Suchbereich (z. B. ein Rechteck oder einen Radius um den Mittelpunkt von A und C), um die Suche nach "Lidl" nicht weltweit durchzuführen.
   - Overpass API Abfrage: Sende eine Abfrage an die Overpass API, um alle Orte zu finden, die der Kategorie B (z. B. `shop=supermarket` UND `name~"Lidl"`) innerhalb der Bounding Box entsprechen.
   - Ergebnis: Eine Liste von potenziellen Zwischenstopps: `[B1, B2, B3, ..., Bn]`.
5. **Die Optimierungs-Schleife (Das Herzstück):**
   - Erstelle eine leere Liste für die Ergebnisse: `ergebnisListe = []`.
   - Für jeden Stopp `Bx` in der Liste `[B1, B2, ..., Bn]`:
     1. API-Aufruf 1: Berechne die Route von `A_coord` nach `Bx` mit der OSRM/GraphHopper API (für das gewählte Transportmittel).
        - Speichere `zeit_A_nach_Bx` und `distanz_A_nach_Bx`.
     2. API-Aufruf 2: Berechne die Route von `Bx` nach `C_coord` mit der OSRM/GraphHopper API.
        - Speichere `zeit_Bx_nach_C` und `distanz_Bx_nach_C`.
     3. Gesamt berechnen:
        - `gesamtZeit = zeit_A_nach_Bx + zeit_Bx_nach_C`
        - `gesamtDistanz = distanz_A_nach_Bx + distanz_Bx_nach_C`
     4. Speichern: Füge das Ergebnis der `ergebnisListe` hinzu: `{ stop: Bx, totalTime: gesamtZeit, totalDistance: gesamtDistanz, route: [route_A_Bx, route_Bx_C] }`.
6. **Ergebnis finden:**
   - Sortiere die `ergebnisListe` nach `totalTime` (oder `totalDistance`, je nach Priorität "schnellste" vs. "kürzeste").
   - `optimaleRoute = ergebnisListe[0]` (das erste Element nach der Sortierung).
7. **Anzeige:**
   - Zeichne `optimaleRoute.route` (bestehend aus zwei Teilen) auf der Osmdroid-Karte ein.
   - Zeige den Namen/Adresse des optimalen Stopps (`optimaleRoute.stop`) an.

## 5. Wichtige Hinweise & Herausforderungen

- **API-Keys & Rate Limits:** Alle diese APIs (Nominatim, Overpass, OSRM) sind oft kostenlos, haben aber strenge Nutzungsbeschränkungen ("Rate Limits"). Bei einer App mit vielen Nutzern muss man ggf. für die APIs bezahlen oder eigene Server (z. B. mit OSRM) hosten.
- **Performance:** Die Optimierungs-Schleife (Schritt 4) kann langsam sein, wenn die Kategorie "Supermarkt" 50 Treffer liefert (das wären 100 API-Aufrufe für die Routenberechnung).
- **ÖPNV (Öffentliche Verkehrsmittel):** Dies ist extrem komplex. OSRM und GraphHopper unterstützen dies oft nicht (oder nur eingeschränkt). Hierfür benötigt man spezielle APIs (z. B. von Google Maps, oder nationale Anbieter wie die Deutsche Bahn), die oft teuer und kompliziert zu integrieren sind. Es wird empfohlen, ÖPNV in der ersten Version wegzulassen.
- **Android-Berechtigungen:** Die App benötigt mindestens `INTERNET` und `ACCESS_FINE_LOCATION` (für "Aktueller Standort").
