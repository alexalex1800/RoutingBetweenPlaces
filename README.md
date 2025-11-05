# MultiStopRouter

Android Jetpack Compose sample that demonstrates routing with a single intermediate stop using Google Maps, Places Autocomplete, and the Directions API.

## Einrichten

1. Erstelle eine `local.properties` im Projektstamm (falls nicht vorhanden) und ergänze deinen API Key:
   ```properties
   MAPS_API_KEY=DEIN_SICHERER_KEY
   ```
   > **Wichtig:** Der gleiche Key wird für Maps SDK, Places SDK und das Directions Web API genutzt.

2. Öffne das Projekt in Android Studio Ladybug oder neuer. Die Mindest-SDK ist 24, Ziel-SDK 35.

3. Baue das Projekt (`Build > Make Project`) oder starte die App auf einem Gerät mit Google Play-Diensten.

> Hinweis: Der eigentliche `gradle-wrapper.jar` ist nicht eingecheckt. Führe bei Bedarf lokal `gradle wrapper --gradle-version 8.9` aus, um ihn zu erzeugen, bevor du `./gradlew assembleRelease` startest.

## Architektur

- **UI:** Jetpack Compose + Google Maps Compose.
- **MVVM:** `MainViewModel` kapselt Status und koordiniert Suchanfragen.
- **Repositories:**
  - `PlacesRepository` nutzt den Places SDK `PlacesClient` für Autocomplete & Details.
  - `DirectionsRepository` spricht das Google Directions Web API via Retrofit/OkHttp an.
  - `RoutePlanner` kombiniert Kandidaten mit einer kleinen In-Memory-LRU für Ergebnisse.
- **Caching:** `SimpleLruCache` reduziert API-Aufrufe für Autocomplete, Details und Routen.

## Tests

- Unit-Test (`RouteSelectionTest`) stellt sicher, dass die beste Route anhand Dauer (und bei Gleichstand Distanz) gewählt wird.

## Hinweise

- Die App erfragt beim Start einmalig die Standortberechtigung (Fused Location Provider).
- Für reale Projekte sollten API-Keys niemals hartcodiert werden; ersetze die TODO-Stellen durch ein sicheres Secret-Management.
- Release-Builds sind mit aktivem R8/Proguard konfiguriert.
