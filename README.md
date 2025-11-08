# MultiStopRouter

Android Studio project using Kotlin, Jetpack Compose, Google Maps Compose, and the Google Maps Platform APIs to plan routes with a stopover automatically chosen from category searches.

## Setup

1. Create a `local.properties` file (if not already present) and add your Google Maps Platform key:
   ```properties
   MAPS_API_KEY=YOUR_REAL_KEY
   ```
2. Open the project in Android Studio (Giraffe or newer) and let Gradle sync.
3. Run `./gradlew test` to execute the included JVM unit tests.
4. Build a release APK with `./gradlew assembleRelease` (requires configuring a release signing config if distributing externally).

## Features

- Autocomplete inputs for start, stopover query, and destination using the Places SDK for Android.
- Mode switch (walking, driving, bicycling, transit) mapped to the Google Directions API.
- Automatic search for the fastest route Start → best matching stopover → destination using the Directions Web API.
- Google Maps Compose map with markers and polylines for the chosen route.
- Simple in-memory caching for autocomplete, place details, and route responses.
- Location permission handling with fallback messaging.
- Unit tests covering the best-route selection logic.

## Notes

- Replace the placeholder API key with a restricted production key before release. All API usage is centralized through `RoutesRepository` and the Retrofit services.
- The Gradle wrapper jar is not included. Run `gradle wrapper` locally if you need to regenerate it.
