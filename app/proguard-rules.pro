# Add project specific ProGuard rules here.
# Keep data models for Gson serialization
-keepclassmembers class com.groupcalendar.app.data.model.** { *; }
-keep class com.groupcalendar.app.data.model.** { *; }

# Retrofit rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# WorkManager
-keep class androidx.work.** { *; }
