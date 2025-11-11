# MultiStopRouter

Android Studio project using Kotlin, Jetpack Compose, and open OpenStreetMap services to plan an optimized route A → B → C where B is automatically chosen from a free-text category query.

## Setup

1. Open the project in Android Studio (Giraffe or newer) and let Gradle sync.
2. Provide the optional `gradle/wrapper/gradle-wrapper.jar` by running `gradle wrapper` locally if Android Studio does not download it automatically. (Network access is required the first time.)
3. Run `./gradlew test` or `./gradlew lint` once the wrapper is available to verify the project builds.

## Features

- Autocomplete inputs for start, stopover category, and destination backed by the open Photon search API.
- Category matching via the Overpass API with automatic bounding box around the midpoint of start and destination.
- Optimized routing that evaluates each candidate stopover using the public OSRM routing service and selects the fastest result.
- Jetpack Compose UI with OSMDroid map embedding, markers for start/stopover/destination, and the decoded OSRM polyline.
- Travel mode selector supporting driving, cycling, and walking profiles.
- Location permission handling and fused location integration for quickly setting the start point.
- Simple in-memory caching to reduce repeated Photon, Overpass, and OSRM calls.

## Notes

- All network access is anonymous and does not require API keys, but the public OSM services impose usage limits. Consider self-hosting if you need higher throughput.
- The Gradle wrapper JAR is excluded from version control. Running `gradle wrapper` locally (or letting Android Studio download the Gradle distribution) restores it when needed.
