# Keep Moshi generated adapters
-keep class com.example.multistoprouter.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json(name=*) <fields>;
}
# Keep Google Maps models
-keep class com.google.android.gms.maps.model.* { *; }
