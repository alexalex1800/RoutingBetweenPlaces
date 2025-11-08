# MultiStopRouter

Android Studio project using Kotlin, Jetpack Compose, and OpenStreetMap-backed services to plan routes with a dynamically chosen stopover.

## Setup

1. Open the project in Android Studio (Giraffe or newer) and let Gradle sync.
2. (Optional) Regenerate the Gradle wrapper locally with `gradle wrapper` if you need the wrapper JAR for command-line builds.
3. (Optional) Add `MAP_STYLE_URL=<your style url>` to `local.properties` to change the MapLibre style (defaults to the public demo tiles).
4. Run `./gradlew test` to execute the JVM unit tests.
5. Build a release APK with `./gradlew assembleRelease` (configure signing for distribution).

## Features

- Autocomplete inputs for start, stopover category, and destination powered by the Photon (OpenStreetMap) API.
- Mode switch (Fuß, Auto, Fahrrad) mapped to the public OSRM routing profiles; ÖPNV reports a friendly unsupported message.
- Automatic search for the fastest route Start → best matching stopover → Ziel using OSRM directions for each candidate.
- MapLibre-based map with OpenStreetMap tiles, markers for the selected points, and a polyline of the fastest route.
- Simple in-memory caching for autocomplete suggestions and route responses to reduce network load.
- Location permission handling with graceful fallbacks when denied.
- Unit tests covering the best-route selection logic.

## Notes

- The app uses public demo endpoints (Photon, OSRM) intended for experimentation. For production deployments host your own stack or obtain appropriate API access.
- Transit routing is not supported by OSRM; selecting ÖPNV informs users accordingly.
- The Gradle wrapper JAR is omitted. Generate it locally if required.
